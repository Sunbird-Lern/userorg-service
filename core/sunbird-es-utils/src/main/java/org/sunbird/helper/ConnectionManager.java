package org.sunbird.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

/**
 * Manages Elasticsearch REST high-level client connections with thread-safe singleton access.
 * Configuration is read from environment variables: SUNBIRD_ES_IP, SUNBIRD_ES_PORT, SUNBIRD_ES_CLUSTER.
 */
public class ConnectionManager {
  
  private static final LoggerUtil logger = new LoggerUtil(ConnectionManager.class);
  
  /** Singleton REST high-level client instance. */
  private static volatile RestHighLevelClient restClient = null;
  
  /** List of Elasticsearch host addresses. */
  private static final List<String> hosts = new ArrayList<>();
  
  /** List of Elasticsearch ports (populated from env but currently unused). */
  private static final List<Integer> ports = new ArrayList<>();
  
  /** Lock object for thread-safe client initialization. */
  private static final Object lock = new Object();
  
  static {
    // Prevent Netty from using runtime available processors check
    System.setProperty("es.set.netty.runtime.available.processors", "false");
    
    // Initialize connection on class loading
    initialiseRestClientConnection();
    
    // Register shutdown hook for cleanup
    registerShutDownHook();
  }
  
  /** Private constructor to prevent instantiation. */
  private ConnectionManager() {}
  
  /**
   * Initializes Elasticsearch REST client from environment variables.
   *
   * @return true if connection established successfully, false otherwise
   */
  private static boolean initialiseRestClientConnection() {
    try {
      String cluster = System.getenv(JsonKey.SUNBIRD_ES_CLUSTER);
      String hostName = System.getenv(JsonKey.SUNBIRD_ES_IP);
      String port = System.getenv(JsonKey.SUNBIRD_ES_PORT);
      
      // Validate required configuration
      if (StringUtils.isBlank(hostName) || StringUtils.isBlank(port)) {
        logger.warn(
            null,
            "Elasticsearch configuration incomplete - SUNBIRD_ES_IP or SUNBIRD_ES_PORT not set",
            null);
        return false;
      }
      
      // Parse comma-separated hosts
      String[] splitedHost = hostName.split(",");
      for (String host : splitedHost) {
        String trimmedHost = host.trim();
        if (StringUtils.isNotBlank(trimmedHost)) {
          hosts.add(trimmedHost);
        }
      }
      
      // Parse comma-separated ports (currently stored but not used)
      String[] splitedPort = port.split(",");
      for (String portStr : splitedPort) {
        String trimmedPort = portStr.trim();
        if (StringUtils.isNotBlank(trimmedPort)) {
          try {
            ports.add(Integer.parseInt(trimmedPort));
          } catch (NumberFormatException e) {
            logger.warn(null, "Invalid port number in SUNBIRD_ES_PORT: " + trimmedPort, null);
          }
        }
      }
      
      // Create REST client
      boolean success = createRestClient(cluster, hosts);
      
      if (success) {
        String clusterName = cluster != null ? cluster : "default";
        String hostList = String.join(",", hosts);
        logger.info(
            null,
            "Elasticsearch connection established successfully - cluster: " + clusterName 
                + ", hosts: " + hostList + ", port: 9200");
      }
      
      return success;
      
    } catch (Exception e) {
      logger.error(null, "Failed to initialize Elasticsearch REST client connection", e);
      return false;
    }
  }
  
  /**
   * Returns the singleton REST client instance with thread-safe lazy initialization.
   *
   * @return RestHighLevelClient instance, or null if initialization failed
   */
  public static RestHighLevelClient getRestClient() {
    // First check without locking (performance optimization)
    if (restClient == null) {
      synchronized (lock) {
        // Double-check after acquiring lock
        if (restClient == null) {
          logger.info(null, "REST client is null, attempting to initialize connection");
          
          boolean initialized = initialiseRestClientConnection();
          
          if (initialized && restClient != null) {
            logger.info(null, "REST client initialized successfully");
          } else {
            logger.error(
                null,
                "Failed to initialize REST client - check Elasticsearch configuration",
                null);
          }
        }
      }
    }
    return restClient;
  }
  
  /**
   * Creates Elasticsearch REST client instance using port 9200 for all hosts.
   *
   * @param clusterName cluster name (informational only)
   * @param hostList list of host addresses
   * @return true if client created successfully, false otherwise
   */
  private static boolean createRestClient(String clusterName, List<String> hostList) {
    try {
      if (hostList == null || hostList.isEmpty()) {
        logger.warn(null, "No Elasticsearch hosts provided for client initialization", null);
        return false;
      }
      
      // Build HttpHost array for all configured hosts
      HttpHost[] httpHosts = new HttpHost[hostList.size()];
      for (int i = 0; i < hostList.size(); i++) {
        httpHosts[i] = new HttpHost(hostList.get(i), 9200, "http");
        logger.debug(null, "Adding Elasticsearch node: " + hostList.get(i) + ":9200");
      }
      
      // Create REST high-level client
      restClient = new RestHighLevelClient(RestClient.builder(httpHosts));
      
      logger.info(
          null,
          "Elasticsearch REST client created successfully with " + hostList.size() + " host(s)");
      
      return true;
      
    } catch (Exception e) {
      logger.error(null, "Failed to create Elasticsearch REST client", e);
      return false;
    }
  }
  
  /** Shutdown hook for graceful Elasticsearch client cleanup. */
  static class ResourceCleanUp extends Thread {
    @Override
    public void run() {
      if (restClient != null) {
        try {
          logger.info(null, "Shutting down Elasticsearch REST client");
          restClient.close();
          logger.info(null, "Elasticsearch REST client closed successfully");
        } catch (IOException e) {
          logger.error(
              null,
              "Error occurred during Elasticsearch REST client resource cleanup: " + e.getMessage(),
              e);
        }
      } else {
        logger.debug(null, "No Elasticsearch REST client to clean up");
      }
    }
  }
  
  /** Registers JVM shutdown hook for resource cleanup. */
  static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
    logger.debug(null, "Elasticsearch connection cleanup shutdown hook registered");
  }
}
