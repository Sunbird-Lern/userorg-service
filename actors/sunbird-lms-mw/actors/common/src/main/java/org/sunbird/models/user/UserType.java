package org.sunbird.models.user;

public enum UserType {
  TEACHER("Teacher"),
  ADMINISTRATOR("Administrator"),
  GUARDIAN("Guardian"),
  STUDENT("Student");

  // Todo to be removed
  private String typeName;

  private UserType(String name) {
    this.typeName = name;
  }

  public String getTypeName() {
    return typeName;
  }
}
