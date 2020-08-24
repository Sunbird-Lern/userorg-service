package org.sunbird.bean;

public class SelfDeclaredUser extends MigrationUser {
  private String schoolName;
  private String schoolId;
  private String userId;
  private String subOrgId;
  private String orgId;
  private String persona;

  private String errorType;

  public String getSubOrgId() {
    return subOrgId;
  }

  public void setSubOrgId(String subOrgId) {
    this.subOrgId = subOrgId;
  }

  public String getSchoolId() {
    return schoolId;
  }

  public void setSchoolId(String schoolId) {
    this.schoolId = schoolId;
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
