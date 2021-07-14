package org.sunbird.request;

import java.util.HashMap;
import java.util.Map;

public class RequestContext {
  private String uid;
  private String did;
  private String sid;
  private String appId;
  private String appVer;
  private String reqId;
  private String debugEnabled;
  private String op;
  private Map<String, Object> contextMap = new HashMap<>();

  public RequestContext() {}

  public RequestContext(
      String uid,
      String did,
      String sid,
      String appId,
      String appVer,
      String reqId,
      String debugEnabled,
      String op) {
    super();
    this.uid = uid;
    this.did = did;
    this.sid = sid;
    this.appId = appId;
    this.appVer = appVer;
    this.reqId = reqId;
    this.debugEnabled = debugEnabled;
    this.op = op;

    contextMap.put("uid", uid);
    contextMap.put("did", did);
    contextMap.put("sid", sid);
    contextMap.put("appId", appId);
    contextMap.put("appVer", appVer);
    contextMap.put("reqId", reqId);
    contextMap.put("op", op);
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

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAppVer() {
    return appVer;
  }

  public void setAppVer(String appVer) {
    this.appVer = appVer;
  }

  public String getReqId() {
    return reqId;
  }

  public void setReqId(String reqId) {
    this.reqId = reqId;
  }

  public String getDebugEnabled() {
    return debugEnabled;
  }

  public void setDebugEnabled(String debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public Map<String, Object> getContextMap() {
    return contextMap;
  }
}
