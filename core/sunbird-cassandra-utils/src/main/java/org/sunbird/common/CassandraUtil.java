package org.sunbird.common;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.cassandraannotation.ClusteringKey;
import org.sunbird.cassandraannotation.PartitioningKey;
import org.sunbird.common.Constants;
import org.sunbird.common.CassandraPropertyReader;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseCode;

/**
 * Utility class providing helper methods for Cassandra database operations.
 * This class offers functionality for:
 * <ul>
 *   <li>Generating prepared statements and queries</li>
 *   <li>Converting ResultSets to Response objects</li>
 *   <li>Handling composite primary keys using annotations</li>
 *   <li>Building dynamic WHERE clauses with various operators</li>
 *   <li>Column mapping and transformation</li>
 * </ul>
 * All methods are static and the class cannot be instantiated.
 */
public final class CassandraUtil {

  private static final Logger logger = LoggerFactory.getLogger(CassandraUtil.class);
  private static final CassandraPropertyReader propertiesCache =
      CassandraPropertyReader.getInstance();
  private static final String SERIAL_VERSION_UID = "serialVersionUID";

  /** Private constructor to prevent instantiation. */
  private CassandraUtil() {}

  /**
   * Generates a prepared INSERT statement for Cassandra.
   * The statement uses placeholders (?) for values to enable prepared statement usage.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where data will be inserted.
   * @param map A map where keys are column names and values are the data to insert.
   * @return A prepared statement string with placeholders for values.
   */
  public static String getPreparedStatement(
      String keyspaceName, String tableName, Map<String, Object> map) {
    StringBuilder query = new StringBuilder();
    query.append(
        Constants.INSERT_INTO + keyspaceName + Constants.DOT + tableName + Constants.OPEN_BRACE);
    Set<String> keySet = map.keySet();
    query.append(String.join(",", keySet) + Constants.VALUES_WITH_BRACE);
    StringBuilder commaSepValueBuilder = new StringBuilder();
    for (int i = 0; i < keySet.size(); i++) {
      commaSepValueBuilder.append(Constants.QUE_MARK);
      if (i != keySet.size() - 1) {
        commaSepValueBuilder.append(Constants.COMMA);
      }
    }
    query.append(commaSepValueBuilder + Constants.CLOSING_BRACE);
    logger.info("Generated prepared statement: {}", query);
    return query.toString();
  }

  /**
   * Converts a Cassandra ResultSet into a Response object.
   * Each row is transformed into a Map with column names mapped to their corresponding
   * property names using the CassandraPropertyReader.
   *
   * @param results The Cassandra ResultSet to convert.
   * @return A Response object containing a list of maps, each representing a row.
   */
  public static Response createResponse(ResultSet results) {
    Response response = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, String> columnsMapping = fetchColumnsMapping(results);
    Iterator<Row> rowIterator = results.iterator();
    rowIterator.forEachRemaining(
        row -> {
          Map<String, Object> rowMap = new HashMap<>();
          columnsMapping
              .entrySet()
              .stream()
              .forEach(entry -> rowMap.put(entry.getKey(), row.getObject(entry.getValue())));
          responseList.add(rowMap);
        });
    logger.info("Total rows fetched from cassandra: {}", responseList.size());
    response.put(Constants.RESPONSE, responseList);
    return response;
  }

  /**
   * Extracts column name mappings from a ResultSet.
   * Maps Cassandra column names to their corresponding property names using
   * the cassandratablecolumn.properties file.
   *
   * @param results The ResultSet containing column definitions.
   * @return A map where keys are property names and values are Cassandra column names.
   */
  public static Map<String, String> fetchColumnsMapping(ResultSet results) {
    return results
        .getColumnDefinitions()
        .asList()
        .stream()
        .collect(
            Collectors.toMap(
                d -> propertiesCache.readProperty(d.getName()).trim(), d -> d.getName()));
  }

  /**
   * Generates a prepared UPDATE statement for Cassandra.
   * The statement uses placeholders (?) for values and assumes the map contains
   * an 'identifier' key for the WHERE clause.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where data will be updated.
   * @param map A map of column names to update (must include 'identifier' key).
   * @return A prepared UPDATE statement string with placeholders.
   */
  public static String getUpdateQueryStatement(
      String keyspaceName, String tableName, Map<String, Object> map) {
    StringBuilder query =
        new StringBuilder(
            Constants.UPDATE + keyspaceName + Constants.DOT + tableName + Constants.SET);
    Set<String> key = new HashSet<>(map.keySet());
    key.remove(Constants.IDENTIFIER);
    query.append(String.join(" = ? ,", key));
    query.append(
        Constants.EQUAL_WITH_QUE_MARK + Constants.WHERE_ID + Constants.EQUAL_WITH_QUE_MARK);
    logger.info("Generated update query: {}", query);
    return query.toString();
  }

