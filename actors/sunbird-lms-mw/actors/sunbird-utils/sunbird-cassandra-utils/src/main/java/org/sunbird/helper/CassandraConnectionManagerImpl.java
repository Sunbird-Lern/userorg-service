package org.sunbird.helper;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.*;
import org.sunbird.common.responsecode.ResponseCode;

public class CassandraConnectionManagerImpl implements CassandraConnectionManager {
  private static Cluster cluster;
  private static Map<String, Session> cassandraSessionMap = new ConcurrentHashMap<>(2);

  static {
    registerShutDownHook();
  }

  @Override
  public void createConnection(String[] hosts) {
    createCassandraConnection(hosts);
  }

  @Override
  public Session getSession(String keyspace) {
    Session session = cassandraSessionMap.get(keyspace);
    if (null != session) {
      return session;
    } else {
      Session session2 = cluster.connect(keyspace);
      cassandraSessionMap.put(keyspace, session2);
      return session2;
    }
  }

  private void createCassandraConnection(String[] hosts) {
    try {
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

      cluster = createCluster(hosts, poolingOptions);

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
    } catch (Exception e) {
      ProjectLogger.log("Error occured while creating cassandra connection :", e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private static Cluster createCluster(String[] hosts, PoolingOptions poolingOptions) {
    Cluster.Builder builder =
        Cluster.builder()
            .addContactPoints(hosts)
            .withProtocolVersion(ProtocolVersion.V3)
            .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
            .withTimestampGenerator(new AtomicMonotonicTimestampGenerator())
            .withPoolingOptions(poolingOptions);

    ConsistencyLevel consistencyLevel = getConsistencyLevel();
    ProjectLogger.log(
        "CassandraConnectionManagerImpl:createCluster: Consistency level = " + consistencyLevel,
        LoggerEnum.INFO);

    if (consistencyLevel != null) {
      builder.withQueryOptions(new QueryOptions().setConsistencyLevel(consistencyLevel));
    }

    return builder.build();
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

  @Override
  public List<String> getTableList(String keyspacename) {
    Collection<TableMetadata> tables = cluster.getMetadata().getKeyspace(keyspacename).getTables();

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
      try {
        ProjectLogger.log("started resource cleanup Cassandra.");
        for (Map.Entry<String, Session> entry : cassandraSessionMap.entrySet()) {
          cassandraSessionMap.get(entry.getKey()).close();
        }
        cluster.close();
        ProjectLogger.log("completed resource cleanup Cassandra.");
      } catch (Exception ex) {
        ProjectLogger.log("Error :", ex);
      }
    }
  }
}
