package org.sunbird.common.models.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Amit Kumar
 *
 * this class is used for reading properties file
 */
public class PropertiesCache {

  private static LoggerUtil logger = new LoggerUtil(PropertiesCache.class);

  private final String[] fileName = {
    "elasticsearch.config.properties",
    "cassandra.config.properties",
    "dbconfig.properties",
    "externalresource.properties",
    "sso.properties",
    "userencryption.properties",
    "mailTemplates.properties"
  };
  private final Properties configProp = new Properties();
  public final Map<String, Float> attributePercentageMap = new ConcurrentHashMap<>();
  private static PropertiesCache propertiesCache = null;

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

    // change the lazy holder implementation to simple singleton implementation ...
    if (null == propertiesCache) {
      synchronized (PropertiesCache.class) {
        if (null == propertiesCache) {
          propertiesCache = new PropertiesCache();
        }
      }
    }

    return propertiesCache;
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