  /**
   * Generates a prepared SELECT statement for specific columns.
   * The statement includes a WHERE clause on the 'identifier' column.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param properties A list of column names to select.
   * @return A prepared SELECT statement string with a placeholder for the identifier.
   */
  public static String getSelectStatement(
      String keyspaceName, String tableName, List<String> properties) {
    StringBuilder query = new StringBuilder(Constants.SELECT);
    query.append(String.join(",", properties));
    query.append(
        Constants.FROM
            + keyspaceName
            + Constants.DOT
            + tableName
            + Constants.WHERE
            + Constants.IDENTIFIER
            + Constants.EQUAL
            + " ?; ");
    logger.info("Generated select statement: {}", query);
    return query.toString();
  }

  /**
   * Generates a prepared SELECT statement for specific columns (varargs version).
   * Convenience method that accepts column names as varargs instead of a List.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param properties Varargs of column names to select.
   * @return A prepared SELECT statement string with a placeholder for the identifier.
   */
  public static String getSelectStatement(
      String keyspaceName, String tableName, String... properties) {
    return getSelectStatement(keyspaceName, tableName, Arrays.asList(properties));
  }

  /**
   * Processes exceptions to extract and format unknown identifier errors.
   * Removes technical identifiers from the error message to make it user-friendly.
   *
   * @param e The exception containing the unknown identifier error.
   * @return A formatted, user-friendly error message.
   */
  public static String processExceptionForUnknownIdentifier(Exception e) {
    String msg = e.getMessage() == null ? "" : e.getMessage();
    return MessageFormat.format(
            ResponseCode.invalidPropertyError.getErrorMessage(),
            msg
                .replace(JsonKey.UNKNOWN_IDENTIFIER, "")
                .replace(JsonKey.UNDEFINED_IDENTIFIER, ""))
        .trim();
  }

  /**
   * Extracts primary key and non-primary key fields from a model object.
   * Uses reflection to identify fields annotated with {@link PartitioningKey}
   * or {@link ClusteringKey} as primary key components.
   *
   * @param <T> The type of the model class.
   * @param clazz The model object instance.
   * @return A map with two keys: 'primaryKey' containing PK fields and 'nonPrimaryKey' containing other fields.
   * @throws ProjectCommonException if reflection fails.
   */
  public static <T> Map<String, Map<String, Object>> batchUpdateQuery(T clazz) {
    Field[] fieldList = clazz.getClass().getDeclaredFields();

    Map<String, Object> primaryKeyMap = new HashMap<>();
    Map<String, Object> nonPKMap = new HashMap<>();
    try {
      for (Field field : fieldList) {
        if (Modifier.isPrivate(field.getModifiers())) {
          field.setAccessible(true);
        }
        
        boolean isFieldPrimaryKeyPart = false;
        Annotation[] annotations = field.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof PartitioningKey || annotation instanceof ClusteringKey) {
            isFieldPrimaryKeyPart = true;
            break;
          }
        }
        
        String fieldName = field.getName();
        if (!fieldName.equalsIgnoreCase(SERIAL_VERSION_UID)) {
          Object fieldValue = field.get(clazz);
          if (isFieldPrimaryKeyPart) {
            primaryKeyMap.put(fieldName, fieldValue);
          } else {
            nonPKMap.put(fieldName, fieldValue);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Exception occurred - batchUpdateQuery", ex);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    Map<String, Map<String, Object>> map = new HashMap<>();
    map.put(JsonKey.PRIMARY_KEY, primaryKeyMap);
    map.put(JsonKey.NON_PRIMARY_KEY, nonPKMap);
    return map;
  }

  /**
   * Extracts only the primary key fields from a model object.
   * Uses reflection to identify fields annotated with {@link PartitioningKey}
   * or {@link ClusteringKey}.
   *
   * @param <T> The type of the model class.
   * @param clazz The model object instance.
   * @return A map containing only the primary key field names and values.
   * @throws ProjectCommonException if reflection fails.
   */
  public static <T> Map<String, Object> getPrimaryKey(T clazz) {
    Field[] fieldList = clazz.getClass().getDeclaredFields();
    Map<String, Object> primaryKeyMap = new HashMap<>();

    try {
      for (Field field : fieldList) {
        if (Modifier.isPrivate(field.getModifiers())) {
          field.setAccessible(true);
        }
        
        boolean isFieldPrimaryKeyPart = false;
        Annotation[] annotations = field.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof PartitioningKey || annotation instanceof ClusteringKey) {
            isFieldPrimaryKeyPart = true;
            break;
          }
        }
        
        String fieldName = field.getName();
        if (!fieldName.equalsIgnoreCase(SERIAL_VERSION_UID) && isFieldPrimaryKeyPart) {
          Object fieldValue = field.get(clazz);
          primaryKeyMap.put(fieldName, fieldValue);
        }
      }
    } catch (Exception ex) {
      logger.error("Exception occurred - getPrimaryKey", ex);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return primaryKeyMap;
  }

  /**
   * Dynamically builds WHERE clause conditions based on the value type.
   * Supports:
   * <ul>
   *   <li>Map values for comparison operators (LTE, LT, GTE, GT)</li>
   *   <li>List values for IN clauses</li>
   *   <li>Simple values for equality checks</li>
   * </ul>
   *
   * @param key The column name for the condition.
   * @param value The value to compare (Map for operators, List for IN, or simple value for EQ).
   * @param where The WHERE clause builder to append conditions to.
   */
  public static void createWhereQuery(String key, Object value, Where where) {
    if (value instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) value;
      map.entrySet()
          .stream()
          .forEach(
              x -> {
                if (Constants.LTE.equalsIgnoreCase(x.getKey())) {
                  where.and(QueryBuilder.lte(key, x.getValue()));
                } else if (Constants.LT.equalsIgnoreCase(x.getKey())) {
                  where.and(QueryBuilder.lt(key, x.getValue()));
                } else if (Constants.GTE.equalsIgnoreCase(x.getKey())) {
                  where.and(QueryBuilder.gte(key, x.getValue()));
                } else if (Constants.GT.equalsIgnoreCase(x.getKey())) {
                  where.and(QueryBuilder.gt(key, x.getValue()));
                }
              });
    } else if (value instanceof List) {
      where.and(QueryBuilder.in(key, (List) value));
    } else {
      where.and(QueryBuilder.eq(key, value));
    }
  }

