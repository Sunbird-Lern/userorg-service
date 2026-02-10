package org.sunbird.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Request parameter details.
 * 
 */
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

  /** @return the uid */
  public String getUid() {
    return uid;
  }

  /** @param uid the uid to set */
  public void setUid(String uid) {
    this.uid = uid;
  }

  /** @return the did */
  public String getDid() {
    return did;
  }

  /** @param did the did to set */
  public void setDid(String did) {
    this.did = did;
  }

  /** @return the key */
  public String getKey() {
    return key;
  }

  /** @param key the key to set */
  public void setKey(String key) {
    this.key = key;
  }

  /** @return the msgid */
  public String getMsgid() {
    return msgid;
  }

  /** @param msgid the msgid to set */
  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  /** @return the cid */
  public String getCid() {
    return cid;
  }

  /** @param cid the cid to set */
  public void setCid(String cid) {
    this.cid = cid;
  }

  /** @return the sid */
  public String getSid() {
    return sid;
  }

  /** @param sid the sid to set */
  public void setSid(String sid) {
    this.sid = sid;
  }
}
