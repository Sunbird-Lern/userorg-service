package org.sunbird.common.factory;

import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

/**
 * Factory class to provide instances of ElasticSearchService.
 * Supports creating clients for different connection types (e.g., REST).
 */
public class EsClientFactory {

  private static volatile ElasticSearchService restClient = null;
  private static final LoggerUtil logger = new LoggerUtil(EsClientFactory.class);

  private EsClientFactory() {
    // Private constructor to prevent instantiation
  }

  /**
   * Returns a REST-based ElasticSearchService instance.
   * This is the default factory method.
   *
   * @return The singleton instance of ElasticSearchService (REST implementation).
   */
  public static ElasticSearchService getInstance() {
    return getRestClient();
  }

  /**
   * Returns an ElasticSearchService instance based on the provided connection type.
   *
   * @param type The connection type (e.g., "rest"). Currently only "rest" is supported.
   * @return The ElasticSearchService instance for the specified type, or null if unsupported.
   */
  public static ElasticSearchService getInstance(String type) {
    if (JsonKey.REST.equalsIgnoreCase(type)) {
      return getRestClient();
    }
    logger.info(null, "EsClientFactory:getInstance: Unsupported client type provided: " + type);
    return null;
  }

  /**
   * Helper method to initialize and return the REST client singleton.
   * Uses double-checked locking for thread safety.
   *
   * @return The singleton instance of ElasticSearchRestHighImpl.
   */
  private static ElasticSearchService getRestClient() {
    if (restClient == null) {
      synchronized (EsClientFactory.class) {
        if (restClient == null) {
          logger.info(null, "EsClientFactory:getRestClient: Initializing new ElasticSearchRestHighImpl.");
          restClient = new ElasticSearchRestHighImpl();
        }
      }
    }
    return restClient;
  }
}
