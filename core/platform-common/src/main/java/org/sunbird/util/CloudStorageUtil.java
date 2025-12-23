package org.sunbird.util;

import java.util.HashMap;
import java.util.Map;
import org.sunbird.cloud.storage.IStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.keys.JsonKey;
import scala.Option;
import scala.Some;

public class CloudStorageUtil {
  private static final int STORAGE_SERVICE_API_RETRY_COUNT = 3;
  private static final Map<String, IStorageService> storageServiceMap = new HashMap<>();

  public static String upload(String storageType, String container, String objectKey, String filePath) {
    IStorageService storageService = getStorageService(storageType);
    return storageService.upload(container, filePath, objectKey, Option.apply(false), Option.apply(1), Option.apply(STORAGE_SERVICE_API_RETRY_COUNT), Option.empty());
  }

  public static String getSignedUrl(String storageType, String container, String objectKey) {
    IStorageService storageService = getStorageService(storageType);
    return getSignedUrl(storageService, container, objectKey,storageType);
  }

  public static String getSignedUrl(IStorageService storageService, String container, String objectKey,String cloudType) {
    int timeoutInSeconds = getTimeoutInSeconds();
    return storageService.getSignedURLV2(container, objectKey, Some.apply(timeoutInSeconds), Some.apply("r"), Some.apply("application/pdf"), Option.empty());
  }

  public static void deleteFile(String storageType, String container, String objectKey) {
    IStorageService storageService = getStorageService(storageType);
    storageService.deleteObject(container, objectKey, Option.apply(false));
  }

  private static IStorageService getStorageService(String storageType) {
    String storageKey = PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_NAME);
    String storageSecret = PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_KEY);
    scala.Option<String> storageEndpoint = scala.Option.apply(PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_ENDPOINT));
    scala.Option<String> storageRegion = scala.Option.apply("");
    return getStorageService(storageType, storageKey, storageSecret,storageEndpoint,storageRegion);
  }


  private static IStorageService getStorageService(
      String storageType, String storageKey, String storageSecret,scala.Option<String> storageEndpoint, scala.Option<String> storageRegion ) {
    String compositeKey = storageType + "-" + storageKey;
    if (storageServiceMap.containsKey(compositeKey)) {
      return storageServiceMap.get(compositeKey);
    }
    synchronized (CloudStorageUtil.class) {

      StorageConfig storageConfig =
          new StorageConfig(storageType, storageKey, storageSecret, storageEndpoint, storageRegion);
      IStorageService storageService = StorageServiceFactory.getStorageService(storageConfig);
      storageServiceMap.put(compositeKey, storageService);
    }
    return storageServiceMap.get(compositeKey);
  }

  private static int getTimeoutInSeconds() {
    String timeoutInSecondsStr = ProjectUtil.getConfigValue(JsonKey.DOWNLOAD_LINK_EXPIRY_TIMEOUT);
    return Integer.parseInt(timeoutInSecondsStr);
  }
}