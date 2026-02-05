package org.sunbird.datasecurity.impl;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;

/**
 * Factory class to provide instances of data security services.
 * Supports EncryptionService, DecryptionService, and DataMaskingService.
 * Provides both parameterized (for backward compatibility) and non-parameterized factory methods.
 */
public class ServiceFactory {

  private static EncryptionService encryptionService;
  private static DecryptionService decryptionService;
  private static DataMaskingService maskingService;

  static {
    encryptionService = new DefaultEncryptionServiceImpl();
    decryptionService = new DefaultDecryptionServiceImpl();
    maskingService = new DefaultDataMaskServiceImpl();
  }

  /**
   * Provides the default instance of EncryptionService.
   *
   * @return The default EncryptionService instance.
   */
  public static EncryptionService getEncryptionServiceInstance() {
    return encryptionService;
  }

  /**
   * Provides an instance of EncryptionService.
   * Currently, returns the default instance regardless of the input value,
   * but supports the parameter for backward compatibility.
   *
   * @param val The type of service implementation required (e.g., "defaultEncryption").
   *            Pass null or empty for the default implementation.
   * @return An instance of EncryptionService.
   */
  public static EncryptionService getEncryptionServiceInstance(String val) {
    if (StringUtils.isBlank(val)) {
      return encryptionService;
    }
    switch (val) {
      case "defaultEncryption":
        return encryptionService;
      default:
        return encryptionService;
    }
  }

  /**
   * Provides the default instance of DecryptionService.
   *
   * @return The default DecryptionService instance.
   */
  public static DecryptionService getDecryptionServiceInstance() {
    return decryptionService;
  }

  /**
   * Provides an instance of DecryptionService.
   * Currently, returns the default instance regardless of the input value,
   * but supports the parameter for backward compatibility.
   *
   * @param val The type of service implementation required (e.g., "defaultDecryption").
   *            Pass null or empty for the default implementation.
   * @return An instance of DecryptionService.
   */
  public static DecryptionService getDecryptionServiceInstance(String val) {
    if (StringUtils.isBlank(val)) {
      return decryptionService;
    }
    switch (val) {
      case "defaultDecryption":
        return decryptionService;
      default:
        return decryptionService;
    }
  }

  /**
   * Provides the default instance of DataMaskingService.
   *
   * @return The default DataMaskingService instance.
   */
  public static DataMaskingService getMaskingServiceInstance() {
    return maskingService;
  }

  /**
   * Provides an instance of DataMaskingService.
   * Currently, returns the default instance regardless of the input value,
   * but supports the parameter for backward compatibility.
   *
   * @param val The type of service implementation required (e.g., "defaultMasking").
   *            Pass null or empty for the default implementation.
   * @return An instance of DataMaskingService.
   */
  public static DataMaskingService getMaskingServiceInstance(String val) {
    if (StringUtils.isBlank(val)) {
      return maskingService;
    }
    switch (val) {
      case "defaultMasking":
        return maskingService;
      default:
        return maskingService;
    }
  }
}
