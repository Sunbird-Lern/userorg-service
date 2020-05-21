package org.sunbird.cassandraimpl;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.querybuilder.*;
import com.datastax.driver.core.querybuilder.Select.Builder;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import com.google.common.util.concurrent.FutureCallback;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

/**
 * @author Amit Kumar
 * @desc this class will hold functions for cassandra db interaction
 */
public abstract class CassandraOperationImpl implements CassandraOperation {

  protected CassandraConnectionManager connectionManager;

  public CassandraOperationImpl() {
    PropertiesCache propertiesCache = PropertiesCache.getInstance();
    String cassandraMode = propertiesCache.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
    connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
  }

  @Override
  public Response insertRecord(String keyspaceName, String tableName, Map<String, Object> request) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service insertRecord method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);
      BoundStatement boundStatement = new BoundStatement(statement);
      Iterator<Object> iterator = request.values().iterator();
      Object[] array = new Object[request.keySet().size()];
      int i = 0;
      while (iterator.hasNext()) {
        array[i++] = iterator.next();
      }
      connectionManager.getSession(keyspaceName).execute(boundStatement.bind(array));
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (Exception e) {
      if (e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)
          || e.getMessage().contains(JsonKey.UNDEFINED_IDENTIFIER)) {
        ProjectLogger.log(
            "Exception occured while inserting record to " + tableName + " : " + e.getMessage(), e);
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      ProjectLogger.log(
          "Exception occured while inserting record to " + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.dbInsertionError.getErrorCode(),
          ResponseCode.dbInsertionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("insertRecord", startTime);
    return response;
  }

  @Override
  public Response updateRecord(String keyspaceName, String tableName, Map<String, Object> request) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service updateRecord method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      String query = CassandraUtil.getUpdateQueryStatement(keyspaceName, tableName, request);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);
      Object[] array = new Object[request.size()];
      int i = 0;
      String str = "";
      int index = query.lastIndexOf(Constants.SET.trim());
      str = query.substring(index + 4);
      str = str.replace(Constants.EQUAL_WITH_QUE_MARK, "");
      str = str.replace(Constants.WHERE_ID, "");
      str = str.replace(Constants.SEMICOLON, "");
      String[] arr = str.split(",");
      for (String key : arr) {
        array[i++] = request.get(key.trim());
      }
      array[i] = request.get(Constants.IDENTIFIER);
      BoundStatement boundStatement = statement.bind(array);
      connectionManager.getSession(keyspaceName).execute(boundStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (Exception e) {
      e.printStackTrace();
      if (e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {
        ProjectLogger.log(
            Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(),
            e,
            LoggerEnum.ERROR.name());
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      ProjectLogger.log(
          Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(),
          e,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.dbUpdateError.getErrorCode(),
          ResponseCode.dbUpdateError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("updateRecord", startTime);
    return response;
  }

  @Override
  public Response deleteRecord(String keyspaceName, String tableName, String identifier) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service deleteRecord method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      Delete.Where delete =
          QueryBuilder.delete()
              .from(keyspaceName, tableName)
              .where(eq(Constants.IDENTIFIER, identifier));
      connectionManager.getSession(keyspaceName).execute(delete);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_DELETE + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("deleteRecord", startTime);
    return response;
  }

  @Override
  public Response getRecordsByProperty(
      String keyspaceName, String tableName, String propertyName, Object propertyValue) {
    return getRecordsByProperty(keyspaceName, tableName, propertyName, propertyValue, null);
  }

  @Override
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      List<String> fields) {
    Response response = new Response();
    Session session = connectionManager.getSession(keyspaceName);
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        selectBuilder = QueryBuilder.select((String[]) fields.toArray());
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      Statement selectStatement =
          selectBuilder.from(keyspaceName, tableName).where(eq(propertyName, propertyValue));
      ResultSet results = null;
      results = session.execute(selectStatement);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return response;
  }

  @Override
  public Response getRecordsByProperty(
      String keyspaceName, String tableName, String propertyName, List<Object> propertyValueList) {
    return getRecordsByProperty(keyspaceName, tableName, propertyName, propertyValueList, null);
  }

  @Override
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      List<Object> propertyValueList,
      List<String> fields) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service getRecordsByProperty method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        selectBuilder = QueryBuilder.select(fields.toArray(new String[fields.size()]));
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      Statement selectStatement =
          selectBuilder
              .from(keyspaceName, tableName)
              .where(QueryBuilder.in(propertyName, propertyValueList));
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectStatement);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByProperty", startTime);
    return response;
  }

  @Override
  public Response getRecordsByProperties(
      String keyspaceName, String tableName, Map<String, Object> propertyMap) {
    return getRecordsByProperties(keyspaceName, tableName, propertyMap, null);
  }

  @Override
  public Response getRecordsByProperties(
      String keyspaceName, String tableName, Map<String, Object> propertyMap, List<String> fields) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service getRecordsByProperties method started at ==" + startTime,
        LoggerEnum.INFO);
    Response response = new Response();
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[fields.size()]);
        selectBuilder = QueryBuilder.select(dbFields);
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      Select selectQuery = selectBuilder.from(keyspaceName, tableName);
      if (MapUtils.isNotEmpty(propertyMap)) {
        Where selectWhere = selectQuery.where();
        for (Entry<String, Object> entry : propertyMap.entrySet()) {
          if (entry.getValue() instanceof List) {
            List<Object> list = (List) entry.getValue();
            if (null != list) {
              Object[] propertyValues = list.toArray(new Object[list.size()]);
              Clause clause = QueryBuilder.in(entry.getKey(), propertyValues);
              selectWhere.and(clause);
            }
          } else {
            Clause clause = eq(entry.getKey(), entry.getValue());
            selectWhere.and(clause);
          }
        }
      }
      ResultSet results =
          connectionManager.getSession(keyspaceName).execute(selectQuery.allowFiltering());
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByProperties", startTime);
    return response;
  }

  @Override
  public Response getPropertiesValueById(
      String keyspaceName, String tableName, String id, String... properties) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service getPropertiesValueById method started at ==" + startTime,
        LoggerEnum.INFO);
    Response response = new Response();
    try {
      String selectQuery = CassandraUtil.getSelectStatement(keyspaceName, tableName, properties);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(selectQuery);
      BoundStatement boundStatement = new BoundStatement(statement);
      ResultSet results =
          connectionManager.getSession(keyspaceName).execute(boundStatement.bind(id));
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getPropertiesValueById", startTime);
    return response;
  }

  @Override
  public Response getAllRecords(String keyspaceName, String tableName) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service getAllRecords method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      Select selectQuery = QueryBuilder.select().all().from(keyspaceName, tableName);
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getAllRecords", startTime);
    return response;
  }

  @Override
  public Response upsertRecord(String keyspaceName, String tableName, Map<String, Object> request) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service upsertRecord method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);
      BoundStatement boundStatement = new BoundStatement(statement);
      Iterator<Object> iterator = request.values().iterator();
      Object[] array = new Object[request.keySet().size()];
      int i = 0;
      while (iterator.hasNext()) {
        array[i++] = iterator.next();
      }
      connectionManager.getSession(keyspaceName).execute(boundStatement.bind(array));
      response.put(Constants.RESPONSE, Constants.SUCCESS);

    } catch (Exception e) {
      if (e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {
        ProjectLogger.log(Constants.EXCEPTION_MSG_UPSERT + tableName + " : " + e.getMessage(), e);
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      ProjectLogger.log(Constants.EXCEPTION_MSG_UPSERT + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("upsertRecord", startTime);
    return response;
  }

  @Override
  public Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey) {

    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service updateRecord method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      Session session = connectionManager.getSession(keyspaceName);
      Update update = QueryBuilder.update(keyspaceName, tableName);
      Assignments assignments = update.with();
      Update.Where where = update.where();
      request
          .entrySet()
          .stream()
          .forEach(
              x -> {
                assignments.and(QueryBuilder.set(x.getKey(), x.getValue()));
              });
      compositeKey
          .entrySet()
          .stream()
          .forEach(
              x -> {
                where.and(eq(x.getKey(), x.getValue()));
              });
      Statement updateQuery = where;
      session.execute(updateQuery);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(), e);
      if (e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      throw new ProjectCommonException(
          ResponseCode.dbUpdateError.getErrorCode(),
          ResponseCode.dbUpdateError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("updateRecord", startTime);
    return response;
  }

  private Response getRecordByIdentifier(
      String keyspaceName, String tableName, Object key, List<String> fields) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service getRecordBy key method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      Session session = connectionManager.getSession(keyspaceName);
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        selectBuilder = QueryBuilder.select(fields.toArray(new String[fields.size()]));
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      Select selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();
      if (key instanceof String) {
        selectWhere.and(eq(Constants.IDENTIFIER, key));
      } else if (key instanceof Map) {
        Map<String, Object> compositeKey = (Map<String, Object>) key;
        compositeKey
            .entrySet()
            .stream()
            .forEach(
                x -> {
                  CassandraUtil.createQuery(x.getKey(), x.getValue(), selectWhere);
                });
      }
      ResultSet results = session.execute(selectWhere);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordByIdentifier", startTime);
    return response;
  }

  @Override
  public Response getRecordById(String keyspaceName, String tableName, String key) {
    return getRecordByIdentifier(keyspaceName, tableName, key, null);
  }

  @Override
  public Response getRecordById(String keyspaceName, String tableName, Map<String, Object> key) {
    return getRecordByIdentifier(keyspaceName, tableName, key, null);
  }

  @Override
  public Response getRecordById(
      String keyspaceName, String tableName, String key, List<String> fields) {
    return getRecordByIdentifier(keyspaceName, tableName, key, fields);
  }

  @Override
  public Response getRecordById(
      String keyspaceName, String tableName, Map<String, Object> key, List<String> fields) {
    return getRecordByIdentifier(keyspaceName, tableName, key, fields);
  }

  @Override
  public Response getRecordWithTTLById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields) {
    return getRecordWithTTLByIdentifier(keyspaceName, tableName, key, ttlFields, fields);
  }

  public Response getRecordWithTTLByIdentifier(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service getRecordBy key method started at ==" + startTime,
        LoggerEnum.INFO.name());
    Response response = new Response();
    try {
      Session session = connectionManager.getSession(keyspaceName);
      Selection select = QueryBuilder.select();
      for (String field : fields) {
        select.column(field);
      }
      for (String field : ttlFields) {
        select.ttl(field).as(field + "_ttl");
      }
      Select.Where selectWhere = select.from(keyspaceName, tableName).where();
      key.entrySet()
          .stream()
          .forEach(
              x -> {
                selectWhere.and(QueryBuilder.eq(x.getKey(), x.getValue()));
              });

      ResultSet results = session.execute(selectWhere);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordByIdentifier", startTime);
    return response;
  }

  @Override
  public Response batchInsert(
      String keyspaceName, String tableName, List<Map<String, Object>> records) {

    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service batchInsert method started at ==" + startTime, LoggerEnum.INFO);

    Session session = connectionManager.getSession(keyspaceName);
    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();
    ResultSet resultSet = null;

    try {
      for (Map<String, Object> map : records) {
        Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);
        map.entrySet()
            .stream()
            .forEach(
                x -> {
                  insert.value(x.getKey(), x.getValue());
                });
        batchStatement.add(insert);
      }
      resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (QueryExecutionException
        | QueryValidationException
        | NoHostAvailableException
        | IllegalStateException e) {
      ProjectLogger.log("Cassandra Batch Insert Failed." + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("batchInsert", startTime);
    return response;
  }

  /**
   * This method updates all the records in a batch
   *
   * @param keyspaceName
   * @param tableName
   * @param records
   * @return
   */
  // @Override
  public Response batchUpdateById(
      String keyspaceName, String tableName, List<Map<String, Object>> records) {

    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service batchUpdateById method started at ==" + startTime, LoggerEnum.INFO);
    Session session = connectionManager.getSession(keyspaceName);
    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();
    ResultSet resultSet = null;

    try {
      for (Map<String, Object> map : records) {
        Update update = createUpdateStatement(keyspaceName, tableName, map);
        batchStatement.add(update);
      }
      resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (QueryExecutionException
        | QueryValidationException
        | NoHostAvailableException
        | IllegalStateException e) {
      ProjectLogger.log("Cassandra Batch Update Failed." + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("batchUpdateById", startTime);
    return response;
  }

  /**
   * This method performs batch operations of insert and update on a same table, further other
   * operations can be added to if it is necessary.
   *
   * @param keySpaceName
   * @param tableName
   * @param inputData
   * @return
   */
  @Override
  public Response performBatchAction(
      String keySpaceName, String tableName, Map<String, Object> inputData) {

    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service performBatchAction method started at ==" + startTime,
        LoggerEnum.INFO.name());

    Session session = connectionManager.getSession(keySpaceName);
    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();
    ResultSet resultSet = null;
    try {
      inputData.forEach(
          (key, inputMap) -> {
            Map<String, Object> record = (Map<String, Object>) inputMap;
            if (key.equals(JsonKey.INSERT)) {
              Insert insert = createInsertStatement(keySpaceName, tableName, record);
              batchStatement.add(insert);
            } else if (key.equals(JsonKey.UPDATE)) {
              Update update = createUpdateStatement(keySpaceName, tableName, record);
              batchStatement.add(update);
            }
          });
      resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (QueryExecutionException
        | QueryValidationException
        | NoHostAvailableException
        | IllegalStateException e) {
      ProjectLogger.log(
          "Cassandra performBatchAction Failed." + e.getMessage(), LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("performBatchAction", startTime);
    return response;
  }

  private Insert createInsertStatement(
      String keySpaceName, String tableName, Map<String, Object> record) {
    Insert insert = QueryBuilder.insertInto(keySpaceName, tableName);
    record
        .entrySet()
        .stream()
        .forEach(
            x -> {
              insert.value(x.getKey(), x.getValue());
            });
    return insert;
  }

  private Update createUpdateStatement(
      String keySpaceName, String tableName, Map<String, Object> record) {
    Update update = QueryBuilder.update(keySpaceName, tableName);
    Assignments assignments = update.with();
    Update.Where where = update.where();
    record
        .entrySet()
        .stream()
        .forEach(
            x -> {
              if (Constants.ID.equals(x.getKey())) {
                where.and(eq(x.getKey(), x.getValue()));
              } else {
                assignments.and(QueryBuilder.set(x.getKey(), x.getValue()));
              }
            });
    return update;
  }

  @Override
  public Response batchUpdate(
      String keyspaceName, String tableName, List<Map<String, Map<String, Object>>> list) {

    Session session = connectionManager.getSession(keyspaceName);
    BatchStatement batchStatement = new BatchStatement();
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "Cassandra Service batchUpdate method started at ==" + startTime, LoggerEnum.INFO);
    Response response = new Response();
    ResultSet resultSet = null;
    try {
      for (Map<String, Map<String, Object>> record : list) {
        Map<String, Object> primaryKey = record.get(JsonKey.PRIMARY_KEY);
        Map<String, Object> nonPKRecord = record.get(JsonKey.NON_PRIMARY_KEY);
        batchStatement.add(
            CassandraUtil.createUpdateQuery(primaryKey, nonPKRecord, keyspaceName, tableName));
      }
      resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (Exception ex) {
      ProjectLogger.log("Cassandra Batch Update failed " + ex.getMessage(), ex);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("batchUpdate", startTime);
    return response;
  }

  private void logQueryElapseTime(String operation, long startTime) {

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    String message =
        "Cassandra operation {0} started at {1} and completed at {2}. Total time elapsed is {3}.";
    MessageFormat mf = new MessageFormat(message);
    ProjectLogger.log(
        mf.format(new Object[] {operation, startTime, stopTime, elapsedTime}), LoggerEnum.PERF_LOG);
  }

  @Override
  public Response getRecordsByIndexedProperty(
      String keyspaceName, String tableName, String propertyName, Object propertyValue) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl:getRecordsByIndexedProperty called at " + startTime,
        LoggerEnum.INFO);
    Response response = new Response();
    try {
      Select selectQuery = QueryBuilder.select().all().from(keyspaceName, tableName);
      selectQuery.where().and(eq(propertyName, propertyValue));
      ResultSet results =
          connectionManager.getSession(keyspaceName).execute(selectQuery.allowFiltering());
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(
          "CassandraOperationImpl:getRecordsByIndexedProperty: "
              + Constants.EXCEPTION_MSG_FETCH
              + tableName
              + " : "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByIndexedProperty", startTime);
    return response;
  }

  @Override
  public void deleteRecord(
      String keyspaceName, String tableName, Map<String, String> compositeKeyMap) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl: deleteRecord by composite key called at " + startTime,
        LoggerEnum.INFO);
    try {
      Delete delete = QueryBuilder.delete().from(keyspaceName, tableName);
      Delete.Where deleteWhere = delete.where();
      compositeKeyMap
          .entrySet()
          .stream()
          .forEach(
              x -> {
                Clause clause = eq(x.getKey(), x.getValue());
                deleteWhere.and(clause);
              });
      connectionManager.getSession(keyspaceName).execute(delete);
    } catch (Exception e) {
      ProjectLogger.log(
          "CassandraOperationImpl: deleteRecord by composite key. "
              + Constants.EXCEPTION_MSG_DELETE
              + tableName
              + " : "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("deleteRecordByCompositeKey", startTime);
  }

  @Override
  public boolean deleteRecords(String keyspaceName, String tableName, List<String> identifierList) {
    long startTime = System.currentTimeMillis();
    ResultSet resultSet;
    ProjectLogger.log(
        "CassandraOperationImpl: deleteRecords called at " + startTime, LoggerEnum.INFO);
    try {
      Delete delete = QueryBuilder.delete().from(keyspaceName, tableName);
      Delete.Where deleteWhere = delete.where();
      Clause clause = QueryBuilder.in(JsonKey.ID, identifierList);
      deleteWhere.and(clause);
      resultSet = connectionManager.getSession(keyspaceName).execute(delete);
    } catch (Exception e) {
      ProjectLogger.log(
          "CassandraOperationImpl: deleteRecords by list of primary key. "
              + Constants.EXCEPTION_MSG_DELETE
              + tableName
              + " : "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("deleteRecords", startTime);
    return resultSet.wasApplied();
  }

  @Override
  public Response getRecordsByCompositeKey(
      String keyspaceName, String tableName, Map<String, Object> compositeKeyMap) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl: getRecordsByCompositeKey called at " + startTime, LoggerEnum.INFO);
    Response response = new Response();
    try {
      Builder selectBuilder = QueryBuilder.select().all();
      Select selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();
      for (Entry<String, Object> entry : compositeKeyMap.entrySet()) {
        Clause clause = eq(entry.getKey(), entry.getValue());
        selectWhere.and(clause);
      }
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(
          "CassandraOperationImpl:getRecordsByCompositeKey: "
              + Constants.EXCEPTION_MSG_FETCH
              + tableName
              + " : "
              + e.getMessage());
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByCompositeKey", startTime);
    return response;
  }

  @Override
  public Response getRecordsByIdsWithSpecifiedColumns(
      String keyspaceName, String tableName, List<String> properties, List<String> ids) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl: getRecordsByIdsWithSpecifiedColumns call started at " + startTime,
        LoggerEnum.INFO);
    Response response = new Response();
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(properties)) {
        selectBuilder = QueryBuilder.select(properties.toArray(new String[properties.size()]));
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      response = executeSelectQuery(keyspaceName, tableName, ids, selectBuilder, "");
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByIdsWithSpecifiedColumns", startTime);
    return response;
  }

  private Response executeSelectQuery(
      String keyspaceName,
      String tableName,
      List<String> ids,
      Builder selectBuilder,
      String primaryKeyColumnName) {
    Response response;
    Select selectQuery = selectBuilder.from(keyspaceName, tableName);
    Where selectWhere = selectQuery.where();
    Clause clause = null;
    if (StringUtils.isBlank(primaryKeyColumnName)) {
      clause = QueryBuilder.in(JsonKey.ID, ids.toArray(new Object[ids.size()]));
    } else {
      clause = QueryBuilder.in(primaryKeyColumnName, ids.toArray(new Object[ids.size()]));
    }

    selectWhere.and(clause);
    ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
    response = CassandraUtil.createResponse(results);
    return response;
  }

  @Override
  public Response getRecordsByPrimaryKeys(
      String keyspaceName,
      String tableName,
      List<String> primaryKeys,
      String primaryKeyColumnName) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl: getRecordsByPrimaryKeys call started at " + startTime,
        LoggerEnum.INFO);
    Response response = new Response();
    try {
      Builder selectBuilder = QueryBuilder.select().all();
      response =
          executeSelectQuery(
              keyspaceName, tableName, primaryKeys, selectBuilder, primaryKeyColumnName);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByPrimaryKeys", startTime);
    return response;
  }

  @Override
  public Response insertRecordWithTTL(
      String keyspaceName, String tableName, Map<String, Object> request, int ttl) {
    long startTime = System.currentTimeMillis();
    Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);
    request
        .entrySet()
        .stream()
        .forEach(
            x -> {
              insert.value(x.getKey(), x.getValue());
            });
    insert.using(QueryBuilder.ttl(ttl));
    ProjectLogger.log(
        "CassandraOperationImpl:insertRecordWithTTL: query = " + insert.getQueryString(),
        LoggerEnum.INFO.name());
    ResultSet results = connectionManager.getSession(keyspaceName).execute(insert);
    Response response = CassandraUtil.createResponse(results);
    logQueryElapseTime("insertRecordWithTTL", startTime);
    return response;
  }

  @Override
  public Response updateRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey,
      int ttl) {
    long startTime = System.currentTimeMillis();
    Session session = connectionManager.getSession(keyspaceName);
    Update update = QueryBuilder.update(keyspaceName, tableName);
    Assignments assignments = update.with();
    Update.Where where = update.where();
    request
        .entrySet()
        .stream()
        .forEach(
            x -> {
              assignments.and(QueryBuilder.set(x.getKey(), x.getValue()));
            });
    compositeKey
        .entrySet()
        .stream()
        .forEach(
            x -> {
              where.and(eq(x.getKey(), x.getValue()));
            });
    update.using(QueryBuilder.ttl(ttl));
    ProjectLogger.log(
        "CassandraOperationImpl:updateRecordWithTTL: query = " + update.getQueryString(),
        LoggerEnum.INFO.name());
    ResultSet results = session.execute(update);
    Response response = CassandraUtil.createResponse(results);
    logQueryElapseTime("updateRecordWithTTL", startTime);
    return response;
  }

  @Override
  public Response getRecordsByIdsWithSpecifiedColumnsAndTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> primaryKeys,
      List<String> properties,
      Map<String, String> ttlPropertiesWithAlias) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl:getRecordsByIdsWithSpecifiedColumnsAndTTL: call started at "
            + startTime,
        LoggerEnum.INFO);
    Response response = new Response();
    try {

      Selection selection = QueryBuilder.select();

      if (CollectionUtils.isNotEmpty(properties)) {
        properties
            .stream()
            .forEach(
                property -> {
                  selection.column(property);
                });
      }

      if (MapUtils.isNotEmpty(ttlPropertiesWithAlias)) {
        ttlPropertiesWithAlias
            .entrySet()
            .stream()
            .forEach(
                property -> {
                  if (StringUtils.isBlank(property.getValue())) {
                    ProjectLogger.log(
                        "CassandraOperationImpl:getRecordsByIdsWithSpecifiedColumnsAndTTL: Alias not provided for ttl key = "
                            + property.getKey(),
                        LoggerEnum.ERROR);
                    ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
                  }
                  selection.ttl(property.getKey()).as(property.getValue());
                });
      }
      Select select = selection.from(keyspaceName, tableName);
      primaryKeys
          .entrySet()
          .stream()
          .forEach(
              primaryKey -> {
                select.where().and(eq(primaryKey.getKey(), primaryKey.getValue()));
              });
      ProjectLogger.log("Query =" + select.getQueryString(), LoggerEnum.INFO);
      ResultSet results = connectionManager.getSession(keyspaceName).execute(select);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("getRecordsByIdsWithSpecifiedColumnsAndTTL", startTime);
    return response;
  }

  @Override
  public Response batchInsertWithTTL(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      List<Integer> ttls) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "CassandraOperationImpl:batchInsertWithTTL: call started at " + startTime, LoggerEnum.INFO);
    if (CollectionUtils.isEmpty(records) || CollectionUtils.isEmpty(ttls)) {
      ProjectLogger.log(
          "CassandraOperationImpl:batchInsertWithTTL: records or ttls is empty", LoggerEnum.ERROR);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    if (ttls.size() != records.size()) {
      ProjectLogger.log(
          "CassandraOperationImpl:batchInsertWithTTL: Mismatch of records and ttls list size",
          LoggerEnum.ERROR);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    Session session = connectionManager.getSession(keyspaceName);
    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();
    ResultSet resultSet = null;
    Iterator<Integer> ttlIterator = ttls.iterator();
    try {
      for (Map<String, Object> map : records) {
        Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);
        map.entrySet()
            .stream()
            .forEach(
                x -> {
                  insert.value(x.getKey(), x.getValue());
                });
        if (ttlIterator.hasNext()) {
          Integer ttlVal = ttlIterator.next();
          if (ttlVal != null & ttlVal > 0) {
            insert.using(QueryBuilder.ttl(ttlVal));
          }
        }
        batchStatement.add(insert);
      }
      resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (QueryExecutionException
        | QueryValidationException
        | NoHostAvailableException
        | IllegalStateException e) {
      ProjectLogger.log(
          "CassandraOperationImpl:batchInsertWithTTL: Exception occurred with error message = "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    logQueryElapseTime("batchInsertWithTTL", startTime);
    return response;
  }

  @Override
  public Response getRecordByObjectType(
      String keyspace,
      String tableName,
      String columnName,
      String key,
      int value,
      String objectType) {
    Select selectQuery = QueryBuilder.select().column(columnName).from(keyspace, tableName);
    Clause clause = QueryBuilder.lt(key, value);
    selectQuery.where(eq(JsonKey.OBJECT_TYPE, objectType)).and(clause);
    selectQuery.allowFiltering();
    ResultSet resultSet = connectionManager.getSession(keyspace).execute(selectQuery);
    Response response = CassandraUtil.createResponse(resultSet);
    return response;
  }

  @Override
  public Response getRecords(
      String keyspace, String table, Map<String, Object> filters, List<String> fields) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void applyOperationOnRecordsAsync(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      FutureCallback<ResultSet> callback) {
    // TODO Auto-generated method stub

  }

  @Override
  public Response searchValueInList(String keyspace, String tableName, String key, String value) {
    return searchValueInList(keyspace, tableName, key, value, null);
  }

  @Override
  public Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String value,
      Map<String, Object> propertyMap) {
    Select selectQuery = QueryBuilder.select().all().from(keyspace, tableName);
    Clause clause = QueryBuilder.contains(key, value);
    selectQuery.where(clause);
    if (MapUtils.isNotEmpty(propertyMap)) {
      for (Entry<String, Object> entry : propertyMap.entrySet()) {
        if (entry.getValue() instanceof List) {
          List<Object> list = (List) entry.getValue();
          if (null != list) {
            Object[] propertyValues = list.toArray(new Object[list.size()]);
            Clause clauseList = QueryBuilder.in(entry.getKey(), propertyValues);
            selectQuery.where(clauseList);
          }
        } else {
          Clause clauseMap = eq(entry.getKey(), entry.getValue());
          selectQuery.where(clauseMap);
        }
      }
    }
    ResultSet resultSet = connectionManager.getSession(keyspace).execute(selectQuery);
    Response response = CassandraUtil.createResponse(resultSet);
    return response;
  }
}
