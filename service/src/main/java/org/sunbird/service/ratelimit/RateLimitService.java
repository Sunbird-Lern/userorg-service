package org.sunbird.service.ratelimit;

import org.sunbird.util.ratelimit.RateLimiter;
import org.sunbird.request.RequestContext;

public interface RateLimitService {

  /**
   * Throttle requests by key as per given rate limiters.
   *
   * @param key Key (e.g. phone number, email address)
   * @param rateLimiters List of rate limiters
   * @param context
   */
  void throttleByKey(String key, RateLimiter[] rateLimiters, RequestContext context);
}
