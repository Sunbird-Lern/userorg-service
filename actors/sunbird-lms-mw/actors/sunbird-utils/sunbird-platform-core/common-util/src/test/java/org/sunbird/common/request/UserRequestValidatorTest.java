/** */
package org.sunbird.common.request;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UserRequestValidatorTest {

  private static final UserRequestValidator userRequestValidator = new UserRequestValidator();

  @Test
  public void testValidatePasswordFailure() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.PASSWORD, "password");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.passwordValidation.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testIsGoodPassword() {
    HashMap<String, Boolean> passwordExpectations = new HashMap<String, Boolean>(){
      {
        // Bad ones.
        put("Test 1234", false); // space is not a valid char
        put("hello1234", false); // no uppercase
        put("helloABCD", false); // no numeral
        put("hello#$%&'", false); // no uppercase/numeral
        put("sho!1", false); // too short, not 8 char
        put("B1!\"#$%&'()*+,-./:;<=>?@[]^_`{|}~", false); // no lowercase
        put("Test @1234", false); // contains space

        // Good ones.
        put("Test123!", true); // good
        put("ALongPassword@123", true); // more than 8 char
        put("Abc1!\"#$%&'()*+,-./:;<=>?@[]^_`{|}~", true); // with all spl char, PASS
      }
    };

    passwordExpectations.forEach((pwd, expectedResult) -> {
        assertEquals(expectedResult, UserRequestValidator.isGoodPassword(pwd));
    });
  }

  @Test
  public void testValidateCreateUserBasicValidationFailure() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.ROLES, "admin");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateFieldsNotAllowedFailure() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.PROVIDER, "AP");
    request.setRequest(requestObj);
    try {
      userRequestValidator.fieldsNotAllowed(
          Arrays.asList(
              JsonKey.REGISTERED_ORG_ID,
              JsonKey.ROOT_ORG_ID,
              JsonKey.PROVIDER,
              JsonKey.EXTERNAL_ID,
              JsonKey.EXTERNAL_ID_PROVIDER,
              JsonKey.EXTERNAL_ID_TYPE,
              JsonKey.ID_TYPE),
          request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidRequestParameter.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateValidateCreateUserV3RequestSuccess() {
    boolean response = false;
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.PASSWORD, "Password@1");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserV3Request(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidatePasswordSuccess() {
    boolean response = false;
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.PASSWORD, "Password@1");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateUserCreateV3Success() {
    boolean response = false;
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.PASSWORD, "Password@1");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateUserCreateV3(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateUserCreateV3Failure() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.FIRST_NAME, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateUserCreateV3(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateUserNameFailure() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.put(JsonKey.USERNAME, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserV1Request(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateLocationCodesSuccess() {
    boolean response = false;
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    List<String> location = new ArrayList<>();
    location.add("KA");
    location.add("AP");
    requestObj.put(JsonKey.LOCATION_CODES, location);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateLocationCodesFailure() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();

    requestObj.put(JsonKey.LOCATION_CODES, "AP");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateForgotPasswordSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "manzarul07");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateForgotPassword(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateForgotPasswordFailureWithEmptyName() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      requestObj.put(JsonKey.USERNAME, "");
      request.setRequest(requestObj);
      userRequestValidator.validateForgotPassword(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.userNameRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateForgotPasswordFailureWithoutName() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      request.setRequest(requestObj);
      userRequestValidator.validateForgotPassword(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.userNameRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateChangePasswordSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.NEW_PASSWORD, "password1");
    requestObj.put(JsonKey.PASSWORD, "password");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateChangePassword(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateChangePasswordFailureWithEmptyNewPassword() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.NEW_PASSWORD, "");
    requestObj.put(JsonKey.PASSWORD, "password");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateChangePassword(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.newPasswordEmpty.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateChangePasswordFailureWithoutNewPassword() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PASSWORD, "password");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateChangePassword(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.newPasswordRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateChangePasswordFailureWithSameOldPassword() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.NEW_PASSWORD, "password");
    requestObj.put(JsonKey.PASSWORD, "password");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateChangePassword(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.samePasswordError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateChangePasswordFailureWithPasswordMissing() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.NEW_PASSWORD, "password");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateChangePassword(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.passwordRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCreateUserSuccess() {
    boolean response = false;
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    List<String> roles = new ArrayList<String>();
    roles.add("PUBLIC");
    roles.add("CONTENT-CREATOR");
    requestObj.put(JsonKey.ROLE, roles);
    List<String> language = new ArrayList<>();
    language.add("English");
    requestObj.put(JsonKey.LANGUAGE, language);
    List<Map<String, Object>> addressList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_LINE1, "test");
    map.put(JsonKey.CITY, "Bangalore");
    map.put(JsonKey.COUNTRY, "India");
    map.put(JsonKey.ADD_TYPE, "current");
    addressList.add(map);
    requestObj.put(JsonKey.ADDRESS, addressList);

    List<Map<String, Object>> educationList = new ArrayList<>();
    Map<String, Object> map1 = new HashMap<>();
    map1.put(JsonKey.COURSE_NAME, "M.C.A");
    map1.put(JsonKey.DEGREE, "Master");
    map1.put(JsonKey.NAME, "CUSAT");
    educationList.add(map1);
    requestObj.put(JsonKey.EDUCATION, educationList);

    List<Map<String, Object>> jobProfileList = new ArrayList<>();
    map1 = new HashMap<>();
    map1.put(JsonKey.JOB_NAME, "SE");
    map1.put(JsonKey.ORGANISATION_NAME, "Tarento");
    jobProfileList.add(map1);
    requestObj.put(JsonKey.JOB_PROFILE, jobProfileList);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateCreateUserFailureWithWrongAddType() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    List<String> roles = new ArrayList<String>();
    roles.add("PUBLIC");
    roles.add("CONTENT-CREATOR");
    requestObj.put(JsonKey.ROLE, roles);
    List<String> language = new ArrayList<>();
    language.add("English");
    requestObj.put(JsonKey.LANGUAGE, language);
    List<Map<String, Object>> addressList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_LINE1, "test");
    map.put(JsonKey.CITY, "Bangalore");
    map.put(JsonKey.COUNTRY, "India");
    map.put(JsonKey.ADD_TYPE, "lmlkmkl");
    addressList.add(map);
    requestObj.put(JsonKey.ADDRESS, addressList);

    List<Map<String, Object>> educationList = new ArrayList<>();
    Map<String, Object> map1 = new HashMap<>();
    map1.put(JsonKey.COURSE_NAME, "M.C.A");
    map1.put(JsonKey.DEGREE, "Master");
    map1.put(JsonKey.NAME, "CUSAT");
    educationList.add(map1);
    requestObj.put(JsonKey.EDUCATION, educationList);

    List<Map<String, Object>> jobProfileList = new ArrayList<>();
    map1 = new HashMap<>();
    map1.put(JsonKey.JOB_NAME, "SE");
    map1.put(JsonKey.ORGANISATION_NAME, "Tarento");
    jobProfileList.add(map1);
    requestObj.put(JsonKey.JOB_PROFILE, jobProfileList);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.addressTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyAddType() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    List<String> roles = new ArrayList<String>();
    roles.add("PUBLIC");
    roles.add("CONTENT-CREATOR");
    requestObj.put(JsonKey.ROLE, roles);
    List<String> language = new ArrayList<>();
    language.add("English");
    requestObj.put(JsonKey.LANGUAGE, language);
    List<Map<String, Object>> addressList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_LINE1, "test");
    map.put(JsonKey.CITY, "Bangalore");
    map.put(JsonKey.COUNTRY, "India");
    map.put(JsonKey.ADD_TYPE, "");
    addressList.add(map);
    requestObj.put(JsonKey.ADDRESS, addressList);

    List<Map<String, Object>> educationList = new ArrayList<>();
    Map<String, Object> map1 = new HashMap<>();
    map1.put(JsonKey.COURSE_NAME, "M.C.A");
    map1.put(JsonKey.DEGREE, "Master");
    map1.put(JsonKey.NAME, "CUSAT");
    educationList.add(map1);
    requestObj.put(JsonKey.EDUCATION, educationList);

    List<Map<String, Object>> jobProfileList = new ArrayList<>();
    map1 = new HashMap<>();
    map1.put(JsonKey.JOB_NAME, "SE");
    map1.put(JsonKey.ORGANISATION_NAME, "Tarento");
    jobProfileList.add(map1);
    requestObj.put(JsonKey.JOB_PROFILE, jobProfileList);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.addressError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testPhoneValidationSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.COUNTRY_CODE, "+91");
    requestObj.put(JsonKey.PROVIDER, "sunbird");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    request.setRequest(requestObj);
    try {
      userRequestValidator.phoneValidation(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testPhoneValidationFailureWithInvalidPhone() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "+9321234123");
    requestObj.put(JsonKey.COUNTRY_CODE, "+91");
    requestObj.put(JsonKey.PROVIDER, "sunbird");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    request.setRequest(requestObj);
    try {
      userRequestValidator.phoneValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.invalidPhoneNumber.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testPhoneValidationFailureWithInvalidCountryCode() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "+9321234123");
    requestObj.put(JsonKey.COUNTRY_CODE, "+91968");
    requestObj.put(JsonKey.PROVIDER, "sunbird");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    request.setRequest(requestObj);
    try {
      userRequestValidator.phoneValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.invalidCountryCode.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testPhoneValidationFailureWithEmptyPhoneVerified() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.COUNTRY_CODE, "+91");
    requestObj.put(JsonKey.PROVIDER, "sunbird");
    requestObj.put(JsonKey.PHONE_VERIFIED, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.phoneValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.phoneVerifiedError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testPhoneValidationFailureWithPhoneVerifiedFalse() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.COUNTRY_CODE, "+91");
    requestObj.put(JsonKey.PROVIDER, "sunbird");
    requestObj.put(JsonKey.PHONE_VERIFIED, false);
    request.setRequest(requestObj);
    try {
      userRequestValidator.phoneValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.phoneVerifiedError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testPhoneValidationFailureWithPhoneVerifiedNull() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.COUNTRY_CODE, "+91");
    requestObj.put(JsonKey.PROVIDER, "sunbird");
    requestObj.put(JsonKey.PHONE_VERIFIED, null);
    request.setRequest(requestObj);
    try {
      userRequestValidator.phoneValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.phoneVerifiedError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testUpdateUserSuccess() {
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    requestObj.remove(JsonKey.USERNAME);
    requestObj.put(JsonKey.USER_ID, "userId");

    List<String> roles = new ArrayList<String>();
    roles.add("PUBLIC");
    roles.add("CONTENT-CREATOR");
    requestObj.put(JsonKey.ROLE, roles);
    List<String> language = new ArrayList<>();
    language.add("English");
    requestObj.put(JsonKey.LANGUAGE, language);
    List<Map<String, Object>> addressList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_LINE1, "test");
    map.put(JsonKey.CITY, "Bangalore");
    map.put(JsonKey.COUNTRY, "India");
    map.put(JsonKey.ADD_TYPE, "current");
    addressList.add(map);
    requestObj.put(JsonKey.ADDRESS, addressList);

    List<Map<String, Object>> educationList = new ArrayList<>();
    Map<String, Object> map1 = new HashMap<>();
    map1.put(JsonKey.COURSE_NAME, "M.C.A");
    map1.put(JsonKey.DEGREE, "Master");
    map1.put(JsonKey.NAME, "CUSAT");
    educationList.add(map1);
    requestObj.put(JsonKey.EDUCATION, educationList);

    List<Map<String, Object>> jobProfileList = new ArrayList<>();
    map1 = new HashMap<>();
    map1.put(JsonKey.JOB_NAME, "SE");
    map1.put(JsonKey.ORGANISATION_NAME, "Tarento");
    jobProfileList.add(map1);
    requestObj.put(JsonKey.JOB_PROFILE, jobProfileList);
    boolean response = false;
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateUpdateUserRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateUploadUserSuccessWithOrgId() {
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORGANISATION_ID, "ORG-1233");
    requestObj.put(JsonKey.EXTERNAL_ID_PROVIDER, "EXT_ID_PROVIDER");
    requestObj.put(JsonKey.FILE, "EXT_ID_PROVIDER");

    try {
      RequestValidator.validateUploadUser(requestObj);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateUploadUserSuccessWithExternalId() {
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PROVIDER, "ORG-provider");
    requestObj.put(JsonKey.EXTERNAL_ID, "ORG-1233");
    requestObj.put(JsonKey.ORGANISATION_ID, "ORG-1233");
    requestObj.put(JsonKey.ORG_PROVIDER, "ORG-Provider");
    requestObj.put(JsonKey.FILE, "ORG-Provider");
    try {
      RequestValidator.validateUploadUser(requestObj);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateAssignRoleSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "ORG-provider");
    requestObj.put(JsonKey.EXTERNAL_ID, "EXT_ID");
    requestObj.put(JsonKey.ORGANISATION_ID, "ORG_ID");
    requestObj.put(JsonKey.ORG_PROVIDER, "ORG_PROVIDER");
    List<String> roles = new ArrayList<>();
    roles.add("PUBLIC");
    requestObj.put(JsonKey.ROLES, roles);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateAssignRole(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateAssignRoleSuccessWithProviderAndExternalId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PROVIDER, "ORG-provider");
    requestObj.put(JsonKey.EXTERNAL_ID, "ORG-1233");
    requestObj.put(JsonKey.USER_ID, "User1");
    List<String> roles = new ArrayList<>();
    roles.add("PUBLIC");
    requestObj.put(JsonKey.ROLES, roles);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateAssignRole(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void testValidateWebPagesFailureWithEmptyWebPages() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.WEB_PAGES, new ArrayList<>());
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateWebPages(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidWebPageData.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateWebPagesFailureWithNullWebPages() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.WEB_PAGES, null);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateWebPages(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidWebPageData.getErrorCode(), e.getCode());
    }
  }

  @Ignore
  public void testCreateUserBasicValidationFailureWithEmptyFirstName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.FIRST_NAME, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.firstNameRequired.getErrorCode(), e.getCode());
    }
  }

  @Ignore
  public void testCreateUserBasicValidationFailureWithInvalidDOB() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.DOB, "20-10-15");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dateFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCreateUserBasicValidationFailureWithoutEmailAndPhone() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.DOB, "2018-10-15");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailorPhoneRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCreateUserBasicValidationFailureWithInvalidEmail() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.DOB, "2018-10-15");
    requestObj.put(JsonKey.EMAIL, "asd@as");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailFormatError.getErrorCode(), e.getCode());
    }
  }

  @Ignore
  public void testCreateUserBasicValidationFailureWithInvalidRoles() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.ROLES, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserRequestFailureWithInvalidLanguage() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.EMAIL, "test123@test.com");
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.LANGUAGE, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserRequestFailureWithInvalidAddress() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL, "test123@test.com");
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.ADDRESS, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidaeCreateUserRequestFailureWithInvalidEducation() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL, "test123@test.com");
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.EDUCATION, "");
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserRequestFailureWithInvalidAddressType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL, "test123@test.com");
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    List<Map<String, Object>> addressList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS_LINE1, "test");
    map.put(JsonKey.CITY, "Bangalore");
    map.put(JsonKey.COUNTRY, "India");
    map.put(JsonKey.ADD_TYPE, "localr");
    addressList.add(map);
    requestObj.put(JsonKey.ADDRESS, addressList);
    request.setRequest(requestObj);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.addressTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserRequestFailureWithInvalidCountryCode() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.EMAIL, "test123@test.com");
    requestObj.put(JsonKey.EMAIL_VERIFIED, true);
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    request.setRequest(requestObj);
    request.getRequest().put(JsonKey.COUNTRY_CODE, "+as");

    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidCountryCode.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserRequestFailureWithEmptyEmailAndPhone() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.EMAIL, "");
    requestObj.put(JsonKey.PHONE, "");
    request.setRequest(requestObj);

    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailorPhoneRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithInvalidEmail() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.EMAIL, "am@ds@cmo");

    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithoutPhoneVerified() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE, "7894561230");

    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.phoneVerifiedError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserSuccess() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE, "7894561230");
    request.getRequest().put(JsonKey.PHONE_VERIFIED, "");
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.phoneVerifiedError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithPhoneVerifiedFalse() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE, "7894561230");
    request.getRequest().put(JsonKey.PHONE_VERIFIED, false);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.phoneVerifiedError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyEducationName() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE_VERIFIED, true);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);

    request.getRequest().put(JsonKey.EDUCATION, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.educationNameError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyEducationDegree() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE_VERIFIED, true);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "name");
    map.put(JsonKey.DEGREE, "");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);

    request.getRequest().put(JsonKey.EDUCATION, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.educationDegreeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyEducationAddress() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE_VERIFIED, true);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "name");
    map.put(JsonKey.DEGREE, "degree");
    Map<String, Object> address = new HashMap<>();
    address.put(JsonKey.ADDRESS_LINE1, "");
    map.put(JsonKey.ADDRESS, address);
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.EDUCATION, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.addressError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyEducationCity() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE_VERIFIED, true);

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "name");
    map.put(JsonKey.DEGREE, "degree");
    Map<String, Object> address = new HashMap<>();
    address.put(JsonKey.ADDRESS_LINE1, "line1");
    address.put(JsonKey.CITY, "");
    map.put(JsonKey.ADDRESS, address);
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.EDUCATION, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.addressError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyJobProfile() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.PHONE_VERIFIED, true);
    request.getRequest().put(JsonKey.JOB_PROFILE, "");
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyJobName() {
    Request request = initailizeRequest();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.JOB_NAME, "");
    map.put(JsonKey.ORG_NAME, "degree");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.JOB_PROFILE, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.jobNameError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithInvalidJobProfileJoiningDate() {
    Request request = initailizeRequest();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.JOB_NAME, "kijklo");
    map.put(JsonKey.ORG_NAME, "degree");
    map.put(JsonKey.JOINING_DATE, "20-15-18");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.JOB_PROFILE, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dateFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithInvalidJobProfileEndDate() {
    Request request = initailizeRequest();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.JOB_NAME, "kijklo");
    map.put(JsonKey.ORG_NAME, "degree");
    map.put(JsonKey.END_DATE, "20-15-18");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.JOB_PROFILE, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dateFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyJobProfileOrgName() {
    Request request = initailizeRequest();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.JOB_NAME, "kijklo");
    map.put(JsonKey.ORG_NAME, "");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.JOB_PROFILE, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.organisationNameError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithEmptyJobProfileCity() {
    Request request = initailizeRequest();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.JOB_NAME, "jabName");
    map.put(JsonKey.ORG_NAME, "orgName");
    Map<String, Object> address = new HashMap<>();
    address.put(JsonKey.ADDRESS_LINE1, "line1");
    address.put(JsonKey.CITY, "");
    map.put(JsonKey.ADDRESS, address);
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(map);
    request.getRequest().put(JsonKey.JOB_PROFILE, list);
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.addressError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateCreateUserFailureWithInvalidPhoneFormat() {
    Request request = new Request();
    request.getRequest().put(JsonKey.EMAIL, "asd@asd.com");
    request.getRequest().put(JsonKey.EMAIL_VERIFIED, true);
    request.getRequest().put(JsonKey.PHONE, "9874561230");
    request.getRequest().put(JsonKey.COUNTRY_CODE, "+001");
    request.getRequest().put(JsonKey.USERNAME, "98745");
    request.getRequest().put(JsonKey.FIRST_NAME, "98745");
    try {
      userRequestValidator.validateCreateUserRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.phoneNoFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateGerUserCountFailureWithInvalidLocationIds() {
    Request request = new Request();
    request.getRequest().put(JsonKey.LOCATION_IDS, "");

    try {
      RequestValidator.validateGetUserCount(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateGerUserCountFailureWithEmptyLocationIds() {
    Request request = new Request();
    List<String> list = new ArrayList<>();
    list.add("");
    request.getRequest().put(JsonKey.LOCATION_IDS, list);

    try {
      RequestValidator.validateGetUserCount(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.locationIdRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateGerUserCountFailureWithInvalidUserLstReq() {
    Request request = new Request();
    List<String> list = new ArrayList<>();
    list.add("4645");
    request.getRequest().put(JsonKey.LOCATION_IDS, list);
    request.getRequest().put(JsonKey.USER_LIST_REQ, null);

    try {
      RequestValidator.validateGetUserCount(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateGerUserCountFailureWithUserLstReqTrue() {
    Request request = new Request();
    List<String> list = new ArrayList<>();
    list.add("4645");
    request.getRequest().put(JsonKey.LOCATION_IDS, list);
    request.getRequest().put(JsonKey.USER_LIST_REQ, true);

    try {
      RequestValidator.validateGetUserCount(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.functionalityMissing.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateGerUserCountFailureWithEmptyEstCntReq() {
    Request request = new Request();
    List<String> list = new ArrayList<>();
    list.add("4645");
    request.getRequest().put(JsonKey.LOCATION_IDS, list);
    request.getRequest().put(JsonKey.ESTIMATED_COUNT_REQ, "");

    try {
      RequestValidator.validateGetUserCount(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateVerifyUserSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.LOGIN_ID, "username@provider");
    request.setRequest(requestObj);
    boolean response = false;
    try {
      new UserRequestValidator().validateVerifyUser(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateGerUserCountFailureWithEstCntReqTrue() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.LOGIN_ID, "");
    request.setRequest(requestObj);
    boolean response = false;
    try {
      new UserRequestValidator().validateVerifyUser(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.loginIdRequired.getErrorCode(), e.getCode());
    }
    Assert.assertFalse(response);
  }

  @Test
  public void validateUserCreateV3Sussess() {
    boolean response = true;
    try {
      Request request = new Request();
      request.getRequest().put(JsonKey.FIRST_NAME, "test name");
      request.getRequest().put(JsonKey.EMAIL, "test@test.com");
      request.getRequest().put(JsonKey.EMAIL_VERIFIED, true);
      request.getRequest().put(JsonKey.PHONE, "9663890445");
      request.getRequest().put(JsonKey.PHONE_VERIFIED, true);
      new UserRequestValidator().validateUserCreateV3(request);
    } catch (Exception e) {
      response = false;
    }
    Assert.assertTrue(response);
  }

  private Request initailizeRequest() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.PHONE, "9321234123");
    requestObj.put(JsonKey.PHONE_VERIFIED, true);
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    request.setRequest(requestObj);
    return request;
  }

  @Test
  public void testValidateVerifyUserFailureWithEmptyId() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.LOGIN_ID, "");
    request.setRequest(requestObj);
    boolean response = false;
    try {
      userRequestValidator.validateVerifyUser(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.loginIdRequired.getErrorCode(), e.getCode());
    }
    Assert.assertFalse(response);
  }

  @Test
  public void testValidateMandatoryFrameworkFieldsSuccess() {
    Request request = initailizeRequest();
    request.getRequest().put(JsonKey.FRAMEWORK, createFrameWork());
    boolean response = false;
    try {
      new UserRequestValidator()
          .validateMandatoryFrameworkFields(
              request.getRequest(), getSupportedFileds(), getMandatoryFields());
      response = true;
    } catch (Exception e) {
      Assert.assertTrue(response);
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateMandatoryFrameworkFieldValueAsString() {
    Request request = initailizeRequest();
    Map<String, Object> frameworkMap = createFrameWork();
    frameworkMap.put("medium", "hindi");
    request.getRequest().put(JsonKey.FRAMEWORK, frameworkMap);
    boolean response = false;
    try {
      new UserRequestValidator()
          .validateMandatoryFrameworkFields(
              request.getRequest(), getSupportedFileds(), getMandatoryFields());
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
    Assert.assertFalse(response);
  }

  @Test
  public void testValidateFrameworkUnknownField() {
    Request request = initailizeRequest();
    Map<String, Object> frameworkMap = createFrameWork();
    frameworkMap.put("school", Arrays.asList("school1"));
    request.getRequest().put(JsonKey.FRAMEWORK, frameworkMap);
    boolean response = false;
    try {
      new UserRequestValidator()
          .validateMandatoryFrameworkFields(
              request.getRequest(), getSupportedFileds(), getMandatoryFields());
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.errorUnsupportedField.getErrorCode(), e.getCode());
    }
    Assert.assertFalse(response);
  }

  @Test
  public void testValidateFrameworkWithEmptyValue() {
    Request request = initailizeRequest();
    Map<String, Object> frameworkMap = createFrameWork();
    frameworkMap.put("medium", Arrays.asList());
    request.getRequest().put(JsonKey.FRAMEWORK, frameworkMap);
    boolean response = false;
    try {
      new UserRequestValidator()
          .validateMandatoryFrameworkFields(
              request.getRequest(), getSupportedFileds(), getMandatoryFields());
      response = true;
    } catch (Exception e) {
      Assert.assertTrue(response);
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateFrameworkWithNullValue() {
    Request request = initailizeRequest();
    Map<String, Object> frameworkMap = createFrameWork();
    frameworkMap.put("medium", null);
    request.getRequest().put(JsonKey.FRAMEWORK, frameworkMap);

    boolean response = false;
    try {
      new UserRequestValidator()
          .validateMandatoryFrameworkFields(
              request.getRequest(), getSupportedFileds(), getMandatoryFields());
      response = true;
    } catch (Exception e) {
      Assert.assertTrue(response);
    }
    Assert.assertTrue(response);
  }

  private static Map<String, Object> createFrameWork() {
    Map<String, Object> frameworkMap = new HashMap<String, Object>();
    frameworkMap.put("gradeLevel", Arrays.asList("Kindergarten"));
    frameworkMap.put("subject", Arrays.asList("English"));
    frameworkMap.put("id", Arrays.asList("NCF"));
    return frameworkMap;
  }

  private static List<String> getSupportedFileds() {
    List<String> frameworkSupportedFields = new ArrayList<String>();
    frameworkSupportedFields.add("id");
    frameworkSupportedFields.add("gradeLevel");
    frameworkSupportedFields.add("subject");
    frameworkSupportedFields.add("board");
    frameworkSupportedFields.add("medium");
    return frameworkSupportedFields;
  }

  private static List<String> getMandatoryFields() {
    List<String> frameworkMandatoryFields = new ArrayList<String>(1);
    frameworkMandatoryFields.add("id");
    return frameworkMandatoryFields;
  }
}
