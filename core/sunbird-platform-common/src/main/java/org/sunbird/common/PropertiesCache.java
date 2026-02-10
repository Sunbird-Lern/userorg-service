package org.sunbird.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.logging.LoggerUtil;

/**
 * Singleton class to load and manage application configuration properties.
 * Reads attributes from multiple property files and provides access validation/defaults.
 * 
 * @author Amit Kumar
 */
public class PropertiesCache {

  private static final LoggerUtil logger = new LoggerUtil(PropertiesCache.class);
  private final String[] fileName = {
    "elasticsearch.config.properties",
    "cassandra.config.properties",
    "dbconfig.properties",
    "externalresource.properties",
    "sso.properties",
    "userencryption.properties",
    "profilecompleteness.properties",
    "mailTemplates.properties"
  };
  private final Properties configProp = new Properties();
  public final Map<String, Float> attributePercentageMap = new ConcurrentHashMap<>();
  private static volatile PropertiesCache propertiesCache = null;

  /**
   * Private constructor to load properties from files.
   * Also initializes weighted attributes for profile completeness.
   */
  private PropertiesCache() {
    for (String file : fileName) {
      try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(file)) {
        if (in != null) {
          configProp.load(in);
        } else {
          logger.warn("PropertiesCache: Configuration file not found: " + file, null);
        }
      } catch (IOException e) {
        logger.error("PropertiesCache: Error loading file: " + file, e);
      }
    }
    loadWeighted();
  }

  /**
   * Returns the singleton instance of PropertiesCache.
   * Uses double-checked locking for thread safety.
   *
   * @return The singleton PropertiesCache instance.
   */
  public static PropertiesCache getInstance() {
    if (propertiesCache == null) {
      synchronized (PropertiesCache.class) {
        if (propertiesCache == null) {
          propertiesCache = new PropertiesCache();
        }
      }
    }
    return propertiesCache;
  }

  /**
   * Saves or updates a configuration property in memory.
   *
   * @param key   The property key.
   * @param value The property value.
   */
  public void saveConfigProperty(String key, String value) {
    configProp.setProperty(key, value);
  }

  /**
   * Retrieves a property value.
   * Checks system environment variables first, then the loaded properties.
   * If the value is not found in properties, returns the key itself.
   *
   * @param key The property key to look up.
   * @return The property value or the key if not found.
   */
  public String getProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) {
      return value;
    }
    return configProp.getProperty(key) != null ? configProp.getProperty(key) : key;
  }

  /**
   * Loads weighted attributes for user profile completeness from configuration.
   * Parses 'user.profile.attribute' and 'user.profile.weighted' properties.
   */
  private void loadWeighted() {
    String key = configProp.getProperty("user.profile.attribute");
    String value = configProp.getProperty("user.profile.weighted");
    
    if (StringUtils.isBlank(key)) {
      logger.info("PropertiesCache:loadWeighted: Profile completeness value is not set.");
      return;
    }

    String[] keys = key.split(",");
    
    if (StringUtils.isNotBlank(value)) {
      String[] values = value.split(",");
      if (keys.length == values.length) {
        logger.info("PropertiesCache:loadWeighted: Weighted value is provided by user.");
        for (int i = 0; i < keys.length; i++) {
          try {
            attributePercentageMap.put(keys[i], Float.valueOf(values[i]));
          } catch (NumberFormatException e) {
            logger.error("PropertiesCache:loadWeighted: Invalid float value for key: " + keys[i], e);
          }
        }
        return;
      }
    }

    // Fallback: equally divide weight if values are missing or mismatched
    logger.info("PropertiesCache:loadWeighted: Weighted value is not provided or mismatched. Distributing equally.");
    float perc = 100.0f / keys.length;
    for (String k : keys) {
      attributePercentageMap.put(k, perc);
    }
  }

  /**
   * Reads a property value from system environment or loaded properties.
   * Unlike getProperty, this returns null if key is not found (instead of returning the key).
   *
   * @param key The property key.
   * @return The property value, or null if not found.
   */
  public String readProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) {
      return value;
    }
    return configProp.getProperty(key);
  }
}
