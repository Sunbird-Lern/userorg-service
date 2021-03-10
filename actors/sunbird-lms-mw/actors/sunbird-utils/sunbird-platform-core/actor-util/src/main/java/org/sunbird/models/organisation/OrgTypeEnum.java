package org.sunbird.models.organisation;

public enum OrgTypeEnum {
  BOARD("board"),
  CONTENT("content"),
  SCHOOL("school");

  private String orgType;

  OrgTypeEnum(String orgType) {
    this.orgType = orgType;
  }

  public String getType() {
    return this.orgType;
  }
}
