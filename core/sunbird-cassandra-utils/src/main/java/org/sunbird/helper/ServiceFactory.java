package org.sunbird.helper;

import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraDACImpl;

/**
 * Factory class for creating and managing the singleton instance of CassandraOperation.
 * This class implements the double-checked locking pattern to ensure thread-safe lazy
 * initialization of the Cassandra data access component.
 *
 * <p>The factory ensures that only one instance of the CassandraOperation implementation
 * (CassandraDACImpl) exists throughout the application lifecycle, providing a centralized
 * point for all Cassandra database operations including CRUD, batch processing, and
 * advanced query operations.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * CassandraOperation cassandraOperation = ServiceFactory.getInstance();
 * Response response = cassandraOperation.getRecordById("keyspace", "table", "id", requestContext);
 * </pre>
 *
 * <p><b>Thread Safety:</b> This implementation uses double-checked locking with volatile
 * to ensure thread-safe singleton initialization without excessive synchronization overhead.
 *
 * @author Manzarul
 */
public final class ServiceFactory {

  /**
   * Singleton instance of CassandraOperation.
   * Volatile ensures visibility of changes across threads.
   */
  private static volatile CassandraOperation operation = null;

  /**
   * Private constructor to prevent instantiation.
   * This class should only be used through its static factory method.
   */
  private ServiceFactory() {
    // Private constructor to prevent instantiation
  }

  /**
   * Returns the singleton instance of CassandraOperation.
   * Uses double-checked locking pattern for thread-safe lazy initialization.
   *
   * <p>The first check (outside synchronized block) avoids unnecessary synchronization
   * once the instance is initialized. The second check (inside synchronized block)
   * ensures only one thread creates the instance.
   *
   * <p>The returned instance is of type CassandraDACImpl, which extends CassandraOperationImpl
   * and provides additional specialized operations beyond the base implementation.
   *
   * @return The singleton CassandraOperation instance (CassandraDACImpl implementation).
   */
  public static CassandraOperation getInstance() {
    if (operation == null) {
      synchronized (ServiceFactory.class) {
        if (operation == null) {
          operation = new CassandraDACImpl();
        }
      }
    }
    return operation;
  }
}
