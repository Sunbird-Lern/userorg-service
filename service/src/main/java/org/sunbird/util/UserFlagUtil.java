package org.sunbird.util;

import java.util.HashMap;
import java.util.Map;
import org.sunbird.keys.JsonKey;

public class UserFlagUtil {

  /**
   * This method return int value of the boolean flag
   *
   * @param userFlagType
   * @param flagEnabled
   * @return
   */
  public static int getFlagValue(String userFlagType, boolean flagEnabled) {
    int decimalValue = 0;
    if (userFlagType.equals(UserFlagEnum.STATE_VALIDATED.getUserFlagType()) && flagEnabled) {
      // if user is state-validated flag should be true then only return flagvalue
      decimalValue = UserFlagEnum.STATE_VALIDATED.getUserFlagValue();
    }
    return decimalValue;
  }

  /**
   * This method returns boolean flags of user for the flagValue
   *
   * @param flagsValue
   * @return
   */
  public static Map<String, Boolean> assignUserFlagValues(int flagsValue) {
    Map<String, Boolean> userFlagMap = new HashMap<>();
    setDefaultValues(userFlagMap);
    if ((flagsValue >= UserFlagEnum.STATE_VALIDATED.getUserFlagValue())) {
      userFlagMap.put(UserFlagEnum.STATE_VALIDATED.getUserFlagType(), true);
    }
    return userFlagMap;
  }

  private static void setDefaultValues(Map<String, Boolean> userFlagMap) {
    userFlagMap.put(JsonKey.STATE_VALIDATED, false);
  }
}
