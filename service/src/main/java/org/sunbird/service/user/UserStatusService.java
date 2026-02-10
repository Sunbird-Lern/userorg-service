package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.User;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.keycloak.SSOManager;
import org.sunbird.keycloak.SSOServiceFactory;
import org.sunbird.common.ProjectUtil;

public class UserStatusService {

  private final LoggerUtil logger = new LoggerUtil(UserStatusService.class);
  private final UserService userService = UserServiceImpl.getInstance();
  private final UserDeletionService userDeletionService = new UserDeletionService();

  public Response updateUserStatus(
      Map<String, Object> userMapES, String operation, RequestContext context) {
    String userId = (String) userMapES.get(JsonKey.USER_ID);
    boolean isBlocked = (Boolean) userMapES.get(JsonKey.IS_BLOCKED);
    boolean isDeleted =
        ((int) userMapES.get(JsonKey.STATUS) == ProjectUtil.Status.DELETED.getValue());
    logger.info(
        "UserStatusService:: userId from request:: "
            + userId
            + " || contextMap:: "
            + context.getContextMap().size()
            + " || TelemetryContextMap:: "
            + context.getTelemetryContext().size());
    User user = userService.getUserById(userId, context);
    logger.info(
        "UserStatusService:: user status details from DB:: "
            + user.getStatus()
            + " || isDeleted:: "
            + user.getIsDeleted());
    if (operation.equals(ActorOperations.BLOCK_USER.getValue())
        && ProjectUtil.Status.DELETED.getValue() != user.getStatus()
        && Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "inactive"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (operation.equals(ActorOperations.UNBLOCK_USER.getValue())
        && ProjectUtil.Status.DELETED.getValue() != user.getStatus()
        && Boolean.FALSE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "active"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (operation.equals(ActorOperations.DELETE_USER.getValue())
        && ProjectUtil.Status.DELETED.getValue() == user.getStatus()
        && Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userStatusError,
          MessageFormat.format(ResponseCode.userStatusError.getErrorMessage(), "deleted"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    ObjectMapper mapper = new ObjectMapper();
    User updatedUser = mapper.convertValue(userMapES, User.class);
    SSOManager ssoManager = SSOServiceFactory.getInstance();
    Response updateUserResponse;

    if (isDeleted) {
      logger.info("UserStatusService:: invoking userDeletionService.deleteUser");
      updateUserResponse =
          userDeletionService.deleteUser(userId, ssoManager, user, userMapES, context);
    } else {
      if (isBlocked) {
        ssoManager.deactivateUser(userMapES, context);
      } else {
        ssoManager.activateUser(userMapES, context);
      }
      UserDao userDao = UserDaoImpl.getInstance();
      updateUserResponse = userDao.updateUser(updatedUser, context);
    }

    return updateUserResponse;
  }

  public Map<String, Object> getUserMap(
      String userId, String updatedBy, boolean blockUser, boolean deleteUser) {
    Map<String, Object> esUserMap = new HashMap<>();
    esUserMap.put(JsonKey.IS_BLOCKED, blockUser);
    if (deleteUser) {
      esUserMap.put(JsonKey.STATUS, ProjectUtil.Status.DELETED.getValue());
      esUserMap.put(JsonKey.MASKED_EMAIL, "");
      esUserMap.put(JsonKey.MASKED_PHONE, "");
      esUserMap.put(JsonKey.FIRST_NAME, "");
      esUserMap.put(JsonKey.LAST_NAME, "");
      esUserMap.put(JsonKey.PHONE, "");
      esUserMap.put(JsonKey.EMAIL, "");
      esUserMap.put(JsonKey.PREV_USED_EMAIL, "");
      esUserMap.put(JsonKey.PREV_USED_PHONE, "");
      esUserMap.put(JsonKey.PROFILE_LOCATION, new ArrayList<>());
      esUserMap.put(JsonKey.RECOVERY_EMAIL, "");
      esUserMap.put(JsonKey.RECOVERY_PHONE, "");
      esUserMap.put(JsonKey.USER_NAME, "");
      esUserMap.put(JsonKey.IS_DELETED, true);
    } else {
      esUserMap.put(
          JsonKey.STATUS,
          blockUser
              ? ProjectUtil.Status.INACTIVE.getValue()
              : ProjectUtil.Status.ACTIVE.getValue());
    }
    esUserMap.put(JsonKey.ID, userId);
    esUserMap.put(JsonKey.USER_ID, userId);
    esUserMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    esUserMap.put(JsonKey.UPDATED_BY, updatedBy);
    return esUserMap;
  }
}
