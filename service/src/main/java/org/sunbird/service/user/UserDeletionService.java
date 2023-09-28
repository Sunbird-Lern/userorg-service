package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.sso.SSOManager;
import org.sunbird.telemetry.util.TelemetryUtil;

public class UserDeletionService {

  private final UserLookUpServiceImpl userLookUpService = new UserLookUpServiceImpl();
  private final UserExternalIdentityService userExternalIdentityService =
      UserExternalIdentityServiceImpl.getInstance();

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
      ssoManager.removeUser(userMapES, context);
      deletionStatus.put(JsonKey.CREDENTIALS_STATUS, true);

      Map userLookUpData = mapper.convertValue(user, Map.class);
      List<String> identifiers = new ArrayList<>();
      identifiers.add(JsonKey.EMAIL);
      identifiers.add(JsonKey.PHONE);
      identifiers.add(JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
      userLookUpService.removeEntryFromUserLookUp(userLookUpData, identifiers, context);
      deletionStatus.put(JsonKey.USER_LOOK_UP_STATUS, true);

      List<Map<String, String>> dbUserExternalIds =
          userExternalIdentityService.getUserExternalIds(userId, context);
      if (dbUserExternalIds != null && !dbUserExternalIds.isEmpty()) {
        userExternalIdentityService.deleteUserExternalIds(dbUserExternalIds, context);
      }
      deletionStatus.put(JsonKey.USER_EXTERNAL_ID_STATUS, true);

      User updatedUser = mapper.convertValue(userMapES, User.class);
      UserDao userDao = UserDaoImpl.getInstance();
      updateUserResponse = userDao.updateUser(updatedUser, context);
      deletionStatus.put(JsonKey.USER_TABLE_STATUS, true);
    } catch (Exception ex) {
      generateTelemetryEvent(deletionStatus, userId, context.getContextMap());
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "delete"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    generateTelemetryEvent(deletionStatus, userId, context.getContextMap());

    // Trigger the delete user kafka event {{env_name}}.delete.user using Background actor

    return updateUserResponse;
  }

  private void generateTelemetryEvent(
      Map<String, Object> requestMap, String userId, Map<String, Object> context) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    Map<String, Object> telemetryAction = new HashMap<>();
    telemetryAction.put(JsonKey.DELETE_USER_STATUS, requestMap);
    TelemetryUtil.telemetryProcessingCall(
        JsonKey.DELETE_USER_STATUS, telemetryAction, targetObject, correlatedObject, context);
  }
}
