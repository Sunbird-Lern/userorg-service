package org.sunbird.service.user.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.TenantMigrationService;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;
import org.sunbird.util.UserFlagEnum;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserActorOperations;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class TenantMigrationServiceImpl implements TenantMigrationService {

  public LoggerUtil logger = new LoggerUtil(this.getClass());
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private Util.DbInfo usrOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);

  @Override
  public void migrateUser(Request request, Map<String, Object> userDetails) {
    {
      logger.info(request.getRequestContext(), "TenantMigrationActor:migrateUser called.");
      validateUserCustodianOrgId(
          (String) userDetails.get(JsonKey.ROOT_ORG_ID), request.getRequestContext());
      validateChannelAndGetRootOrgId(request);
      Map<String, String> rollup = new HashMap<>();
      rollup.put("l1", (String) request.getRequest().get(JsonKey.ROOT_ORG_ID));
      request.getContext().put(JsonKey.ROLLUP, rollup);
      String orgId =
          validateOrgExternalIdOrOrgIdAndGetOrgId(
              request.getRequest(), request.getRequestContext());
      request.getRequest().put(JsonKey.ORG_ID, orgId);
      int userFlagValue = UserFlagEnum.STATE_VALIDATED.getUserFlagValue();
      if (userDetails.containsKey(JsonKey.FLAGS_VALUE)) {
        userFlagValue += Integer.parseInt(String.valueOf(userDetails.get(JsonKey.FLAGS_VALUE)));
      }
      request.getRequest().put(JsonKey.FLAGS_VALUE, userFlagValue);
      Map<String, Object> userUpdateRequest = createUserUpdateRequest(request);
      // Update user channel and rootOrgId
      CassandraOperation cassandraOperation = ServiceFactory.getInstance();
      Response response =
          cassandraOperation.updateRecord(
              usrDbInfo.getKeySpace(),
              usrDbInfo.getTableName(),
              userUpdateRequest,
              request.getRequestContext());
      if (null == response
          || null == (String) response.get(JsonKey.RESPONSE)
          || (null != (String) response.get(JsonKey.RESPONSE)
              && !((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS))) {
        // throw exception for migration failed
        ProjectCommonException.throwServerErrorException(ResponseCode.errorUserMigrationFailed);
      }
      if (null != userUpdateRequest.get(JsonKey.IS_DELETED)
          && (Boolean) userUpdateRequest.get(JsonKey.IS_DELETED)) {
        deactivateUserFromKC(
            (String) userUpdateRequest.get(JsonKey.ID), request.getRequestContext());
      }
      logger.info(
          request.getRequestContext(), "TenantMigrationActor:migrateUser user record got updated.");
      // Update user org details
      Response userOrgResponse =
          updateUserOrg(
              request, (List<Map<String, Object>>) userDetails.get(JsonKey.ORGANISATIONS));

      // Update user externalIds
      Response userExternalIdsResponse = updateUserExternalIds(request);
      // Collect all the error message
      List<Map<String, Object>> userOrgErrMsgList = new ArrayList<>();
      if (MapUtils.isNotEmpty(userOrgResponse.getResult())
          && CollectionUtils.isNotEmpty(
              (List<Map<String, Object>>) userOrgResponse.getResult().get(JsonKey.ERRORS))) {
        userOrgErrMsgList =
            (List<Map<String, Object>>) userOrgResponse.getResult().get(JsonKey.ERRORS);
      }
      List<Map<String, Object>> userExtIdErrMsgList = new ArrayList<>();
      if (MapUtils.isNotEmpty(userExternalIdsResponse.getResult())
          && CollectionUtils.isNotEmpty(
              (List<Map<String, Object>>)
                  userExternalIdsResponse.getResult().get(JsonKey.ERRORS))) {
        userExtIdErrMsgList =
            (List<Map<String, Object>>) userExternalIdsResponse.getResult().get(JsonKey.ERRORS);
      }
      userOrgErrMsgList.addAll(userExtIdErrMsgList);
      response.getResult().put(JsonKey.ERRORS, userOrgErrMsgList);
      // send the response
      /*sender().tell(response, self());
      // save user data to ES
      saveUserDetailsToEs(
        (String) request.getRequest().get(JsonKey.USER_ID), request.getRequestContext());
      if (notify) {
        notify(userDetails, request.getRequestContext());
      }
      targetObject =
        TelemetryUtil.generateTargetObject(
          (String) reqMap.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
      reqMap.put(JsonKey.TYPE, JsonKey.MIGRATE_USER);
      TelemetryUtil.telemetryProcessingCall(
        reqMap, targetObject, correlatedObject, request.getContext());*/
    }
  }

  private void validateChannelAndGetRootOrgId(Request request) {
    String rootOrgId = "";
    String channel = (String) request.getRequest().get(JsonKey.CHANNEL);
    if (StringUtils.isNotBlank(channel)) {
      rootOrgId =
          UserServiceImpl.getInstance()
              .getRootOrgIdFromChannel(channel, request.getRequestContext());
      request.getRequest().put(JsonKey.ROOT_ORG_ID, rootOrgId);
    }
  }

  private void validateUserCustodianOrgId(String rootOrgId, RequestContext context) {
    String custodianOrgId =
        UserServiceImpl.getInstance().getCustodianOrgId(systemSettingActorRef, context);
    if (!rootOrgId.equalsIgnoreCase(custodianOrgId)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.parameterMismatch,
          MessageFormat.format(
              ResponseCode.parameterMismatch.getErrorMessage(),
              "user rootOrgId and custodianOrgId"));
    }
  }

  private String validateOrgExternalIdOrOrgIdAndGetOrgId(
      Map<String, Object> migrateReq, RequestContext context) {
    logger.info(context, "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called.");
    String orgId = "";
    if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_ID))
        || StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID))) {
      if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_ID))) {
        orgId = (String) migrateReq.get(JsonKey.ORG_ID);
        OrgService orgService = OrgServiceImpl.getInstance();
        Map<String, Object> result = orgService.getOrgById(orgId, context);
        if (MapUtils.isEmpty(result)) {
          logger.info(
              context,
              "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called. OrgId is Invalid");
          ProjectCommonException.throwClientErrorException(ResponseCode.invalidOrgId);
        } else {
          String reqOrgRootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
          if (StringUtils.isNotBlank(reqOrgRootOrgId)
              && !reqOrgRootOrgId.equalsIgnoreCase((String) migrateReq.get(JsonKey.ROOT_ORG_ID))) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.parameterMismatch,
                MessageFormat.format(
                    ResponseCode.parameterMismatch.getErrorMessage(),
                    StringFormatter.joinByComma(JsonKey.CHANNEL, JsonKey.ORG_ID)));
          } else {
            if (MapUtils.isNotEmpty(result)) {
              fetchLocationIds(context, migrateReq, result);
            }
          }
        }
      } else if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID))) {
        OrgExternalService orgExternalService = new OrgExternalService();
        orgId =
            orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                (String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID),
                (String) migrateReq.get(JsonKey.CHANNEL),
                context);
        if (StringUtils.isBlank(orgId)) {
          logger.info(
              context,
              "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called. OrgExternalId is Invalid");
          ProjectCommonException.throwClientErrorException(
              ResponseCode.invalidParameterValue,
              MessageFormat.format(
                  ResponseCode.invalidParameterValue.getErrorMessage(),
                  (String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID),
                  JsonKey.ORG_EXTERNAL_ID));
        } else {
          // Fetch locationids of the suborg and update the location of sso user
          OrgService orgService = OrgServiceImpl.getInstance();
          Map<String, Object> orgMap = orgService.getOrgById(orgId, context);
          if (MapUtils.isNotEmpty(orgMap)) {
            fetchLocationIds(context, migrateReq, orgMap);
          }
        }
      }
    }
    return orgId;
  }

  private Map<String, Object> createUserUpdateRequest(Request request) {
    Map<String, Object> userRequest = new HashMap<>();
    userRequest.put(JsonKey.ID, request.getRequest().get(JsonKey.USER_ID));
    userRequest.put(JsonKey.CHANNEL, request.getRequest().get(JsonKey.CHANNEL));
    userRequest.put(JsonKey.ROOT_ORG_ID, request.getRequest().get(JsonKey.ROOT_ORG_ID));
    userRequest.put(JsonKey.FLAGS_VALUE, request.getRequest().get(JsonKey.FLAGS_VALUE));
    userRequest.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    if (request.getRequest().containsKey(JsonKey.PROFILE_LOCATION)
        && StringUtils.isNotEmpty((String) request.getRequest().get(JsonKey.PROFILE_LOCATION))) {
      userRequest.put(JsonKey.PROFILE_LOCATION, request.getRequest().get(JsonKey.PROFILE_LOCATION));
    }
    if (request.getRequest().containsKey(JsonKey.STATUS)) {
      userRequest.put(JsonKey.STATUS, request.getRequest().get(JsonKey.STATUS));
      userRequest.put(
          JsonKey.IS_DELETED,
          (int) request.getRequest().get(JsonKey.STATUS) == ProjectUtil.Status.ACTIVE.getValue()
              ? false
              : true);
    }
    return userRequest;
  }

  private void deactivateUserFromKC(String userId, RequestContext context) {
    try {
      Map<String, Object> userDbMap = new HashMap<>();
      userDbMap.put(JsonKey.USER_ID, userId);
      String status = getSSOManager().deactivateUser(userDbMap, context);
      logger.info(
          context,
          "TenantMigrationActor:deactivateUserFromKC:user status in deactivating Keycloak"
              + status);
    } catch (Exception e) {
      logger.error(
          context,
          "TenantMigrationActor:deactivateUserFromKC:Error occurred while deactivating user from  Keycloak",
          e);
    }
  }

  private SSOManager getSSOManager() {
    return SSOServiceFactory.getInstance();
  }

  private Response updateUserOrg(Request request, List<Map<String, Object>> userOrgList) {
    logger.info(request.getRequestContext(), "TenantMigrationActor:updateUserOrg called.");
    Response response = new Response();
    deleteOldUserOrgMapping(userOrgList, request.getRequestContext());
    Map<String, Object> userDetails = request.getRequest();
    // add mapping root org
    createUserOrgRequestAndUpdate(
        (String) userDetails.get(JsonKey.USER_ID),
        (String) userDetails.get(JsonKey.ROOT_ORG_ID),
        request.getRequestContext());
    String orgId = (String) userDetails.get(JsonKey.ORG_ID);
    if (StringUtils.isNotBlank(orgId)
        && !((String) userDetails.get(JsonKey.ROOT_ORG_ID)).equalsIgnoreCase(orgId)) {
      try {
        createUserOrgRequestAndUpdate(
            (String) userDetails.get(JsonKey.USER_ID), orgId, request.getRequestContext());
        logger.info(
            request.getRequestContext(),
            "TenantMigrationActor:updateUserOrg user org data got updated.");
      } catch (Exception ex) {
        logger.error(
            request.getRequestContext(),
            "TenantMigrationActor:updateUserOrg:Exception occurred while updating user Org.",
            ex);
        List<Map<String, Object>> errMsgList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.ERROR_MSG, ex.getMessage());
        errMsgList.add(map);
        response.getResult().put(JsonKey.ERRORS, errMsgList);
      }
    }
    return response;
  }

  private void deleteOldUserOrgMapping(
      List<Map<String, Object>> userOrgList, RequestContext context) {
    logger.info(
        context,
        "TenantMigrationActor:deleteOldUserOrgMapping: delete old user org association started.");
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    for (Map<String, Object> userOrg : userOrgList) {
      Map<String, String> compositeKey = new LinkedHashMap<>(2);
      compositeKey.put(JsonKey.USER_ID, (String) userOrg.get(JsonKey.USER_ID));
      compositeKey.put(JsonKey.ORGANISATION_ID, (String) userOrg.get(JsonKey.ORGANISATION_ID));
      cassandraOperation.deleteRecord(
          usrOrgDbInfo.getKeySpace(), usrOrgDbInfo.getTableName(), compositeKey, context);
    }
  }

  private void createUserOrgRequestAndUpdate(String userId, String orgId, RequestContext context) {
    Map<String, Object> userOrgRequest = new HashMap<>();
    userOrgRequest.put(JsonKey.ID, userId);
    userOrgRequest.put(JsonKey.HASHTAGID, orgId);
    userOrgRequest.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>();
    roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
    userOrgRequest.put(JsonKey.ROLES, roles);
    Util.registerUserToOrg(userOrgRequest, context);
  }

  private Response updateUserExternalIds(Request request) {
    logger.info(request.getRequestContext(), "TenantMigrationActor:updateUserExternalIds called.");
    Response response = new Response();
    Map<String, Object> userExtIdsReq = new HashMap<>();
    userExtIdsReq.put(JsonKey.ID, request.getRequest().get(JsonKey.USER_ID));
    userExtIdsReq.put(JsonKey.USER_ID, request.getRequest().get(JsonKey.USER_ID));
    userExtIdsReq.put(JsonKey.EXTERNAL_IDS, request.getRequest().get(JsonKey.EXTERNAL_IDS));
    try {
      ObjectMapper mapper = new ObjectMapper();
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      // Update channel to orgId  for provider field in usr_external_identiy table
      UserUtil.updateExternalIdsProviderWithOrgId(userExtIdsReq, request.getRequestContext());
      User user = mapper.convertValue(userExtIdsReq, User.class);
      UserUtil.validateExternalIds(user, JsonKey.CREATE, request.getRequestContext());
      userExtIdsReq.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
      Request userRequest = new Request();
      userRequest.setOperation(
          UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      userExtIdsReq.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userRequest.getRequest().putAll(userExtIdsReq);

      ActorRef actorRef =
          getActorRef(UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      Future<Object> future = Patterns.ask(actorRef, userRequest, t);
      response = (Response) Await.result(future, t.duration());
      UserLookUpServiceImpl userLookUpService = new UserLookUpServiceImpl();
      userLookUpService.insertExternalIdIntoUserLookup(
          (List) userExtIdsReq.get(JsonKey.EXTERNAL_IDS),
          (String) request.getRequest().get(JsonKey.USER_ID),
          request.getRequestContext());
      logger.info(
          request.getRequestContext(),
          "TenantMigrationActor:updateUserExternalIds user externalIds got updated.");
    } catch (Exception ex) {
      logger.error(
          request.getRequestContext(),
          "TenantMigrationActor:updateUserExternalIds:Exception occurred while updating user externalIds.",
          ex);
      List<Map<String, Object>> errMsgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ERROR_MSG, ex.getMessage());
      errMsgList.add(map);
      response.getResult().put(JsonKey.ERRORS, errMsgList);
    }
    return response;
  }

  private void fetchLocationIds(
      RequestContext context, Map<String, Object> migrateReq, Map<String, Object> orgMap) {
    List orgLocation = (List) orgMap.get(JsonKey.ORG_LOCATION);
    if (CollectionUtils.isNotEmpty(orgLocation)) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        migrateReq.put(JsonKey.PROFILE_LOCATION, mapper.writeValueAsString(orgLocation));
      } catch (Exception e) {
        logger.info(context, "Exception occurred while converting orgLocation to String.");
      }
    }
  }
}
