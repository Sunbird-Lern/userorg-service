package org.sunbird.ratelimit.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.ratelimit.dao.RateLimitDao;
import org.sunbird.ratelimit.dao.RateLimitDaoImpl;
import org.sunbird.ratelimit.limiter.RateLimit;
import org.sunbird.ratelimit.limiter.RateLimiter;

public class RateLimitServiceImpl implements RateLimitService {

  private RateLimitDao rateLimitDao = RateLimitDaoImpl.getInstance();

  public boolean isRateLimitOn() {
    return Boolean.TRUE
        .toString()
        .equalsIgnoreCase(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_RATE_LIMIT_ENABLED));
  }

  @Override
  public void throttleByKey(String key, RateLimiter[] rateLimiters) {
    if (!isRateLimitOn()) {
      ProjectLogger.log(
          "RateLimitServiceImpl:throttleByKey: Rate limiter is disabled", LoggerEnum.INFO);
      return;
    }
    Map<String, RateLimit> entryByRate = new HashMap<>();

    List<Map<String, Object>> ratesByKey = getRatesByKey(key);
    if (CollectionUtils.isNotEmpty(ratesByKey)) {
      ratesByKey
          .stream()
          .forEach(
              rate -> {
                if (!MapUtils.isEmpty(rate)) {
                  ProjectLogger.log(
                      "RateLimitServiceImpl:throttleByKey: key = " + key + " rate =" + rate,
                      LoggerEnum.INFO);
                  RateLimit rateLimit = new RateLimit(key, rate);

                  if (rateLimit.getCount() >= rateLimit.getLimit()) {
                    ProjectLogger.log(
                        "RateLimitServiceImpl:throttleByKey: Rate limit threshold crossed for key = "
                            + key,
                        LoggerEnum.ERROR);
                    throw new ProjectCommonException(
                        ResponseCode.errorRateLimitExceeded.getErrorCode(),
                        ResponseCode.errorRateLimitExceeded.getErrorMessage(),
                        ResponseCode.TOO_MANY_REQUESTS.getResponseCode(),
                        rateLimit.getUnit().toLowerCase());
                  }
                  rateLimit.incrementCount();
                  entryByRate.put(rateLimit.getUnit(), rateLimit);
                }
              });
    }

    Arrays.stream(rateLimiters)
        .forEach(
            rateLimiter -> {
              if (!entryByRate.containsKey(rateLimiter.name())
                  && rateLimiter.getRateLimit() != null) {
                RateLimit rateLimit =
                    new RateLimit(
                        key, rateLimiter.name(), rateLimiter.getRateLimit(), rateLimiter.getTTL());
                ProjectLogger.log(
                    "RateLimitServiceImpl:throttleByKey: Initialise rate limit for key = "
                        + key
                        + " rate ="
                        + rateLimit.getLimit(),
                    LoggerEnum.INFO);
                entryByRate.put(rateLimiter.name(), rateLimit);
              }
            });

    rateLimitDao.insertRateLimits(new ArrayList<>(entryByRate.values()));
  }

  private List<Map<String, Object>> getRatesByKey(String key) {
    return rateLimitDao.getRateLimits(key);
  }

}
