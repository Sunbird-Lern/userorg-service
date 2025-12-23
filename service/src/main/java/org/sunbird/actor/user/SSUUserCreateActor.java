package org.sunbird.actor.user;

import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.Mapper;
import org.apache.pekko.pattern.Patterns;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.AssociationMechanism;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserOrgServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.UserFlagUtil;
import org.sunbird.util.UserUtility;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;
import scala.Tuple2;
import scala.concurrent.Future;

public class SSUUserCreateActor extends UserBaseActor {

  private final UserService userService = UserServiceImpl.getInstance();
  private final UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "createUserV3":
      case "createSSUUser":
        createSSUUser(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  /** This method will create user in user in cassandra and update to ES as well at same time. */
  private void createSSUUser(Request actorMessage) {
    logger.debug(
        actorMessage.getRequestContext(), "SSUUserCreateActor:createSSUUser: User creation starts");
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    userMap.put(
        JsonKey.ROOT_ORG_ID, DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID));
    userMap.put(
        JsonKey.CHANNEL, DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL));
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.CREATE_SSU_USER.getValue())) {
      populateUserTypeAndSubType(userMap);
    }
    populateProfileUserType(userMap, actorMessage.getRequestContext());
    processSSUUser(userMap, actorMessage);
  }

  private void processSSUUser(Map<String, Object> userMap, Request actorMessage) {
    UserUtil.setUserDefaultValue(userMap, actorMessage.getRequestContext());
    removeUnwanted(userMap);
    UserUtil.toLower(userMap);
    // check phone and uniqueness using user look table
    userLookupService.checkPhoneUniqueness(
        (String) userMap.get(JsonKey.PHONE), actorMessage.getRequestContext());
    userLookupService.checkEmailUniqueness(
        (String) userMap.get(JsonKey.EMAIL), actorMessage.getRequestContext());
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    try {
      UserUtility.encryptUserData(userMap);
    } catch (Exception ex) {
      logger.error(actorMessage.getRequestContext(), ex.getMessage(), ex);
    }
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.FLAGS_VALUE, UserFlagUtil.getFlagValue(JsonKey.STATE_VALIDATED, false));
    final String password = (String) userMap.get(JsonKey.PASSWORD);
    userMap.remove(JsonKey.PASSWORD);
    userMap.remove(JsonKey.DOB_VALIDATION_DONE);
    String userId = ProjectUtil.generateUniqueId();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.USER_ID, userId);
    Response response = userService.createUser(userMap, actorMessage.getRequestContext());
    userLookupService.insertRecords(userMap, actorMessage.getRequestContext());
    response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    Map<String, Object> esResponse = new HashMap<>();
    if (JsonKey.SUCCESS.equalsIgnoreCase((String) response.get(JsonKey.RESPONSE))) {
      saveUserOrgInfo(userMap, actorMessage.getRequestContext());
      esResponse =
          userService.getUserDetailsForES(
              (String) userMap.get(JsonKey.ID), actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "SSUUserCreateActor:processSSUUser: User creation failure");
    }
    if ("kafka".equalsIgnoreCase(ProjectUtil.getConfigValue("sunbird_user_create_sync_type"))) {
      writeDataToKafka(esResponse);
      sender().tell(response, self());
    } else {
      Future<Boolean> kcFuture =
          Futures.future(
              () -> {
                try {
                  Map<String, Object> updatePasswordMap = new HashMap<>();
                  updatePasswordMap.put(JsonKey.ID, userMap.get(JsonKey.ID));
                  updatePasswordMap.put(JsonKey.PASSWORD, password);
                  return UserUtil.updatePassword(
                      updatePasswordMap, actorMessage.getRequestContext());
                } catch (Exception e) {
                  logger.error(
                      actorMessage.getRequestContext(),
                      "Error occurred during update password : " + e.getMessage(),
                      e);
                  return false;
                }
              },
              getContext().dispatcher());
      Map<String, Object> finalEsResponse = esResponse;
      Future<Response> future =
          Futures.future(
                  () ->
                      userService.saveUserToES(
                          (String) finalEsResponse.get(JsonKey.USER_ID),
                          finalEsResponse,
                          actorMessage.getRequestContext()),
                  getContext().dispatcher())
              .zip(kcFuture)
              .map(
                  new Mapper<>() {
                    @Override
                    public Response apply(Tuple2<String, Boolean> parameter) {
                      boolean updatePassResponse = parameter._2;
                      if (!updatePassResponse) {
                        response.put(
                            JsonKey.ERROR_MSG, ResponseMessage.Message.ERROR_USER_UPDATE_PASSWORD);
                      }
                      return response;
                    }
                  },
                  getContext().dispatcher());
      Patterns.pipe(future, getContext().dispatcher()).to(sender());
    }
    generateUserTelemetry(userMap, actorMessage, userId, JsonKey.CREATE);
  }

  private void saveUserOrgInfo(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.ID, userMap.get(JsonKey.USER_ID));
    userOrgMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    userOrgMap.put(JsonKey.IS_DELETED, false);
    userOrgMap.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SELF_DECLARATION);
    userOrgService.registerUserToOrg(userOrgMap, context);
  }
}