  /**
   * Alias for {@link #createWhereQuery(String, Object, Where)}.
   * Provided for backward compatibility.
   *
   * @param key The column name for the condition.
   * @param value The value to compare.
   * @param where The WHERE clause builder to append conditions to.
   */
  public static void createQuery(String key, Object value, Where where) {
    createWhereQuery(key, value, where);
  }

  /**
   * Constructs a Cassandra UPDATE statement using QueryBuilder.
   * The statement updates non-primary key fields for rows matching the primary key.
   *
   * @param primaryKey A map of primary key column names to their values (for WHERE clause).
   * @param nonPKRecord A map of non-primary key column names to their new values (for SET clause).
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to update.
   * @return A RegularStatement representing the UPDATE query.
   */
  public static RegularStatement createUpdateQuery(
      Map<String, Object> primaryKey,
      Map<String, Object> nonPKRecord,
      String keyspaceName,
      String tableName) {

    Update update = QueryBuilder.update(keyspaceName, tableName);
    Assignments assignments = update.with();
    Update.Where where = update.where();
    nonPKRecord
        .entrySet()
        .stream()
        .forEach(
            x -> {
              assignments.and(QueryBuilder.set(x.getKey(), x.getValue()));
            });
    primaryKey
        .entrySet()
        .stream()
        .forEach(
            x -> {
              where.and(QueryBuilder.eq(x.getKey(), x.getValue()));
            });
    return where;
  }

  /**
   * Constructs a Cassandra DELETE statement using QueryBuilder.
   * The statement deletes rows matching the specified primary key.
   *
   * @param primaryKey A map of primary key column names to their values (for WHERE clause).
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name from which to delete.
   * @return A RegularStatement representing the DELETE query.
   */
  public static RegularStatement createDeleteQuery(
      Map<String, Object> primaryKey, String keyspaceName, String tableName) {

    Delete delete = QueryBuilder.delete().from(keyspaceName, tableName);
    Delete.Where where = delete.where();
    primaryKey
        .entrySet()
        .stream()
        .forEach(
            x -> {
              where.and(QueryBuilder.eq(x.getKey(), x.getValue()));
            });
    return where;
  }

  /**
   * Transforms a map by converting property names to Cassandra column names.
   * Uses the cassandratablecolumn.properties file for the reverse mapping.
   *
   * @param map The original map with property names as keys.
   * @return A new map with Cassandra column names as keys and the same values.
   */
  public static Map<String, Object> changeCassandraColumnMapping(Map<String, Object> map) {
    Map<String, Object> newMap = new HashMap<>();
    map.entrySet()
        .forEach(
            entry ->
                newMap.put(
                    propertiesCache.readPropertyValue(entry.getKey()), entry.getValue()));
    return newMap;
  }
}

