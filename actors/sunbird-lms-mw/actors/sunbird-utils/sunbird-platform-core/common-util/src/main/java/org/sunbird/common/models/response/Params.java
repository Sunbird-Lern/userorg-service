package org.sunbird.common.models.response;

import java.io.Serializable;

/**
 * Common response parameter bean
 *
 * @author Manzarul
 */
public class Params implements Serializable {

  private static final long serialVersionUID = -8786004970726124473L;
  private String resmsgid;
  private String msgid;
  private String err;
  private String status;
  private String errmsg;

  /** @return String */
  public String getResmsgid() {
    return resmsgid;
  }

  /** @param resmsgid Stirng */
  public void setResmsgid(String resmsgid) {
    this.resmsgid = resmsgid;
  }

  /** @return Stirng */
  public String getMsgid() {
    return msgid;
  }

  /** @param msgid String */
  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  /** @return String */
  public String getErr() {
    return err;
  }

  /** @param err String */
  public void setErr(String err) {
    this.err = err;
  }

  /** @return String */
  public String getStatus() {
    return status;
  }

  /** @param status Stirng */
  public void setStatus(String status) {
    this.status = status;
  }

  /** @return Stirng */
  public String getErrmsg() {
    return errmsg;
  }

  /** @param errmsg Stirng */
  public void setErrmsg(String errmsg) {
    this.errmsg = errmsg;
  }
}
