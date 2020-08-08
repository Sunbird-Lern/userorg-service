package org.sunbird.common.request;

public class RequestContext {

  private String traceId;
  private String logLevel;
  private String actorOperation;

  public RequestContext() {}

  public RequestContext(String traceId, String logLevel, String actorOperation) {
    this.traceId = traceId;
    this.logLevel = logLevel;
    this.actorOperation = actorOperation;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
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
}
