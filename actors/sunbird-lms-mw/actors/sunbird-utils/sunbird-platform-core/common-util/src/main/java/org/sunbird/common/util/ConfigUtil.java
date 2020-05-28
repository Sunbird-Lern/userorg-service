package org.sunbird.common.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This util class for providing type safe config to any service that requires it.
 *
 * @author Manzarul
 */
public class ConfigUtil {

  private static Config config;
  private static final String DEFAULT_TYPE_SAFE_CONFIG_FILE_NAME = "service.conf";
  private static final String INVALID_FILE_NAME = "Please provide a valid file name.";

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

  /**
   * This method will create a type safe config object and return to caller. It will read the config
   * value from System env first and as a fall back it will use provided file name. If file name is
   * null or empty then it will throw ProjectCommonException with status code as 500.
   *
   * @return Type safe config object
   */
  public static Config getConfig(String fileName) {
    if (StringUtils.isBlank(fileName)) {
      ProjectLogger.log(
          "ConfigUtil:getConfigWithFilename: Given file name is null or empty: " + fileName,
          LoggerEnum.INFO.name());
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

  public static void validateMandatoryConfigValue(String configParameter) {
    if (StringUtils.isBlank(configParameter)) {
      ProjectLogger.log(
          "ConfigUtil:validateMandatoryConfigValue: Missing mandatory configuration parameter: "
              + configParameter,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.mandatoryConfigParamMissing.getErrorCode(),
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

  /*
   * Parse configuration in JSON format and return a type safe config object.
   *
   * @param jsonString Configuration in JSON format
   * @return Type safe config object
   */
  public static Config getConfigFromJsonString(String jsonString, String configType) {
    ProjectLogger.log("ConfigUtil: getConfigFromJsonString called", LoggerEnum.DEBUG.name());

    if (null == jsonString || StringUtils.isBlank(jsonString)) {
      ProjectLogger.log(
          "ConfigUtil:getConfigFromJsonString: Empty string", LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadEmptyString,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadEmptyString.getErrorMessage(), configType));
    }

    Config jsonConfig = null;
    try {
      jsonConfig = ConfigFactory.parseString(jsonString);
    } catch (Exception e) {
      ProjectLogger.log(
          "ConfigUtil:getConfigFromJsonString: Exception occurred during parse with error message = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadParseString,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadParseString.getErrorMessage(), configType));
    }

    if (null == jsonConfig || jsonConfig.isEmpty()) {
      ProjectLogger.log(
          "ConfigUtil:getConfigFromJsonString: Empty configuration", LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorConfigLoadEmptyConfig,
          ProjectUtil.formatMessage(
              ResponseCode.errorConfigLoadEmptyConfig.getErrorMessage(), configType));
    }

    ProjectLogger.log(
        "ConfigUtil:getConfigFromJsonString: Successfully constructed type safe configuration",
        LoggerEnum.DEBUG.name());

    return jsonConfig;
  }
}
