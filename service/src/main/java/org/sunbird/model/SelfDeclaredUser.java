package org.sunbird.model;

public class SelfDeclaredUser extends MigrationUser {
  private String schoolName;
  private String subOrgExternalId;
  private String userId;
  private String orgId;
  private String persona;
  private String errorType;

  public String getSubOrgExternalId() {
    return subOrgExternalId;
  }

  public void setSubOrgExternalId(String subOrgExternalId) {
    this.subOrgExternalId = subOrgExternalId;
  }

  public String getSchoolName() {
    return schoolName;
  }

  public void setSchoolName(String schoolName) {
    this.schoolName = schoolName;
  }

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

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }
}
