package org.sunbird.badge.actors;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;

import java.util.Arrays;
import java.util.List;

/** @author Mahesh Kumar Gangula */
@ActorConfig(
  tasks = {},
  asyncTasks = {"assignBadgeMessage", "revokeBadgeMessage"}
)
public class BadgeNotifier extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    List<String> asyncTasks =
              Arrays.asList("assignBadgeMessage", "revokeBadgeMessage");
    String operation = request.getOperation();
    String objectType = (String) request.getRequest().get(JsonKey.OBJECT_TYPE);
    ProjectLogger.log(
        "Processing badge notification for " + operation,
        request.getRequest(),
        LoggerEnum.INFO.name());
    Response response;
    if (StringUtils.isNotBlank(operation) && asyncTasks.contains(operation)) {
      response = processBadge(operation, objectType, request);
      sender().tell(response, self());
    } else {
      onReceiveUnsupportedMessage(request.getOperation());
    }
  }

  private Response processBadge(String operation, String objectType, Request request)
      throws Exception {
    Response response;
    String INVALID_BADGE_NOTIFICATION_REQUEST =
              "INVALID_BADGE_NOTIFICATION_REQUEST";
    if (StringUtils.isBlank(objectType)) {
      response = invalidObjectType(INVALID_BADGE_NOTIFICATION_REQUEST, objectType);
    } else {
      String caseVal = objectType.toUpperCase() + ":" + operation.toUpperCase();
      switch (caseVal) {
        case "USER:ASSIGNBADGEMESSAGE":
          request.setOperation(BadgeOperations.assignBadgeToUser.name());
          response = notifyUser(request);
          break;
        case "USER:REVOKEBADGEMESSAGE":
          request.setOperation(BadgeOperations.revokeBadgeFromUser.name());
          response = notifyUser(request);
          break;
        default:
          response = invalidObjectType(INVALID_BADGE_NOTIFICATION_REQUEST, objectType);
          break;
      }
    }
    return response;
  }

  private Response notifyUser(Request request) {
    tellToAnother(request);
    Response response = new Response();
    response.setResponseCode(ResponseCode.success);
    return response;
  }

  private Response invalidObjectType(String error, String objectType) {
    Response response = new Response();
    response.setResponseCode(ResponseCode.CLIENT_ERROR);
    ResponseParams params = new ResponseParams();
    params.setErrmsg("ObjectType is invalid to assign/revoke badge: " + objectType);
    params.setErr(error);
    response.setParams(params);
    return response;
  }
}
