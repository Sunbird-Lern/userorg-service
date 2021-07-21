package org.sunbird.mail;

import java.util.Properties;
import javax.mail.Session;
import javax.mail.Transport;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.PropertiesCache;

public class SendgridConnection {

  public LoggerUtil logger = new LoggerUtil(SendgridConnection.class);

  private Properties props = null;
  private String host;
  private String port;
  private String userName;
  private String password;
  private String fromEmail;
  private Session session;
  private Transport transport;

  public Transport createConnection() {
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
      logger.error("Exception occurred while smtp session and creating transport connection", e);
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

  public void initialiseFromProperty() {
    host = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_HOST);
    port = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PORT);
    userName = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_USERNAME);
    password = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PASSWORD);
    fromEmail = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_FROM);
  }
}
