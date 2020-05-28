package org.sunbird.ratelimit.dao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.ratelimit.limiter.OtpRateLimiter;
import org.sunbird.ratelimit.limiter.RateLimit;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class RateLimitDaoTest {

  private static final String KEY = "9999888898";
  private static final int HOUR_LIMIT = 10;

  @InjectMocks private RateLimitDao rateLimitdDao = RateLimitDaoImpl.getInstance();

  @Mock private CassandraOperation cassandraOperation;

  @Before
  public void beforeEachTest() {
    MockitoAnnotations.initMocks(this);
    when(cassandraOperation.batchInsertWithTTL(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyList()))
        .thenReturn(getSuccessResponse());
  }

  @Test
  public void testInsertRateLimitsSuccess() {
    doAnswer(
            (Answer)
                invocation -> {
                  List<Map<String, Object>> rateLimits = invocation.getArgumentAt(2, List.class);
                  assertTrue(CollectionUtils.isNotEmpty(rateLimits));
                  assertSame(1, rateLimits.size());
                  assertSame(1, rateLimits.get(0).get(JsonKey.COUNT));
                  return null;
                })
        .when(cassandraOperation)
        .batchInsertWithTTL(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyList());
    rateLimitdDao.insertRateLimits(getRateLimits());
  }

  @Test(expected = ProjectCommonException.class)
  public void testInsertRateLimitsFailureWithInvalidData() {
    try {
      rateLimitdDao.insertRateLimits(getInvalidRateLimits());
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), e.getResponseCode());
      throw e;
    }
  }

  @Test
  public void testGetRateLimitsSuccess() {
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumnsAndTTL(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyList(),
            Mockito.any()))
        .then(
            (Answer)
                invocation -> {
                  return getRateLimitRecords();
                });

    List<Map<String, Object>> results = rateLimitdDao.getRateLimits(KEY);
    assertTrue(CollectionUtils.isNotEmpty(results));
    assertSame(KEY, results.get(0).get(JsonKey.KEY));
    assertSame(5, results.get(0).get(JsonKey.COUNT));
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
