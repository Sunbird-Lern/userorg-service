package org.sunbird.common.models.util;

import java.util.Map;

public class AuditLog {

  private String requestId;
  private String objectId;
  private String objectType;
  private String operationType;
  private String date;
  private String userId;
  private Map<String, Object> logRecord;

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public String getOperationType() {
    return operationType;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Map<String, Object> getLogRecord() {
    return logRecord;
  }

  public void setLogRecord(Map<String, Object> logRecord) {
    this.logRecord = logRecord;
  }
}
