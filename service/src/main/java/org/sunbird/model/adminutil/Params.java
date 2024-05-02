package org.sunbird.model.adminutil;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class Params implements Serializable {
  private static final long serialVersionUID = -8580469966189743283L;

  @JsonProperty("did")
  private String did;

  @JsonProperty("key")
  private String key;

  @JsonProperty("msgid")
  private String msgid;

  /** No args constructor for use in serialization */
  public Params() {}

  /**
   * @param msgid
   * @param did
   * @param key
   */
  public Params(String did, String key, String msgid) {
    super();
    this.did = did;
    this.key = key;
    this.msgid = msgid;
  }

  @JsonProperty("did")
  public String getDid() {
    return did;
  }

  @JsonProperty("did")
  public void setDid(String did) {
    this.did = did;
  }

  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(String key) {
    this.key = key;
  }

  @JsonProperty("msgid")
  public String getMsgid() {
    return msgid;
  }

  @JsonProperty("msgid")
  public void setMsgid(String msgid) {
    this.msgid = msgid;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("did", did)
        .append("key", key)
        .append("msgid", msgid)
        .toString();
  }
}
