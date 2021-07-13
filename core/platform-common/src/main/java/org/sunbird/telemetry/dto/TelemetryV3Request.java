package org.sunbird.common.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Created by arvind on 23/3/18. */
public class TelemetryV3Request implements Serializable {

  private String id;
  private String ver;
  private Long ets;
  private Params params;

  private List<Map<String, Object>> events = new ArrayList<>();

  public TelemetryV3Request() {
    params = new Params();
  }

  class Params implements Serializable {

    private String did;
    private String key;
    private String msgid;

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
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVer() {
    return ver;
  }

  public void setVer(String ver) {
    this.ver = ver;
  }

  public Long getEts() {
    return ets;
  }

  public void setEts(Long ets) {
    this.ets = ets;
  }

  public Params getParams() {
    return params;
  }

  public void setParams(Params params) {
    this.params = params;
  }

  public List<Map<String, Object>> getEvents() {
    return events;
  }

  public void setEvents(List<Map<String, Object>> events) {
    this.events = events;
  }
}
