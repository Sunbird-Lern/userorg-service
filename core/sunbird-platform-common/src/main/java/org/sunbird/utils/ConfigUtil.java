package org.sunbird.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.ProjectUtil;
import org.sunbird.response.ResponseCode;

/**
 * Utility class for type-safe configuration management.
 * Provides methods to load configuration from system environment or files.
 */
public class ConfigUtil {

  public static LoggerUtil logger = new LoggerUtil(ConfigUtil.class);
  private static Config config;
  private static final String DEFAULT_TYPE_SAFE_CONFIG_FILE_NAME = "service.conf";
  private static final String INVALID_FILE_NAME = "Please provide a valid file name.";

  private ConfigUtil() {}

  /**
   * Loads the type-safe config object.
   * Reads from system environment first, falling back to 'service.conf'.
   *
   * @return The loaded type-safe Config object.
   */
  public static Config getConfig() {
    if (config == null) {
      synchronized (ConfigUtil.class) {
        config = createConfig(DEFAULT_TYPE_SAFE_CONFIG_FILE_NAME);
      }
    }
    return config;
  }

  /**
   * Loads the type-safe config object from a specific file.
   * Reads from system environment first, falling back to the provided file name.
   *
   * @param fileName The name of the configuration file to load.
   * @return The loaded type-safe Config object.
   * @throws ProjectCommonException If the file name is null or empty.
   */
  public static Config getConfig(String fileName) {
    if (StringUtils.isBlank(fileName)) {
      logger.info("ConfigUtil:getConfig: Given file name is null or empty: " + fileName);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          INVALID_FILE_NAME,
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (config == null) {
      synchronized (ConfigUtil.class) {
        config = createConfig(fileName);
      }
    }
    return config;
  }

  /**
   * Validates if a mandatory configuration parameter is present.
   *
   * @param configParameter The configuration parameter value to check.
   * @throws ProjectCommonException If the parameter is null or empty.
   */
  public static void validateMandatoryConfigValue(String configParameter) {
    if (StringUtils.isBlank(configParameter)) {
      logger.error("ConfigUtil:validateMandatoryConfigValue: Missing mandatory configuration parameter: " + configParameter, null);
      throw new ProjectCommonException(
          ResponseCode.mandatoryConfigParamMissing,
          ResponseCode.mandatoryConfigParamMissing.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode(),
          configParameter);
    }
  }

  private static Config createConfig(String fileName) {
    Config defaultConf = ConfigFactory.load(fileName);
    Config envConf = ConfigFactory.systemEnvironment();
    return envConf.withFallback(defaultConf);
  }

  /**
   * Parses a JSON string into a type-safe config object.
   *
   * @param jsonString The configuration string in JSON format.
   * @param configType A label for the configuration type (used in error messages).
   * @return The parsed Config object.
   * @throws ProjectCommonException If the string is empty, parsing fails, or the result is empty.
   */
  public static Config getConfigFromJsonString(String jsonString, String configType) {
    if (StringUtils.isBlank(jsonString)) {
      logger.error("ConfigUtil:getConfigFromJsonString: Empty string provided for " + configType, null);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadEmptyString,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadEmptyString.getErrorMessage(), configType));
    }

    Config jsonConfig = null;
    try {
      jsonConfig = ConfigFactory.parseString(jsonString);
      logger.info("ConfigUtil:getConfigFromJsonString: Successfully constructed configuration for " + configType);
    } catch (Exception e) {
      logger.error("ConfigUtil:getConfigFromJsonString: Exception occurred during parse", e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadParseString,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadParseString.getErrorMessage(), configType));
    }

    if (jsonConfig == null || jsonConfig.isEmpty()) {
      logger.error("ConfigUtil:getConfigFromJsonString: Empty configuration resulting from parse for " + configType, null);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadEmptyConfig,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadEmptyConfig.getErrorMessage(), configType));
    }
    return jsonConfig;
  }
}
