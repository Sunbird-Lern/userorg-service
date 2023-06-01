package org.sunbird.util;

/** DataSecurityLevelsEnum provides all the levels of data security */
public enum DataSecurityLevelsEnum {
  PLAIN_DATASET(1),
  PASSWORD_PROTECTED_DATASET(2),
  TEXT_KEY_ENCRYPTED_DATASET(3),
  PUBLIC_KEY_ENCRYPTED_DATASET(4);

  private int dataSecurityLevelValue;

  DataSecurityLevelsEnum(int dataSecurityLevelValue) {
    this.dataSecurityLevelValue = dataSecurityLevelValue;
  }

  public int getDataSecurityLevelValue() {
    return dataSecurityLevelValue;
  }
}
