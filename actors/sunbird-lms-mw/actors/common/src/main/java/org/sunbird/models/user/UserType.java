package org.sunbird.models.user;

public enum UserType {
  TEACHER("TEACHER"),
  OTHER("OTHER");

  private String typeName;

  private UserType(String name) {
    this.typeName = name;
  }

  public String getTypeName() {
    return typeName;
  }
}
