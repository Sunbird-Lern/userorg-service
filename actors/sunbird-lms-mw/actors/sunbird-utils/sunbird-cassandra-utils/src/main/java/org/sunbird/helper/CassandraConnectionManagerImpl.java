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
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * @author Amit Kumar
 * @author Arvind
 */
public class CassandraConnectionManagerImpl implements CassandraConnectionManager {

  private String mode;
  private static Map<String, Session> cassandraSessionMap = new HashMap<>();
  private static Map<String, Cluster> cassandraclusterMap = new HashMap<>();

  static {
    registerShutDownHook();
  }

  public CassandraConnectionManagerImpl(String mode) {
    this.mode = mode;
  }

  /**
   * Method to create the cassandra connection on the basis of mode i.e. standalone or embedde read
   * from properties file .
   *
   * @param ip
   * @param port
   * @param userName
   * @param password
   * @param keyspace
   * @return boolean
   */
  @Override
  public boolean createConnection(
      String ip, String port, String userName, String password, String keyspace) {
    return createStandaloneConnection(ip, port, userName, password, keyspace);
  }

  /**
   * Method to create the standalone cassandra connection .
   *
   * @param ip
   * @param port
   * @param userName
   * @param password
   * @param keyspace
   * @return
   */
  private boolean createStandaloneConnection(
      String ip, String port, String userName, String password, String keyspace) {

    Session cassandraSession = null;
    boolean connection = false;
    Cluster cluster = null;
    try {
      if (null == cassandraSessionMap.get(keyspace)) {
        PropertiesCache cache = PropertiesCache.getInstance();
        PoolingOptions poolingOptions = new PoolingOptions();
        poolingOptions.setCoreConnectionsPerHost(
            HostDistance.LOCAL,
            Integer.parseInt(cache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)));
        poolingOptions.setMaxConnectionsPerHost(
            HostDistance.LOCAL,
            Integer.parseInt(cache.getProperty(Constants.MAX_CONNECTIONS_PER_HOST_FOR_LOCAl)));
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
        if (!StringUtils.isBlank(userName) && !StringUtils.isBlank(password)) {
          cluster = createCluster(ip, port, userName, password, poolingOptions);
        } else {
          cluster = createCluster(ip, port, poolingOptions);
        }
        cassandraSession = cluster.connect(keyspace);

        if (null != cassandraSession) {
          connection = true;
          cassandraSessionMap.put(keyspace, cassandraSession);
          cassandraclusterMap.put(keyspace, cluster);
        }
        final Metadata metadata = cluster.getMetadata();
        String msg = String.format("Connected to cluster: %s", metadata.getClusterName());
        ProjectLogger.log(msg);

        for (final Host host : metadata.getAllHosts()) {
          msg =
              String.format(
                  "Datacenter: %s; Host: %s; Rack: %s",
                  host.getDatacenter(), host.getAddress(), host.getRack());
          ProjectLogger.log(msg);
        }
      }
    } catch (Exception e) {
      ProjectLogger.log("Error occured while creating connection :", e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    if (null != cassandraSessionMap.get(keyspace)) {
      connection = true;
    }
    return connection;
  }

  private static ConsistencyLevel getConsistencyLevel() {
    String consistency = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL);

    ProjectLogger.log(
        "CassandraConnectionManagerImpl:getConsistencyLevel: level = " + consistency,
        LoggerEnum.INFO);

    if (StringUtils.isBlank(consistency)) return null;

    try {
      return ConsistencyLevel.valueOf(consistency.toUpperCase());
    } catch (IllegalArgumentException exception) {
      ProjectLogger.log(
          "CassandraConnectionManagerImpl:getConsistencyLevel: Exception occurred with error message = "
              + exception.getMessage(),
          LoggerEnum.ERROR);
    }
    return null;
  }

  /**
   * Create cassandra cluster with user credentials.
   *
   * @param ip IP address of cluster node
   * @param port Port of cluster node
   * @param userName DB username
   * @param password DB password
   * @param poolingOptions Pooling options
   * @return Cassandra cluster
   */
  private static Cluster createCluster(
      String ip, String port, String userName, String password, PoolingOptions poolingOptions) {
    Cluster.Builder builder =
        Cluster.builder()
            .addContactPoint(ip)
            .withPort(Integer.parseInt(port))
            .withProtocolVersion(ProtocolVersion.V3)
            .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
            .withTimestampGenerator(new AtomicMonotonicTimestampGenerator())
            .withPoolingOptions(poolingOptions);

    if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
      builder.withCredentials(userName, password);
    }

    ConsistencyLevel consistencyLevel = getConsistencyLevel();
    ProjectLogger.log(
        "CassandraConnectionManagerImpl:createCluster: Consistency level = " + consistencyLevel,
        LoggerEnum.INFO);

    if (consistencyLevel != null) {
      builder.withQueryOptions(new QueryOptions().setConsistencyLevel(consistencyLevel));
    }

    return builder.build();
  }

  /**
   * Create cassandra cluster.
   *
   * @param ip IP address of cluster node
   * @param port Port of cluster node
   * @param poolingOptions Pooling options
   * @return Cassandra cluster
   */
  private static Cluster createCluster(String ip, String port, PoolingOptions poolingOptions) {
    return createCluster(ip, port, null, null, poolingOptions);
  }

  @Override
  public Session getSession(String keyspaceName) {
    if (null == cassandraSessionMap.get(keyspaceName)) {
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          Constants.SESSION_IS_NULL + keyspaceName,
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return cassandraSessionMap.get(keyspaceName);
  }

  @Override
  public Cluster getCluster(String keyspaceName) {
    if (null == cassandraclusterMap.get(keyspaceName)) {
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          Constants.CLUSTER_IS_NULL + keyspaceName,
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return cassandraclusterMap.get(keyspaceName);
  }

  @Override
  public List<String> getTableList(String keyspacename) {
    Collection<TableMetadata> tables =
        cassandraclusterMap.get(keyspacename).getMetadata().getKeyspace(keyspacename).getTables();

    // to convert to list of the names
    return tables.stream().map(tm -> tm.getName()).collect(Collectors.toList());
  }

  /** Register the hook for resource clean up. this will be called when jvm shut down. */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
    ProjectLogger.log("Cassandra ShutDownHook registered.");
  }

  /**
   * This class will be called by registerShutDownHook to register the call inside jvm , when jvm
   * terminate it will call the run method to clean up the resource.
   */
  static class ResourceCleanUp extends Thread {
    @Override
    public void run() {
      ProjectLogger.log("started resource cleanup Cassandra.");
      for (Map.Entry<String, Session> entry : cassandraSessionMap.entrySet()) {
        cassandraSessionMap.get(entry.getKey()).close();
        cassandraclusterMap.get(entry.getKey()).close();
      }
      ProjectLogger.log("completed resource cleanup Cassandra.");
    }
  }
}
