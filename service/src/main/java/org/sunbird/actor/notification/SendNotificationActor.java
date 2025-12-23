package org.sunbird.actor.notification;

import org.apache.pekko.actor.ActorRef;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.notification.NotificationService;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

public class SendNotificationActor extends BaseActor {

  private final NotificationService notificationService = new NotificationService();

  @Inject
  @Named("background_notification_actor")
  private ActorRef backGroundNotificationActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase(ActorOperations.V2_NOTIFICATION.getValue())) {
      sendNotification(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void sendNotification(Request request) {
    Map<String, Object> requestMap =
        (Map<String, Object>) request.getRequest().get(JsonKey.EMAIL_REQUEST);
    List<String> userIds = (List<String>) requestMap.remove(JsonKey.RECIPIENT_USERIDS);
    Set<String> phoneOrEmailList;
    Map<String, Object> notificationReq = null;
    String mode = (String) requestMap.remove(JsonKey.MODE);
    if (StringUtils.isNotBlank(mode) && JsonKey.SMS.equalsIgnoreCase(mode)) {
      mode = JsonKey.SMS;
    } else {
      mode = JsonKey.EMAIL;
    }
    if (JsonKey.SMS.equalsIgnoreCase(mode)) {
      phoneOrEmailList =
          notificationService.getEmailOrPhoneListByUserIds(
              userIds, JsonKey.PHONE, request.getRequestContext());
      notificationReq =
          notificationService.getV2NotificationRequest(
              phoneOrEmailList, requestMap, JsonKey.SMS, null);
    }
    if (JsonKey.EMAIL.equalsIgnoreCase(mode)) {
      phoneOrEmailList =
          notificationService.getEmailOrPhoneListByUserIds(
              userIds, JsonKey.EMAIL, request.getRequestContext());
      String template =
          notificationService.getEmailTemplateFile(
              (String) requestMap.get(JsonKey.EMAIL_TEMPLATE_TYPE), request.getRequestContext());
      notificationReq =
          notificationService.getV2NotificationRequest(
              phoneOrEmailList, requestMap, JsonKey.EMAIL, template);
    }
    logger.debug(
        request.getRequestContext(),
        "SendNotificationActor:sendNotification : called for userIds =" + userIds);
    process(notificationReq, request.getRequestId(), request.getRequestContext());

    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(res, self());
  }

  private void process(
      Map<String, Object> notificationReq, String requestId, RequestContext context) {
    List<Map<String, Object>> notificationList = new ArrayList<>();
    notificationList.add(notificationReq);
    Map<String, Object> reqMap = new HashMap<>();
    Map<String, Object> request = new HashMap<>();
    request.put("notifications", notificationList);
    reqMap.put(JsonKey.REQUEST, request);

    Request bgRequest = new Request();
    bgRequest.setRequestContext(context);
    bgRequest.setRequestId(requestId);
    bgRequest.getRequest().putAll(reqMap);
    bgRequest.setOperation("processNotification");
    try {
      backGroundNotificationActor.tell(bgRequest, self());
    } catch (Exception ex) {
      logger.error(context, "Exception while sending notification", ex);
    }
  }
}
