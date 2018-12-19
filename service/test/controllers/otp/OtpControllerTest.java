package controllers.otp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.http.HttpMethods;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

@Ignore
public class OtpControllerTest extends BaseControllerTest {

  private static final String VALID_EMAIL = "someEmail@someDomain.com";
  private static final String INVALID_EMAIL = "someEmailWithoutAnyDomain";
  private static final String INVALID_PHONE = "invalidPhone";
  private static final String VALID_PHONE_TYPE = "phone";
  private static final String VALID_EMAIL_TYPE = "email";
  private static final String INVALID_TYPE = "invalidType";
  private static final String INVALID_OTP = "anyOtp";
  private static final String GENERATE_OTP_URL = "/v1/otp/generate";
  private static final String VERIFY_OTP_URL = "/v1/otp/verify";

  // Generate Otp test case

  @Test
  public void testGenerateOtpWithoutPhoneKeyFailure() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_PHONE_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithInvalidPhoneFailure() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, INVALID_PHONE, true, VALID_PHONE_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithoutEmailKeyFailure() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithInVALID_EMAILFailure() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, INVALID_EMAIL, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.emailFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithInvalidTypeFailure() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, VALID_EMAIL, true, INVALID_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  // VerifyOtptestCase
  @Test
  public void testVerifyOtpWithoutPhoneKeyFailure() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_PHONE_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithInvalidPhoneFailure() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(
                true, INVALID_PHONE, true, VALID_PHONE_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithoutEmailKeyFailure() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_EMAIL_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithInVALID_EMAILFailure() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(
                true, INVALID_EMAIL, true, VALID_EMAIL_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.emailFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithoutOtpFailure() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, VALID_EMAIL, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithInvalidTypeFailure() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, VALID_EMAIL, true, INVALID_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createInvalidOtpRequest(
      boolean isKeyRequired,
      String key,
      boolean isTypeRequired,
      String type,
      boolean isOtpRequired,
      String otp) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isKeyRequired) {
      innerMap.put(JsonKey.KEY, key);
    }
    if (isTypeRequired) {
      innerMap.put(JsonKey.TYPE, type);
    }
    if (isOtpRequired) {
      innerMap.put(JsonKey.OTP, otp);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
