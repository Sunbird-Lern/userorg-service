package org.sunbird.common.request;

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
  private final String pid = "learner-service";
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

    contextMap.putAll(
        new HashMap<String, Object>() {
          {
            put("uid", uid);
            put("did", did);
            put("sid", sid);
            put("appId", appId);
            put("appVer", appVer);
            put("reqId", reqId);
            put("op", op);
          }
        });
  }

  public String getReqId() {
    return reqId;
  }

  public String getActorOperation() {
    return op;
  }

  public void setActorOperation(String actorOperation) {
    this.op = actorOperation;
  }

  public String getUid() {
    return uid;
  }

  public String getDid() {
    return did;
  }

  public String getSid() {
    return sid;
  }

  public String getAppId() {
    return appId;
  }

  public String getAppVer() {
    return appVer;
  }

  public String getDebugEnabled() {
    return debugEnabled;
  }

  public String getOp() {
    return op;
  }

  public Map<String, Object> getContextMap() {
    return contextMap;
  }
}
