package org.sunbird.cache;

import org.sunbird.cache.interfaces.Cache;
import org.sunbird.redis.RedisCache;

/**
 * CacheFactory is a factory class for obtaining instances of {@link Cache}. Currently, it provides
 * a singleton instance of {@link RedisCache}.
 */
public class CacheFactory {

  private static Cache cache = null;

  /** Private constructor to prevent instantiation. */
  private CacheFactory() {}

  /**
   * Returns a singleton instance of the configured Cache implementation.
   *
   * @return A Cache instance.
   */
  public static Cache getInstance() {
    if (null == cache) {
      cache = new RedisCache();
    }
    return cache;
  }
}
