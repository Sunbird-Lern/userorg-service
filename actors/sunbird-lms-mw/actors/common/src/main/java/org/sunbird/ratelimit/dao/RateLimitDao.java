package org.sunbird.ratelimit.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.ratelimit.limiter.RateLimit;

public interface RateLimitDao {

  /**
   * Inserts one or more rate limits for throttling
   *
   * @param rateLimits List of rate limits
   */
  void insertRateLimits(List<RateLimit> rateLimits);

  /**
   * Fetches list of rate limits for given (partition) key
   *
   * @param key Partition key (e.g. phone number, email address)
   * @return List of rate limits for given key
   */
  List<Map<String, Object>> getRateLimits(String key);
}
