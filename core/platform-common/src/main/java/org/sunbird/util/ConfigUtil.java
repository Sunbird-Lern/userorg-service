package org.sunbird.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.logging.LoggerUtil;

/**
 * This util class for providing type safe config to any service that requires it.
 *
 * @author Manzarul
 */
public class ConfigUtil {

  public static LoggerUtil logger = new LoggerUtil(ConfigUtil.class);
  private static Config config;
  private static final String DEFAULT_TYPE_SAFE_CONFIG_FILE_NAME = "service.conf";

  /** Private default constructor. */
  private ConfigUtil() {}

  /**
   * This method will create a type safe config object and return to caller. It will read the config
   * value from System env first and as a fall back it will use service.conf file.
   *
   * @return Type safe config object
   */
  public static Config getConfig() {
    if (config == null) {
      synchronized (ConfigUtil.class) {
        config = createConfig(DEFAULT_TYPE_SAFE_CONFIG_FILE_NAME);
      }
    }
    return config;
  }

  private static Config createConfig(String fileName) {
    Config defaultConf = ConfigFactory.load(fileName);
    Config envConf = ConfigFactory.systemEnvironment();
    return envConf.withFallback(defaultConf);
  }

  /*
   * Parse configuration in JSON format and return a type safe config object.
   *
   * @param jsonString Configuration in JSON format
   * @return Type safe config object
   */
  public static Config getConfigFromJsonString(String jsonString, String configType) {
    if (null == jsonString || StringUtils.isBlank(jsonString)) {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadEmptyString,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadEmptyString.getErrorMessage(), configType));
    }

    Config jsonConfig = null;
    try {
      jsonConfig = ConfigFactory.parseString(jsonString);
    } catch (Exception e) {
      logger.error(
          "ConfigUtil:getConfigFromJsonString: Exception occurred during parse with error message = "
              + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadParseString,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadParseString.getErrorMessage(), configType));
    }

    if (null == jsonConfig || jsonConfig.isEmpty()) {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadEmptyConfig,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadEmptyConfig.getErrorMessage(), configType));
    }
    return jsonConfig;
  }
}
