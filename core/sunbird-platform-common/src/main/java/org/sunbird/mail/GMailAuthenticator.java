/** */
package org.sunbird.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * This class handles Gmail authentication.
 */
public class GMailAuthenticator extends Authenticator {
  private String user;
  private String pw;

  /**
   * Constructor to initialize username and password.
   *
   * @param username The Gmail username.
   * @param password The Gmail password.
   */
  public GMailAuthenticator(String username, String password) {
    super();
    this.user = username;
    this.pw = password;
  }

  /**
   * Returns a PasswordAuthentication instance.
   *
   * @return A PasswordAuthentication object containing the username and password.
   */
  @Override
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(this.user, this.pw);
  }
}
