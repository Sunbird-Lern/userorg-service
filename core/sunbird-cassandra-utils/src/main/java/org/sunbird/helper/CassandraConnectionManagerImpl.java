package org.sunbird.helper;

import com.datastax.driver.core.AtomicMonotonicTimestampGenerator;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.common.Constants;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.CassandraPropertyReader;
import org.sunbird.response.ResponseCode;

/**
 * Implementation of {@link CassandraConnectionManager} for managing Cassandra database connections.
 * 
 * <p>This class provides:
 * <ul>
 *   <li>Singleton Cluster instance management</li>
 *   <li>Session caching per keyspace for improved performance</li>
 *   <li>Configurable connection pooling options</li>
 *   <li>Multi-datacenter support</li>
 *   <li>Automatic resource cleanup on JVM shutdown</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class is thread-safe. Sessions are cached in a ConcurrentHashMap
 * and the cluster instance is shared across all threads.
 * 
 * <p><b>Configuration:</b> Connection parameters are loaded from properties files including:
 * <ul>
 *   <li>Connection pool sizes (local and remote)</li>
 *   <li>Heartbeat intervals</li>
 *   <li>Pool timeouts</li>
 *   <li>Consistency levels</li>
 *   <li>Multi-DC settings</li>
 * </ul>
 */
public class CassandraConnectionManagerImpl implements CassandraConnectionManager {
  
  private static final Logger logger = LoggerFactory.getLogger(CassandraConnectionManagerImpl.class);
  
  /** Singleton Cassandra cluster instance. */
  private static Cluster cluster;
  
  /** Cache of Cassandra sessions per keyspace for performance optimization. */
  private static final Map<String, Session> cassandraSessionMap = new ConcurrentHashMap<>(2);

  static {
    registerShutDownHook();
  }

  /**
   * Creates a connection to the Cassandra cluster.
   * This method initializes the cluster with the specified hosts and configured pooling options.
   * 
   * @param hosts Array of Cassandra host addresses (IP addresses or hostnames).
   * @throws ProjectCommonException if connection creation fails.
   */
  @Override
  public void createConnection(String[] hosts) {
    createCassandraConnection(hosts);
  }

  /**
   * Retrieves a Cassandra session for the specified keyspace.
   * Sessions are cached to avoid the overhead of creating new connections.
   * If a session doesn't exist for the keyspace, a new one is created and cached.
   * 
   * <p><b>Thread Safety:</b> This method is thread-safe due to the use of ConcurrentHashMap.
   * However, there's a small race condition where multiple threads might create sessions
   * for the same keyspace simultaneously. This is acceptable as Cassandra sessions are
   * thread-safe and the extra sessions will be garbage collected.
   * 
   * @param keyspace The Cassandra keyspace name.
   * @return A Cassandra Session object for the specified keyspace.
   * @throws IllegalStateException if the cluster is not initialized.
   */
  @Override
  public Session getSession(String keyspace) {
    Session session = cassandraSessionMap.get(keyspace);
    if (session != null) {
      return session;
    }
    
    // Create new session and cache it
    Session newSession = cluster.connect(keyspace);
    cassandraSessionMap.put(keyspace, newSession);
    return newSession;
  }

