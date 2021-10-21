package org.sunbird.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.logging.LoggerUtil;

/**
 * This class will be used to read cassandratablecolumn properties file.
 *
 * @author Amit Kumar
 */
public class CassandraPropertyReader {
  private static final LoggerUtil logger = new LoggerUtil(CassandraPropertyReader.class);

  private final Properties properties = new Properties();
  private final String[] fileName = {
    "cassandratablecolumn.properties", "cassandra.config.properties"
  };
  private static CassandraPropertyReader cassandraPropertyReader = null;

  /** private default constructor */
  private CassandraPropertyReader() {
    for (String file : fileName) {
      InputStream in = this.getClass().getClassLoader().getResourceAsStream(file);
      try {
        properties.load(in);
      } catch (IOException e) {
        logger.error("Error in properties cache", e);
      }
    }
  }

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
   * Method to read value from resource file .
   *
   * @param key property value to read
   * @return value corresponding to given key if found else will return key itself.
   */
  public String readProperty(String key) {
    return properties.getProperty(key) != null ? properties.getProperty(key) : key;
  }

  public String getProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) return value;
    return properties.getProperty(key) != null ? properties.getProperty(key) : key;
  }
}
