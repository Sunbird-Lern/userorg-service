package org.sunbird.actor.notification;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.mail.SendEmail;
import org.sunbird.mail.SendgridConnection;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.notification.NotificationService;
import org.sunbird.util.ProjectUtil;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmailServiceActor extends BaseActor {

  private final NotificationService notificationService = new NotificationService();
  private final SendgridConnection connection = new SendgridConnection();
  private final String resetInterval =
      ProjectUtil.getConfigValue("sendgrid_connection_reset_interval");
  private volatile long timer;

  @Override
  public void onReceive(Request request) throws Throwable {
    if (null == connection.getTransport()) {
      connection.createConnection(request.getRequestContext());
      // set timer value
      timer = System.currentTimeMillis();
    }
    if (request.getOperation().equalsIgnoreCase(ActorOperations.EMAIL_SERVICE.getValue())) {
      sendMail(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void sendMail(Request actorMessage) {
    RequestContext requestContext = actorMessage.getRequestContext();
    Map<String, Object> request =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.EMAIL_REQUEST);
    List<String> userIds =
        (CollectionUtils.isEmpty((List<String>) request.get(JsonKey.RECIPIENT_USERIDS)))
            ? new ArrayList<>()
            : (List<String>) request.get(JsonKey.RECIPIENT_USERIDS);
    List<String> phones =
        (CollectionUtils.isEmpty((List<String>) request.get(JsonKey.RECIPIENT_PHONES)))
            ? new ArrayList<>()
            : (List<String>) request.get(JsonKey.RECIPIENT_PHONES);
    List<String> emails =
        (CollectionUtils.isEmpty((List<String>) request.get(JsonKey.RECIPIENT_EMAILS)))
            ? new ArrayList<>()
            : (List<String>) request.get(JsonKey.RECIPIENT_EMAILS);
    String mode;
    if (request.get(JsonKey.MODE) != null
        && JsonKey.SMS.equalsIgnoreCase((String) request.get(JsonKey.MODE))) {
      mode = JsonKey.SMS;
    } else {
      mode = JsonKey.EMAIL;
    }
    if (JsonKey.SMS.equalsIgnoreCase(mode)) {
      notificationService.processSMS(
          userIds, phones, (String) request.get(JsonKey.BODY), requestContext);
    }

    if (JsonKey.EMAIL.equalsIgnoreCase(mode)) {
      // Fetch user emails from Elastic Search based on recipient search query given in
      // request.
      Map<String, Object> recipientSearchQuery =
          (Map<String, Object>) request.get(JsonKey.RECIPIENT_SEARCH_QUERY);
      List<String> emailList =
          notificationService.validateAndGetEmailList(
              userIds, emails, recipientSearchQuery, requestContext);
      notificationService.updateFirstNameAndOrgNameInEmailContext(
          userIds, emailList, request, requestContext);

      if (CollectionUtils.isNotEmpty(emailList)) {
        String template =
            notificationService.getEmailTemplateFile(
                (String) request.get(JsonKey.EMAIL_TEMPLATE_TYPE), requestContext);
        sendMail(request, emailList, template, requestContext);
      }
    }

    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(res, self());
  }

  private void sendMail(
      Map<String, Object> request,
      List<String> emails,
      String template,
      RequestContext requestContext) {
    try {
      SendEmail sendEmail = new SendEmail();
      Velocity.init();
      VelocityContext context = ProjectUtil.getContext(request);
      StringWriter writer = new StringWriter();
      Velocity.evaluate(context, writer, "SimpleVelocity", template);
      long interval = 60000L;
      if (StringUtils.isNotBlank(resetInterval)) {
        interval = Long.parseLong(resetInterval);
      }
      if (null == connection.getTransport()
          || ((System.currentTimeMillis()) - timer >= interval)
          || (!connection.getTransport().isConnected())) {
        resetConnection(requestContext);
      }
      sendEmail.send(
          emails.toArray(new String[emails.size()]),
          (String) request.get(JsonKey.SUBJECT),
          context,
          writer,
          connection.getSession(),
          connection.getTransport());
    } catch (Exception e) {
      logger.error(
          requestContext,
          "EmailServiceActor:sendMail: Exception occurred with message = " + e.getMessage(),
          e);
    }
  }

  private void resetConnection(RequestContext context) {
    logger.info(
        context,
        "EmailServiceActor:resetConnection : SMTP Transport client connection is closed or timed out. Create new connection.");
    connection.createConnection(context);
    // set timer value
    timer = System.currentTimeMillis();
  }
}
