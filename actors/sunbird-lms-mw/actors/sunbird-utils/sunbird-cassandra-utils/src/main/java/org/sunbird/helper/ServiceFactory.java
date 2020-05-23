package org.sunbird.helper;

import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraDACImpl;

/**
 * This class will provide cassandraOperationImpl instance.
 *
 * @author Manzarul
 */
public class ServiceFactory {
  private static CassandraOperation operation = null;

  private ServiceFactory() {}

  /**
   * On call of this method , it will provide a new CassandraOperationImpl instance on each call.
   *
   * @return
   */
  public static CassandraOperation getInstance() {
    if (null == operation) {
      synchronized (ServiceFactory.class) {
        if (null == operation) {
          operation = new CassandraDACImpl();
        }
      }
    }
    return operation;
  }

  public CassandraOperation readResolve() {
    return getInstance();
  }
}
