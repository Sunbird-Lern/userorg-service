package org.sunbird.actor.otp;

import java.time.Duration;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.ratelimit.RateLimitDao;
import org.sunbird.dao.ratelimit.RateLimitDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.ClientErrorResponse;
import org.sunbird.response.Response;
import org.sunbird.util.ratelimit.OtpRateLimiter;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  RateLimitDaoImpl.class,
  RateLimitDao.class,
  SendOTPActor.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class OTPActorTest {

  private TestKit probe;
  private ActorRef subject;

  private static final ActorSystem system = ActorSystem.create("system");
  private static final CassandraOperationImpl mockCassandraOperation =
      mock(CassandraOperationImpl.class);
  private static final Props props = Props.create(OTPActor.class);
  private static final String PHONE_TYPE = "phone";
  private static final String EMAIL_TYPE = "email";
  private static final String PHONE_KEY = "0000000000";
  private static final String EMAIL_KEY = "someEmail@someDomain.anything";
  private static final String REQUEST_OTP = "000000";
  private static final String INVALID_OTP = "111111";
  private static final String USER_ID = "user123";

  @BeforeClass
  public static void before() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(mockCassandraOperation);
    PowerMockito.mock(SendOTPActor.class);
  }

  @Before
  public void beforeEachTestCase() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(mockCassandraOperation);
    probe = new TestKit(system);
    subject = system.actorOf(props);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidPhoneOtp() {
    Response mockedCassandraResponse =
        getMockCassandraRecordByIdSuccessResponse(PHONE_KEY, PHONE_TYPE, INVALID_OTP);
    verifyOtpFailureTest(true, mockedCassandraResponse);
  }

  @Test
  public void testVerifyOtpFailureWithInvalidEmailOtp() {
    Response mockedCassandraResponse =
        getMockCassandraRecordByIdSuccessResponse(EMAIL_KEY, EMAIL_TYPE, INVALID_OTP);
    verifyOtpFailureTest(false, mockedCassandraResponse);
  }

  @Test
  public void testVerifyOtpFailureWithExpiredOtp() {
    Response mockedCassandraResponse = getMockCassandraRecordByIdFailureResponse();
    verifyOtpFailureWithExpiredOtp(false, mockedCassandraResponse);
  }

  @Test
  public void testVerifyOtpSuccessWithPhoneOtp() {
    Response mockedCassandraResponse =
        getMockCassandraRecordByIdSuccessResponse(PHONE_KEY, PHONE_TYPE, REQUEST_OTP);
    verifyOtpSuccessTest(true, mockedCassandraResponse);
  }

  @Test
  public void testVerifyOtpSuccessWithEmailOtp() {
    Response mockedCassandraResponse =
        getMockCassandraRecordByIdSuccessResponse(EMAIL_KEY, EMAIL_TYPE, REQUEST_OTP);
    verifyOtpSuccessTest(false, mockedCassandraResponse);
  }

  @Test
  public void testVerifyOtpSuccessWithEmailOtp2() {
    Response mockedCassandraResponse =
        getMockCassandraRecordByIdSuccessResponse(EMAIL_KEY, EMAIL_TYPE, REQUEST_OTP);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) mockedCassandraResponse.getResult().get(JsonKey.RESPONSE);
    responseList.get(0).put(JsonKey.EMAIL, "xyz@xyz.com");
    Request request = createRequestForVerifyOtp(EMAIL_KEY, EMAIL_TYPE);
    request.getRequest().put(JsonKey.USER_ID, "123123-546-4566");
    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void testVerifyOtpFailureWithEmailOtp3() {
    Response mockedCassandraResponse =
        getMockCassandraRecordByIdSuccessResponse(EMAIL_KEY, EMAIL_TYPE, REQUEST_OTP);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) mockedCassandraResponse.getResult().get(JsonKey.RESPONSE);
    responseList.get(0).put(JsonKey.EMAIL, "xyz@xyz.com");
    Request request = createRequestForVerifyOtp(EMAIL_KEY, EMAIL_TYPE);
    request.getRequest().clear();
    request.getRequest().put(JsonKey.OTP, "");
    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
    Assert.assertEquals(
        exception.getErrorCode(), "UOS_OTPVERFY" + ResponseCode.errorInvalidOTP.getErrorCode());
  }

  @Test
  public void testWithInvalidRequest() {
    Request request = new Request();
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  private Request createRequestForVerifyOtp(String key, String type) {
    Request request = new Request();
    request.setOperation(ActorOperations.VERIFY_OTP.getValue());
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.TYPE, type);
    innerMap.put(JsonKey.KEY, key);
    innerMap.put(JsonKey.OTP, REQUEST_OTP);
    request.setRequest(innerMap);
    return request;
  }

  private void verifyOtpSuccessTest(boolean isPhone, Response mockedCassandraResponse) {
    Request request;
    if (isPhone) {
      request = createRequestForVerifyOtp(PHONE_KEY, PHONE_TYPE);
    } else {
      request = createRequestForVerifyOtp(EMAIL_KEY, EMAIL_TYPE);
    }
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
  }

  private void verifyOtpFailureWithExpiredOtp(boolean isPhone, Response mockedCassandraResponse) {
    Request request;
    if (isPhone) {
      request = createRequestForVerifyOtp(PHONE_KEY, PHONE_TYPE);
    } else {
      request = createRequestForVerifyOtp(EMAIL_KEY, EMAIL_TYPE);
    }
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getErrorCode()
            .equals("UOS_OTPVERFY" + ResponseCode.errorInvalidOTP.getErrorCode()));
  }

  private void verifyOtpFailureTest(boolean isPhone, Response mockedCassandraResponse) {
    Request request;
    if (isPhone) {
      request = createRequestForVerifyOtp(PHONE_KEY, PHONE_TYPE);
    } else {
      request = createRequestForVerifyOtp(EMAIL_KEY, EMAIL_TYPE);
    }
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    subject.tell(request, probe.getRef());
    ClientErrorResponse errorResponse =
        probe.expectMsgClass(Duration.ofSeconds(100), ClientErrorResponse.class);
    Assert.assertTrue(
        (errorResponse.getResponseCode().name()).equals(ResponseCode.CLIENT_ERROR.name()));
  }

  @Test
  public void generateOtpForPhoneSuccess2() {
    Request request;
    request = createGenerateOtpRequest(PHONE_TYPE, PHONE_KEY);
    when(mockCassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(getRateLimitRecords(5));

    Response mockedCassandraResponse = new Response();
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void generateOtpForPhoneSuccess() {
    Request request;
    request = createGenerateOtpRequest(PHONE_TYPE, PHONE_KEY);
    when(mockCassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(getRateLimitRecords(5));

    Response mockedCassandraResponse = getCassandraRecordByIdForUserResponse();
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
  }

  @Test
  public void generateOtpForEmailSuccess() {
    Request request;
    request = createGenerateOtpRequest(EMAIL_TYPE, EMAIL_KEY);
    when(mockCassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(getRateLimitRecords(5));
    Response mockedCassandraResponse = getCassandraRecordByIdForUserResponse();
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
  }

  @Test
  public void generateOtpForEmailSuccessForUser() {
    Request request;
    request = createOtpRequest(EMAIL_TYPE, EMAIL_KEY, USER_ID);
    when(mockCassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(getRateLimitRecords(5));
    Response mockedCassandraResponse = getCassandraRecordByIdForUserResponse();
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
  }

  @Test
  public void generateOtpForInvalidType() {
    Request request;
    request = createGenerateOtpRequest("InvalidType", "InvalidType");
    when(mockCassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(getRateLimitRecords(5));
    Response mockedCassandraResponse = getCassandraRecordByIdForUserResponse();
    when(mockCassandraOperation.getRecordWithTTLById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(mockedCassandraResponse);
    when(mockCassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
  }

  private Response getRateLimitRecords(int count) {
    Response response = new Response();
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put(JsonKey.KEY, "9999888898");
    record.put(JsonKey.RATE_LIMIT_UNIT, OtpRateLimiter.HOUR.name());
    record.put(JsonKey.RATE, 10);
    record.put(JsonKey.TTL, 3500);
    record.put(JsonKey.COUNT, count);
    results.add(record);
    response.put(JsonKey.RESPONSE, results);
    return response;
  }

  private Response getCassandraRecordByIdForUserResponse() {
    Response response = new Response();
    List<Map<String, Object>> userList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, USER_ID);
    map.put(JsonKey.CHANNEL, "anyChannel");
    map.put(JsonKey.EMAIL, EMAIL_KEY);
    map.put(JsonKey.OTP, REQUEST_OTP);
    userList.add(map);
    response.put(JsonKey.RESPONSE, userList);
    return response;
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Request createGenerateOtpRequest(String type, String key) {
    Request request = new Request();
    request.setOperation(ActorOperations.GENERATE_OTP.getValue());
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.TYPE, type);
    innerMap.put(JsonKey.KEY, key);
    request.setRequest(innerMap);
    return request;
  }

  private Request createOtpRequest(String type, String key, String userId) {
    Request request = new Request();
    request.setOperation(ActorOperations.GENERATE_OTP.getValue());
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.TYPE, type);
    innerMap.put(JsonKey.KEY, key);
    innerMap.put(JsonKey.USER_ID, userId);
    request.setRequest(innerMap);
    return request;
  }

  private Response getMockCassandraRecordByIdSuccessResponse(String key, String type, String otp) {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> otpResponse = new HashMap<>();
    otpResponse.put(JsonKey.OTP, otp);
    otpResponse.put(JsonKey.TYPE, type);
    otpResponse.put(JsonKey.KEY, key);
    otpResponse.put(JsonKey.ATTEMPTED_COUNT, 0);
    otpResponse.put("otp_ttl", 120);
    list.add(otpResponse);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response getMockCassandraRecordByIdFailureResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
