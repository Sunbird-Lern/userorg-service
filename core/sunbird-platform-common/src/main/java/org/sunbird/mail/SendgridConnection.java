package org.sunbird.mail;

import java.util.Properties;
import javax.mail.Session;
import javax.mail.Transport;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.common.PropertiesCache;

/**
 * Manages the connection to the Sendgrid (or SMTP) email server.
 * Handles configuration retrieval from environment variables or properties file
 * and creates the JavaMail Session and Transport objects.
 */
public class SendgridConnection {

  public final LoggerUtil logger = new LoggerUtil(SendgridConnection.class);

  private Properties props = null;
  private String host;
  private String port;
  private String userName;
  private String password;
  private String fromEmail;
  private Session session;
  private Transport transport;

  /**
   * Creates and connects a Transport object for sending emails.
   * Retrieves configuration from environment variables first, falling back to properties file if missing.
   *
   * @param context The request context for logging.
   * @return The connected Transport object, or null if connection fails.
   */
  public Transport createConnection(RequestContext context) {
    try {
      host = System.getenv(JsonKey.EMAIL_SERVER_HOST);
      port = System.getenv(JsonKey.EMAIL_SERVER_PORT);
      userName = System.getenv(JsonKey.EMAIL_SERVER_USERNAME);
      password = System.getenv(JsonKey.EMAIL_SERVER_PASSWORD);
      fromEmail = System.getenv(JsonKey.EMAIL_SERVER_FROM);

      if (StringUtils.isBlank(host)
          || StringUtils.isBlank(port)
          || StringUtils.isBlank(userName)
          || StringUtils.isBlank(password)
          || StringUtils.isBlank(fromEmail)) {
        logger.info(
            context,
            "SendgridConnection:createConnection: Email settings not found in environment variables. Host: "
                + host
                + " Port: "
                + port
                + " FromEmail: "
                + fromEmail
                + ". Falling back to properties file.");
        initialiseFromProperty();
      }

      props = System.getProperties();
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.socketFactory.port", port);
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.port", port);

      session = Session.getInstance(props, new GMailAuthenticator(userName, password));
      transport = session.getTransport("smtp");
      transport.connect(host, userName, password);
      return transport;
    } catch (Exception e) {
      logger.error(
          context,
          "SendgridConnection:createConnection: Exception occurred while creating SMTP session and transport connection: "
              + e.getMessage(),
          e);
    }
    return null;
  }

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  public Transport getTransport() {
    return transport;
  }

  /**
   * Initializes email configuration from the properties cache.
   */
  public void initialiseFromProperty() {
    host = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_HOST);
    port = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PORT);
    userName = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_USERNAME);
    password = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PASSWORD);
    fromEmail = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_FROM);
  }
}
