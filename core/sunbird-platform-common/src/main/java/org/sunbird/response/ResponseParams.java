package org.sunbird.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Encapsulates the response parameter envelope for API responses.
 * Contains metadata such as message IDs, status (SUCCESSFUL/FAILED), and error details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseParams implements Serializable {

  private static final long serialVersionUID = 6772142067149203497L;
  
  /** Unique response message ID. */
  private String resmsgid;
  
  /** Request-specific message ID. */
  private String msgid;
  
  /** Error code, if applicable. */
  private String err;
  
  /** API call status (e.g., "successful"). */
  private String status;
  
  /** Descriptive error message in English. */
  private String errmsg;

  /**
   * Enum representing standard API status types.
   */
  public enum StatusType {
    SUCCESSFUL,
    WARNING,
    FAILED;
  }

  /**
   * Gets the unique response message ID.
   *
   * @return The response message ID string.
   */
  public String getResmsgid() {
    return resmsgid;
  }

  /**
   * Sets the unique response message ID.
   *
   * @param resmsgid The response message ID string.
   */
  public void setResmsgid(String resmsgid) {
    this.resmsgid = resmsgid;
  }

  /**
   * Gets the request-specific message ID.
   *
   * @return The message ID string.
   */
  public String getMsgid() {
    return msgid;
  }

  /**
   * Sets the request-specific message ID.
   *
   * @param msgid The message ID string.
   */
  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  /**
   * Gets the error code.
   *
   * @return The error code string, or null if successful.
   */
  public String getErr() {
    return err;
  }

  /**
   * Sets the error code.
   *
   * @param err The error code string.
   */
  public void setErr(String err) {
    this.err = err;
  }

  /**
   * Gets the API status.
   *
   * @return The status string.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the API status.
   *
   * @param status The status string.
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Gets the descriptive error message.
   *
   * @return The error message string.
   */
  public String getErrmsg() {
    return errmsg;
  }

  /**
   * Sets the descriptive error message.
   *
   * @param message The error message string.
   */
  public void setErrmsg(String message) {
    this.errmsg = message;
  }
}