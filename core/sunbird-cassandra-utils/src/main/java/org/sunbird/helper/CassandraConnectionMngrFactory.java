package org.sunbird.helper;

/**
 * Factory class for creating and managing the singleton instance of CassandraConnectionManager.
 * This class implements the double-checked locking pattern to ensure thread-safe lazy
 * initialization of the connection manager.
 *
 * <p>The factory ensures that only one instance of the Cassandra connection manager exists
 * throughout the application lifecycle, providing a centralized point for managing database
 * connections.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * CassandraConnectionManager connectionManager = CassandraConnectionMngrFactory.getInstance();
 * Session session = connectionManager.getSession("myKeyspace");
 * </pre>
 *
 * <p><b>Thread Safety:</b> This implementation uses double-checked locking with volatile
 * to ensure thread-safe singleton initialization without excessive synchronization overhead.
 */
public final class CassandraConnectionMngrFactory {

  /**
   * Singleton instance of CassandraConnectionManager.
   * Volatile ensures visibility of changes across threads.
   */
  private static volatile CassandraConnectionManager instance;

  /**
   * Private constructor to prevent instantiation.
   * This class should only be used through its static factory method.
   */
  private CassandraConnectionMngrFactory() {
    // Private constructor to prevent instantiation
  }

  /**
   * Returns the singleton instance of CassandraConnectionManager.
   * Uses double-checked locking pattern for thread-safe lazy initialization.
   *
   * <p>The first check (outside synchronized block) avoids unnecessary synchronization
   * once the instance is initialized. The second check (inside synchronized block)
   * ensures only one thread creates the instance.
   *
   * @return The singleton CassandraConnectionManager instance.
   */
  public static CassandraConnectionManager getInstance() {
    if (instance == null) {
      synchronized (CassandraConnectionMngrFactory.class) {
        if (instance == null) {
          instance = new CassandraConnectionManagerImpl();
        }
      }
    }
    return instance;
  }
}
