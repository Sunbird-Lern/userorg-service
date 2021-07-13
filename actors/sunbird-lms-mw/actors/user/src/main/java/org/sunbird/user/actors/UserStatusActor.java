package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.operations.ActorOperations;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.util.ProjectUtil;

@ActorConfig(
  tasks = {"unblockUser", "blockUser"},
  asyncTasks = {}
)
public class UserStatusActor extends UserBaseActor {

  private UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "blockUser":
        blockUser(request);
        break;

      case "unblockUser":
        unblockUser(request);
        break;

      default:
        onReceiveUnsupportedOperation("UserStatusActor");
    }
  }

  private void blockUser(Request actorMessage) {
    updateUserStatus(actorMessage, true);
  }

  private void unblockUser(Request actorMessage) {
    updateUserStatus(actorMessage, false);
  }

  private void updateUserStatus(Request request, boolean isBlocked) {
    String operation = request.getOperation();
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    String logMsgPrefix =
        MessageFormat.format("UserStatusActor:updateUserStatus:{0}:{1}: ", operation, userId);
    User user = userService.getUserById(userId, request.getRequestContext());

    if (operation.equals(ActorOperations.BLOCK_USER.getValue())
        && Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userAlreadyInactive.getErrorCode(),
          ResponseCode.userAlreadyInactive.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (operation.equals(ActorOperations.UNBLOCK_USER.getValue())
        && Boolean.FALSE.equals(user.getIsDeleted())) {
      throw new ProjectCommonException(
          ResponseCode.userAlreadyActive.getErrorCode(),
          ResponseCode.userAlreadyActive.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    Map<String, Object> userMapES =
        getUserMapES(userId, (String) request.getContext().get(JsonKey.REQUESTED_BY), isBlocked);

    ObjectMapper mapper = new ObjectMapper();
    User updatedUser = mapper.convertValue(userMapES, User.class);
    SSOManager ssoManager = SSOServiceFactory.getInstance();
    if (isBlocked) {
      ssoManager.deactivateUser(userMapES, request.getRequestContext());
    } else {
      ssoManager.activateUser(userMapES, request.getRequestContext());
    }
    UserDao userDao = UserDaoImpl.getInstance();
    Response response = userDao.updateUser(updatedUser, request.getRequestContext());
    sender().tell(response, self());

    // Update status in ES
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      logger.info(request.getRequestContext(), logMsgPrefix + "Update user data to ES.");

      Request userRequest = new Request();
      userRequest.setRequestContext(request.getRequestContext());
      userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
      userRequest.getRequest().put(JsonKey.ID, userId);

      try {
        tellToAnother(userRequest);
      } catch (Exception e) {
        logger.error(
            request.getRequestContext(),
            logMsgPrefix + "Exception occurred with error message = " + e.getMessage(),
            e);
      }
    } else {
      logger.info(request.getRequestContext(), logMsgPrefix + "Update user data to ES is skipped.");
    }

    generateTelemetryEvent(request.getRequest(), userId, operation, request.getContext());
  }

  private Map<String, Object> getUserMapES(String userId, String updatedBy, boolean isDeleted) {
    Map<String, Object> esUserMap = new HashMap<>();
    esUserMap.put(JsonKey.IS_DELETED, isDeleted);
    esUserMap.put(
        JsonKey.STATUS,
        isDeleted ? ProjectUtil.Status.INACTIVE.getValue() : ProjectUtil.Status.ACTIVE.getValue());
    esUserMap.put(JsonKey.ID, userId);
    esUserMap.put(JsonKey.USER_ID, userId);
    esUserMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    esUserMap.put(JsonKey.UPDATED_BY, updatedBy);
    return esUserMap;
  }
}
