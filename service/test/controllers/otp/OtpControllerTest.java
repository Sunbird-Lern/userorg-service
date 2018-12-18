package controllers.otp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

@Ignore
public class OtpControllerTest extends BaseControllerTest {

  private static String validEmail = "someEmail@someDomain.com";
  private static String invalidEmail = "someEmailWithoutAnyDomain";
  private static String invalidPhone = "i73456";
  private static String validPhoneType = "phone";
  private static String validEmailType = "email";
  private static String invalidType = "invalidType";
  private static String otp = "123456";

  // Generate Otp test case

  @Test
  public void testGenerateOtpWithoutPhoneKeyFailure() {
    Result result =
        performTest(
            "/v1/otp/generate",
            "POST",
            createInvalidOtpRequest(false, null, true, validPhoneType, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithInvalidPhoneFailure() {
    Result result =
        performTest(
            "/v1/otp/generate",
            "POST",
            createInvalidOtpRequest(true, invalidPhone, true, validPhoneType, false, null));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithoutEmailKeyFailure() {
    Result result =
        performTest(
            "/v1/otp/generate",
            "POST",
            createInvalidOtpRequest(false, null, true, validEmailType, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithInvalidEmailFailure() {
    Result result =
        performTest(
            "/v1/otp/generate",
            "POST",
            createInvalidOtpRequest(true, invalidEmail, true, validEmailType, false, null));
    assertEquals(getResponseCode(result), ResponseCode.emailFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpWithInvalidTypeFailure() {
    Result result =
        performTest(
            "/v1/otp/generate",
            "POST",
            createInvalidOtpRequest(true, validEmail, true, invalidType, false, null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  // VerifyOtptestCase
  @Test
  public void testVerifyOtpWithoutPhoneKeyFailure() {
    Result result =
        performTest(
            "/v1/otp/verify",
            "POST",
            createInvalidOtpRequest(false, null, true, validPhoneType, true, otp));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithInvalidPhoneFailure() {
    Result result =
        performTest(
            "/v1/otp/verify",
            "POST",
            createInvalidOtpRequest(true, invalidPhone, true, validPhoneType, true, otp));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithoutEmailKeyFailure() {
    Result result =
        performTest(
            "/v1/otp/verify",
            "POST",
            createInvalidOtpRequest(false, null, true, validEmailType, true, otp));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithInvalidEmailFailure() {
    Result result =
        performTest(
            "/v1/otp/verify",
            "POST",
            createInvalidOtpRequest(true, invalidEmail, true, validEmailType, true, otp));
    assertEquals(getResponseCode(result), ResponseCode.emailFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithoutOtpFailure() {
    Result result =
        performTest(
            "/v1/otp/verify",
            "POST",
            createInvalidOtpRequest(true, validEmail, true, validEmailType, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpWithInvalidTypeFailure() {
    Result result =
        performTest(
            "/v1/otp/verify",
            "POST",
            createInvalidOtpRequest(true, validEmail, true, invalidType, true, otp));
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
