package org.sunbird.user.actors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
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
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
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
  private UserDao userDao = UserDaoImpl.getInstance();
  private OrganisationClient organisationClient = new OrganisationClientImpl();

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
              Map<String, Object> searchQueryMap = new HashMap<>();
              searchQueryMap.put(JsonKey.USER_ID, userId);
              // ES search call on userId , to fetch user with location
              List<User> userList = userDao.searchUser(searchQueryMap, context);
              if (CollectionUtils.isNotEmpty(userList) && userList.size() > 0) {
                User usr = userList.get(0);
                if (CollectionUtils.isNotEmpty(usr.getLocationIds())
                    && CollectionUtils.isNotEmpty(usr.getOrganisations())
                    && usr.getOrganisations().size() > 0) {

                  // Get all org id list
                  List<String> orgidList = new ArrayList<>();
                  List<Map<String, Object>> orgList = usr.getOrganisations();
                  orgList
                      .stream()
                      .forEach(
                          orgMap -> {
                            orgidList.add((String) orgMap.get(JsonKey.ORGANISATION_ID));
                          });

                  // Get location ids of all org
                  if (CollectionUtils.isNotEmpty(orgidList)) {
                    List<String> fields = new ArrayList<>();
                    List<Organisation> suborgDetailsList =
                        organisationClient.esSearchOrgByIds(orgidList, fields, context);

                    boolean locationExists = false;
                    List<String> subOrgLocationIds = null;
                    for (Organisation org : suborgDetailsList) {
                      // If org is a suborg, then check location ids
                      if (org.isRootOrg() == null || !org.isRootOrg()) {
                        if (org.getLocationIds() != null
                            && org.getLocationIds().containsAll(usr.getLocationIds())) {
                          locationExists = true;
                        } else {
                          if (org.getLocationIds() != null) {
                            subOrgLocationIds = org.getLocationIds();
                          }
                        }
                      }
                    }

                    if (!locationExists) {
                      if (subOrgLocationIds != null) {
                        updateUserLocation(
                            context, userRespList, finalDryRun, usr.getId(), subOrgLocationIds);
                      }
                    }
                  } else {
                    logger.info(
                        "User location is same as suborg location for userid " + usr.getId());
                  }
                } else {
                  logger.info("Location not updated for userid " + usr.getId());
                }

              } else {
                logger.info("For the given userid no user exist in ES. " + userId);
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
          logger.info(context, "unable to save the user data to ES with identifier " + userId);
        } else {
          logger.info(context, "saved the user data to ES with identifier " + userId);
        }
      }
    } catch (Exception ex) {
      logger.error(
          context, "Exception occurred while updating userName in user and userlookup table.", ex);
    }
  }
}
