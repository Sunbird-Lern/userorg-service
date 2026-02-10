package org.sunbird.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Validator class for Gmail authentication. Extends javax.mail.Authenticator to provide password
 * authentication.
 */
public class GMailAuthenticator extends Authenticator {
  private String user;
  private String pw;

  /**
   * Constructor to initialize the authenticator with username and password.
   *
   * @param username The username for authentication.
   * @param password The password for authentication.
   */
  public GMailAuthenticator(String username, String password) {
    super();
    this.user = username;
    this.pw = password;
  }

  /**
   * Returns the PasswordAuthentication object containing the username and password.
   *
   * @return PasswordAuthentication object.
   */
  @Override
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(this.user, this.pw);
  }
}
