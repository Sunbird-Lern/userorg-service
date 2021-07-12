package org.sunbird.notification.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.sunbird.logging.LoggerUtil;

public class PropertiesCache {
  private static LoggerUtil logger = new LoggerUtil(PropertiesCache.class);
  private final String fileName = "configuration.properties";
  private final Properties configProp = new Properties();
  private static PropertiesCache instance;

  /** private default constructor */
  private PropertiesCache() {
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(fileName);
    try {
      configProp.load(in);
    } catch (IOException e) {
      logger.error("Error in properties cache", e);
    }
  }

  public static PropertiesCache getInstance() {
    // To make thread safe
    synchronized (PropertiesCache.class) {
      // check again as multiple threads
      // can reach above step
      if (instance == null) instance = new PropertiesCache();
    }
    return instance;
  }

  /**
   * Method to read value from resource file and if key not found then by default return key values
   * itself.
   *
   * @param key
   * @return
   */
  public String getProperty(String key) {
    return configProp.getProperty(key) != null ? configProp.getProperty(key) : null;
  }

  /**
   * Method to read value from resource file .
   *
   * @param key
   * @return
   */
  public String readProperty(String key) {
    return configProp.getProperty(key);
  }
}
