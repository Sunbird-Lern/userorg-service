package org.sunbird.mail;

import java.io.StringWriter;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.velocity.VelocityContext;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

/**
 * Utility class for sending emails using JavaMail API.
 * Supports sending simple HTML emails with Velocity template context.
 */
public class SendEmail {

  public LoggerUtil logger = new LoggerUtil(SendEmail.class);
  private static final String fromEmail = System.getenv(JsonKey.EMAIL_SERVER_FROM);

  /**
   * Sends an email to the specified recipients.
   *
   * @param emailList List of recipient email addresses.
   * @param subject Subject of the email.
   * @param context Velocity context for template rendering (optional).
   * @param writer StringWriter containing the email content.
   * @param session JavaMail Session object.
   * @param transport JavaMail Transport object.
   * @return true if the email was sent successfully, false otherwise.
   */
  public boolean send(
      String[] emailList,
      String subject,
      VelocityContext context,
      StringWriter writer,
      Session session,
      Transport transport) {
    boolean sentStatus = true;
    try {
      if (context != null) {
        context.put(JsonKey.FROM_EMAIL, fromEmail);
      }
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(fromEmail));
      Message.RecipientType recipientType;
      if (emailList.length > 1) {
        recipientType = Message.RecipientType.BCC;
      } else {
        recipientType = Message.RecipientType.TO;
      }
      for (String email : emailList) {
        message.addRecipient(recipientType, new InternetAddress(email));
      }
      if (recipientType == Message.RecipientType.BCC) {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(fromEmail));
      }
      message.setSubject(subject);
      message.setContent(writer.toString(), "text/html; charset=utf-8");
      transport.sendMessage(message, message.getAllRecipients());
    } catch (Exception e) {
      sentStatus = false;
      logger.error("SendEmail:send: Exception occurred while sending email: " + e.getMessage(), e);
    }
    return sentStatus;
  }
}
