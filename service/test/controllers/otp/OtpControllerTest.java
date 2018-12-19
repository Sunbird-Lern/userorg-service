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
  public void testGenerateOtpFailureWithoutPhoneKey() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_PHONE_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithInvalidPhone() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, INVALID_PHONE, true, VALID_PHONE_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithoutEmailKey() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithInvalidEmail() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, INVALID_EMAIL, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.emailFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithInvalidType() {
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
  public void testVerifyOtpFailureWithoutPhoneKey() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_PHONE_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidPhone() {
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
  public void testVerifyOtpFailureWithoutEmailKey() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(false, null, true, VALID_EMAIL_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidEmail() {
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
  public void testVerifyOtpFailureWithoutOtp() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST,
            createInvalidOtpRequest(true, VALID_EMAIL, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidType() {
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
