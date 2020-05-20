package org.sunbird.common.models.response;

import java.io.Serializable;

/**
 * This class will contains response envelop.
 *
 * @author Manzarul
 */
public class ResponseParams implements Serializable {

  private static final long serialVersionUID = 6772142067149203497L;
  private String resmsgid;
  private String msgid;
  private String err;
  private String status;
  private String errmsg;

  public enum StatusType {
    SUCCESSFUL,
    WARNING,
    FAILED;
  }

  /**
   * This will contains response message id.
   *
   * @return String
   */
  public String getResmsgid() {
    return resmsgid;
  }

  /**
   * set the response message id.
   *
   * @param resmsgid String
   */
  public void setResmsgid(String resmsgid) {
    this.resmsgid = resmsgid;
  }

  /**
   * This will provide request specific message id.
   *
   * @return String
   */
  public String getMsgid() {
    return msgid;
  }

  /**
   * Set the request specific message id.
   *
   * @param msgid
   */
  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  /**
   * This will provide error message
   *
   * @return String
   */
  public String getErr() {
    return err;
  }

  /**
   * Set the error message
   *
   * @param err String
   */
  public void setErr(String err) {
    this.err = err;
  }

  /**
   * This will return api call status
   *
   * @return String
   */
  public String getStatus() {
    return status;
  }

  /**
   * Set the api call status
   *
   * @param status
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * This will provide Error message in english
   *
   * @return String
   */
  public String getErrmsg() {
    return errmsg;
  }

  /**
   * Set the error message in English.
   *
   * @param message String
   */
  public void setErrmsg(String message) {
    this.errmsg = message;
  }
}
