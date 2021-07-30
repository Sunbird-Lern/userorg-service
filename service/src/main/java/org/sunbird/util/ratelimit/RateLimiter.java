package org.sunbird.util.ratelimit;

public interface RateLimiter {

  Integer getRateLimit();

  int getTTL();

  String name();
}
