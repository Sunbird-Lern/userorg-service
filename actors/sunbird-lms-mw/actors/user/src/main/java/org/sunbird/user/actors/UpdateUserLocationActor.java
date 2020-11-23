package org.sunbird.user.actors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import scala.concurrent.Await;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"updateUserLocation"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class UpdateUserLocationActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if ("updateUserLocation".equalsIgnoreCase(operation)) {
      updateUserLocation(request);
    } else {
      onReceiveUnsupportedOperation("UpdateUserLocationActor");
    }
  }

  private void updateUserLocation(Request request) {
    RequestContext context = request.getRequestContext();
    Map<String, Object> reqMap = request.getRequest();
    List<Map<String, Object>> userRespList = new ArrayList<>();
    boolean dryRun = true;
    if (null != reqMap.get("dryRun")) {
      dryRun = (boolean) reqMap.get("dryRun");
    }

    List<String> userIds = (List<String>) reqMap.get(JsonKey.USERIDS);
    if (userIds.size() > 100) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorMaxSizeExceeded,
          MessageFormat.format(
              ResponseCode.errorMaxSizeExceeded.getErrorMessage(), JsonKey.USERIDS, 100));
    }
    boolean finalDryRun = dryRun;
    // Iterate requested userids and get user location ids and organisation list
    userIds
        .stream()
        .forEach(
            userId -> {
              Map<String, Object> usr = null;
              Future<Map<String, Object>> resultF =
                esUtil.getDataByIdentifier(
                  ProjectUtil.EsType.user.getTypeName(), userId, context);
              try {
                Object object = Await.result(resultF, ElasticSearchHelper.timeout.duration());
                if (object != null) {
                  usr = (Map<String, Object>) object;
                }
              } catch (Exception e) {
                logger.error(
                  context,
                  String.format(
                    "%s:%s:User not found with provided id == %s and error %s",
                    this.getClass().getSimpleName(), "updateUserLocation",userId, e.getMessage()),
                  e);
              }


              if (MapUtils.isNotEmpty(usr) && usr.size() > 0) {
                List<String> userLocationIds = (List<String>) usr.get(JsonKey.LOCATION_IDS);
                List<Map<String,Object>> userOrganisations = (List<Map<String, Object>>) usr.get(JsonKey.ORGANISATIONS);
                if (CollectionUtils.isNotEmpty(userOrganisations)
                    && userOrganisations.size() > 0) {

                  if (CollectionUtils.isEmpty(userLocationIds)) {
                    userLocationIds = new ArrayList<>(1);
                  }
                  // Get all org id list
                  List<String> orgidList = new ArrayList<>();
                  userOrganisations
                      .stream()
                      .forEach(
                          orgMap -> {
                            orgidList.add((String) orgMap.get(JsonKey.ORGANISATION_ID));
                          });

                  // Get location ids of all org
                  if (CollectionUtils.isNotEmpty(orgidList)) {
                    List<String> fields = new ArrayList<>();
                    fields.add(JsonKey.IS_ROOT_ORG);
                    fields.add(JsonKey.LOCATION_IDS);
                    fields.add(JsonKey.ID);
                    Util.DbInfo orgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
                    Response response =
                      cassandraOperation.getPropertiesValueById(orgDb.getKeySpace(), orgDb.getTableName(), orgidList, fields, context);
                    List<Map<String, Object>> suborgDetailsList =
                      (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

                    boolean locationExists = false;
                    List<String> subOrgLocationIds = null;
                    for (Map<String,Object> org : suborgDetailsList) {
                      // If org is a suborg, then check location ids

                      if (org.get(JsonKey.IS_ROOT_ORG) != null) {
                        boolean isRootOrg = (boolean) org.get(JsonKey.IS_ROOT_ORG);
                        if (!isRootOrg) {
                          if (org.get(JsonKey.LOCATION_IDS) != null) {
                            List<String> orgLocationIds = (List<String>) org.get(JsonKey.LOCATION_IDS);
                            subOrgLocationIds = orgLocationIds;
                            if (orgLocationIds.size() <= userLocationIds.size()) {
                              locationExists = true;
                            }
                          }
                        }
                      }
                    }

                    if (!locationExists) {
                      if (subOrgLocationIds != null) {
                        updateUserLocation(
                            context, userRespList, finalDryRun, userId, subOrgLocationIds);
                      }
                    } else {
                      logger.info(
                        "User location update not required, location ids are same as org location for userid : " + userId);
                      Map<String,Object> resMap = new HashMap<>();
                      resMap.put("userlocnUpdateNotRequired","User location update not required, location ids are same as org location for userid : " + userId);
                      resMap.put("userLocIds",userLocationIds);
                      resMap.put("subOrgLocIds",subOrgLocationIds);
                      userRespList.add(resMap);
                    }
                  } else {
                    logger.info(
                        "User not associated with any org for userid " + userId);
                    Map<String,Object> resMap = new HashMap<>();
                    resMap.put("userOrgEmpty","UserOrg is empty for the given userid : " + userId);
                    userRespList.add(resMap);
                  }
                } else {
                  logger.info("UserOrg is empty for the given userid " + userId);
                  Map<String,Object> resMap = new HashMap<>();
                  resMap.put("userOrgEmpty","UserOrg is empty for the given userid : " + userId);
                  userRespList.add(resMap);
                }

              } else {
                logger.info("For the given userid no user exist in ES : " + userId);
                Map<String,Object> resMap = new HashMap<>();
                resMap.put("userNotFound","userid no user exist in ES : " + userId);
                userRespList.add(resMap);
              }
            });
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, userRespList);
    sender().tell(response, self());
  }

  private void updateUserLocation(
      RequestContext context,
      List<Map<String, Object>> userRespList,
      boolean finalDryRun,
      String userId,
      List<String> subOrgLocationIds) {

    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.LOCATION_IDS, subOrgLocationIds);

    try {
      if (finalDryRun) {
        Map<String, Object> userOpMap = new HashMap<>(userMap);
        logger.info("usermap which update user table is :: " + userOpMap);
        userRespList.add(userOpMap);
      } else {
        // fetch before updating user table (id, locationids) for validation
        Map<String, Object> userResMap = new HashMap<>();
        List<String> fields = new ArrayList<>();
        fields.add(JsonKey.ID);
        fields.add(JsonKey.LOCATION_IDS);
        Response res =
            cassandraOperation.getPropertiesValueById(
                usrDbInfo.getKeySpace(),
                usrDbInfo.getTableName(),
                (String) userMap.get(JsonKey.ID),
                fields,
                context);

        List<Map<String, Object>> usrList = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
        if (CollectionUtils.isNotEmpty(usrList)) {
          Map<String, Object> userOpMap = new HashMap<>(usrList.get(0));
          userResMap.put("beforeUserTableUpdateResponse", userOpMap);
        }

        cassandraOperation.updateRecord(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userMap, context);

        // print user map as after result
        Map<String, Object> opMap = new HashMap<>(userMap);
        userResMap.put("afterUserTableUpdateResponse", opMap);

        Future<Boolean> responseF =
            esUtil.update(ProjectUtil.EsType.user.getTypeName(), userId, userMap, context);
        boolean esResponse = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
        userResMap.put("esResponse", esResponse);
        userRespList.add(userResMap);
        if (esResponse) {
          logger.info(context, "unable to save the user location data to ES with identifier " + userId);
        } else {
          logger.info(context, "saved the user location data to ES with identifier " + userId);
        }
      }
    } catch (Exception ex) {
      logger.error(
          context, "Exception occurred while updating userName in user and userlookup table.", ex);
    }
  }
}
