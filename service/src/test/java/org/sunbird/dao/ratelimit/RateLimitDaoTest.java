package org.sunbird.dao.ratelimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
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
import org.sunbird.response.Response;
import org.sunbird.util.ratelimit.OtpRateLimiter;
import org.sunbird.util.ratelimit.RateLimit;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class})
public class RateLimitDaoTest {

  private static final String KEY = "9999888898";
  private static final int HOUR_LIMIT = 10;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.batchInsertWithTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(getSuccessResponse());

    doAnswer(
            (Answer)
                invocation -> {
                  List<Map<String, Object>> rateLimits = invocation.getArgument(2);
                  assertTrue(CollectionUtils.isNotEmpty(rateLimits));
                  assertSame(1, rateLimits.size());
                  assertSame(1, rateLimits.get(0).get(JsonKey.COUNT));
                  return null;
                })
        .when(cassandraOperationImpl)
        .batchInsertWithTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyList(),
            Mockito.any());

    when(cassandraOperationImpl.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyList(),
            Mockito.any(),
            Mockito.any()))
        .then((Answer) invocation -> getRateLimitRecords());
  }

  @Test
  public void testInsertRateLimitsSuccess() {
    RateLimitDao rateLimitDao = RateLimitDaoImpl.getInstance();
    rateLimitDao.insertRateLimits(getRateLimits(), null);
    Assert.assertNotNull(rateLimitDao);
  }

  @Test(expected = ProjectCommonException.class)
  public void testInsertRateLimitsFailureWithInvalidData() {
    RateLimitDao rateLimitDao = RateLimitDaoImpl.getInstance();
    try {
      rateLimitDao.insertRateLimits(getInvalidRateLimits(), null);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), e.getErrorResponseCode());
      throw e;
    }
  }

  @Test
  public void testGetRateLimitsSuccess() {
    RateLimitDao rateLimitDao = RateLimitDaoImpl.getInstance();
    List<Map<String, Object>> results = rateLimitDao.getRateLimits(KEY, null);
    assertTrue(CollectionUtils.isNotEmpty(results));
  }

  private List<RateLimit> getRateLimits() {
    List<RateLimit> rateLimits = new ArrayList<>();
    rateLimits.add(
        new RateLimit(KEY, OtpRateLimiter.HOUR.name(), 20, OtpRateLimiter.HOUR.getTTL()));
    return rateLimits;
  }

  private List<RateLimit> getInvalidRateLimits() {
    List<RateLimit> rateLimits = new ArrayList<>();
    rateLimits.add(new RateLimit(KEY, null, 0, OtpRateLimiter.HOUR.getTTL()));
    return rateLimits;
  }

  private Response getRateLimitRecords() {
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put(JsonKey.KEY, KEY);
    record.put(JsonKey.RATE_LIMIT_UNIT, OtpRateLimiter.HOUR.name());
    record.put(JsonKey.RATE, HOUR_LIMIT);
    record.put(JsonKey.TTL, 3500);
    record.put(JsonKey.COUNT, 5);
    results.add(record);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, results);
    return response;
  }

  private Response getSuccessResponse() {
    Response response = new Response();
    return response;
  }
}
