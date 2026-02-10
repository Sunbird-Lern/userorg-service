package org.sunbird.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for retrieving Elasticsearch configuration values.
 * Checks system environment variables first, then falls back to properties cache.
 */
public class EsConfigUtil {

  private EsConfigUtil() {}

  /**
   * Retrieves the configuration value for the given key.
   * Priority: System Environment Variable -> Properties Cache.
   *
   * @param key The configuration key.
   * @return The configuration value.
   */
  public static String getConfigValue(String key) {
    if (StringUtils.isNotBlank(System.getenv(key))) {
      return System.getenv(key);
    }
    return org.sunbird.common.PropertiesCache.getInstance().getProperty(key);
  }
}
