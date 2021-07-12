package org.sunbird.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.logging.LoggerUtil;

/*
 * @author Amit Kumar
 *
 * this class is used for reading properties file
 */
public class PropertiesCache {

  private static LoggerUtil logger = new LoggerUtil(PropertiesCache.class);

  private final String[] fileName = {
    "cassandratablecolumn.properties",
    "elasticsearch.config.properties",
    "cassandra.config.properties",
    "dbconfig.properties",
    "externalresource.properties",
    "sso.properties",
    "userencryption.properties",
    "mailTemplates.properties"
  };
  private final Properties configProp = new Properties();
  private static PropertiesCache instance;

  /** private default constructor */
  private PropertiesCache() {
    for (String file : fileName) {
      InputStream in = this.getClass().getClassLoader().getResourceAsStream(file);
      try {
        configProp.load(in);
      } catch (IOException e) {
        logger.error("Error in properties cache", e);
      }
    }
  }

  public static PropertiesCache getInstance() {
    if (instance == null) {
      // To make thread safe
      synchronized (PropertiesCache.class) {
        // check again as multiple threads
        // can reach above step
        if (instance == null) instance = new PropertiesCache();
      }
    }
    return instance;
  }

  public void saveConfigProperty(String key, String value) {
    configProp.setProperty(key, value);
  }

  public String getProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) return value;
    return configProp.getProperty(key) != null ? configProp.getProperty(key) : key;
  }

  /**
   * Method to read value from resource file .
   *
   * @param key
   * @return
   */
  public String readProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) return value;
    return configProp.getProperty(key);
  }
}
