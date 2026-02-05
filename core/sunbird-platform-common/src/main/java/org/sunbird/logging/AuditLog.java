package org.sunbird.logging;

import java.util.Map;

/**
 * Represents an audit log entry for tracking operations within the system.
 * Captures details such as the user, operation type, object affected, and timestamp.
 */
public class AuditLog {

  private String requestId;
  private String objectId;
  private String objectType;
  private String operationType;
  /** Format: yyyy-MM-dd HH:mm:ss */
  private String date;
  private String userId;
  private Map<String, Object> logRecord;

  /**
   * Gets the unique request identifier.
   *
   * @return The request ID.
   */
  public String getRequestId() {
    return requestId;
  }

  /**
   * Sets the unique request identifier.
   *
   * @param requestId The request ID to set.
   */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /**
   * Gets the ID of the object being operated on.
   *
   * @return The object ID.
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * Sets the ID of the object being operated on.
   *
   * @param objectId The object ID to set.
   */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /**
   * Gets the type of the object (e.g., "User", "Course").
   *
   * @return The object type.
   */
  public String getObjectType() {
    return objectType;
  }

  /**
   * Sets the type of the object.
   *
   * @param objectType The object type to set.
   */
  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  /**
   * Gets the type of operation performed (e.g., "Create", "Update").
   *
   * @return The operation type.
   */
  public String getOperationType() {
    return operationType;
  }

  /**
   * Sets the type of operation performed.
   *
   * @param operationType The operation type to set.
   */
  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  /**
   * Gets the timestamp of the operation.
   *
   * @return The date string.
   */
  public String getDate() {
    return date;
  }

  /**
   * Sets the timestamp of the operation.
   *
   * @param date The date string to set.
   */
  public void setDate(String date) {
    this.date = date;
  }

  /**
   * Gets the ID of the user performing the operation.
   *
   * @return The user ID.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the ID of the user performing the operation.
   *
   * @param userId The user ID to set.
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Gets the detailed record of the changes or operation data.
   *
   * @return A map containing log details.
   */
  public Map<String, Object> getLogRecord() {
    return logRecord;
  }

  /**
   * Sets the detailed record of the changes or operation data.
   *
   * @param logRecord The map of log details to set.
   */
  public void setLogRecord(Map<String, Object> logRecord) {
    this.logRecord = logRecord;
  }
}