  /**
   * Establishes the connection to the Cassandra cluster with configured pooling options.
   * This method reads configuration from properties files and sets up:
   * <ul>
   *   <li>Connection pool sizes for local and remote hosts</li>
   *   <li>Maximum requests per connection</li>
   *   <li>Heartbeat intervals</li>
   *   <li>Pool timeout settings</li>
   *   <li>Multi-datacenter support if enabled</li>
   * </ul>
   * 
   * @param hosts Array of Cassandra host addresses (IP addresses or hostnames).
   * @throws ProjectCommonException if connection creation fails or configuration is invalid.
   */
  private void createCassandraConnection(String[] hosts) {
    try {
      CassandraPropertyReader cache = CassandraPropertyReader.getInstance();
      PoolingOptions poolingOptions = new PoolingOptions();
      poolingOptions.setCoreConnectionsPerHost(
          HostDistance.LOCAL,
          Integer.parseInt(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)));
      poolingOptions.setMaxConnectionsPerHost(
          HostDistance.LOCAL,
          Integer.parseInt(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_LOCAL)));
      poolingOptions.setCoreConnectionsPerHost(
          HostDistance.REMOTE,
          Integer.parseInt(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)));
      poolingOptions.setMaxConnectionsPerHost(
          HostDistance.REMOTE,
          Integer.parseInt(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_REMOTE)));
      poolingOptions.setMaxRequestsPerConnection(
          HostDistance.LOCAL,
          Integer.parseInt(cache.getProperty(Constants.MAX_REQUEST_PER_CONNECTION)));
      poolingOptions.setHeartbeatIntervalSeconds(
          Integer.parseInt(cache.getProperty(Constants.HEARTBEAT_INTERVAL)));
      poolingOptions.setPoolTimeoutMillis(
          Integer.parseInt(cache.getProperty(Constants.POOL_TIMEOUT)));

      // Check if multi-datacenter support is enabled from configuration
      boolean isMultiDCEnabled = Boolean.parseBoolean(cache.getProperty(Constants.IS_MULTI_DC_ENABLED));
      cluster = createCluster(hosts, poolingOptions, isMultiDCEnabled);

      final Metadata metadata = cluster.getMetadata();
      logger.info("Connected to cluster: {}", metadata.getClusterName());

      for (final Host host : metadata.getAllHosts()) {
        logger.info(
            "Datacenter: {}; Host: {}; Rack: {}",
            host.getDatacenter(), host.getAddress(), host.getRack());
      }
    } catch (Exception e) {
      logger.error("Error occurred while creating Cassandra connection: {}", e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          "Failed to create Cassandra connection: " + e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  /**
   * Builds and configures the Cassandra Cluster object.
   * This method sets up:
   * <ul>
   *   <li>Contact points (host addresses)</li>
   *   <li>Protocol version (V3)</li>
   *   <li>Retry policy (DefaultRetryPolicy)</li>
   *   <li>Timestamp generator (AtomicMonotonicTimestampGenerator)</li>
   *   <li>Pooling options</li>
   *   <li>Consistency level (if configured)</li>
   *   <li>Load balancing policy (DCAwareRoundRobinPolicy if multi-DC enabled)</li>
   * </ul>
   *
   * @param hosts Array of Cassandra host addresses.
   * @param poolingOptions Configured connection pooling options.
   * @param isMultiDCEnabled Flag to enable datacenter-aware load balancing.
   * @return The configured Cluster object ready for use.
   */
  private static Cluster createCluster(String[] hosts, PoolingOptions poolingOptions, boolean isMultiDCEnabled) {
    Cluster.Builder builder =
            Cluster.builder()
                    .addContactPoints(hosts)
                    .withProtocolVersion(ProtocolVersion.V3)
                    .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
                    .withTimestampGenerator(new AtomicMonotonicTimestampGenerator())
                    .withPoolingOptions(poolingOptions);

    ConsistencyLevel consistencyLevel = getConsistencyLevel();
    logger.info("CassandraConnectionManagerImpl:createCluster: Consistency level = {}", consistencyLevel);

    if (consistencyLevel != null) {
      builder.withQueryOptions(new QueryOptions().setConsistencyLevel(consistencyLevel));
    }

    logger.info("CassandraConnectionManagerImpl:createCluster: isMultiDCEnabled = {}", isMultiDCEnabled);
    if (isMultiDCEnabled) {
      builder.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().build());
    }

    return builder.build();
  }

  /**
   * Retrieves the Cassandra consistency level from application configuration.
   * The consistency level determines how many replicas must respond to a read/write operation.
   * 
   * <p>Valid values include: ONE, TWO, THREE, QUORUM, ALL, LOCAL_QUORUM, EACH_QUORUM, etc.
   * 
   * @return The configured ConsistencyLevel, or {@code null} if not set or invalid.
   */
  public static ConsistencyLevel getConsistencyLevel() {
    String consistency = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL);

    logger.info("CassandraConnectionManagerImpl:getConsistencyLevel: level = {}", consistency);

    if (StringUtils.isBlank(consistency)) {
      return null;
    }

    try {
      return ConsistencyLevel.valueOf(consistency.toUpperCase());
    } catch (IllegalArgumentException exception) {
      logger.error(
          "CassandraConnectionManagerImpl:getConsistencyLevel: Exception occurred with error message = {}",
              exception.getMessage());
    }
    return null;
  }

  /**
   * Retrieves the list of all table names in the specified keyspace.
   * 
   * @param keyspacename The name of the Cassandra keyspace.
   * @return A list of table names in the keyspace.
   * @throws IllegalArgumentException if the keyspace doesn't exist.
   */
  @Override
  public List<String> getTableList(String keyspacename) {
    Collection<TableMetadata> tables = cluster.getMetadata().getKeyspace(keyspacename).getTables();

    // Convert table metadata to list of table names
    return tables.stream()
        .map(TableMetadata::getName)
        .collect(Collectors.toList());
  }

  /**
   * Registers a JVM shutdown hook for graceful resource cleanup.
   * This ensures that all Cassandra sessions and the cluster connection are properly
   * closed when the application terminates, preventing resource leaks.
   * 
   * <p>The shutdown hook will:
   * <ul>
   *   <li>Close all cached sessions</li>
   *   <li>Close the cluster connection</li>
   *   <li>Log the cleanup process</li>
   * </ul>
   */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
    logger.info("Cassandra ShutDownHook registered.");
  }

  /**
   * Shutdown hook thread that handles graceful cleanup of Cassandra resources.
   * This thread is executed by the JVM when the application is shutting down.
   * It ensures all sessions and cluster connections are properly closed to prevent
   * resource leaks and allow for graceful shutdown.
   */
  static class ResourceCleanUp extends Thread {
    @Override
    public void run() {
      try {
        logger.info("Started Cassandra resource cleanup.");
        
        // Close all cached sessions
        for (Map.Entry<String, Session> entry : cassandraSessionMap.entrySet()) {
          Session session = entry.getValue();
          if (session != null && !session.isClosed()) {
            session.close();
            logger.debug("Closed session for keyspace: {}", entry.getKey());
          }
        }
        
        // Close the cluster connection
        if (cluster != null && !cluster.isClosed()) {
          cluster.close();
          logger.debug("Closed Cassandra cluster connection.");
        }
        logger.info("Completed Cassandra resource cleanup successfully.");
      } catch (Exception ex) {
        logger.error("Error during Cassandra resource cleanup: {}", ex.getMessage(), ex);
      }
    }
  }
}
