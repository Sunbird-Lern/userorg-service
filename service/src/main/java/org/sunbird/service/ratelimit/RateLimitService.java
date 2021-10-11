package org.sunbird.service.ratelimit;

import org.sunbird.request.RequestContext;
import org.sunbird.util.ratelimit.RateLimiter;

public interface RateLimitService {

  /**
   * Throttle requests by key as per given rate limiters.
   *
   * @param key Key (e.g. phone number, email address)
   * @param rateLimiters List of rate limiters
   * @param context
   */
  void throttleByKey(String key, String type, RateLimiter[] rateLimiters, RequestContext context);
}
