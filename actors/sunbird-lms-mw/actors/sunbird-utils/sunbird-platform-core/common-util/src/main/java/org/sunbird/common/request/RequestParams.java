package org.sunbird.common.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/** @author rayulu */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestParams implements Serializable {

  private static final long serialVersionUID = -759588115950763188L;

  private String did;
  private String key;
  private String msgid;
  private String uid;
  private String cid;
  private String sid;
  private String authToken;

  /** @return the authToken */
  public String getAuthToken() {
    return authToken;
  }

  /** @param authToken the authToken to set */
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getDid() {
    return did;
  }

  public void setDid(String did) {
    this.did = did;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getMsgid() {
    return msgid;
  }

  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  public String getCid() {
    return cid;
  }

  public void setCid(String cid) {
    this.cid = cid;
  }

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }
}
