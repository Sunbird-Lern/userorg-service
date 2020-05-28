package org.sunbird.ratelimit.limiter;

public interface RateLimiter {

  Integer getRateLimit();

  int getTTL();

  String name();
}
