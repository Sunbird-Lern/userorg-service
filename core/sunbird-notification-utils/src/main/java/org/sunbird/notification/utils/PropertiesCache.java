package org.sunbird.notification.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.sunbird.logging.LoggerUtil;

/**
 * Singleton class that caches properties from the 'configuration.properties' file. Provides methods
 * to retrieve property values by key.
 */
public class PropertiesCache {
  private static final LoggerUtil logger = new LoggerUtil(PropertiesCache.class);
  private final String fileName = "configuration.properties";
  private final Properties configProp = new Properties();
  private static PropertiesCache instance;

  /** Private constructor to initialize the property cache by loading the configuration file. */
  private PropertiesCache() {
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(fileName);
    if (in != null) {
      try {
        configProp.load(in);
      } catch (IOException e) {
        logger.error("Error in properties cache while loading " + fileName, e);
      }
    } else {
      logger.info("Configuration file " + fileName + " not found in classpath.");
    }
  }

  /**
   * Returns the singleton instance of {@link PropertiesCache}.
   *
   * @return The singleton instance.
   */
  public static PropertiesCache getInstance() {
    synchronized (PropertiesCache.class) {
      if (instance == null) {
        instance = new PropertiesCache();
      }
    }
    return instance;
  }

  /**
   * Retrieves a property value by its key.
   *
   * @param key The property key.
   * @return The property value, or null if not found.
   */
  public String getProperty(String key) {
    return configProp.getProperty(key);
  }

  /**
   * Reads a property value by its key. (Alias for {@link #getProperty(String)}).
   *
   * @param key The property key.
   * @return The property value.
   */
  public String readProperty(String key) {
    return configProp.getProperty(key);
  }
}
