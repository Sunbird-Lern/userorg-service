package org.sunbird.helper;

import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;

/** Created by arvind on 10/10/17. */
public class CassandraConnectionMngrFactory {

  private static Map<String, CassandraConnectionManager> connectionFactoryMap = new HashMap<>();

  /**
   * Factory method to get the cassandra connection manager oject on basis of mode name pass in
   * argument . default mode is standalone mode .
   *
   * @param name
   * @return
   */
  public static CassandraConnectionManager getObject(String name) {

    if (!connectionFactoryMap.containsKey(name)) {
      // create object
      synchronized (CassandraConnectionMngrFactory.class) {
        if (name.equalsIgnoreCase(JsonKey.EMBEDDED_MODE)) {
          String mode = JsonKey.EMBEDDED_MODE;
          if (null == connectionFactoryMap.get(JsonKey.EMBEDDED_MODE)) {
            CassandraConnectionManager embeddedcassandraConnectionManager =
                new CassandraConnectionManagerImpl(mode);
            connectionFactoryMap.put(JsonKey.EMBEDDED_MODE, embeddedcassandraConnectionManager);
          }
        } else {
          if (null == connectionFactoryMap.get(JsonKey.STANDALONE_MODE)) {
            String mode = JsonKey.STANDALONE_MODE;
            CassandraConnectionManager standaloneCassandraConnectionManager =
                new CassandraConnectionManagerImpl(mode);
            connectionFactoryMap.put(JsonKey.STANDALONE_MODE, standaloneCassandraConnectionManager);
          }
        }
      }
    }
    return connectionFactoryMap.get(name);
  }
}
