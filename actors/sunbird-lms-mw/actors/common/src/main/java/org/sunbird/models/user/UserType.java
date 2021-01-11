package org.sunbird.models.user;

public enum UserType {
  TEACHER("teacher"),
  ADMINISTRATOR("administrator"),
  GUARDIAN("guardian"),
  STUDENT("student");

  // Todo to be removed
  private String typeName;

  private UserType(String name) {
    this.typeName = name;
  }

  public String getTypeName() {
    return typeName;
  }
}
