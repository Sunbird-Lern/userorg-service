package org.sunbird.util;


/** DataSecurityLevelsEnum provides all the levels of data security */
public enum DataSecurityLevelsEnum {
  L1(1),
  L2(2),
  L3(3),
  L4(4);

  private int dataSecurityLevelValue;

  DataSecurityLevelsEnum(int dataSecurityLevelValue) {
    this.dataSecurityLevelValue = dataSecurityLevelValue;
  }

  public int getDataSecurityLevelValue() {
    return dataSecurityLevelValue;
  }
}
