package org.sunbird.actor.user.validator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;

public class UserCreateRequestValidatorTest {

  @Test
  public void testValidateLocationCodesDataType() {
    List locationCodes = new ArrayList<String>();
    locationCodes.add("loc1");
    UserCreateRequestValidator.validateLocationCodesDataType(locationCodes);
  }

  @Test
  public void testValidateLocationCodesDataTypeWithEmpty() {
    String locationCodes = new String();
    try {
      UserCreateRequestValidator.validateLocationCodesDataType(locationCodes);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getErrorResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getErrorCode());
    }
  }

  @Test
  public void testIsValidLocationType() {
    List locationCodes = new ArrayList<String>();
    locationCodes.add("type");
    UserCreateRequestValidator.isValidLocationType("type", locationCodes);
  }

  @Test
  public void testValidateAndGetStateLocationCode() {
    Location location = new Location();
    location.setType("state");
    location.setCode("stateCode");
    List<Location> locList = new ArrayList<>();
    locList.add(location);
    UserCreateRequestValidator.validateAndGetStateLocationCode(locList);
  }

  @Test
  public void testValidateAndGetStateWithEmptyStateCode() {
    Location location = new Location();
    location.setType("state");
    List<Location> locList = new ArrayList<>();
    locList.add(location);
    try {
      UserCreateRequestValidator.validateAndGetStateLocationCode(locList);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), e.getErrorCode());
    }
  }

  @Test
  public void testValidatePrimaryAndRecoveryKeys() {
    Map<String, Object> userReqMap = new HashMap<>();
    userReqMap.put(JsonKey.PHONE, "8888888888");
    userReqMap.put(JsonKey.EMAIL, "dummy@org.com");
    userReqMap.put(JsonKey.RECOVERY_EMAIL, "dummy1@org.com");
    userReqMap.put(JsonKey.RECOVERY_PHONE, "8888888889");
    UserCreateRequestValidator.validatePrimaryAndRecoveryKeys(userReqMap);
  }

  @Test
  public void testValidatePrimaryAndRecoveryFailedCase() {
    Map<String, Object> userReqMap = new HashMap<>();
    userReqMap.put(JsonKey.PHONE, "8888888888");
    userReqMap.put(JsonKey.EMAIL, "dummy@org.com");
    userReqMap.put(JsonKey.RECOVERY_EMAIL, "dummy@org.com");
    try {
      UserCreateRequestValidator.validatePrimaryAndRecoveryKeys(userReqMap);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getErrorResponseCode());
      assertEquals(ResponseCode.recoveryParamsMatchException.getErrorCode(), e.getErrorCode());
    }
  }

  @Test
  public void testValidatePrimaryEmailOrPhone() {
    Map<String, Object> userReqMap = new HashMap<>();
    Map<String, Object> dbUserReqMap = new HashMap<>();
    userReqMap.put(JsonKey.PHONE, "8888888888");
    userReqMap.put(JsonKey.EMAIL, "dummy@org.com");
    dbUserReqMap.put(JsonKey.RECOVERY_EMAIL, "dummy1@org.com");
    dbUserReqMap.put(JsonKey.RECOVERY_PHONE, "8888888889");
    UserCreateRequestValidator.validatePrimaryEmailOrPhone(dbUserReqMap, userReqMap);
  }

  @Test
  public void testValidatePrimaryEmailOrPhoneWithError() {
    Map<String, Object> userReqMap = new HashMap<>();
    Map<String, Object> dbUserReqMap = new HashMap<>();
    userReqMap.put(JsonKey.PHONE, "8888888888");
    userReqMap.put(JsonKey.EMAIL, "dummy@org.com");
    dbUserReqMap.put(JsonKey.RECOVERY_EMAIL, "dummy1@org.com");
    dbUserReqMap.put(JsonKey.RECOVERY_PHONE, "8888888888");
    try {
      UserCreateRequestValidator.validatePrimaryEmailOrPhone(dbUserReqMap, userReqMap);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getErrorResponseCode());
      assertEquals(ResponseCode.recoveryParamsMatchException.getErrorCode(), e.getErrorCode());
    }
  }
}
