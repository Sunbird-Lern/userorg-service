package org.sunbird.ratelimit.service;

import org.sunbird.ratelimit.limiter.RateLimiter;

public interface RateLimitService {

  /**
   * Throttle requests by key as per given rate limiters.
   *
   * @param key Key (e.g. phone number, email address)
   * @param rateLimiters List of rate limiters
   */
  void throttleByKey(String key, RateLimiter[] rateLimiters);

}
