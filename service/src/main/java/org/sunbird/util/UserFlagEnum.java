package org.sunbird.util;

import org.sunbird.keys.JsonKey;

/**
 * UserFlagEnum provides all the flags of user type It contains flagtype and corresponding value in
 * decimal format value should be bit enabled and equivalent to decimal format. If any flag is
 * added, please add value as 2 pow (position-1)
 */
public enum UserFlagEnum {
  STATE_VALIDATED(JsonKey.STATE_VALIDATED, 4);

  private String userFlagType;
  private int userFlagValue;

  UserFlagEnum(String userFlagType, int userFlagValue) {
    this.userFlagType = userFlagType;
    this.userFlagValue = userFlagValue;
  }

  public int getUserFlagValue() {
    return userFlagValue;
  }

  public String getUserFlagType() {
    return userFlagType;
  }
}
