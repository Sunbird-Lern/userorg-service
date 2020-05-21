package org.sunbird.common.models.util;

/**
 * Enum contains the database related constants
 *
 * @author arvind
 */
public enum DbConstant {
  sunbirdKeyspaceName("sunbird"),
  userTableName("user");

  DbConstant(String value) {
    this.value = value;
  }

  String value;

  public String getValue() {
    return this.value;
  }
}
