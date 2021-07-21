package org.sunbird.ratelimit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

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
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.ratelimit.dao.RateLimitDao;
import org.sunbird.ratelimit.limiter.OtpRateLimiter;
import org.sunbird.ratelimit.limiter.RateLimit;
import org.sunbird.ratelimit.limiter.RateLimiter;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
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
    doNothing().when(rateLimitdDao).insertRateLimits(anyList(), Mockito.any());
  }

  @Test
  public void testThrottleByKeyOnGoingSuccess() {
    when(rateLimitdDao.getRateLimits(anyString(), Mockito.any()))
        .thenReturn(getRateLimitRecords(5));
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 6);
    assertRateLimitsOnInsert(countsByRateLimiter);
    rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter}, null);
  }

  @Test
  public void testThrottleByKeyNew() {
    when(rateLimitdDao.getRateLimits(anyString(), Mockito.any())).thenReturn(null);
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 1);
    assertRateLimitsOnInsert(countsByRateLimiter);
    rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter}, null);
  }

  @Test
  public void testThrottleByKeyMultipleLimit() {
    when(rateLimitdDao.getRateLimits(anyString(), Mockito.any()))
        .thenReturn(getRateLimitRecords(5));
    Map<String, Integer> countsByRateLimiter = new HashMap<>();
    countsByRateLimiter.put(hourRateLimiter.name(), 6);
    countsByRateLimiter.put(dayRateLimiter.name(), 1);
    assertRateLimitsOnInsert(countsByRateLimiter);
    rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter, dayRateLimiter}, null);
  }

  @Test(expected = ProjectCommonException.class)
  public void testThrottleByKeyFailure() {
    when(rateLimitdDao.getRateLimits(anyString(), Mockito.any()))
        .thenReturn(getRateLimitRecords(HOUR_LIMIT));
    try {
      rateLimitService.throttleByKey(KEY, new RateLimiter[] {hourRateLimiter}, null);
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
                  List<RateLimit> rateLimits = invocation.getArgument(0);
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
        .insertRateLimits(anyList(), Mockito.any());
  }
}
