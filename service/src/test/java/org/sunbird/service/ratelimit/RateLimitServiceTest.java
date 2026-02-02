package org.sunbird.service.ratelimit;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ratelimit.OtpRateLimiter;
import org.sunbird.util.ratelimit.RateLimiter;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class})
public class RateLimitServiceTest {

  private static final String KEY = "9999888898";
  private static final int HOUR_LIMIT = 10;
  private RateLimiter hourRateLimiter = OtpRateLimiter.HOUR;
  private RateLimiter dayRateLimiter = OtpRateLimiter.DAY;
  private static CassandraOperation cassandraOperationImpl = null;

  @BeforeClass
  public static void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
  }

  @Test
  public void testThrottleByKeyOnGoingSuccess() {
    RateLimitService rateLimitService = new RateLimitServiceImpl();
    PowerMockito.when(
            cassandraOperationImpl.getRecordsByIdsWithSpecifiedColumnsAndTTL(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any()))
        .then((Answer) invocation -> getRateLimitRecords(5));
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 6);
    rateLimitService.throttleByKey(KEY, JsonKey.PHONE, new RateLimiter[] {hourRateLimiter}, null);
    Assert.assertNotNull(rateLimitService);
  }

  @Test
  public void testThrottleByKeyNew() {
    RateLimitService rateLimitService = new RateLimitServiceImpl();
    PowerMockito.when(
            cassandraOperationImpl.getRecordsByIdsWithSpecifiedColumnsAndTTL(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any()))
        .then((Answer) invocation -> new Response());
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 1);
    rateLimitService.throttleByKey(
        KEY, JsonKey.PHONE, new RateLimiter[] {hourRateLimiter}, new RequestContext());
    Assert.assertNotNull(rateLimitService);
  }

  @Test
  public void testThrottleByKeyMultipleLimit() {
    RateLimitService rateLimitService = new RateLimitServiceImpl();
    PowerMockito.when(
            cassandraOperationImpl.getRecordsByIdsWithSpecifiedColumnsAndTTL(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any()))
        .then((Answer) invocation -> getRateLimitRecords(5));
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 6);
    countsByRateLimiter.put(dayRateLimiter.name(), 1);
    rateLimitService.throttleByKey(
        KEY, JsonKey.PHONE, new RateLimiter[] {hourRateLimiter, dayRateLimiter}, null);
    Assert.assertNotNull(rateLimitService);
  }

  @Test(expected = ProjectCommonException.class)
  public void testThrottleByKeyFailure() {
    RateLimitService rateLimitService = new RateLimitServiceImpl();
    PowerMockito.when(
            cassandraOperationImpl.getRecordsByIdsWithSpecifiedColumnsAndTTL(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any()))
        .then((Answer) invocation -> getRateLimitRecords(HOUR_LIMIT));
    try {
      rateLimitService.throttleByKey(KEY, JsonKey.PHONE, new RateLimiter[] {hourRateLimiter}, null);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.TOO_MANY_REQUESTS.getResponseCode(), e.getErrorResponseCode());
      throw e;
    }
  }

  private Response getRateLimitRecords(int count) {
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put(JsonKey.KEY, KEY);
    record.put(JsonKey.RATE_LIMIT_UNIT, OtpRateLimiter.HOUR.name());
    record.put(JsonKey.RATE, HOUR_LIMIT);
    record.put(JsonKey.TTL, 3500);
    record.put(JsonKey.COUNT, count);
    results.add(record);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, results);
    return response;
  }
}
