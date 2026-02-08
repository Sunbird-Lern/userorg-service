package org.sunbird.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.sunbird.common.ProjectUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

/**
 * RedisConnectionManager handles the lifecycle of the Redis client. It supports both standalone and
 * cluster Redis configurations.
 */
public class RedisConnectionManager {
  private static final String host = ProjectUtil.getConfigValue(JsonKey.REDIS_HOST_VALUE);
  private static final String port = ProjectUtil.getConfigValue(JsonKey.REDIS_PORT_VALUE);
  private static final Boolean isRedisCluster = host.contains(",");
  private static final String scanInterval =
      ProjectUtil.getConfigValue(JsonKey.SUNBIRD_REDIS_SCAN_INTERVAL);
  private static final int poolsize =
      Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_REDIS_CONN_POOL_SIZE));
  private static RedissonClient client = null;
  private static final LoggerUtil logger = new LoggerUtil(RedisConnectionManager.class);

  /**
   * Returns the singleton RedissonClient instance, initializing it if necessary.
   *
   * @return The RedissonClient instance.
   */
  public static RedissonClient getClient() {
    if (client == null) {
      logger.info( "RedisConnectionManager:getClient: Redis client is null, initializing...");
      boolean start = initialiseConnection();
      logger.info( "RedisConnectionManager:getClient: Connection status = " + start);
    }
    return client;
  }

  /**
   * Initialises the Redis connection based on the configuration (cluster or single server).
   *
   * @return True if successful, false otherwise.
   */
  private static boolean initialiseConnection() {
    try {
      if (isRedisCluster) {
        initialisingClusterServer(host, port);
      } else {
        initialiseSingleServer(host, port);
      }
    } catch (Exception e) {
      logger.error(
          "RedisConnectionManager:initialiseConnection: Error occurred: " + e.getMessage(),
          e);
      return false;
    }
    return true;
  }

  /**
   * Initialises a single server Redis connection.
   *
   * @param host Redis host.
   * @param port Redis port.
   */
  private static void initialiseSingleServer(String host, String port) {
    logger.info( "RedisConnectionManager: initialiseSingleServer called");

    Config config = new Config();
    SingleServerConfig singleServerConfig = config.useSingleServer();
    singleServerConfig.setAddress("redis://" + host + ":" + port);
    singleServerConfig.setConnectionPoolSize(poolsize);
    config.setCodec(new StringCodec());
    client = Redisson.create(config);
  }

  /**
   * Initialising a cluster Redis connection.
   *
   * @param host Comma-separated list of Redis hosts.
   * @param port Comma-separated list of Redis ports.
   */
  private static void initialisingClusterServer(String host, String port) {
    logger.info(
        "RedisConnectionManager: initialisingClusterServer called with host = "
            + host
            + " port = "
            + port);

    String[] hosts = host.split(",");
    String[] ports = port.split(",");

    Config config = new Config();

    try {
      config.setCodec(new StringCodec());
      ClusterServersConfig clusterConfig = config.useClusterServers();

      clusterConfig.setScanInterval(Integer.parseInt(scanInterval));
      clusterConfig.setMasterConnectionPoolSize(poolsize);

      for (int i = 0; i < hosts.length && i < ports.length; i++) {
        clusterConfig.addNodeAddress("redis://" + hosts[i] + ":" + ports[i]);
      }

      client = Redisson.create(config);
      logger.info( "RedisConnectionManager:initialisingClusterServer: Redis client created.");
    } catch (Exception e) {
      logger.error(
          "RedisConnectionManager:initialisingClusterServer: Error occurred: " + e.getMessage(),
          e);
    }
  }
}
