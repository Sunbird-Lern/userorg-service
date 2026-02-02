package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.keycloak.SSOManager;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.user.UserUtil;

public class UserDeletionService {

  private final UserExternalIdentityService userExternalIdentityService =
      UserExternalIdentityServiceImpl.getInstance();
  private final LoggerUtil logger = new LoggerUtil(UserDeletionService.class);

  public Response deleteUser(
      String userId,
      SSOManager ssoManager,
      User user,
      Map<String, Object> userMapES,
      RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();

    Response updateUserResponse;
    Map<String, Object> deletionStatus = new HashMap<>();
    deletionStatus.put(JsonKey.CREDENTIALS_STATUS, false);
    deletionStatus.put(JsonKey.USER_LOOK_UP_STATUS, false);
    deletionStatus.put(JsonKey.USER_EXTERNAL_ID_STATUS, false);
    deletionStatus.put(JsonKey.USER_TABLE_STATUS, false);

    try {
      logger.info(
          "UserDeletionService::deleteUser:: invoking ssoManager.removeUser:: "
              + userMapES.getOrDefault(JsonKey.USER_ID, ""));
      try {
        ssoManager.removeUser(userMapES, context);
        deletionStatus.put(JsonKey.CREDENTIALS_STATUS, true);
      } catch (Exception ex) {
        logger.error(
            "UserDeletionService::deleteUser:: Exception in ssoManager.removeUser:: "
                + ex.getMessage(),
            ex);
        ssoManager.removePII(userId, context);
      }

      Map userLookUpData = mapper.convertValue(user, Map.class);
      List<String> identifiers = new ArrayList<>();
      identifiers.add(JsonKey.USERNAME);
      if (StringUtils.isNotBlank((String) userLookUpData.get(JsonKey.EMAIL)))
        identifiers.add(JsonKey.EMAIL);
      if (StringUtils.isNotBlank((String) userLookUpData.get(JsonKey.PHONE)))
        identifiers.add(JsonKey.PHONE);
      if (StringUtils.isNotBlank((String) userLookUpData.get(JsonKey.EXTERNAL_ID)))
        identifiers.add(JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
      logger.info("UserDeletionService::deleteUser:: invoking UserUtil.removeEntryFromUserLookUp");
      UserUtil.removeEntryFromUserLookUp(userLookUpData, identifiers, context);
      deletionStatus.put(JsonKey.USER_LOOK_UP_STATUS, true);

      List<Map<String, String>> dbUserExternalIds =
          userExternalIdentityService.getUserExternalIds(userId, context);
      logger.info(
          "UserDeletionService::deleteUser:: dbUserExternalIds:: " + dbUserExternalIds.size());
      if (!dbUserExternalIds.isEmpty()) {
        logger.info(
            "UserDeletionService::deleteUser:: invoking userExternalIdentityService.deleteUserExternalIds");
        userExternalIdentityService.deleteUserExternalIds(dbUserExternalIds, context);
      }
      deletionStatus.put(JsonKey.USER_EXTERNAL_ID_STATUS, true);

      User updatedUser = mapper.convertValue(userMapES, User.class);
      UserDao userDao = UserDaoImpl.getInstance();
      logger.info("UserDeletionService::deleteUser:: invoking userDao.updateUser");
      updateUserResponse = userDao.updateUser(updatedUser, context);
      deletionStatus.put(JsonKey.USER_TABLE_STATUS, true);
    } catch (Exception ex) {
      generateAuditTelemetryEvent(deletionStatus, userId, context);
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    logger.info("UserDeletionService::deleteUser:: invoking generateAuditTelemetryEvent:: ");
    generateAuditTelemetryEvent(deletionStatus, userId, context);

    return updateUserResponse;
  }

  private void generateAuditTelemetryEvent(
      Map<String, Object> requestMap, String userId, RequestContext context) {
    logger.info(
        "UserDeletionService::deleteUser:: generateAuditTelemetryEvent:: env: "
            + context.getTelemetryContext().get(JsonKey.CONTEXT));

    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> telemetryAction = new HashMap<>();
    String strTelemetryAction = convertWithIteration(requestMap);
    logger.info(
        "UserDeletionService::deleteUser:: generateAuditTelemetryEvent:: strTelemetryAction: "
            + strTelemetryAction);
    telemetryAction.put(strTelemetryAction, strTelemetryAction);
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.DELETE, null);
    TelemetryUtil.telemetryProcessingCall(
        JsonKey.DELETE_USER_STATUS,
        telemetryAction,
        targetObject,
        correlatedObject,
        (Map<String, Object>) context.getTelemetryContext().get(JsonKey.CONTEXT));
  }

  private String convertWithIteration(Map<String, Object> map) {
    StringBuilder mapAsString = new StringBuilder("{");
    for (String key : map.keySet()) {
      mapAsString.append(key + ":" + map.get(key) + ", ");
    }
    mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");
    return mapAsString.toString();
  }
}
