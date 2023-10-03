package org.sunbird.actor.user;

import akka.actor.ActorRef;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.UserStatusService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.Util;

public class UserStatusActor extends UserBaseActor {

  @Inject
  @Named("user_deletion_background_job_actor")
  private ActorRef userDeletionBackgroundJobActor;

  private final UserStatusService userStatusService = new UserStatusService();
  private final UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "blockUser":
        updateStatus(request, true, false);
        break;

      case "unblockUser":
        updateStatus(request, false, false);
        break;

      case "deleteUser":
        updateStatus(request, true, true);
        break;

      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void updateStatus(Request request, boolean blockUser, boolean deleteUser) {
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    String operation = request.getOperation();
    String requestedBy = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    Map<String, Object> userMap =
        userStatusService.getUserMap(userId, requestedBy, blockUser, deleteUser);
    Response response =
        userStatusService.updateUserStatus(userMap, operation, request.getRequestContext());

    if (deleteUser) {
      Map<String, Object> userData = new HashMap<>();
      userData.put(JsonKey.USER_ID, userId);

      Request bgRequest = new Request();
      bgRequest.setRequestContext(request.getRequestContext());
      bgRequest.setRequestId(request.getRequestId());
      bgRequest.getRequest().putAll(userData);
      bgRequest.setOperation("inputKafkaTopic");
      try {
        userDeletionBackgroundJobActor.tell(bgRequest, self());
      } catch (Exception ex) {
        logger.error(
            request.getRequestContext(),
            "Exception while sending event to user deletion kafka topic",
            ex);
      }
    }

    sender().tell(response, self());

    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      String logMsgPrefix =
          MessageFormat.format("UserStatusActor:updateUserStatus:{0}:{1}: ", operation, userId);
      logger.info(request.getRequestContext(), logMsgPrefix + "Update user data to ES.");
      userService.updateUserDataToES(userId, userMap, request.getRequestContext());
    }
    generateTelemetryEvent(request.getRequest(), userId, operation, request.getContext());
  }
}
