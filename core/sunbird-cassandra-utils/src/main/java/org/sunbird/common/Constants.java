package org.sunbird.common;

/**
 * Constants for the Sunbird Cassandra Utils module.
 */
public interface Constants {

  // ===========================================================================
  // CASSANDRA CONFIGURATION PROPERTIES
  // ===========================================================================
  String CASSANDRA_PROPERTIES_FILE = "cassandra.config.properties";
  String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
  String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
  String MAX_CONNECTIONS_PER_HOST_FOR_LOCAL = "maxConnectionsPerHostForLocal";
  String MAX_CONNECTIONS_PER_HOST_FOR_REMOTE = "maxConnectionsPerHostForRemote";
  String MAX_REQUEST_PER_CONNECTION = "maxRequestsPerConnection";
  String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
  String POOL_TIMEOUT = "poolTimeoutMillis";
  String CONTACT_POINT = "contactPoint";
  String PORT = "port";
  String QUERY_LOGGER_THRESHOLD = "queryLoggerConstantThreshold";
  String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "sunbird_cassandra_consistency_level";
  String IS_MULTI_DC_ENABLED = "isMultiDCEnabled";
  String STANDALONE_MODE = "standalone";

  // ===========================================================================
  // COMMON IDENTIFIERS & KEYS
  // ===========================================================================
  String ID = "id";
  String IDENTIFIER = "id";
  String COURSE_ID = "courseId";
  String USER_ID = "userId";
  String CONTENT_ID = "contentId";
  String OBJECT_TYPE = "objectType";
  String PRIMARY_KEY = "PK";
  String NON_PRIMARY_KEY = "NonPK";

  // ===========================================================================
  // CASSANDRA QUERY SYNTAX & SYMBOLS
  // ===========================================================================
  String INSERT_INTO = "INSERT INTO ";
  String UPDATE = "UPDATE ";
  String SELECT = "SELECT ";
  String FROM = " FROM ";
  String WHERE = " where ";
  String SET = " SET ";
  String WHERE_ID = "where id";
  String INSERT = "insert"; // Operation name

  String QUE_MARK = "?";
  String DOT = ".";
  String COMMA = ",";
  String COMMA_WITH_SPACE = ", ";
  String SEMICOLON = ";";
  String OPEN_BRACE = "(";
  String OPEN_BRACE_WITH_SPACE = " (";
  String CLOSING_BRACE = ");";
  String VALUES_WITH_BRACE = ") VALUES (";
  String COMMA_BRAC = "),";
  String EQUAL = " = ";
  String EQUAL_WITH_QUE_MARK = " = ? ";

  String IF_EXISTS = " IF EXISTS;";
  String IF_NOT_EXISTS = " IF NOT EXISTS;";

  String LTE = "<=";
  String LT = "<";
  String GTE = ">=";
  String GT = ">";

  // ===========================================================================
  // ERROR MESSAGES & STATUS
  // ===========================================================================
  String SUCCESS = "SUCCESS";
  String RESPONSE = "response";
  
  String SESSION_IS_NULL = "cassandra session is null for this ";
  String CLUSTER_IS_NULL = "cassandra cluster value is null for this ";
  String INCORRECT_DATA = "Incorrect Data";
  String ALREADY_EXIST = "Record with this primary key already exist.";
  String UNKNOWN_IDENTIFIER = "Unknown identifier ";
  String UNDEFINED_IDENTIFIER = "Undefined column name ";

  String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
  String EXCEPTION_MSG_UPSERT = "Exception occurred while upserting record from ";
  String EXCEPTION_MSG_DELETE = "Exception occurred while deleting record from ";
  String EXCEPTION_MSG_UPDATE = "Exception occurred while updating record to ";

}

