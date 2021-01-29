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
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Builder;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.querybuilder.Update.Assignments;
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
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

/** This class will hold functions for cassandra db interaction */
public abstract class CassandraOperationImpl implements CassandraOperation {

  protected CassandraConnectionManager connectionManager;
  private LoggerUtil logger = new LoggerUtil(CassandraOperationImpl.class);

  public CassandraOperationImpl() {
    connectionManager = CassandraConnectionMngrFactory.getInstance();
  }

  @Override
  public Response insertRecord(
      String keyspaceName, String tableName, Map<String, Object> request, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service insertRecord method started at ==" + startTime);
    Response response = new Response();
    String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
    try {
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
        logger.error(
            context,
            "Exception occurred while inserting record to " + tableName + " : " + e.getMessage(),
            e);
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      logger.error(
          context,
          "Exception occured while inserting record to " + tableName + " : " + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.dbInsertionError.getErrorCode(),
          ResponseCode.dbInsertionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      logQueryElapseTime("insertRecord", startTime, query, context);
    }
    return response;
  }

  @Override
  public Response updateRecord(
      String keyspaceName, String tableName, Map<String, Object> request, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service updateRecord method started at ==" + startTime);
    Response response = new Response();
    String query = CassandraUtil.getUpdateQueryStatement(keyspaceName, tableName, request);
    try {
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
      if (e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {
        logger.error(
            context, Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(), e);
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      logger.error(context, Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.dbUpdateError.getErrorCode(),
          ResponseCode.dbUpdateError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      logQueryElapseTime("updateRecord", startTime, query, context);
    }
    return response;
  }

  @Override
  public Response deleteRecord(
      String keyspaceName, String tableName, String identifier, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service deleteRecord method started at ==" + startTime);
    Response response = new Response();
    Delete.Where delete = null;
    try {
      delete =
          QueryBuilder.delete()
              .from(keyspaceName, tableName)
              .where(eq(Constants.IDENTIFIER, identifier));
      connectionManager.getSession(keyspaceName).execute(delete);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_DELETE + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != delete) {
        logQueryElapseTime("deleteRecord", startTime, delete.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      List<Object> propertyValueList,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "Cassandra Service getRecordsByProperty method started at ==" + startTime);
    Response response;
    Statement selectStatement = null;
    try {
      Builder selectBuilder = QueryBuilder.select().all();
      selectStatement =
          selectBuilder
              .from(keyspaceName, tableName)
              .where(QueryBuilder.in(propertyName, propertyValueList));
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectStatement);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectStatement) {
        logQueryElapseTime("getRecordsByProperty", startTime, selectStatement.toString(), context);
      }
    }
    return response;
  }

  @Override
  public Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      List<String> fields,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "Cassandra Service getRecordsByProperties method started at ==" + startTime);
    Response response;
    Select selectQuery = null;
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[fields.size()]);
        selectBuilder = QueryBuilder.select(dbFields);
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      selectQuery = selectBuilder.from(keyspaceName, tableName);
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
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime(
            "getRecordsByProperties", startTime, selectQuery.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      RequestContext context) {
    return getRecordsByProperties(keyspaceName, tableName, propertyMap, null, context);
  }

  @Override
  public Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      String id,
      List<String> properties,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "Cassandra Service getPropertiesValueById method started at ==" + startTime);
    Response response;
    String selectQuery = null;
    try {
      selectQuery = CassandraUtil.getSelectStatement(keyspaceName, tableName, properties);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(selectQuery);
      BoundStatement boundStatement = new BoundStatement(statement);
      ResultSet results =
          connectionManager.getSession(keyspaceName).execute(boundStatement.bind(id));
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime("getPropertiesValueById", startTime, selectQuery, context);
      }
    }
    return response;
  }

  @Override
  public Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      List<String> ids,
      List<String> properties,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "Cassandra Service getPropertiesValueById method started at ==" + startTime);
    Response response;
    Select selectQuery = null;
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(properties)) {
        String[] dbFields = properties.toArray(new String[properties.size()]);
        selectBuilder = QueryBuilder.select(dbFields);
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      selectQuery.where(QueryBuilder.in(JsonKey.ID, ids));
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);

      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime(
            "getPropertiesValueById", startTime, selectQuery.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response getAllRecords(String keyspaceName, String tableName, RequestContext context) {
    return getAllRecords(keyspaceName, tableName, null, context);
  }

  @Override
  public Response getAllRecords(
      String keyspaceName, String tableName, List<String> fields, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service getAllRecords method started at ==" + startTime);
    Response response;
    Select selectQuery = null;
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[fields.size()]);
        selectBuilder = QueryBuilder.select(dbFields);
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime("getAllRecords", startTime, selectQuery.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response upsertRecord(
      String keyspaceName, String tableName, Map<String, Object> request, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service upsertRecord method started at ==" + startTime);
    Response response = new Response();
    String query = "";
    try {
      query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
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
        logger.error(
            context, Constants.EXCEPTION_MSG_UPSERT + tableName + " : " + e.getMessage(), e);
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            CassandraUtil.processExceptionForUnknownIdentifier(e),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      logger.error(context, Constants.EXCEPTION_MSG_UPSERT + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != query) {
        logQueryElapseTime("upsertRecord", startTime, query, context);
      }
    }
    return response;
  }

  @Override
  public Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey,
      RequestContext context) {

    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service updateRecord method started at ==" + startTime);
    Response response = new Response();
    Statement updateQuery = null;
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
      updateQuery = where;
      session.execute(updateQuery);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(), e);
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
    } finally {
      if (null != updateQuery) {
        logQueryElapseTime("updateRecord", startTime, updateQuery.toString(), context);
      }
    }
    return response;
  }

  private Response getRecordByIdentifier(
      String keyspaceName,
      String tableName,
      Object key,
      List<String> fields,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service getRecordBy key method started at ==" + startTime);
    Response response;
    Where selectWhereQuery = null;
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
      selectWhereQuery = selectWhere;
      ResultSet results = session.execute(selectWhere);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectWhereQuery) {
        logQueryElapseTime(
            "getRecordByIdentifier", startTime, selectWhereQuery.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response getRecordById(
      String keyspaceName, String tableName, String key, RequestContext context) {
    return getRecordByIdentifier(keyspaceName, tableName, key, null, context);
  }

  @Override
  public Response getRecordById(
      String keyspaceName, String tableName, Map<String, Object> key, RequestContext context) {
    return getRecordByIdentifier(keyspaceName, tableName, key, null, context);
  }

  @Override
  public Response getRecordById(
      String keyspaceName,
      String tableName,
      String key,
      List<String> fields,
      RequestContext context) {
    return getRecordByIdentifier(keyspaceName, tableName, key, fields, context);
  }

  @Override
  public Response getRecordById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> fields,
      RequestContext context) {
    return getRecordByIdentifier(keyspaceName, tableName, key, fields, context);
  }

  @Override
  public Response getRecordWithTTLById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext context) {
    return getRecordWithTTLByIdentifier(keyspaceName, tableName, key, ttlFields, fields, context);
  }

  public Response getRecordWithTTLByIdentifier(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service getRecordBy key method started at ==" + startTime);
    Response response;
    Select.Where selectWhereQuery = null;
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
      selectWhereQuery = selectWhere;
      ResultSet results = session.execute(selectWhere);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectWhereQuery) {
        logQueryElapseTime(
            "getRecordByIdentifier", startTime, selectWhereQuery.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response batchInsert(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext context) {

    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service batchInsert method started at ==" + startTime);

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
      logger.error(context, "Cassandra Batch Insert Failed." + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != batchStatement) {
        logQueryElapseTime(
            "batchInsert", startTime, batchStatement.getStatements().toString(), context);
      }
    }
    return response;
  }

  /**
   * This method updates all the records in a batch
   *
   * @param keyspaceName
   * @param tableName
   * @param records
   * @param context
   * @return
   */
  @Override
  public Response batchUpdateById(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext context) {

    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service batchUpdateById method started at ==" + startTime);
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
      logger.error(context, "Cassandra Batch Update Failed." + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != batchStatement) {
        logQueryElapseTime(
            "batchUpdateById", startTime, batchStatement.getStatements().toString(), context);
      }
    }
    return response;
  }

  /**
   * This method performs batch operations of insert and update on a same table, further other
   * operations can be added to if it is necessary.
   *
   * @param keySpaceName
   * @param tableName
   * @param inputData
   * @param context
   * @return
   */
  @Override
  public Response performBatchAction(
      String keySpaceName,
      String tableName,
      Map<String, Object> inputData,
      RequestContext context) {

    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service performBatchAction method started at ==" + startTime);

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
      logger.error(context, "Cassandra performBatchAction Failed." + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != batchStatement) {
        logQueryElapseTime(
            "performBatchAction", startTime, batchStatement.getStatements().toString(), context);
      }
    }
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
      String keyspaceName,
      String tableName,
      List<Map<String, Map<String, Object>>> list,
      RequestContext context) {

    Session session = connectionManager.getSession(keyspaceName);
    BatchStatement batchStatement = new BatchStatement();
    long startTime = System.currentTimeMillis();
    logger.debug(context, "Cassandra Service batchUpdate method started at ==" + startTime);
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
      logger.error(context, "Cassandra Batch Update failed " + ex.getMessage(), ex);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != batchStatement) {
        logQueryElapseTime(
            "batchUpdate", startTime, batchStatement.getStatements().toString(), context);
      }
    }
    return response;
  }

  protected void logQueryElapseTime(
      String operation, long startTime, String query, RequestContext context) {
    logger.info(context, "Cassandra query : " + query);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    String message =
        "Cassandra operation {0} started at {1} and completed at {2}. Total time elapsed is {3}.";
    MessageFormat mf = new MessageFormat(message);
    logger.debug(context, mf.format(new Object[] {operation, startTime, stopTime, elapsedTime}));
  }

  @Override
  public void deleteRecord(
      String keyspaceName,
      String tableName,
      Map<String, String> compositeKeyMap,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "CassandraOperationImpl: deleteRecord by composite key called at " + startTime);
    Delete delete = null;
    try {
      delete = QueryBuilder.delete().from(keyspaceName, tableName);
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
      logger.error(
          context,
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
    } finally {
      if (null != delete) {
        logQueryElapseTime(
            "deleteRecordByCompositeKey", startTime, delete.getQueryString(), context);
      }
    }
  }

  @Override
  public boolean deleteRecords(
      String keyspaceName, String tableName, List<String> identifierList, RequestContext context) {
    long startTime = System.currentTimeMillis();
    ResultSet resultSet;
    logger.debug(context, "CassandraOperationImpl: deleteRecords called at " + startTime);
    Delete delete = null;
    try {
      delete = QueryBuilder.delete().from(keyspaceName, tableName);
      Delete.Where deleteWhere = delete.where();
      Clause clause = QueryBuilder.in(JsonKey.ID, identifierList);
      deleteWhere.and(clause);
      resultSet = connectionManager.getSession(keyspaceName).execute(delete);
    } catch (Exception e) {
      logger.error(
          context,
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
    } finally {
      if (null != delete) {
        logQueryElapseTime("deleteRecords", startTime, delete.getQueryString(), context);
      }
    }
    return resultSet.wasApplied();
  }

  @Override
  public Response getRecordsByCompositeKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> compositeKeyMap,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "CassandraOperationImpl: getRecordsByCompositeKey called at " + startTime);
    Response response;
    Select selectQuery = null;
    try {
      Builder selectBuilder = QueryBuilder.select().all();
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();
      for (Entry<String, Object> entry : compositeKeyMap.entrySet()) {
        Clause clause = eq(entry.getKey(), entry.getValue());
        selectWhere.and(clause);
      }
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(
          context,
          "CassandraOperationImpl:getRecordsByCompositeKey: "
              + Constants.EXCEPTION_MSG_FETCH
              + tableName
              + " : "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime(
            "getRecordsByCompositeKey", startTime, selectQuery.getQueryString(), context);
      }
    }
    return response;
  }

  @Override
  public Response getRecordsByIdsWithSpecifiedColumns(
      String keyspaceName,
      String tableName,
      List<String> properties,
      List<String> ids,
      RequestContext context) {
    Response response;
    try {
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(properties)) {
        selectBuilder = QueryBuilder.select(properties.toArray(new String[properties.size()]));
      } else {
        selectBuilder = QueryBuilder.select().all();
      }
      response = executeSelectQuery(keyspaceName, tableName, ids, selectBuilder, "", context);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return response;
  }

  private Response executeSelectQuery(
      String keyspaceName,
      String tableName,
      List<String> ids,
      Builder selectBuilder,
      String primaryKeyColumnName,
      RequestContext context) {
    Response response;
    long startTime = System.currentTimeMillis();
    Select selectQuery = selectBuilder.from(keyspaceName, tableName);
    Where selectWhere = selectQuery.where();
    Clause clause = null;
    if (StringUtils.isBlank(primaryKeyColumnName)) {
      clause = QueryBuilder.in(JsonKey.ID, ids.toArray(new Object[ids.size()]));
    } else {
      clause = QueryBuilder.in(primaryKeyColumnName, ids.toArray(new Object[ids.size()]));
    }

    selectWhere.and(clause);
    if (null != selectQuery) {
      logQueryElapseTime("read", startTime, selectQuery.getQueryString(), context);
    }
    ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
    response = CassandraUtil.createResponse(results);
    return response;
  }

  @Override
  public Response getRecordsByPrimaryKeys(
      String keyspaceName,
      String tableName,
      List<String> primaryKeys,
      String primaryKeyColumnName,
      RequestContext context) {
    Response response;
    try {
      Builder selectBuilder = QueryBuilder.select().all();
      response =
          executeSelectQuery(
              keyspaceName, tableName, primaryKeys, selectBuilder, primaryKeyColumnName, context);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return response;
  }

  @Override
  public Response insertRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      int ttl,
      RequestContext context) {
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
    if (null != insert) {
      logQueryElapseTime("insertRecordWithTTL", startTime, insert.getQueryString(), context);
    }
    ResultSet results = connectionManager.getSession(keyspaceName).execute(insert);
    Response response = CassandraUtil.createResponse(results);
    return response;
  }

  @Override
  public Response updateRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey,
      int ttl,
      RequestContext context) {
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
    if (null != update) {
      logQueryElapseTime("updateRecordWithTTL", startTime, update.getQueryString(), context);
    }
    ResultSet results = session.execute(update);
    Response response = CassandraUtil.createResponse(results);
    return response;
  }

  @Override
  public Response getRecordsByIdsWithSpecifiedColumnsAndTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> primaryKeys,
      List<String> properties,
      Map<String, String> ttlPropertiesWithAlias,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context,
        "CassandraOperationImpl:getRecordsByIdsWithSpecifiedColumnsAndTTL: call started at "
            + startTime);
    Response response;
    Select selectQuery = null;
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
                    logger.info(
                        context,
                        "CassandraOperationImpl:getRecordsByIdsWithSpecifiedColumnsAndTTL: Alias not provided for ttl key = "
                            + property.getKey());
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
      selectQuery = select;
      ResultSet results = connectionManager.getSession(keyspaceName).execute(select);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(context, Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime(
            "getRecordsByIdsWithSpecifiedColumnsAndTTL",
            startTime,
            selectQuery.getQueryString(),
            context);
      }
    }
    return response;
  }

  @Override
  public Response batchInsertWithTTL(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      List<Integer> ttls,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context, "CassandraOperationImpl:batchInsertWithTTL: call started at " + startTime);
    if (CollectionUtils.isEmpty(records) || CollectionUtils.isEmpty(ttls)) {
      logger.debug(context, "CassandraOperationImpl:batchInsertWithTTL: records or ttls is empty");
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    if (ttls.size() != records.size()) {
      logger.debug(
          context,
          "CassandraOperationImpl:batchInsertWithTTL: Mismatch of records and ttls list size");
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
      logger.error(
          context,
          "CassandraOperationImpl:batchInsertWithTTL: Exception occurred with error message = "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != batchStatement) {
        logQueryElapseTime(
            "batchInsertWithTTL", startTime, batchStatement.getStatements().toString(), context);
      }
    }
    return response;
  }

  @Override
  public Response searchValueInList(
      String keyspace, String tableName, String key, String value, RequestContext context) {
    return searchValueInList(keyspace, tableName, key, value, context);
  }

  @Override
  public Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String value,
      Map<String, Object> propertyMap,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
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
    if (null != selectQuery) {
      logQueryElapseTime("searchValueInList", startTime, selectQuery.getQueryString(), context);
    }
    ResultSet resultSet = connectionManager.getSession(keyspace).execute(selectQuery);
    Response response = CassandraUtil.createResponse(resultSet);
    return response;
  }

  @Override
  public Response getRecordsByCompositePartitionKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> partitionKeyMap,
      RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(
        context,
        "CassandraOperationImpl: getRecordsByCompositePartitionKey called at " + startTime);
    Response response;
    Select selectQuery = null;
    try {
      Builder selectBuilder = QueryBuilder.select().all();
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();
      for (Entry<String, Object> entry : partitionKeyMap.entrySet()) {
        if (entry.getValue() instanceof String) {
          Clause clause = eq(entry.getKey(), entry.getValue());
          selectWhere.and(clause);
        } else if (entry.getValue() instanceof List) {

          Object[] propertyValues =
              ((List) entry.getValue()).toArray(new Object[((List) entry.getValue()).size()]);
          Clause clauseList = QueryBuilder.in(entry.getKey(), propertyValues);
          selectWhere.and(clauseList);
        }
      }
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);
    } catch (Exception e) {
      logger.error(
          context,
          "CassandraOperationImpl:getRecordsByCompositeKey: "
              + Constants.EXCEPTION_MSG_FETCH
              + tableName
              + " : "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != selectQuery) {
        logQueryElapseTime(
            "getRecordsByCompositeKey", startTime, selectQuery.getQueryString(), context);
      }
    }
    return response;
  }
}
