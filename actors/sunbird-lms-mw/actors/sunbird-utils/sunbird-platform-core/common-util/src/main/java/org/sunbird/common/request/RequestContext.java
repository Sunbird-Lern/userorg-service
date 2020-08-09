package org.sunbird.common.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class RequestContext {
  private String uid;
  private String did;
  private String sid;
  private String appId;
  private String appVer;
  private String reqId;
  private String logLevel;
  private String actorOperation;
  private final String pid = "learner-service";

  public String getReqId() {
    return reqId;
  }

  public void setReqId(String reqId) {
    this.reqId = reqId;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public String getActorOperation() {
    return actorOperation;
  }

  public void setActorOperation(String actorOperation) {
    this.actorOperation = actorOperation;
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

  public Map<String, Object> toMap() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> reqMap = mapper.convertValue(this, Map.class);
    reqMap.remove("logLevel");
    reqMap.keySet().removeIf(key -> null == reqMap.get(key));
    return reqMap;
  }
}
