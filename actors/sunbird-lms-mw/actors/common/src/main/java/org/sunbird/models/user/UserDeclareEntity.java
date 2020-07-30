package org.sunbird.models.user;

import java.sql.Timestamp;
import java.util.Map;

public class UserDeclareEntity {
  private String operation;
  private String userId;
  private String orgId;
  private String persona;
  private Map<String, Object> userInfo;
  private String status;
  private String errorType;
  private Timestamp createdOn;
  private String createdBy;
  private Timestamp updatedOn;
  private String updatedBy;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public String getPersona() {
    return persona;
  }

  public void setPersona(String persona) {
    this.persona = persona;
  }

  public Map<String, Object> getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(Map<String, Object> userInfo) {
    this.userInfo = userInfo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public Timestamp getCreatedOn() {
    return createdOn;
  }

  public Timestamp getUpdatedOn() {
    return updatedOn;
  }

  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public void setUpdatedOn(Timestamp updatedOn) {
    this.updatedOn = updatedOn;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }
}
