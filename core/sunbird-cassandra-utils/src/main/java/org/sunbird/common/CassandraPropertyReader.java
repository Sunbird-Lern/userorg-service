package org.sunbird.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.logging.LoggerUtil;

/**
 * Utility class to read configuration properties for Cassandra tables and columns.
 * Can read from 'cassandratablecolumn.properties' and 'cassandra.config.properties'.
 * Implements a thread-safe singleton pattern.
 */
public class CassandraPropertyReader {
  private static final LoggerUtil logger = new LoggerUtil(CassandraPropertyReader.class);

  private final Properties properties = new Properties();
  private final String[] fileName = {
    "cassandratablecolumn.properties", "cassandra.config.properties"
  };
  private static volatile CassandraPropertyReader cassandraPropertyReader = null;

  /** Private default constructor. */
  private CassandraPropertyReader() {
    for (String file : fileName) {
      try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(file)) {
        if (in != null) {
          properties.load(in);
        }
      } catch (IOException e) {
        logger.error(null, "Error in properties cache", e);
      }
    }
  }

  /**
   * Returns the singleton instance of CassandraPropertyReader.
   *
   * @return The singleton instance.
   */
  public static CassandraPropertyReader getInstance() {
    if (null == cassandraPropertyReader) {
      synchronized (CassandraPropertyReader.class) {
        if (null == cassandraPropertyReader) {
          cassandraPropertyReader = new CassandraPropertyReader();
        }
      }
    }
    return cassandraPropertyReader;
  }

  /**
   * Reads a property value from the loaded configuration files.
   *
   * @param key The property key to read.
   * @return The value corresponding to the given key, or the key itself if not found.
   */
  public String readProperty(String key) {
    return properties.getProperty(key, key);
  }

  /**
   * Method to get property from system environment or properties file.
   *
   * @param key The key to look up.
   * @return The value from system env, or file properties, or the key itself if not found.
   */
  public String getProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) {
      return value;
    }
    return properties.getProperty(key, key);
  }

  /**
   * Finds a property key associated with a specific value in the configuration files.
   * Note: This might return the first key found if multiple keys have the same value.
   *
   * @param value The value to search for.
   * @return The key associated with the value, or the value itself if not found.
   */
  public String readPropertyValue(String value) {
    List<Map.Entry<Object, Object>> entries =
        properties.entrySet().stream()
            .filter(entry -> value.equals(entry.getValue()))
            .collect(Collectors.toList());
    return entries.isEmpty() ? value : (String) entries.get(0).getKey();
  }
}
