/** */
package org.sunbird.common.models.util.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/** @author Manzarul.Haque */
public class GMailAuthenticator extends Authenticator {
  private String user;
  private String pw;

  /**
   * this method is used to authenticate gmail user name and password.
   *
   * @param username
   * @param password
   */
  public GMailAuthenticator(String username, String password) {
    super();
    this.user = username;
    this.pw = password;
  }

  /** */
  @Override
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(this.user, this.pw);
  }
}
