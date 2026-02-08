package org.sunbird.redis;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.sunbird.cache.interfaces.Cache;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.notification.utils.JsonUtil;

/**
 * RedisCache implementation of the {@link Cache} interface using Redisson. This class handles cache
 * operations for various maps defined in the configuration.
 */
public class RedisCache implements Cache {
  private static final String CACHE_MAP_LIST = "cache.mapNames";
  private final Map<String, String> properties = readConfig();
  private final String[] mapNameList = properties.get(CACHE_MAP_LIST).split(",");
  private final RedissonClient client;
  private final LoggerUtil logger = new LoggerUtil(RedisCache.class);

  /** Initializes the Redisson client. */
  public RedisCache() {
    client = RedisConnectionManager.getClient();
  }

  @Override
  public String get(String mapName, String key) {
    try {
      RMap<String, String> map = client.getMap(mapName);
      return map.get(key);
    } catch (Exception e) {
      logger.error(
          "RedisCache:get: Error occurred for mapName = " + mapName + ", key = " + key,
          e);
    }
    return null;
  }

  @Override
  public boolean clear(String mapName) {
    logger.info( "RedisCache:clear: mapName = " + mapName);
    try {
      RMap<String, String> map = client.getMap(mapName);
      map.clear();
      return true;
    } catch (Exception e) {
      logger.error(
          "RedisCache:clear: Error occurred for mapName = " + mapName, e);
    }
    return false;
  }

  @Override
  public void clearAll() {
    logger.info( "RedisCache: clearAll called");
    for (String mapName : mapNameList) {
      clear(mapName);
    }
  }

  @Override
  public boolean setMapExpiry(String name, long seconds) {
    boolean result = client.getMap(name).expire(seconds, TimeUnit.SECONDS);
    logger.info(
        "RedisCache:setMapExpiry: name = "
            + name
            + ", seconds = "
            + seconds
            + ", result = "
            + result);
    return result;
  }

  @Override
  public boolean put(String mapName, String key, Object value) {
    logger.info(
        "RedisCache:put: mapName = " + mapName + ", key = " + key);
    try {
      String res;
      if (value instanceof String) {
        res = (String) value;
      } else {
        res = JsonUtil.toJson(value);
      }
      RMap<String, String> map = client.getMap(mapName);
      map.put(key, res);
      return true;
    } catch (Exception e) {
      logger.error(
          "RedisCache:put: Error occurred for mapName = "
              + mapName
              + ", key = "
              + key,
          e);
    }
    return false;
  }

  @Override
  public Object get(String mapName, String key, Class<?> cls) {
    try {
      RMap<String, String> map = client.getMap(mapName);
      String s = map.get(key);
      return JsonUtil.getAsObject(s, cls);
    } catch (Exception e) {
      logger.error(
          "RedisCache:get: Error occurred for mapName = " + mapName + ", key = " + key,
          e);
    }
    return null;
  }
}
