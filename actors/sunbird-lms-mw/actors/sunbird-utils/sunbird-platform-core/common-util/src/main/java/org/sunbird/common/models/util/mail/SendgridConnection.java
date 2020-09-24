package org.sunbird.common.models.util.mail;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.RequestContext;

public class SendgridConnection {
  private static LoggerUtil logger = new LoggerUtil(SendgridConnection.class);
  private static String resetInterval =
      ProjectUtil.getConfigValue("sendgrid_connection_reset_interval");
  private static volatile long timer;
  private static Properties props = null;
  private static String host;
  private static String port;
  private static String userName;
  private static String password;
  private static String fromEmail;
  private static Session session;
  private static Transport transport;

  static {
    createConnection();
  }

  private static Transport createConnection() {
    try {
      host = System.getenv(JsonKey.EMAIL_SERVER_HOST);
      port = System.getenv(JsonKey.EMAIL_SERVER_PORT);
      userName = System.getenv(JsonKey.EMAIL_SERVER_USERNAME);
      password = System.getenv(JsonKey.EMAIL_SERVER_PASSWORD);

      if (StringUtils.isBlank(host)
          || StringUtils.isBlank(port)
          || StringUtils.isBlank(userName)
          || StringUtils.isBlank(password)
          || StringUtils.isBlank(fromEmail)) {
        logger.info(
            "Email setting value is not provided by Env variable=="
                + host
                + " "
                + port
                + " "
                + fromEmail);
        initializeFromProperty();
      }

      props = System.getProperties();
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.socketFactory.port", port);

      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.port", port);

      session = Session.getInstance(props, new GMailAuthenticator(userName, password));
      transport = session.getTransport("smtp");
      transport.connect(host, userName, password);

      // set timer value
      timer = System.currentTimeMillis();

      return transport;
    } catch (Exception e) {
      logger.error("Exception occurred while smtp session and creating transport connection", e);
    }
    return null;
  }

  private static void initializeFromProperty() {
    host = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_HOST);
    port = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PORT);
    userName = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_USERNAME);
    password = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PASSWORD);
    fromEmail = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_FROM);
  }

  private static boolean sendEmail(
      String[] emailList,
      String subject,
      VelocityContext context,
      StringWriter writer,
      Session session,
      Transport transport,
      RequestContext requestContext) {
    boolean sentStatus = true;
    try {
      if (context != null) {
        context.put(JsonKey.FROM_EMAIL, fromEmail);
      }
      if (null == transport) {
        createConnection();
      }
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(fromEmail));
      Message.RecipientType recipientType = null;
      if (emailList.length > 1) {
        recipientType = Message.RecipientType.BCC;
      } else {
        recipientType = Message.RecipientType.TO;
      }
      for (String email : emailList) {
        message.addRecipient(recipientType, new InternetAddress(email));
      }
      if (recipientType == Message.RecipientType.BCC)
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(fromEmail));
      message.setSubject(subject);
      message.setContent(writer.toString(), "text/html; charset=utf-8");
      long interval = 60000L;
      if (StringUtils.isNotBlank(resetInterval)) {
        interval = Long.parseLong(resetInterval);
      }
      if (null == transport
          || ((System.currentTimeMillis()) - timer >= interval)
          || (!transport.isConnected())) {
        logger.info(
            requestContext, "SMTP Transport client connection is closed. Create new connection.");
        createConnection();
      }
      transport.sendMessage(message, message.getAllRecipients());
    } catch (Exception e) {
      sentStatus = false;
      ProjectLogger.log("SendEmail:send: Exception occurred with message = " + e.getMessage(), e);
    }
    return sentStatus;
  }

  public static void sendMail(
      Map<String, Object> request,
      List<String> emails,
      String template,
      RequestContext requestContext) {
    try {
      Velocity.init();
      VelocityContext context = ProjectUtil.getContext(request);
      StringWriter writer = new StringWriter();
      Velocity.evaluate(context, writer, "SimpleVelocity", template);
      boolean status =
          sendEmail(
              emails.toArray(new String[emails.size()]),
              (String) request.get(JsonKey.SUBJECT),
              context,
              writer,
              session,
              transport,
              requestContext);
      logger.info(requestContext, "Email sent : " + status);
    } catch (Exception e) {
      logger.error(
          requestContext,
          "EmailServiceActor:sendMail: Exception occurred with message = " + e.getMessage(),
          e);
    }
  }
}
