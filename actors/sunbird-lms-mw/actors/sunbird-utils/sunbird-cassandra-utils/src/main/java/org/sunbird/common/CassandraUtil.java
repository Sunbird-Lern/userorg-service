package org.sunbird.common;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sunbird.cassandraannotation.ClusteringKey;
import org.sunbird.cassandraannotation.PartitioningKey;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.CassandraPropertyReader;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * @desc This class will provide all required helper method for cassandra db operation.
 * @author Amit Kumar
 */
public final class CassandraUtil {

  private static final CassandraPropertyReader propertiesCache =
      CassandraPropertyReader.getInstance();
  private static final String SERIAL_VERSION_UID = "serialVersionUID";

  private CassandraUtil() {}

  /**
   * @desc This method is used to create prepared statement based on table name and column name
   *     provided in request
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param map Map where key is column name and value is column value
   * @return Prepared statement
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
    ProjectLogger.log(query.toString());
    return query.toString();
  }

  /**
   * @desc This method is used for creating response from the resultset i.e return map
   *     <String,Object> or map<columnName,columnValue>
   * @param results ResultSet
   * @return Response Response
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
    ProjectLogger.log(responseList.toString());
    response.put(Constants.RESPONSE, responseList);
    return response;
  }

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
   * @desc This method is used to create update query statement based on table name and column name
   *     provided
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param map Map<String, Object>
   * @return String String
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
    ProjectLogger.log(query.toString());
    return query.toString();
  }

  /**
   * @desc This method is used to create prepared statement based on table name and column name
   *     provided as varargs
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param properties(String varargs)
   * @return String String
   */
  public static String getSelectStatement(
      String keyspaceName, String tableName, String... properties) {
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
    ProjectLogger.log(query.toString());
    return query.toString();
  }

  public static String processExceptionForUnknownIdentifier(Exception e) {
    // Unknown identifier
    return ProjectUtil.formatMessage(
            ResponseCode.invalidPropertyError.getErrorMessage(),
            e.getMessage()
                .replace(JsonKey.UNKNOWN_IDENTIFIER, "")
                .replace(JsonKey.UNDEFINED_IDENTIFIER, ""))
        .trim();
  }

  /**
   * Method to create the update query for composite keys. Create two separate map one for primary
   * key and other one for attributes which are going to set.
   *
   * @param clazz class of Model class corresponding to table.
   * @return Map containing two submap with keys PK(containing primary key attributes) and
   *     NonPk(containing updatable attributes).
   */
  public static <T> Map<String, Map<String, Object>> batchUpdateQuery(T clazz) {
    Field[] fieldList = clazz.getClass().getDeclaredFields();

    Map<String, Object> primaryKeyMap = new HashMap<>();
    Map<String, Object> nonPKMap = new HashMap<>();
    try {
      for (Field field : fieldList) {
        String fieldName = null;
        Object fieldValue = null;
        Boolean isFieldPrimaryKeyPart = false;
        if (Modifier.isPrivate(field.getModifiers())) {
          field.setAccessible(true);
        }
        Annotation[] annotations = field.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof PartitioningKey) {
            isFieldPrimaryKeyPart = true;
          } else if (annotation instanceof ClusteringKey) {
            isFieldPrimaryKeyPart = true;
          }
        }
        fieldName = field.getName();
        fieldValue = field.get(clazz);
        if (!(fieldName.equalsIgnoreCase(SERIAL_VERSION_UID))) {
          if (isFieldPrimaryKeyPart) {
            primaryKeyMap.put(fieldName, fieldValue);
          } else {
            nonPKMap.put(fieldName, fieldValue);
          }
        }
      }
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred - batchUpdateQuery", ex);
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
   * Method to create the composite primary key.
   *
   * @param clazz class of Model class corresponding to table.
   * @return Map containing primary key attributes.
   */
  public static <T> Map<String, Object> getPrimaryKey(T clazz) {
    Field[] fieldList = clazz.getClass().getDeclaredFields();
    Map<String, Object> primaryKeyMap = new HashMap<>();

    try {
      for (Field field : fieldList) {
        String fieldName = null;
        Object fieldValue = null;
        Boolean isFieldPrimaryKeyPart = false;
        if (Modifier.isPrivate(field.getModifiers())) {
          field.setAccessible(true);
        }
        Annotation[] annotations = field.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof PartitioningKey) {
            isFieldPrimaryKeyPart = true;
          } else if (annotation instanceof ClusteringKey) {
            isFieldPrimaryKeyPart = true;
          }
        }
        fieldName = field.getName();
        fieldValue = field.get(clazz);
        if (!(fieldName.equalsIgnoreCase(SERIAL_VERSION_UID))) {
          if (isFieldPrimaryKeyPart) {
            primaryKeyMap.put(fieldName, fieldValue);
          }
        }
      }
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred - getPrimaryKey", ex);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return primaryKeyMap;
  }

  /**
   * Method to create the where clause.
   *
   * @param key represents the column name.
   * @param value represents the column value.
   * @param where where clause.
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
   * Method to create the cassandra update query.
   *
   * @param primaryKey map representing the composite primary key.
   * @param nonPKRecord map contains the fields that has to update.
   * @param keyspaceName cassandra keyspace name.
   * @param tableName cassandra table name.
   * @return RegularStatement.
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

  public static void createQuery(String key, Object value, Where where) {
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
}
