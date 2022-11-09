/** */
package org.sunbird.cloud.aws;

import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

/**
 * This class will manage azure connection.
 *
 * @author Manzarul
 */
public class AwsConnectionManager {

  private static final LoggerUtil logger = new LoggerUtil(AwsConnectionManager.class);

  private static BaseStorageService storageService = null;

  private AwsConnectionManager() {}

  public static BaseStorageService getStorageService() {
    if (null == storageService) {
      String accountName = System.getenv(JsonKey.ACCOUNT_NAME);
      String accountKey = System.getenv(JsonKey.ACCOUNT_KEY);

      StorageConfig storageConfig = new StorageConfig("aws", accountName, accountKey);
      logger.info("StorageParams:init:all storage params initialized for aws block");
      storageService = StorageServiceFactory.getStorageService(storageConfig);
    }
    return storageService;
  }
}
