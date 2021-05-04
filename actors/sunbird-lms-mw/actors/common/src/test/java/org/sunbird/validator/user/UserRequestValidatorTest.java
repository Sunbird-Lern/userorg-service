/** */
package org.sunbird.validator.user;

import static org.junit.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.request.RequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;

public class UserRequestValidatorTest {

  private static final UserRequestValidator userRequestValidator = new UserRequestValidator();

  @Test
  public void isValidLocationTypeTestSuccess() {
    boolean bool = userRequestValidator.isValidLocationType("state");
    Assert.assertTrue(bool);
  }

  @Test
  public void isValidLocationTypeTestFailure() {
    try {
      userRequestValidator.isValidLocationType("state2");
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

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
    HashMap<String, Boolean> passwordExpectations =
        new HashMap<String, Boolean>() {
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

    passwordExpectations.forEach(
        (pwd, expectedResult) -> {
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
  public void testValidateLocationCodesAsMapFormatWithSuccess() {
    boolean response = false;
    Request request = initailizeRequest();
    Map<String, Object> requestObj = request.getRequest();
    List<Map<String, String>> location = new ArrayList<>();
    Map locationCode1 = new HashMap();
    Map locationCode2 = new HashMap();
    locationCode1.put("type", "state");
    locationCode1.put("code", "KA");
    locationCode2.put("type", "district");
    locationCode2.put("code", "KA-DIS");
    location.add(locationCode1);
    location.add(locationCode2);
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
    requestObj.put(JsonKey.DOB, "20");
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
    requestObj.put(JsonKey.DOB, "2018");
    request.setRequest(requestObj);
    try {
      userRequestValidator.createUserBasicValidation(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailorPhoneorManagedByRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCreateUserBasicValidationFailureWithInvalidEmail() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USERNAME, "test123");
    requestObj.put(JsonKey.FIRST_NAME, "test123");
    requestObj.put(JsonKey.DOB, "2018");
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
      assertEquals(ResponseCode.emailorPhoneorManagedByRequired.getErrorCode(), e.getCode());
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
    requestObj.put(JsonKey.DOB, "2000");
    request.setRequest(requestObj);
    return request;
  }

  private Request initailizeLookupRequest() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.KEY, JsonKey.FIRST_NAME);
    requestObj.put(JsonKey.VALUE, "9321234123");
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

  @Test
  public void testValidateUserDeclarationRequest() {
    Request request = initailizeRequest();
    List<Map<String, Object>> declarations = createUpdateUserDeclarationRequests();
    request.getRequest().put(JsonKey.DECLARATIONS, declarations);
    boolean response = false;
    try {
      new UserRequestValidator().validateUserDeclarationRequest(request);
      response = true;
    } catch (Exception e) {
      Assert.assertTrue(response);
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateUserMissingDeclarationsFieldRequest() {
    Request request = initailizeRequest();
    List<Map<String, Object>> declarations = createUpdateUserDeclarationMissingUserIdRequests();
    request.getRequest().put(JsonKey.DECLARATIONS, declarations);
    boolean response = false;
    try {
      new UserRequestValidator().validateUserDeclarationRequest(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(
          MessageFormat.format(
              ResponseMessage.Message.MISSING_SELF_DECLARED_MANDATORY_PARAMETERS,
              new String[] {JsonKey.USER_ID, JsonKey.ORG_ID, JsonKey.PERSONA}),
          e.getMessage());
      response = true;
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateUserMissingInfoDeclarationsFieldRequest() {
    Request request = initailizeRequest();
    List<Map<String, Object>> declarations = createUpdateUserDeclarationMissingUserInfoIdRequests();
    request.getRequest().put(JsonKey.DECLARATIONS, declarations);
    boolean response = false;
    try {
      new UserRequestValidator().validateUserDeclarationRequest(request);
    } catch (ProjectCommonException e) {
      response = true;
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateUserLookupRequest() {
    Request request = initailizeLookupRequest();
    boolean response = false;
    try {
      new UserRequestValidator().validateUserLookupRequest(request);
    } catch (ProjectCommonException e) {
      response = true;
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateUserType() {
    UserRequestValidator validator = new UserRequestValidator();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.USER_TYPE, "userType");
    try {
      validator.validateUserType(userMap, null, new RequestContext());
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  private List createUpdateUserDeclarationRequests() {
    Map<String, Object> declarationMap = new HashMap<>();
    declarationMap.put(JsonKey.ORG_ID, "1234");
    declarationMap.put(JsonKey.USER_ID, "userid");

    declarationMap.put(JsonKey.PERSONA, JsonKey.TEACHER_PERSONA);
    List<Map<String, Object>> declarations = new ArrayList<>();
    declarations.add(declarationMap);
    return declarations;
  }

  private List createUpdateUserDeclarationMissingUserIdRequests() {
    Map<String, Object> info = new HashMap<>();
    info.put(JsonKey.DECLARED_EMAIL, "email");
    Map<String, Object> declarationMap = new HashMap<>();
    declarationMap.put(JsonKey.ORG_ID, "1234");
    declarationMap.put(JsonKey.PERSONA, JsonKey.TEACHER_PERSONA);
    declarationMap.put(JsonKey.INFO, info);
    List<Map<String, Object>> declarations = new ArrayList<>();
    declarations.add(declarationMap);
    return declarations;
  }

  private List createUpdateUserDeclarationMissingUserInfoIdRequests() {
    Map<String, Object> info = new HashMap<>();
    info.put(JsonKey.DECLARED_EMAIL, null);
    Map<String, Object> declarationMap = new HashMap<>();
    declarationMap.put(JsonKey.USER_ID, "1234");
    declarationMap.put(JsonKey.INFO, info);
    List<Map<String, Object>> declarations = new ArrayList<>();
    declarations.add(declarationMap);
    return declarations;
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
