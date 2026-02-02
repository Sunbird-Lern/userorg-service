package org.sunbird.response;

import java.io.Serializable;

/**
 * Represents the standard response parameters for API responses.
 * Contains metadata about the transaction, such as message IDs, status, and error details.
 */
public class Params implements Serializable {

  private static final long serialVersionUID = -8786004970726124473L;
  
  /** The unique response message ID. */
  private String resmsgid;
  
  /** The message ID. */
  private String msgid;
  
  /** The error code, if any. */
  private String err;
  
  /** The status of the response (e.g., "success", "failed"). */
  private String status;
  
  /** The descriptive error message, if any. */
  private String errmsg;

  /**
   * Gets the response message ID.
   *
   * @return The response message ID.
   */
  public String getResmsgid() {
    return resmsgid;
  }

  /**
   * Sets the response message ID.
   *
   * @param resmsgid The response message ID to set.
   */
  public void setResmsgid(String resmsgid) {
    this.resmsgid = resmsgid;
  }

  /**
   * Gets the message ID.
   *
   * @return The message ID.
   */
  public String getMsgid() {
    return msgid;
  }

  /**
   * Sets the message ID.
   *
   * @param msgid The message ID to set.
   */
  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  /**
   * Gets the error code.
   *
   * @return The error code.
   */
  public String getErr() {
    return err;
  }

  /**
   * Sets the error code.
   *
   * @param err The error code to set.
   */
  public void setErr(String err) {
    this.err = err;
  }

  /**
   * Gets the operation status.
   *
   * @return The status string.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the operation status.
   *
   * @param status The status string to set.
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Gets the error message description.
   *
   * @return The error message.
   */
  public String getErrmsg() {
    return errmsg;
  }

  /**
   * Sets the error message description.
   *
   * @param errmsg The error message to set.
   */
  public void setErrmsg(String errmsg) {
    this.errmsg = errmsg;
  }
}
