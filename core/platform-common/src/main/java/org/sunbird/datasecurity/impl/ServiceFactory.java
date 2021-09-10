/** */
package org.sunbird.datasecurity.impl;

import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;

/**
 * This factory will provide encryption service instance and decryption service instance with
 * default implementation.
 *
 * @author Manzarul
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
   * this method will provide encryptionServiceImple instance. by default it will provide
   * DefaultEncryptionServiceImpl instance to get a particular service impl instance , need to
   * change the object creation and provided logic.
   *
   * @return EncryptionService
   */
  public static EncryptionService getEncryptionServiceInstance() {
    return encryptionService;
  }

  /**
   * this method will provide decryptionServiceImple instance. by default it will provide
   * DefaultDecryptionServiceImpl instance to get a particular service impl instance , need to
   * change the object creation and provided logic.
   *
   * @return DecryptionService
   */
  public static DecryptionService getDecryptionServiceInstance() {
    return decryptionService;
  }

  public static DataMaskingService getMaskingServiceInstance() {
    return maskingService;
  }
}
