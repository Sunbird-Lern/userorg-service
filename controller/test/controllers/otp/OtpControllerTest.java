package controllers.otp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.pekko.http.javadsl.model.HttpMethods;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@Ignore
public class OtpControllerTest extends BaseApplicationTest {

  private static final String VALID_EMAIL = "someEmail@someDomain.com";
  private static final String INVALID_EMAIL = "someEmailWithoutAnyDomain";
  private static final String INVALID_PHONE = "invalidPhone";
  private static final String VALID_PHONE_TYPE = "phone";
  private static final String VALID_EMAIL_TYPE = "email";
  private static final String INVALID_TYPE = "invalidType";
  private static final String INVALID_OTP = "anyOtp";
  private static final String GENERATE_OTP_URL = "/v1/otp/generate";
  private static final String VERIFY_OTP_URL = "/v1/otp/verify";

  @Before
  public void before() {
    setup(ACTORS.OTP_ACTOR, DummyActor.class);
  }

  @Test
  public void testGenerateOtpFailureWithoutPhoneKey() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(false, null, true, VALID_PHONE_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithInvalidPhone() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(true, INVALID_PHONE, true, VALID_PHONE_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithoutEmailKey() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(false, null, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithInvalidEmail() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(true, INVALID_EMAIL, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGenerateOtpFailureWithInvalidType() {
    Result result =
        performTest(
            GENERATE_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(true, VALID_EMAIL, true, INVALID_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithoutPhoneKey() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(false, null, true, VALID_PHONE_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidPhone() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(
                true, INVALID_PHONE, true, VALID_PHONE_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithoutEmailKey() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(false, null, true, VALID_EMAIL_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidEmail() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(
                true, INVALID_EMAIL, true, VALID_EMAIL_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithoutOtp() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(true, VALID_EMAIL, true, VALID_EMAIL_TYPE, false, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidType() {
    Result result =
        performTest(
            VERIFY_OTP_URL,
            HttpMethods.POST.name(),
            createInvalidOtpRequest(true, VALID_EMAIL, true, INVALID_TYPE, true, INVALID_OTP));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
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

  public Result performTest(String url, String method, Map map) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (StringUtils.isNotBlank(data)) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    // req.headers(new Http.Headers(headerMap));
    Result result = Helpers.route(application, req);
    return result;
  }

  public String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);
      ResponseParams params = response.getParams();
      if (result.status() != 200) {
        return response.getResponseCode().name();
      } else {
        return params.getStatus();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }
}
