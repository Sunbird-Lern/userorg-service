package org.sunbird.cache.interfaces;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cache interface defines the contract for caching operations in the Sunbird platform.
 * It provides methods for getting, putting, and clearing cache entries, as well as
 * managing cache map expiry.
 */
public interface Cache {
  
  /** Configuration file name for cache settings. */
  String CACHE_CONFIG_FILE = "cache.conf";

  /**
   * Reads the cache configuration from the specified config file.
   *
   * @return A map of configuration keys and values.
   */
  default Map<String, String> readConfig() {
    Map<String, String> properties = new HashMap<>();

    Config config = ConfigFactory.load(CACHE_CONFIG_FILE);

    Set<Map.Entry<String, ConfigValue>> configEntrySet = config.entrySet();

    for (Map.Entry<String, ConfigValue> configEntry : configEntrySet) {
      properties.put(configEntry.getKey(), configEntry.getValue().unwrapped().toString());
    }

    return properties;
  }

  /**
   * Retrieves a string value from the cache.
   *
   * @param mapName The name of the cache map.
   * @param key The key to look up.
   * @return The cached string value, or null if not found.
   */
  String get(String mapName, String key);

  /**
   * Retrieves an object from the cache and deserializes it to the specified class.
   *
   * @param mapName The name of the cache map.
   * @param key The key to look up.
   * @param cls The class to deserialize the value to.
   * @return The cached object, or null if not found.
   */
  Object get(String mapName, String key, Class<?> cls);

  /**
   * Puts a value into the cache.
   *
   * @param mapName The name of the cache map.
   * @param key The key.
   * @param value The value to cache.
   * @return True if successful, false otherwise.
   */
  boolean put(String mapName, String key, Object value);

  /**
   * Clears all entries in the specified cache map.
   *
   * @param mapName The name of the cache map.
   * @return True if successful, false otherwise.
   */
  boolean clear(String mapName);

  /** Clears all cache maps. */
  void clearAll();

  /**
   * Sets the expiry time for a cache map.
   *
   * @param name The name of the cache map.
   * @param seconds The expiry time in seconds.
   * @return True if successful, false otherwise.
   */
  boolean setMapExpiry(String name, long seconds);
}
