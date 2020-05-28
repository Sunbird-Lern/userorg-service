package org.sunbird.ratelimit.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

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
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.ratelimit.dao.RateLimitDao;
import org.sunbird.ratelimit.limiter.OtpRateLimiter;
import org.sunbird.ratelimit.limiter.RateLimit;
import org.sunbird.ratelimit.limiter.RateLimiter;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class RateLimitServiceTest {

  private static final String KEY = "9999888898";
  private static final int HOUR_LIMIT = 10;

  @InjectMocks private RateLimitService rateLimitService = new RateLimitServiceImpl();

  @Mock private RateLimitDao rateLimitdDao;

  private RateLimiter hourRateLimiter = OtpRateLimiter.HOUR;

  private RateLimiter dayRateLimiter = OtpRateLimiter.DAY;

  @Before
  public void beforeEachTest() {
    MockitoAnnotations.initMocks(this);
    doNothing().when(rateLimitdDao).insertRateLimits(anyList());
  }

  @Test
  public void testThrottleByKeyOnGoingSuccess() {
    when(rateLimitdDao.getRateLimits(anyString())).thenReturn(getRateLimitRecords(5));
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 6);
    assertRateLimitsOnInsert(countsByRateLimiter);
    rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter});
  }

  @Test
  public void testThrottleByKeyNew() {
    when(rateLimitdDao.getRateLimits(anyString())).thenReturn(null);
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 1);
    assertRateLimitsOnInsert(countsByRateLimiter);
    rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter});
  }

  @Test
  public void testThrottleByKeyMultipleLimit() {
    when(rateLimitdDao.getRateLimits(anyString())).thenReturn(getRateLimitRecords(5));
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 6);
    countsByRateLimiter.put(dayRateLimiter.name(), 1);
    assertRateLimitsOnInsert(countsByRateLimiter);
    rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter, dayRateLimiter});
  }

  @Test(expected = ProjectCommonException.class)
  public void testThrottleByKeyFailure() {
    when(rateLimitdDao.getRateLimits(anyString())).thenReturn(getRateLimitRecords(HOUR_LIMIT));
    try {
      rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter});
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.TOO_MANY_REQUESTS.getResponseCode(), e.getResponseCode());
      throw e;
    }
  }

  private List<Map<String, Object>> getRateLimitRecords(int count) {
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put(JsonKey.KEY, KEY);
    record.put(JsonKey.RATE_LIMIT_UNIT, OtpRateLimiter.HOUR.name());
    record.put(JsonKey.RATE, HOUR_LIMIT);
    record.put(JsonKey.TTL, 3500);
    record.put(JsonKey.COUNT, count);
    results.add(record);
    return results;
  }

  private void assertRateLimitsOnInsert(Map<String, Integer> countsByRateLimiter) {
    doAnswer(
            (Answer)
                invocation -> {
                  List<RateLimit> rateLimits = invocation.getArgumentAt(0, List.class);
                  assertTrue(CollectionUtils.isNotEmpty(rateLimits));
                  assertSame(countsByRateLimiter.size(), rateLimits.size());
                  rateLimits.forEach(
                      rateLimit -> {
                        assertSame(
                            countsByRateLimiter.get(rateLimit.getUnit()), rateLimit.getCount());
                      });
                  return null;
                })
        .when(rateLimitdDao)
        .insertRateLimits(anyList());
  }
}
