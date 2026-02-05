package org.sunbird.utils;

import static org.sunbird.keys.JsonKey.CLOUD_STORAGE_CNAME_URL;
import static org.sunbird.keys.JsonKey.CLOUD_STORE_BASE_PATH;
import static org.sunbird.common.ProjectUtil.getConfigValue;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.PropertiesCache;
import scala.Option;
import scala.Some;

public class CloudStorageUtil {
  private static final int STORAGE_SERVICE_API_RETRY_COUNT = 3;
  private static final Map<String, BaseStorageService> storageServiceMap = new HashMap<>();

  /**
   * Uploads a file to the cloud storage.
   *
   * @param storageType The type of storage (e.g., azure, aws).
   * @param container The container or bucket name.
   * @param objectKey The key (path) for the object in storage.
   * @param filePath The local path of the file to upload.
   * @return The URL of the uploaded file.
   */
  public static String upload(
      String storageType, String container, String objectKey, String filePath) {
    BaseStorageService storageService = getStorageService(storageType);
    return storageService.upload(
        container,
        filePath,
        objectKey,
        Option.apply(false),
        Option.apply(1),
        Option.apply(STORAGE_SERVICE_API_RETRY_COUNT),
        Option.empty());
  }

  /**
   * Generates a signed URL for an object in cloud storage.
   *
   * @param storageType The type of storage.
   * @param container The container or bucket name.
   * @param objectKey The key of the object.
   * @return The signed URL.
   */
  public static String getSignedUrl(String storageType, String container, String objectKey) {
    BaseStorageService storageService = getStorageService(storageType);
    return getSignedUrl(storageService, container, objectKey, storageType);
  }

  /**
   * Generates a signed URL for an object using a specific storage service instance.
   *
   * @param storageService The storage service instance.
   * @param container The container or bucket name.
   * @param objectKey The key of the object.
   * @param cloudType The cloud type (not directly used but part of signature).
   * @return The signed URL.
   */
  public static String getSignedUrl(
      BaseStorageService storageService, String container, String objectKey, String cloudType) {
    return storageService.getSignedURLV2(
        container,
        objectKey,
        Some.apply(getTimeoutInSeconds()),
        Some.apply("r"),
        Some.apply("application/pdf"),
        Option.empty());
  }

  /**
   * Deletes a file from cloud storage.
   *
   * @param storageType The type of storage.
   * @param container The container or bucket name.
   * @param objectKey The key of the object to delete.
   */
  public static void deleteFile(String storageType, String container, String objectKey) {
    BaseStorageService storageService = getStorageService(storageType);
    storageService.deleteObject(container, objectKey, Option.apply(false));
  }

  /**
   * Gets the URI for a specific prefix in the container.
   *
   * @param storageType The type of storage.
   * @param container The container name.
   * @param prefix The prefix to list/get.
   * @param isDirectory Whether it is a directory.
   * @return The URI.
   */
  public static String getUri(
      String storageType, String container, String prefix, boolean isDirectory) {
    BaseStorageService storageService = getStorageService(storageType);
    return storageService.getUri(container, prefix, Option.apply(isDirectory));
  }

  /**
   * Gets the base URL for cloud storage from configuration.
   *
   * @return The base URL.
   */
  public static String getBaseUrl() {
    String baseUrl = getConfigValue(CLOUD_STORAGE_CNAME_URL);
    if (StringUtils.isEmpty(baseUrl)) baseUrl = getConfigValue(CLOUD_STORE_BASE_PATH);
    return baseUrl;
  }

  /**
   * Retrieves a storage service instance based on the provided storage type.
   * Loads account name and key from properties cache.
   *
   * @param storageType The type of storage (e.g., azure, aws).
   * @return A BaseStorageService instance.
   */
  private static BaseStorageService getStorageService(String storageType) {
    String storageKey = PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_NAME);
    String storageSecret = PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_KEY);
    return getStorageService(storageType, storageKey, storageSecret);
  }

  /**
   * Retrieves or creates a storage service instance.
   * Uses a composite key (type-key) to cache instances.
   *
   * @param storageType The type of storage.
   * @param storageKey The storage account key/name.
   * @param storageSecret The storage account secret.
   * @return A BaseStorageService instance.
   */
  private static BaseStorageService getStorageService(
      String storageType, String storageKey, String storageSecret) {
    String compositeKey = storageType + "-" + storageKey;
    if (storageServiceMap.containsKey(compositeKey)) {
      return storageServiceMap.get(compositeKey);
    }
    synchronized (CloudStorageUtil.class) {
      if (storageServiceMap.containsKey(compositeKey)) {
        return storageServiceMap.get(compositeKey);
      }
      scala.Option<String> storageEndpoint =
          scala.Option.apply(PropertiesCache.getInstance().getProperty(JsonKey.ACCOUNT_ENDPOINT));
      scala.Option<String> storageRegion = scala.Option.apply("");
      StorageConfig storageConfig =
          new StorageConfig(storageType, storageKey, storageSecret, storageEndpoint, storageRegion);
      BaseStorageService storageService = StorageServiceFactory.getStorageService(storageConfig);
      storageServiceMap.put(compositeKey, storageService);
    }
    return storageServiceMap.get(compositeKey);
  }

  /**
   * Retrieves the download link expiry timeout from configuration.
   *
   * @return The timeout in seconds.
   */
  private static int getTimeoutInSeconds() {
    String timeoutInSecondsStr = ProjectUtil.getConfigValue(JsonKey.DOWNLOAD_LINK_EXPIRY_TIMEOUT);
    return Integer.parseInt(timeoutInSecondsStr);
  }
}