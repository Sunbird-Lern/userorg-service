package org.sunbird.learner.util;

import org.sunbird.common.models.util.JsonKey;

/**
 * UserFlagEnum provides all the flags of user type
 * It contains flagtype and corresponding value in decimal format
 * value should be bit enabled and equivalent to decimal format.
 * If any flag is added, please add value as 2 pow (position-1)
 */
public enum UserFlagEnum {
  PHONE_VERIFIED(JsonKey.PHONE_VERIFIED,1),
  EMAIL_VERIFIED(JsonKey.EMAIL_VERIFIED,2),
  STATE_VALIDATED(JsonKey.STATE_VALIDATED,4);

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
