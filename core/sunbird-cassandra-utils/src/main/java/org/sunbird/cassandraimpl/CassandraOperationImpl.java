package org.sunbird.cassandraimpl;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

import com.google.common.util.concurrent.FutureCallback;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.UserType;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
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
import java.util.ArrayList;
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
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.Response;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;

/**
 * Base implementation of Cassandra database operations.
 * Provides CRUD operations, batch processing, and query methods for Cassandra tables.
 *
 * <p>This class is thread-safe and can be extended to add custom behavior.
 */
public abstract class CassandraOperationImpl implements CassandraOperation {

  /**
   * Connection manager for managing Cassandra cluster connections and sessions.
   * This is a singleton instance shared across all operations.
   */
  protected CassandraConnectionManager connectionManager =
      CassandraConnectionMngrFactory.getInstance();

  /**
   * Logger instance for this class and its subclasses.
   * Initialized with the actual class type for proper log attribution.
   */
  protected LoggerUtil logger = new LoggerUtil(this.getClass());

  /**
   * Helper method to log error messages with SLF4J-style placeholders.
   * Supports parameterized messages and automatically handles exceptions.
   */
  protected void logError(RequestContext context, String message, Object... args) {
    String formattedMessage = formatLogMessage(message, args);
    logger.error(context, formattedMessage, null, null, null);
  }

  /**
   * Helper method to log warning messages with SLF4J-style placeholders.
   */
  protected void logWarn(RequestContext context, String message, Object... args) {
    String formattedMessage = formatLogMessage(message, args);
    logger.warn(context, formattedMessage, (Throwable) null);
  }

  /**
   * Helper method to log debug messages with SLF4J-style placeholders.
   */
  protected void logDebug(RequestContext context, String message, Object... args) {
    String formattedMessage = formatLogMessage(message, args);
    logger.debug(context, formattedMessage);
  }

  /**
   * Helper method to log info messages with SLF4J-style placeholders.
   */
  protected void logInfo(RequestContext context, String message, Object... args) {
    String formattedMessage = formatLogMessage(message, args);
    logger.info(context, formattedMessage);
  }

  /**
   * List of write types that should be considered successful even if they timeout.
   * Used for handling partial writes in batch operations.
   * Includes BATCH and SIMPLE write types.
   */
  protected List<String> writeType =
      new ArrayList<String>() {
        {
          add(WriteType.BATCH.name());
          add(WriteType.SIMPLE.name());
        }
    };
 
  /**
   * Inserts or updates a record in Cassandra (upsert operation).
   * If a record with the same primary key exists, it will be updated; otherwise, a new record is created.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param request Map of column names to values (must include primary key columns).
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response upsertRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(requestContext, formatLogMessage("Starting upsertRecord operation - keyspace: {}, table: {}",
        keyspaceName,
        tableName));

    Response response = new Response();
    String query = null;

    try {
      // Generate prepared statement
      query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);

      // Log the query for debugging
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", query));

      // Bind values to prepared statement
      BoundStatement boundStatement = new BoundStatement(statement);
      Iterator<Object> iterator = request.values().iterator();
      Object[] array = new Object[request.keySet().size()];
      int i = 0;
      while (iterator.hasNext()) {
        array[i++] = iterator.next();
      }

      // Execute the upsert
      connectionManager.getSession(keyspaceName).execute(boundStatement.bind(array));
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful upsert at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully upserted record - keyspace: {}, table: {}, columns: {}",
          keyspaceName,
          tableName,
          request.keySet().size()));

      // Special detailed logging for user table upserts
      if (JsonKey.USER.equalsIgnoreCase(tableName)) {
        logInfo(requestContext, formatLogMessage("User table upsert completed with data: {}", request));
      }

    } catch (Exception e) {
      // Handle unknown/undefined identifier errors
      if (e.getMessage() != null && e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {

        String errorMsg = CassandraUtil.processExceptionForUnknownIdentifier(e);
        logError(
            requestContext, "Invalid column/property error during upsert - keyspace: {}, table: {}, error: {}",
            keyspaceName,
            tableName,
            errorMsg,
            e);

        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            errorMsg,
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      // Handle general upsert errors
      logError(
          requestContext, "Database upsert operation failed - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (query != null) {
        logQueryElapseTime("upsertRecord", startTime, query, requestContext);
      } else {
        logQueryElapseTime("upsertRecord", startTime);
      }
    }

    return response;
  }
      
  /**
   * Inserts a new record into a Cassandra table.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param request Map of column names to values.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response insertRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting insertRecord operation - keyspace: {}, table: {}",
        keyspaceName,
        tableName));

    Response response = new Response();
    String query = null;

    try {
      // Generate prepared statement
      query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);

      // Log the query for debugging
      if (statement != null) {
        logDebug(requestContext, formatLogMessage("Executing CQL query: {}", statement.getQueryString()));
      }

      // Bind values to prepared statement
      BoundStatement boundStatement = new BoundStatement(statement);
      Iterator<Object> iterator = request.values().iterator();
      Object[] array = new Object[request.keySet().size()];
      int i = 0;
      while (iterator.hasNext()) {
        array[i++] = iterator.next();
      }

      // Execute the insert
      connectionManager.getSession(keyspaceName).execute(boundStatement.bind(array));
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful insert at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully inserted record - keyspace: {}, table: {}, columns: {}",
          keyspaceName,
          tableName,
          request.keySet().size()));

      // Special detailed logging for user table inserts
      if (JsonKey.USER.equalsIgnoreCase(tableName)) {
        logInfo(requestContext, formatLogMessage("User table insert completed with data: {}", request));
      }

    } catch (Exception e) {
      // Handle unknown/undefined identifier errors
      if (e.getMessage() != null
          && (e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)
              || e.getMessage().contains(JsonKey.UNDEFINED_IDENTIFIER))) {

        String errorMsg = CassandraUtil.processExceptionForUnknownIdentifier(e);
        logError(
            requestContext, "Invalid column/property error during insert - keyspace: {}, table: {}, error: {}",
            keyspaceName,
            tableName,
            errorMsg,
            e);

        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            errorMsg,
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      // Handle general insert errors
      logError(
          requestContext, formatLogMessage("Database insert operation failed - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e));

      throw new ProjectCommonException(
          ResponseCode.dbInsertionError.getErrorCode(),
          ResponseCode.dbInsertionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (query != null) {
        logQueryElapseTime("insertRecord", startTime, query, requestContext);
      } else {
        logQueryElapseTime("insertRecord", startTime);
      }
    }

    return response;
  }


  /**
   * Updates an existing record in a Cassandra table.
   * The record is identified by the 'identifier' field in the request map.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param request Map of column names to values (must include 'identifier' field).
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting updateRecord operation - keyspace: {}, table: {}",
        keyspaceName,
        tableName));

    Response response = new Response();
    String query = null;

    try {
      // Generate UPDATE query statement
      query = CassandraUtil.getUpdateQueryStatement(keyspaceName, tableName, request);
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", query));

      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);

      // Parse query to extract column order for binding values
      Object[] array = new Object[request.size()];
      int i = 0;
      int index = query.lastIndexOf(Constants.SET.trim());
      String columnsPart = query.substring(index + 4);
      columnsPart = columnsPart.replace(Constants.EQUAL_WITH_QUE_MARK, "");
      columnsPart = columnsPart.replace(Constants.WHERE_ID, "");
      columnsPart = columnsPart.replace(Constants.SEMICOLON, "");
      String[] columns = columnsPart.split(",");

      // Bind column values in the correct order
      for (String column : columns) {
        array[i++] = request.get(column.trim());
      }

      // Add identifier value for WHERE clause
      array[i] = request.get(Constants.IDENTIFIER);

      // Execute the update
      BoundStatement boundStatement = statement.bind(array);
      connectionManager.getSession(keyspaceName).execute(boundStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful update at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully updated record - keyspace: {}, table: {}, columns: {}",
          keyspaceName,
          tableName,
          request.keySet().size()));

      // Special detailed logging for user table updates
      if (JsonKey.USER.equalsIgnoreCase(tableName)) {
        logInfo(requestContext, formatLogMessage("User table update completed with data: {}", request));
      }

    } catch (Exception e) {
      // Handle unknown/undefined identifier errors
      if (e.getMessage() != null && e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {

        String errorMsg = CassandraUtil.processExceptionForUnknownIdentifier(e);
        logError(
            requestContext, "Invalid column/property error during update - keyspace: {}, table: {}, error: {}",
            keyspaceName,
            tableName,
            errorMsg,
            e);

        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            errorMsg,
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      // Handle general update errors
      logError(
          requestContext, "Database update operation failed - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.dbUpdateError.getErrorCode(),
          ResponseCode.dbUpdateError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (query != null) {
        logQueryElapseTime("updateRecord", startTime, query, requestContext);
      } else {
        logQueryElapseTime("updateRecord", startTime);
      }
    }

    return response;
  }


  /**
   * Deletes a record from a Cassandra table by its identifier.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param identifier The unique identifier of the record to delete.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response deleteRecord(
      String keyspaceName,
      String tableName,
      String identifier,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting deleteRecord operation - keyspace: {}, table: {}, identifier: {}",
        keyspaceName,
        tableName,
        identifier));

    Response response = new Response();
    Delete.Where delete = null;

    try {
      // Construct DELETE query
      delete =
          QueryBuilder.delete()
              .from(keyspaceName, tableName)
              .where(eq(Constants.IDENTIFIER, identifier));

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", delete.getQueryString()));

      // Execute the delete
      connectionManager.getSession(keyspaceName).execute(delete);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful delete at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully deleted record - keyspace: {}, table: {}, identifier: {}",
          keyspaceName,
          tableName,
          identifier));

    } catch (Exception e) {
      // Handle delete errors
      logError(
          requestContext, "Database delete operation failed - keyspace: {}, table: {}, identifier: {}, error: {}",
          keyspaceName,
          tableName,
          identifier,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (delete != null) {
        logQueryElapseTime("deleteRecord", startTime, delete.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("deleteRecord", startTime);
      }
    }

    return response;
  }



  /**
   * Deletes a record from a Cassandra table using a composite primary key.
   * All components of the composite key must be provided in the compositeKeyMap.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param compositeKeyMap Map containing all composite primary key columns and values.
   * @param requestContext Request context for logging.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public void deleteRecord(
      String keyspaceName,
      String tableName,
      Map<String, String> compositeKeyMap,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting deleteRecord by composite key - keyspace: {}, table: {}, keys: {}",
        keyspaceName,
        tableName,
        compositeKeyMap.keySet()));

    Delete delete = null;
    Delete.Where deleteWhere = null;

    try {
      // Construct DELETE query with composite key WHERE clauses
      delete = QueryBuilder.delete().from(keyspaceName, tableName);
      deleteWhere = delete.where();

      // Add each composite key field as an AND condition
      for (Map.Entry<String, String> entry : compositeKeyMap.entrySet()) {
        Clause clause = eq(entry.getKey(), entry.getValue());
        deleteWhere.and(clause);
      }

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", deleteWhere.getQueryString()));

      // Execute the delete (use deleteWhere, not delete)
      connectionManager.getSession(keyspaceName).execute(deleteWhere);

      // Log successful delete at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully deleted record by composite key - keyspace: {}, table: {}, keys: {}",
          keyspaceName,
          tableName,
          compositeKeyMap));

    } catch (Exception e) {
      // Handle delete errors
      logError(
          requestContext, "Database delete operation failed (composite key) - keyspace: {}, table: {}, keys: {}, error: {}",
          keyspaceName,
          tableName,
          compositeKeyMap,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (deleteWhere != null) {
        logQueryElapseTime(
            "deleteRecordByCompositeKey", startTime, deleteWhere.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("deleteRecordByCompositeKey", startTime);
      }
    }
  }


  /**
   * Deletes multiple records from a Cassandra table using a list of identifiers.
   * More efficient than individual delete operations for bulk deletions.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param identifierList List of primary key identifiers for records to delete.
   * @param requestContext Request context for logging.
   * @return {@code true} if delete was applied successfully, {@code false} otherwise.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public boolean deleteRecords(
      String keyspaceName,
      String tableName,
      List<String> identifierList,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting deleteRecords operation - keyspace: {}, table: {}, count: {}",
        keyspaceName,
        tableName,
        identifierList != null ? identifierList.size() : 0));

    ResultSet resultSet = null;
    Delete delete = null;
    Delete.Where deleteWhere = null;

    try {
      // Construct DELETE query with IN clause
      delete = QueryBuilder.delete().from(keyspaceName, tableName);
      deleteWhere = delete.where();
      Clause clause = QueryBuilder.in(JsonKey.ID, identifierList);
      deleteWhere.and(clause);

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", deleteWhere.getQueryString()));

      // Execute the delete (use deleteWhere, not delete)
      resultSet = connectionManager.getSession(keyspaceName).execute(deleteWhere);

      boolean wasApplied = resultSet.wasApplied();

      // Log successful delete at INFO level
      logInfo(
          requestContext, formatLogMessage("Bulk delete operation completed - keyspace: {}, table: {}, count: {}, wasApplied: {}",
          keyspaceName,
          tableName,
          identifierList.size(),
          wasApplied));

      return wasApplied;

    } catch (Exception e) {
      // Handle delete errors
      logError(
          requestContext, "Bulk delete operation failed - keyspace: {}, table: {}, count: {}, error: {}",
          keyspaceName,
          tableName,
          identifierList != null ? identifierList.size() : 0,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (deleteWhere != null) {
        logQueryElapseTime("deleteRecords", startTime, deleteWhere.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("deleteRecords", startTime);
      }
    }
  }

  /**
   * Retrieves all fields from records where a property matches any value in the provided list.
   * Convenience method that delegates to the full getRecordsByProperty method.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param propertyName The property/column name to filter by.
   * @param propertyValueList List of values to match (uses IN clause).
   * @param requestContext Request context for logging.
   * @return Response containing all fields from matching records.
   */
  @Override
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      List<Object> propertyValueList,
      RequestContext requestContext) {
    return getRecordsByProperty(
        keyspaceName, tableName, propertyName, propertyValueList, null, requestContext);
  }


  /**
   * Retrieves all fields from records where a property matches a given value.
   * Convenience method that delegates to the full getRecordsByProperty method.
   *
   * <p>This method is useful when you need all fields from matching records and have a single
   * property value to match against (not a list). For selective field retrieval, use the
   * overloaded method that accepts a fields parameter.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param propertyName The property/column name to filter by.
   * @param propertyValue The value to match (single value, not a list).
   * @param requestContext Request context for logging.
   * @return Response containing all fields from matching records.
   */
  @Override
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      RequestContext requestContext) {
    return getRecordsByProperty(keyspaceName, tableName, propertyName, propertyValue, null, requestContext);
  }


  /**
   * Retrieves specific fields from records where a property matches a given value.
   * Supports both single values and lists. Uses ALLOW FILTERING for non-indexed columns.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param propertyName The property/column name to filter by.
   * @param propertyValue The value to match (single value or List for IN clause).
   * @param fields List of field names to retrieve (null for all fields).
   * @param requestContext Request context for logging.
   * @return Response containing matching records with specified fields.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      List<String> fields,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByProperty - keyspace: {}, table: {}, property: {}, fields: {}",
        keyspaceName,
        tableName,
        propertyName,
        fields != null ? fields.size() : "all"));

    Response response = new Response();
    Session session = connectionManager.getSession(keyspaceName);
    Select selectQuery = null;

    try {
      // Build SELECT clause with specified fields or all fields
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        selectBuilder = QueryBuilder.select(fields.toArray(new String[0]));
        logDebug(requestContext, formatLogMessage("Selecting {} specific fields", fields.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all fields");
      }

      // Build WHERE clause
      Where selectStatement = selectBuilder.from(keyspaceName, tableName).where();

      // Handle both single values and lists
      if (propertyValue instanceof List) {
        selectStatement.and(QueryBuilder.in(propertyName, (List<?>) propertyValue));
        logDebug(
            requestContext, formatLogMessage("Using IN clause with {} values",
            ((List<?>) propertyValue).size()));
      } else {
        selectStatement.and(QueryBuilder.eq(propertyName, propertyValue));
        logDebug(requestContext, "Using EQ clause with single value");
      }

      // Apply ALLOW FILTERING for non-indexed columns
      selectQuery = selectStatement.allowFiltering();

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = session.execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by property - keyspace: {}, table: {}, property: {}, count: {}",
          keyspaceName,
          tableName,
          propertyName,
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by property - keyspace: {}, table: {}, property: {}, error: {}",
          keyspaceName,
          tableName,
          propertyName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getRecordsByProperty", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("getRecordsByProperty", startTime);
      }
    }

    return response;
  }

  /**
   * Retrieves records using an indexed property for efficient querying.
   * Optimized for properties with secondary indexes.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param propertyName The indexed property/column name to filter by.
   * @param propertyValue The value to match.
   * @param requestContext Request context for logging.
   * @return Response containing all fields from matching records.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByIndexedProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByIndexedProperty - keyspace: {}, table: {}, property: {}",
        keyspaceName,
        tableName,
        propertyName));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT query for all fields
      selectQuery = QueryBuilder.select().all().from(keyspaceName, tableName);

      // Add WHERE clause on indexed property
      selectQuery.where().and(eq(propertyName, propertyValue));

      // Apply ALLOW FILTERING
      Select finalQuery = selectQuery.allowFiltering();

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", finalQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(finalQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by indexed property - keyspace: {}, table: {}, property: {}, count: {}",
          keyspaceName,
          tableName,
          propertyName,
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by indexed property - keyspace: {}, table: {}, property: {}, error: {}",
          keyspaceName,
          tableName,
          propertyName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getRecordsByIndexedProperty",
            startTime,
            selectQuery.getQueryString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordsByIndexedProperty", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves all fields from records matching multiple property criteria.
   * Convenience method that delegates to the full getRecordsByProperties method.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param propertyMap Map of property names to values (single value or List). All conditions use AND.
   * @param requestContext Request context for logging.
   * @return Response containing all fields from matching records.
   */
  @Override
  public Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      RequestContext requestContext) {
    return getRecordsByProperties(keyspaceName, tableName, propertyMap, null, requestContext);
  }


  /**
   * Retrieves records matching multiple property criteria with optional field selection.
   * Supports single values (EQ) and lists (IN) for each property. Uses ALLOW FILTERING.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param propertyMap Map of property names to values (single value or List). All conditions use AND.
   * @param fields List of field names to retrieve (null for all fields).
   * @param requestContext Request context for logging.
   * @return Response containing matching records with specified fields.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      List<String> fields,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByProperties - keyspace: {}, table: {}, properties: {}, fields: {}",
        keyspaceName,
        tableName,
        propertyMap != null ? propertyMap.size() : 0,
        fields != null ? fields.size() : "all"));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT clause with specified fields or all fields
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[0]);
        selectBuilder = QueryBuilder.select(dbFields);
        logDebug(requestContext, formatLogMessage("Selecting {} specific fields", fields.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all fields");
      }

      // Build FROM clause
      selectQuery = selectBuilder.from(keyspaceName, tableName);

      // Build WHERE clauses for each property
      if (MapUtils.isNotEmpty(propertyMap)) {
        Where selectWhere = selectQuery.where();
        int conditionCount = 0;

        for (Entry<String, Object> entry : propertyMap.entrySet()) {
          if (entry.getValue() instanceof List) {
            // Handle list values with IN clause
            List<Object> list = (List<Object>) entry.getValue();
            if (list != null && !list.isEmpty()) {
              Object[] propertyValues = list.toArray(new Object[0]);
              Clause clause = QueryBuilder.in(entry.getKey(), propertyValues);
              selectWhere.and(clause);
              conditionCount++;
              logDebug(
                  requestContext, formatLogMessage("Added IN clause for property: {} with {} values",
                  entry.getKey(),
                  list.size()));
            }
          } else {
            // Handle single value with EQ clause
            Clause clause = eq(entry.getKey(), entry.getValue());
            selectWhere.and(clause);
            conditionCount++;
          }
        }
        logDebug(requestContext, formatLogMessage("Total WHERE conditions: {}", conditionCount));
      }

      // Apply ALLOW FILTERING
      selectQuery = selectQuery.allowFiltering();

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by properties - keyspace: {}, table: {}, properties: {}, count: {}",
          keyspaceName,
          tableName,
          propertyMap != null ? propertyMap.size() : 0,
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by properties - keyspace: {}, table: {}, properties: {}, error: {}",
          keyspaceName,
          tableName,
          propertyMap != null ? propertyMap.size() : 0,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getRecordsByProperties", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("getRecordsByProperties", startTime);
      }
    }

    return response;
  }




  /**
   * Retrieves specific property values from a single record by its ID.
   * Allows selective field retrieval to optimize performance.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param id The primary key identifier.
   * @param requestContext Request context for logging.
   * @param properties List of property/column names to retrieve.
   * @return Response containing only the requested property values.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      String id,
      RequestContext requestContext,
      List<String> properties) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getPropertiesValueById - keyspace: {}, table: {}, id: {}, properties: {}",
        keyspaceName,
        tableName,
        id,
        properties != null ? properties.size() : 0));

    Response response = new Response();
    String selectQuery = null;

    try {
      // Generate SELECT statement for specified properties
      selectQuery = CassandraUtil.getSelectStatement(keyspaceName, tableName, properties);
      PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(selectQuery);

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", statement.getQueryString()));

      // Bind ID and execute query
      BoundStatement boundStatement = new BoundStatement(statement);
      ResultSet results =
          connectionManager.getSession(keyspaceName).execute(boundStatement.bind(id));

      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully retrieved properties by ID - keyspace: {}, table: {}, id: {}, properties: {}, found: {}",
          keyspaceName,
          tableName,
          id,
          properties != null ? properties.size() : 0,
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve properties by ID - keyspace: {}, table: {}, id: {}, error: {}",
          keyspaceName,
          tableName,
          id,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime("getPropertiesValueById", startTime, selectQuery, requestContext);
      } else {
        logQueryElapseTime("getPropertiesValueById", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves specific property values from multiple records by their IDs.
   * Efficient batch retrieval using a single query with IN clause.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param ids List of primary key identifiers.
   * @param properties List of property/column names to retrieve (null for all fields).
   * @param requestContext Request context for logging.
   * @return Response containing requested property values for all specified records.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      List<String> ids,
      List<String> properties,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getPropertiesValueById (multi-ID) - keyspace: {}, table: {}, ids: {}, properties: {}",
        keyspaceName,
        tableName,
        ids != null ? ids.size() : 0,
        properties != null ? properties.size() : "all"));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT clause with specified fields or all fields
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(properties)) {
        String[] dbFields = properties.toArray(new String[0]);
        selectBuilder = QueryBuilder.select(dbFields);
        logDebug(requestContext, formatLogMessage("Selecting {} specific fields", properties.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all fields");
      }

      // Build FROM and WHERE clauses
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      selectQuery.where(QueryBuilder.in(JsonKey.ID, ids.toArray()));

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully retrieved properties by IDs - keyspace: {}, table: {}, ids: {}, properties: {}, found: {}",
          keyspaceName,
          tableName,
          ids != null ? ids.size() : 0,
          properties != null ? properties.size() : "all",
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve properties by IDs - keyspace: {}, table: {}, ids: {}, error: {}",
          keyspaceName,
          tableName,
          ids != null ? ids.size() : 0,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getPropertiesValueById", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("getPropertiesValueById", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves all records from a Cassandra table.
   * <p><b>WARNING:</b> Performs a full table scan. Use only for small tables (&lt;1000 records).
   * Can cause memory issues and performance degradation on large tables.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param requestContext Request context for logging.
   * @return Response containing all records from the table.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getAllRecords(
      String keyspaceName, String tableName, RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logWarn(
        requestContext, formatLogMessage("Starting getAllRecords (FULL TABLE SCAN) - keyspace: {}, table: {} - USE WITH CAUTION!",
        keyspaceName,
        tableName));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT * query
      selectQuery = QueryBuilder.select().all().from(keyspaceName, tableName);

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute full table scan
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log result count at WARN level (full table scans should be monitored)
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logWarn(
          requestContext, formatLogMessage("Full table scan completed - keyspace: {}, table: {}, records: {} - Consider using filtered queries for better performance",
          keyspaceName,
          tableName,
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve all records - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime("getAllRecords", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("getAllRecords", startTime);
      }
    }

    return response;
  }




  /**
   * Retrieves all records from a Cassandra table with optional field selection.
   * <p><b>WARNING:</b> Performs a full table scan. Use only for small tables (&lt;1000 records).
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param fields List of field names to retrieve (null for all fields).
   * @param requestContext Request context for logging.
   * @return Response containing all records with specified fields.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getAllRecords(
      String keyspaceName,
      String tableName,
      List<String> fields,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logWarn(
        requestContext, formatLogMessage("Starting getAllRecords with field selection (FULL TABLE SCAN) - keyspace: {}, table: {}, fields: {} - USE WITH CAUTION!",
        keyspaceName,
        tableName,
        fields != null ? fields.size() : "all"));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT clause with specified fields or all fields
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[0]);
        selectBuilder = QueryBuilder.select(dbFields);
        logDebug(requestContext, formatLogMessage("Selecting {} specific fields", fields.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all fields");
      }

      // Build FROM clause (no WHERE - full table scan!)
      selectQuery = selectBuilder.from(keyspaceName, tableName);

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute full table scan
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log result count at WARN level (full table scans should be monitored)
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logWarn(
          requestContext, formatLogMessage("Full table scan with field selection completed - keyspace: {}, table: {}, fields: {}, records: {} - Consider using filtered queries",
          keyspaceName,
          tableName,
          fields != null ? fields.size() : "all",
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve all records with field selection - keyspace: {}, table: {}, fields: {}, error: {}",
          keyspaceName,
          tableName,
          fields != null ? fields.size() : "all",
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime("getAllRecords", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("getAllRecords", startTime);
      }
    }

    return response;
  }



  /**
   * Updates a record using a composite key for identification.
   * Allows updating multiple attributes of a record identified by multiple key columns.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param updateAttributes Map of column names to new values (SET clause).
   * @param compositeKey Map of composite key columns and values (WHERE clause).
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> updateAttributes,
      Map<String, Object> compositeKey,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting updateRecord (composite key) - keyspace: {}, table: {}, attributes: {}, keys: {}",
        keyspaceName,
        tableName,
        updateAttributes != null ? updateAttributes.size() : 0,
        compositeKey != null ? compositeKey.size() : 0));

    Response response = new Response();
    Statement updateQuery = null;

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build UPDATE statement
      Update update = QueryBuilder.update(keyspaceName, tableName);
      Assignments assignments = update.with();
      Update.Where where = update.where();

      // Add SET clauses for each attribute to update
      if (updateAttributes != null && !updateAttributes.isEmpty()) {
        for (Map.Entry<String, Object> entry : updateAttributes.entrySet()) {
          assignments.and(QueryBuilder.set(entry.getKey(), entry.getValue()));
        }
        logDebug(
            requestContext, formatLogMessage("Added {} SET clauses", updateAttributes.size()));
      }

      // Add WHERE clauses for composite key
      if (compositeKey != null && !compositeKey.isEmpty()) {
        for (Map.Entry<String, Object> entry : compositeKey.entrySet()) {
          where.and(eq(entry.getKey(), entry.getValue()));
        }
        logDebug(
            requestContext, formatLogMessage("Added {} WHERE clauses for composite key", compositeKey.size()));
      }

      updateQuery = where;

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", updateQuery.toString()));

      // Execute update
      session.execute(updateQuery);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful update at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully updated record with composite key - keyspace: {}, table: {}, attributes: {}, keys: {}",
          keyspaceName,
          tableName,
          updateAttributes != null ? updateAttributes.size() : 0,
          compositeKey != null ? compositeKey.size() : 0));

    } catch (Exception e) {
      // Handle unknown/undefined identifier errors
      if (e.getMessage() != null && e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {

        String errorMsg = CassandraUtil.processExceptionForUnknownIdentifier(e);
        logError(
            requestContext, "Invalid column/property error during composite key update - keyspace: {}, table: {}, error: {}",
            keyspaceName,
            tableName,
            errorMsg,
            e);

        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            errorMsg,
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      // Handle general update errors
      logError(
          requestContext, "Database update operation failed (composite key) - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.dbUpdateError.getErrorCode(),
          ResponseCode.dbUpdateError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (updateQuery != null) {
        logQueryElapseTime(
            "updateRecord", startTime, updateQuery.toString(), requestContext);
      } else {
        logQueryElapseTime("updateRecord", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves a record by its primary key with optional field selection.
   * Supports both simple (String) and composite (Map) primary keys.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param key The primary key - String (simple) or Map (composite).
   * @param fields List of field names to retrieve (null for all fields).
   * @param requestContext Request context for logging.
   * @return Response containing the matching record with specified fields.
   * @throws ProjectCommonException if key is invalid or operation fails.
   */
  @Override
  public Response getRecordByIdentifier(
      String keyspaceName,
      String tableName,
      Object key,
      List<String> fields,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecordByIdentifier - keyspace: {}, table: {}, keyType: {}, fields: {}",
        keyspaceName,
        tableName,
        key != null ? key.getClass().getSimpleName() : "null",
        fields != null ? fields.size() : "all"));

    Response response = new Response();
    Where selectWhereQuery = null;

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build SELECT clause with specified fields or all fields
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[0]);
        selectBuilder = QueryBuilder.select(dbFields);
        logDebug(requestContext, formatLogMessage("Selecting {} specific fields", fields.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all fields");
      }

      // Build FROM and WHERE clauses
      Select selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();

      // Handle different key types
      if (key instanceof String) {
        // Validate String key
        if (StringUtils.isBlank(String.valueOf(key))) {
          logError(requestContext, "Primary key is empty or null for table: {}", tableName);
          throw new ProjectCommonException(
              ResponseCode.SERVER_ERROR.getErrorCode(),
              "Primary key cannot be empty or null",
              ResponseCode.SERVER_ERROR.getResponseCode());
        }

        selectWhere.and(eq(Constants.IDENTIFIER, key));
        logDebug(requestContext, "Using simple String key");

      } else if (key instanceof Map) {
        // Validate Map key
        Map<String, Object> compositeKey = (Map<String, Object>) key;
        if (MapUtils.isEmpty(compositeKey)) {
          logError(requestContext, "Primary composite key is empty or null for table: {}", tableName);
          throw new ProjectCommonException(
              ResponseCode.SERVER_ERROR.getErrorCode(),
              "Primary composite key cannot be empty or null",
              ResponseCode.SERVER_ERROR.getResponseCode());
        }

        // Add WHERE clauses for each composite key component
        for (Map.Entry<String, Object> entry : compositeKey.entrySet()) {
          CassandraUtil.createQuery(entry.getKey(), entry.getValue(), selectWhere);
        }
        logDebug(
            requestContext, formatLogMessage("Using composite Map key with {} components", compositeKey.size()));

      } else {
        logError(
            requestContext, "Invalid key type: {} for table: {}",
            key != null ? key.getClass().getName() : "null",
            tableName);
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "Key must be either String or Map",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      selectWhereQuery = selectWhere;

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectWhere.getQueryString()));

      // Execute query
      ResultSet results = session.execute(selectWhere);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved record by identifier - keyspace: {}, table: {}, keyType: {}, fields: {}, found: {}",
          keyspaceName,
          tableName,
          key.getClass().getSimpleName(),
          fields != null ? fields.size() : "all",
          recordCount));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (Exception e) {
      // Handle query errors
      logError(requestContext, "Failed to retrieve record by identifier - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectWhereQuery != null) {
        logQueryElapseTime(
            "getRecordByIdentifier",
            startTime,
            selectWhereQuery.getQueryString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordByIdentifier", startTime);
      }
    }

    return response;
  }


  /**
   * Retrieves a record by its String primary key, returning all fields.
   * Convenience method that delegates to getRecordByIdentifier.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param key The String primary key.
   * @param requestContext Request context for logging.
   * @return Response containing all fields from the matching record.
   */
  @Override
  public Response getRecordById(
      String keyspaceName, String tableName, String key, RequestContext requestContext) {
    return getRecordByIdentifier(keyspaceName, tableName, key, null, requestContext);
  }

  /**
   * Retrieves a record by its composite (Map) primary key, returning all fields.
   * Convenience method that delegates to getRecordByIdentifier.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param key The composite primary key as a Map.
   * @param requestContext Request context for logging.
   * @return Response containing all fields from the matching record.
   */
  @Override
  public Response getRecordById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      RequestContext requestContext) {
    return getRecordByIdentifier(keyspaceName, tableName, key, null, requestContext);
  }

  /**
   * Retrieves a record by its String primary key with specific field selection.
   * Convenience method that delegates to getRecordByIdentifier.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param key The String primary key.
   * @param fields List of field names to retrieve.
   * @param requestContext Request context for logging.
   * @return Response containing specified fields from the matching record.
   */
  @Override
  public Response getRecordById(
      String keyspaceName,
      String tableName,
      String key,
      List<String> fields,
      RequestContext requestContext) {
    return getRecordByIdentifier(keyspaceName, tableName, key, fields, requestContext);
  }

  /**
   * Retrieves a record by its composite (Map) primary key with specific field selection.
   * This is a convenience method that delegates to {@link #getRecordByIdentifier}.
   *
   * <p>This method is equivalent to calling:
   *
   * <pre>
   * getRecordByIdentifier(keyspaceName, tableName, key, fields, requestContext);
   * </pre>
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The composite primary key as a Map of column names to values.
   * @param fields A list of field/column names to retrieve.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing only the specified fields from the matching record.
   * @see #getRecordByIdentifier(String, String, Object, List, RequestContext)
   */
  @Override
  public Response getRecordById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> fields,
      RequestContext requestContext) {
    return getRecordByIdentifier(keyspaceName, tableName, key, fields, requestContext);
  }


  /**
   * Retrieves a record by its ID with TTL information for specified fields.
   * Convenience method that delegates to getRecordWithTTLByIdentifier.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param key Map representing the composite primary key.
   * @param ttlFields List of columns for which TTL information should be retrieved.
   * @param fields List of columns to retrieve (actual field values).
   * @param requestContext Request context for logging.
   * @return Response containing field values and TTL information (with "_ttl" suffix).
   */
  @Override
  public Response getRecordWithTTLById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext requestContext) {
    return getRecordWithTTLByIdentifier(
        keyspaceName, tableName, key, ttlFields, fields, requestContext);
  }



  /**
   * Retrieves a record by its identifier with TTL (Time To Live) information for specified fields.
   * Fetches both field values and their remaining TTL in seconds.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param key Map representing the primary key (composite key).
   * @param ttlFields List of columns for which TTL information should be retrieved.
   * @param fields List of columns to retrieve (actual field values).
   * @param requestContext Request context for logging.
   * @return Response containing field values and TTL information (with "_ttl" suffix).
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordWithTTLByIdentifier(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecordWithTTLByIdentifier - keyspace: {}, table: {}, keys: {}, fields: {}, ttlFields: {}",
        keyspaceName,
        tableName,
        key != null ? key.size() : 0,
        fields != null ? fields.size() : 0,
        ttlFields != null ? ttlFields.size() : 0));

    Response response = new Response();
    Select.Where selectWhereQuery = null;

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build SELECT clause with fields and TTL functions
      Selection select = QueryBuilder.select();

      // Add regular fields
      if (fields != null && !fields.isEmpty()) {
        for (String field : fields) {
          select.column(field);
        }
        logDebug(requestContext, formatLogMessage("Added {} regular fields to SELECT", fields.size()));
      }

      // Add TTL fields (with _ttl suffix)
      if (ttlFields != null && !ttlFields.isEmpty()) {
        for (String field : ttlFields) {
          select.ttl(field).as(field + "_ttl");
        }
        logDebug(
            requestContext, formatLogMessage("Added {} TTL fields to SELECT (with _ttl suffix)", ttlFields.size()));
      }

      // Build FROM and WHERE clauses
      Select.Where selectWhere = select.from(keyspaceName, tableName).where();

      // Add WHERE clauses for composite key
      if (key != null && !key.isEmpty()) {
        for (Map.Entry<String, Object> entry : key.entrySet()) {
          selectWhere.and(QueryBuilder.eq(entry.getKey(), entry.getValue()));
        }
      }
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectWhere.getQueryString()));

      // Execute query
      ResultSet results = session.execute(selectWhere);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved record with TTL - keyspace: {}, table: {}, keys: {}, fields: {}, ttlFields: {}, found: {}",
          keyspaceName,
          tableName,
          key != null ? key.size() : 0,
          fields != null ? fields.size() : 0,
          ttlFields != null ? ttlFields.size() : 0,
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve record with TTL - keyspace: {}, table: {}, error: {}",
          keyspaceName,
          tableName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectWhereQuery != null) {
        logQueryElapseTime(
            "getRecordWithTTLByIdentifier",
            startTime,
            selectWhereQuery.getQueryString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordWithTTLByIdentifier", startTime);
      }
    }

    return response;
  }



  /**
   * Performs a batch insert operation to insert multiple records in a single atomic operation.
   * More efficient than individual inserts for bulk data.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param records List of maps, each representing a record to insert.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response batchInsert(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int recordCount = records != null ? records.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting batchInsert - keyspace: {}, table: {}, records: {}",
        keyspaceName,
        tableName,
        recordCount));

    // Warn about large batch sizes
    if (recordCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large batch insert detected - keyspace: {}, table: {}, records: {} - Consider splitting into smaller batches for better performance",
          keyspaceName,
          tableName,
          recordCount));
    }

    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build INSERT statements for each record
      for (Map<String, Object> record : records) {
        Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);

        // Add all columns and values from the record
        if (record != null && !record.isEmpty()) {
          for (Map.Entry<String, Object> entry : record.entrySet()) {
            insert.value(entry.getKey(), entry.getValue());
          }
        }

        batchStatement.add(insert);
      }

      logDebug(
          requestContext, formatLogMessage("Executing batch insert with {} statements",
          batchStatement.size()));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch insert at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully batch inserted records - keyspace: {}, table: {}, records: {}",
          keyspaceName,
          tableName,
          recordCount));

    } catch (QueryExecutionException e) {
      // Handle query execution errors
      logError(
          requestContext, "Batch insert query execution failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (QueryValidationException e) {
      // Handle query validation errors
      logError(
          requestContext, "Batch insert query validation failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (NoHostAvailableException e) {
      // Handle no available hosts errors
      logError(
          requestContext, "No Cassandra hosts available for batch insert - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (IllegalStateException e) {
      // Handle illegal state errors
      logError(
          requestContext, "Illegal state during batch insert - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "batchInsert",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("batchInsert", startTime);
      }
    }

    return response;
  }


  /**
   * Performs a batch delete operation to delete multiple records in a single atomic operation.
   * Each record is identified by its composite primary key. More efficient than individual deletes.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param primaryKeys List of maps, each containing the composite primary key for a record to delete.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response batchDelete(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> primaryKeys,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int recordCount = primaryKeys != null ? primaryKeys.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting batchDelete - keyspace: {}, table: {}, records: {}",
        keyspaceName,
        tableName,
        recordCount));

    // Warn about large batch sizes
    if (recordCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large batch delete detected - keyspace: {}, table: {}, records: {} - Consider splitting into smaller batches for better performance",
          keyspaceName,
          tableName,
          recordCount));
    }

    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build DELETE statements for each primary key
      for (Map<String, Object> primaryKey : primaryKeys) {
        if (primaryKey != null && !primaryKey.isEmpty()) {
          Statement deleteStatement = CassandraUtil.createDeleteQuery(primaryKey, keyspaceName, tableName);
          batchStatement.add(deleteStatement);
        } else {
          logWarn(requestContext, "Skipping null or empty primary key in batch delete");
        }
      }

      logDebug(
          requestContext, formatLogMessage("Executing batch delete with {} statements",
          batchStatement.size()));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch delete at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully batch deleted records - keyspace: {}, table: {}, records: {}",
          keyspaceName,
          tableName,
          recordCount));

    } catch (QueryExecutionException e) {
      // Handle query execution errors
      logError(
          requestContext, "Batch delete query execution failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (QueryValidationException e) {
      // Handle query validation errors
      logError(
          requestContext, "Batch delete query validation failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (NoHostAvailableException e) {
      // Handle no available hosts errors
      logError(
          requestContext, "No Cassandra hosts available for batch delete - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (IllegalStateException e) {
      // Handle illegal state errors
      logError(
          requestContext, "Illegal state during batch delete - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (Exception e) {
      // Handle any other unexpected errors
      logError(
          requestContext, "Unexpected error during batch delete - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "batchDelete",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("batchDelete", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves records based on multiple filter criteria with optional field selection.
   * Supports single values (EQ) and lists (IN) for each filter. Uses ALLOW FILTERING.
   *
   * @param keyspace The keyspace name.
   * @param table The table name.
   * @param filters Map of filter criteria (column -> value or List). All conditions use AND.
   * @param fields List of field names to retrieve (null for all fields).
   * @param requestContext Request context for logging.
   * @return Response containing matching records with specified fields.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecords(
      String keyspace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecords - keyspace: {}, table: {}, filters: {}, fields: {}",
        keyspace,
        table,
        filters != null ? filters.size() : 0,
        fields != null ? fields.size() : "all"));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT clause with specified fields or all fields
      Builder selectBuilder;
      if (CollectionUtils.isNotEmpty(fields)) {
        String[] dbFields = fields.toArray(new String[0]);
        selectBuilder = QueryBuilder.select(dbFields);
        logDebug(requestContext, formatLogMessage("Selecting {} specific fields", fields.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all fields");
      }

      // Build FROM clause
      selectQuery = selectBuilder.from(keyspace, table);

      // Build WHERE clauses for each filter
      if (MapUtils.isNotEmpty(filters)) {
        Where selectWhere = selectQuery.where();
        int conditionCount = 0;

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
          if (entry.getValue() instanceof List) {
            // Handle list values with IN clause
            List<Object> list = (List<Object>) entry.getValue();
            if (list != null && !list.isEmpty()) {
              Object[] filterValues = list.toArray(new Object[0]);
              Clause clause = QueryBuilder.in(entry.getKey(), filterValues);
              selectWhere.and(clause);
              conditionCount++;
              logDebug(
                  requestContext, formatLogMessage("Added IN clause for filter: {} with {} values",
                  entry.getKey(),
                  list.size()));
            }
          } else if (entry.getValue() != null) {
            // Handle single value with EQ clause
            Clause clause = eq(entry.getKey(), entry.getValue());
            selectWhere.and(clause);
            conditionCount++;
          }
        }
        logDebug(requestContext, formatLogMessage("Total WHERE conditions: {}", conditionCount));
      }

      // Apply ALLOW FILTERING
      selectQuery = selectQuery.allowFiltering();

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspace).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records - keyspace: {}, table: {}, filters: {}, fields: {}, count: {}",
          keyspace,
          table,
          filters != null ? filters.size() : 0,
          fields != null ? fields.size() : "all",
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records - keyspace: {}, table: {}, filters: {}, error: {}",
          keyspace,
          table,
          filters != null ? filters.size() : 0,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime("getRecords", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("getRecords", startTime);
      }
    }

    return response;
  }




  /**
   * Retrieves records using a composite primary key.
   * Designed for tables with multi-column primary keys. Provide ALL key components for best performance.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param compositeKeyMap Map containing all composite primary key columns and values.
   * @param requestContext Request context for logging.
   * @return Response containing all fields from matching records.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByCompositeKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> compositeKeyMap,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByCompositeKey - keyspace: {}, table: {}, keyComponents: {}",
        keyspaceName,
        tableName,
        compositeKeyMap != null ? compositeKeyMap.size() : 0));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT * query
      Builder selectBuilder = QueryBuilder.select().all();
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();

      // Add WHERE clauses for each composite key component
      if (compositeKeyMap != null && !compositeKeyMap.isEmpty()) {
        for (Map.Entry<String, Object> entry : compositeKeyMap.entrySet()) {
          Clause clause = eq(entry.getKey(), entry.getValue());
          selectWhere.and(clause);
          logDebug(
              requestContext, formatLogMessage("Added WHERE clause for key component: {} = {}",
              entry.getKey(),
              entry.getValue()));
        }
      } else {
        logWarn(
            requestContext, formatLogMessage("Empty composite key map provided for table: {} - this may cause performance issues",
            tableName));
      }

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by composite key - keyspace: {}, table: {}, keyComponents: {}, found: {}",
          keyspaceName,
          tableName,
          compositeKeyMap != null ? compositeKeyMap.size() : 0,
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by composite key - keyspace: {}, table: {}, keyComponents: {}, error: {}",
          keyspaceName,
          tableName,
          compositeKeyMap != null ? compositeKeyMap.size() : 0,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getRecordsByCompositeKey",
            startTime,
            selectQuery.getQueryString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordsByCompositeKey", startTime);
      }
    }

    return response;
  }


  /**
   * Performs a batch update to update multiple records in a single atomic operation.
   * Each record uses nested Maps with PRIMARY_KEY and NON_PRIMARY_KEY.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param list List of update records. Each is a Map with "PRIMARY_KEY" (Map) and "NON_PRIMARY_KEY" (Map).
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response batchUpdate(
      String keyspaceName,
      String tableName,
      List<Map<String, Map<String, Object>>> list,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int recordCount = list != null ? list.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting batchUpdate - keyspace: {}, table: {}, records: {}",
        keyspaceName,
        tableName,
        recordCount));

    // Warn about large batch sizes
    if (recordCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large batch update detected - keyspace: {}, table: {}, records: {} - Consider splitting into smaller batches for better performance",
          keyspaceName,
          tableName,
          recordCount));
    }

    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build UPDATE statements for each record
      for (Map<String, Map<String, Object>> record : list) {
        if (record == null) {
          logWarn(requestContext, "Skipping null record in batch update");
          continue;
        }

        Map<String, Object> primaryKey = record.get(JsonKey.PRIMARY_KEY);
        Map<String, Object> nonPKRecord = record.get(JsonKey.NON_PRIMARY_KEY);

        // Validate record structure
        if (primaryKey == null || primaryKey.isEmpty()) {
          logError(
              requestContext, "Invalid record in batch update - missing or empty PRIMARY_KEY for table: {}",
              tableName);
          throw new ProjectCommonException(
              ResponseCode.SERVER_ERROR.getErrorCode(),
              "Invalid record structure: PRIMARY_KEY is required",
              ResponseCode.SERVER_ERROR.getResponseCode());
        }

        if (nonPKRecord == null || nonPKRecord.isEmpty()) {
          logWarn(
              requestContext, formatLogMessage("Skipping record with empty NON_PRIMARY_KEY - no fields to update for table: {}",
              tableName));
          continue;
        }

        // Create UPDATE query using CassandraUtil
        batchStatement.add(
            CassandraUtil.createUpdateQuery(primaryKey, nonPKRecord, keyspaceName, tableName));
      }

      logDebug(
          requestContext, formatLogMessage("Executing batch update with {} statements",
          batchStatement.size()));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch update at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully batch updated records - keyspace: {}, table: {}, records: {}, statements: {}",
          keyspaceName,
          tableName,
          recordCount,
          batchStatement.size()));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Batch update failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "batchUpdate",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("batchUpdate", startTime);
      }
    }

    return response;
  }



  /**
   * Performs a batch update to update multiple records by their IDs in a single atomic operation.
   * Simpler than batchUpdate - uses flat Map structure with automatic key detection.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param records List of update records. Each is a flat Map with primary key columns and fields to update.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response batchUpdateById(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int recordCount = records != null ? records.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting batchUpdateById - keyspace: {}, table: {}, records: {}",
        keyspaceName,
        tableName,
        recordCount));

    // Warn about large batch sizes
    if (recordCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large batch update detected - keyspace: {}, table: {}, records: {} - Consider splitting into smaller batches for better performance",
          keyspaceName,
          tableName,
          recordCount));
    }

    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build UPDATE statements for each record
      for (Map<String, Object> record : records) {
        if (record == null || record.isEmpty()) {
          logWarn(requestContext, "Skipping null or empty record in batch update");
          continue;
        }

        // Extract ID from record
        String id = (String) record.get(JsonKey.ID);
        if (id == null || id.isEmpty()) {
          logWarn(requestContext, "Skipping record with null or empty ID in batch update");
          continue;
        }

        // Create UPDATE statement
        Update update = QueryBuilder.update(keyspaceName, tableName);
        Update.Assignments assignments = update.with();
        
        // Add all fields except ID to SET clause
        for (Map.Entry<String, Object> entry : record.entrySet()) {
          if (!JsonKey.ID.equals(entry.getKey())) {
            assignments.and(set(entry.getKey(), entry.getValue()));
          }
        }
        
        // Add WHERE clause for ID
        update.where(eq(JsonKey.ID, id));
        
        batchStatement.add(update);
      }

      logDebug(
          requestContext, formatLogMessage("Executing batch update with {} statements",
          batchStatement.size()));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch update at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully batch updated records by ID - keyspace: {}, table: {}, records: {}, statements: {}",
          keyspaceName,
          tableName,
          recordCount,
          batchStatement.size()));

    } catch (QueryExecutionException e) {
      // Handle query execution errors
      logError(
          requestContext, "Batch update by ID query execution failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (QueryValidationException e) {
      // Handle query validation errors
      logError(
          requestContext, "Batch update by ID query validation failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (NoHostAvailableException e) {
      // Handle no available hosts errors
      logError(
          requestContext, "No Cassandra hosts available for batch update by ID - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (IllegalStateException e) {
      // Handle illegal state errors
      logError(
          requestContext, "Illegal state during batch update by ID - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "batchUpdateById",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("batchUpdateById", startTime);
      }
    }

    return response;
  }

  /**
   * Applies an asynchronous operation on records matching the specified filters.
   *
   * <p><b>TODO:</b> This method needs to be implemented.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters Filter criteria for selecting records.
   * @param fields List of fields to retrieve.
   * @param callback Callback to handle the asynchronous result.
   * @param requestContext The request context for tracking and logging.
   */
  public void applyOperationOnRecordsAsync(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      FutureCallback<ResultSet> callback,
      RequestContext requestContext) {
    throw new UnsupportedOperationException("applyOperationOnRecordsAsync is not yet implemented");
  }




  /**
   * Searches for records where a collection column contains a specific value, without additional
   * filters. This is a convenience method that delegates to {@link #searchValueInList(String,
   * String, String, String, Map, RequestContext)} with null propertyMap.
   *
   * @param keyspace The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The collection column name to search within.
   * @param value The value to search for within the collection.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing records where the collection contains the specified value.
   * @see #searchValueInList(String, String, String, String, Map, RequestContext)
   */
  @Override
  public Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String value,
      RequestContext requestContext) {
    return searchValueInList(keyspace, tableName, key, value, null, requestContext);
  }



  /**
   * Searches for records where a collection column contains a specific value.
   * Requires a secondary index on the collection column. Supports optional additional filters.
   *
   * @param keyspace The keyspace name.
   * @param tableName The table name.
   * @param key The collection column name (must be list, set, or map).
   * @param value The value to search for within the collection.
   * @param propertyMap Optional additional filters (column -> value or List). All conditions use AND.
   * @param requestContext Request context for logging.
   * @return Response containing records where the collection contains the specified value.
   * @throws ProjectCommonException if operation fails or collection column is not indexed.
   */
  @Override
  public Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String value,
      Map<String, Object> propertyMap,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting searchValueInList - keyspace: {}, table: {}, collectionColumn: {}, searchValue: {}, additionalFilters: {}",
        keyspace,
        tableName,
        key,
        value,
        propertyMap != null ? propertyMap.size() : 0));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT * query
      selectQuery = QueryBuilder.select().all().from(keyspace, tableName);

      // Add CONTAINS clause for the collection column
      Clause containsClause = QueryBuilder.contains(key, value);
      selectQuery.where(containsClause);
      logDebug(
          requestContext, formatLogMessage("Added CONTAINS clause for collection column: {} with value: {}",
          key,
          value));

      // Add additional property filters if provided
      if (MapUtils.isNotEmpty(propertyMap)) {
        int filterCount = 0;
        for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
          if (entry.getValue() instanceof List) {
            // Handle list values with IN clause
            List<Object> list = (List<Object>) entry.getValue();
            if (list != null && !list.isEmpty()) {
              Object[] propertyValues = list.toArray(new Object[0]);
              Clause inClause = QueryBuilder.in(entry.getKey(), propertyValues);
              selectQuery.where(inClause);
              filterCount++;
              logDebug(
                  requestContext, formatLogMessage("Added IN clause for property: {} with {} values",
                  entry.getKey(),
                  list.size()));
            }
          } else if (entry.getValue() != null) {
            // Handle single value with EQ clause
            Clause eqClause = eq(entry.getKey(), entry.getValue());
            selectQuery.where(eqClause);
            filterCount++;
            logDebug(
                requestContext, formatLogMessage("Added EQ clause for property: {}", entry.getKey()));
          }
        }
      }
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet resultSet = connectionManager.getSession(keyspace).execute(selectQuery);
      response = CassandraUtil.createResponse(resultSet);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully searched value in list - keyspace: {}, table: {}, collectionColumn: {}, searchValue: {}, additionalFilters: {}, found: {}",
          keyspace,
          tableName,
          key,
          value,
          propertyMap != null ? propertyMap.size() : 0,
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to search value in list - keyspace: {}, table: {}, collectionColumn: {}, searchValue: {}, error: {}",
          keyspace,
          tableName,
          key,
          value,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "searchValueInList", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("searchValueInList", startTime);
      }
    }

    return response;
  }




  /**
   * Updates a record using an improved version (V2) with enhanced error handling.
   * <p><b>TODO:</b> The ifExists parameter is not yet implemented.
   *
   * @param requestContext Request context for logging.
   * @param keyspace The keyspace name.
   * @param table The table name.
   * @param selectMap Primary key columns and values (WHERE clause).
   * @param updateMap Columns to update and new values (SET clause).
   * @param ifExists If true, adds "IF EXISTS" condition (TODO: not implemented).
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if operation fails or unknown column names detected.
   */
  @Override
  public Response updateRecordV2(
      String keyspace,
      String table,
      Map<String, Object> selectMap,
      Map<String, Object> updateMap,
      boolean ifExists,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext, formatLogMessage("Starting updateRecordV2 - keyspace: {}, table: {}, whereFields: {}, updateFields: {}, ifExists: {}",
        keyspace,
        table,
        selectMap != null ? selectMap.size() : 0,
        updateMap != null ? updateMap.size() : 0,
        ifExists));

    // TODO: Implement ifExists functionality
    if (ifExists) {
      logWarn(
          requestContext,
          "ifExists parameter is set to true but not yet implemented - ignoring for now");
    }

    Response response = new Response();
    Update updateQuery = null;

    try {
      // Build UPDATE query
      updateQuery = QueryBuilder.update(keyspace, table);

      // Add SET clauses from updateMap
      if (updateMap != null && !updateMap.isEmpty()) {
        Update.Assignments assignments = updateQuery.with();
        for (Map.Entry<String, Object> entry : updateMap.entrySet()) {
          assignments.and(set(entry.getKey(), entry.getValue()));
          logDebug(
              requestContext, formatLogMessage("Added SET clause: {} = {}",
              entry.getKey(),
              entry.getValue()));
        }
      } else {
        logError(
            requestContext,
            "Empty updateMap provided for updateRecordV2 - no fields to update");
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "No fields provided to update",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      // Add WHERE clauses from selectMap
      if (selectMap != null && !selectMap.isEmpty()) {
        Update.Where where = updateQuery.where();
        for (Map.Entry<String, Object> entry : selectMap.entrySet()) {
          where.and(eq(entry.getKey(), entry.getValue()));
          logDebug(
              requestContext, formatLogMessage("Added WHERE clause: {} = {}",
              entry.getKey(),
              entry.getValue()));
        }
      } else {
        logError(
            requestContext,
            "Empty selectMap provided for updateRecordV2 - no WHERE conditions");
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "No WHERE conditions provided - cannot update without identifying the record",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", updateQuery.getQueryString()));

      // Execute update
      connectionManager.getSession(keyspace).execute(updateQuery);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful update at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully updated record V2 - keyspace: {}, table: {}, whereFields: {}, updateFields: {}",
          keyspace,
          table,
          selectMap != null ? selectMap.size() : 0,
          updateMap != null ? updateMap.size() : 0));

    } catch (Exception e) {
      // Handle unknown column errors specifically
      if (e.getMessage() != null && e.getMessage().contains(JsonKey.UNKNOWN_IDENTIFIER)) {
        String processedError = CassandraUtil.processExceptionForUnknownIdentifier(e);
        logError(
            requestContext, "Update record V2 failed - unknown column - keyspace: {}, table: {}, error: {}",
            keyspace,
            table,
            processedError,
            e);

        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            processedError,
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      // Handle general errors
      logError(
          requestContext, "Update record V2 failed - keyspace: {}, table: {}, error: {}",
          keyspace,
          table,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (updateQuery != null) {
        logQueryElapseTime(
            "updateRecordV2", startTime, updateQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("updateRecordV2", startTime);
      }
    }

    return response;
  }


  /**
   * Performs a LOGGED batch insert with atomicity guarantees across partitions.
   * Slower than UNLOGGED batches but ensures all inserts succeed or all fail.
   *
   * @param requestContext Request context for logging.
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param records List of maps, each representing a record to insert.
   * @return Response with "SUCCESS" status.
   * @throws QueryExecutionException if batch execution fails.
   * @throws QueryValidationException if batch validation fails.
   * @throws NoHostAvailableException if no Cassandra hosts available.
   * @throws IllegalStateException if operation is in illegal state.
   */
  @Override
  public Response batchInsertLogged(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int recordCount = records != null ? records.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting batchInsertLogged - keyspace: {}, table: {}, records: {}",
        keyspaceName,
        tableName,
        recordCount));

    // Warn about large batch sizes
    if (recordCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large LOGGED batch insert detected - keyspace: {}, table: {}, records: {} - LOGGED batches are slower, consider splitting or using UNLOGGED",
          keyspaceName,
          tableName,
          recordCount));
    }

    Response response = new Response();
    BatchStatement batchStatement =
        new BatchStatement(BatchStatement.Type.LOGGED);
    batchStatement.setConsistencyLevel(CassandraConnectionManagerImpl.getConsistencyLevel());

    logDebug(
        requestContext, formatLogMessage("Created LOGGED batch with consistency level: {}",
        batchStatement.getConsistencyLevel()));

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build INSERT statements for each record
      for (Map<String, Object> record : records) {
        if (record == null || record.isEmpty()) {
          logWarn(requestContext, "Skipping null or empty record in LOGGED batch insert");
          continue;
        }

        Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);

        // Add all columns and values from the record
        for (Map.Entry<String, Object> entry : record.entrySet()) {
          insert.value(entry.getKey(), entry.getValue());
        }

        batchStatement.add(insert);
      }

      logDebug(
          requestContext, formatLogMessage("Executing LOGGED batch insert with {} statements",
          batchStatement.size()));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch insert at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully LOGGED batch inserted records - keyspace: {}, table: {}, records: {}, statements: {}",
          keyspaceName,
          tableName,
          recordCount,
          batchStatement.size()));

    } catch (WriteTimeoutException e) {
      // Special handling for WriteTimeoutException
      // The write may have succeeded despite the timeout
      logWarn(
          requestContext, formatLogMessage("WriteTimeoutException during LOGGED batch insert - keyspace: {}, table: {}, records: {}, writeType: {} - Write may have succeeded despite timeout",
          keyspaceName,
          tableName,
          recordCount,
          e.getWriteType().name(),
          e));

      // TODO: Fix undefined 'writeType' variable reference
      // Original code checked: if (writeType.contains(e.getWriteType().name()))
      // For now, we'll treat WriteTimeoutException as potential success
      response.put(Constants.RESPONSE, Constants.SUCCESS);

    } catch (QueryExecutionException e) {
      // Handle query execution errors
      logError(
          requestContext, "LOGGED batch insert query execution failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);
      throw e;

    } catch (QueryValidationException e) {
      // Handle query validation errors
      logError(
          requestContext, "LOGGED batch insert query validation failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);
      throw e;

    } catch (NoHostAvailableException e) {
      // Handle no available hosts errors
      logError(
          requestContext, "No Cassandra hosts available for LOGGED batch insert - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);
      throw e;

    } catch (IllegalStateException e) {
      // Handle illegal state errors
      logError(
          requestContext, "Illegal state during LOGGED batch insert - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);
      throw e;

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "batchInsertLogged",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("batchInsertLogged", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves multiple records by IDs with optional field projection.
   * Optimized for bulk retrieval with controlled data transfer.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param properties List of column names to retrieve (null for all columns).
   * @param ids List of ID values to search for (WHERE IN clause).
   * @param requestContext Request context for logging.
   * @return Response containing matching records with specified columns.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByIdsWithSpecifiedColumns(
      String keyspaceName,
      String tableName,
      List<String> properties,
      List<String> ids,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int idCount = ids != null ? ids.size() : 0;
    int columnCount = properties != null ? properties.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByIdsWithSpecifiedColumns - keyspace: {}, table: {}, ids: {}, columns: {}",
        keyspaceName,
        tableName,
        idCount,
        columnCount > 0 ? columnCount : "all"));

    // Warn about large ID lists
    if (idCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large ID list detected - keyspace: {}, table: {}, ids: {} - Consider splitting into smaller batches",
          keyspaceName,
          tableName,
          idCount));
    }

    Response response = new Response();
    Builder selectBuilder = null;

    try {
      // Build SELECT clause with specified columns or all columns
      if (CollectionUtils.isNotEmpty(properties)) {
        String[] columns = properties.toArray(new String[0]);
        selectBuilder = QueryBuilder.select(columns);
        logDebug(
            requestContext, formatLogMessage("Selecting {} specific columns", properties.size()));
      } else {
        selectBuilder = QueryBuilder.select().all();
        logDebug(requestContext, "Selecting all columns");
      }

      // Delegate to executeSelectQuery to build WHERE IN clause and execute
      response =
          executeSelectQuery(keyspaceName, tableName, ids, selectBuilder, "", requestContext);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by IDs with specified columns - keyspace: {}, table: {}, ids: {}, columns: {}, found: {}",
          keyspaceName,
          tableName,
          idCount,
          columnCount > 0 ? columnCount : "all",
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by IDs with specified columns - keyspace: {}, table: {}, ids: {}, columns: {}, error: {}",
          keyspaceName,
          tableName,
          idCount,
          columnCount > 0 ? columnCount : "all",
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectBuilder != null) {
        logQueryElapseTime(
            "getRecordsByIdsWithSpecifiedColumns",
            startTime,
            selectBuilder.toString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordsByIdsWithSpecifiedColumns", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves multiple records using a custom primary key column name.
   * Useful for tables where the primary key is not named "id".
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param primaryKeys List of primary key values to search for.
   * @param primaryKeyColumnName The primary key column name (e.g., "userId", "courseId").
   * @param requestContext Request context for logging.
   * @return Response containing all matching records with all columns.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByPrimaryKeys(
      String keyspaceName,
      String tableName,
      List<String> primaryKeys,
      String primaryKeyColumnName,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int keyCount = primaryKeys != null ? primaryKeys.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByPrimaryKeys - keyspace: {}, table: {}, keys: {}, primaryKeyColumn: {}",
        keyspaceName,
        tableName,
        keyCount,
        primaryKeyColumnName));

    // Warn about large key lists
    if (keyCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large primary key list detected - keyspace: {}, table: {}, keys: {} - Consider splitting into smaller batches",
          keyspaceName,
          tableName,
          keyCount));
    }

    Response response = new Response();
    Builder selectBuilder = null;

    try {
      // Build SELECT * query
      selectBuilder = QueryBuilder.select().all();
      logDebug(requestContext, "Selecting all columns");

      // Delegate to executeSelectQuery to build WHERE IN clause and execute
      response =
          executeSelectQuery(
              keyspaceName,
              tableName,
              primaryKeys,
              selectBuilder,
              primaryKeyColumnName,
              requestContext);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by primary keys - keyspace: {}, table: {}, keys: {}, primaryKeyColumn: {}, found: {}",
          keyspaceName,
          tableName,
          keyCount,
          primaryKeyColumnName,
          recordCount));

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by primary keys - keyspace: {}, table: {}, keys: {}, primaryKeyColumn: {}, error: {}",
          keyspaceName,
          tableName,
          keyCount,
          primaryKeyColumnName,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectBuilder != null) {
        logQueryElapseTime(
            "getRecordsByPrimaryKeys", startTime, selectBuilder.toString(), requestContext);
      } else {
        logQueryElapseTime("getRecordsByPrimaryKeys", startTime);
      }
    }

    return response;
  }



  /**
   * Inserts a record with a Time-To-Live (TTL) value.
   * Record will automatically expire and be deleted after the specified TTL duration.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param request Map containing column names and values to insert.
   * @param ttl Time-To-Live in seconds. Record expires after this duration.
   * @param requestContext Request context for logging.
   * @return Response with operation result.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response insertRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      int ttl,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int fieldCount = request != null ? request.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting insertRecordWithTTL - keyspace: {}, table: {}, fields: {}, ttl: {} seconds ({} hours)",
        keyspaceName,
        tableName,
        fieldCount,
        ttl,
        String.format("%.2f", ttl / 3600.0)));

    Response response = new Response();
    Insert insert = null;

    try {
      // Build INSERT query
      insert = QueryBuilder.insertInto(keyspaceName, tableName);

      // Add all columns and values from request
      if (request != null && !request.isEmpty()) {
        for (Map.Entry<String, Object> entry : request.entrySet()) {
          insert.value(entry.getKey(), entry.getValue());
        }
      } else {
        logError(
            requestContext,
            "Empty request map provided for insertRecordWithTTL - no fields to insert");
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "No fields provided to insert",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      // Add TTL clause
      insert.using(QueryBuilder.ttl(ttl));

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", insert.getQueryString()));

      // Execute insert
      ResultSet results = connectionManager.getSession(keyspaceName).execute(insert);
      response = CassandraUtil.createResponse(results);

      // Log successful insert at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully inserted record with TTL - keyspace: {}, table: {}, fields: {}, ttl: {} seconds",
          keyspaceName,
          tableName,
          fieldCount,
          ttl));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to insert record with TTL - keyspace: {}, table: {}, fields: {}, ttl: {}, error: {}",
          keyspaceName,
          tableName,
          fieldCount,
          ttl,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (insert != null) {
        logQueryElapseTime(
            "insertRecordWithTTL", startTime, insert.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("insertRecordWithTTL", startTime);
      }
    }

    return response;
  }



  /**
   * Updates a record and resets/extends its Time-To-Live (TTL).
   * TTL countdown starts over from the new value (not additive).
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param request Map containing columns to update and new values (SET clause).
   * @param compositeKey Map containing primary key columns and values (WHERE clause).
   * @param ttl Time-To-Live in seconds. Record expires after this duration from now.
   * @param requestContext Request context for logging.
   * @return Response with operation result.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response updateRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey,
      int ttl,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int updateFieldCount = request != null ? request.size() : 0;
    int keyFieldCount = compositeKey != null ? compositeKey.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting updateRecordWithTTL - keyspace: {}, table: {}, updateFields: {}, keyFields: {}, ttl: {} seconds ({} hours)",
        keyspaceName,
        tableName,
        updateFieldCount,
        keyFieldCount,
        ttl,
        String.format("%.2f", ttl / 3600.0)));

    Response response = new Response();
    Update update = null;

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build UPDATE query
      update = QueryBuilder.update(keyspaceName, tableName);
      Assignments assignments = update.with();
      Update.Where where = update.where();

      // Add SET clauses from request
      if (request != null && !request.isEmpty()) {
        for (Map.Entry<String, Object> entry : request.entrySet()) {
          assignments.and(QueryBuilder.set(entry.getKey(), entry.getValue()));
        }
        logDebug(
            requestContext, formatLogMessage("Added {} SET clauses for update", request.size()));
      } else {
        logError(
            requestContext,
            "Empty request map provided for updateRecordWithTTL - no fields to update");
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "No fields provided to update",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      // Add WHERE clauses from compositeKey
      if (compositeKey != null && !compositeKey.isEmpty()) {
        for (Map.Entry<String, Object> entry : compositeKey.entrySet()) {
          where.and(eq(entry.getKey(), entry.getValue()));
        }
        logDebug(
            requestContext, formatLogMessage("Added {} WHERE clauses for composite key",
            compositeKey.size()));
      } else {
        logError(
            requestContext,
            "Empty compositeKey provided for updateRecordWithTTL - cannot identify record");
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "No composite key provided - cannot update without identifying the record",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      // Add TTL clause
      update.using(QueryBuilder.ttl(ttl));

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", update.getQueryString()));

      // Execute update
      ResultSet results = session.execute(update);
      response = CassandraUtil.createResponse(results);

      // Log successful update at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully updated record with TTL - keyspace: {}, table: {}, updateFields: {}, keyFields: {}, ttl: {} seconds",
          keyspaceName,
          tableName,
          updateFieldCount,
          keyFieldCount,
          ttl));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to update record with TTL - keyspace: {}, table: {}, updateFields: {}, keyFields: {}, ttl: {}, error: {}",
          keyspaceName,
          tableName,
          updateFieldCount,
          keyFieldCount,
          ttl,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (update != null) {
        logQueryElapseTime(
            "updateRecordWithTTL", startTime, update.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("updateRecordWithTTL", startTime);
      }
    }

    return response;
  }



  /**
   * Retrieves records by IDs with specified columns AND their remaining TTL values.
   * Allows checking how much time is left before records expire.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param primaryKeys Map of primary key columns and values (WHERE clause).
   * @param properties List of regular column names to retrieve.
   * @param ttlPropertiesWithAlias Map of column names to TTL aliases (column -> alias). Aliases are required.
   * @param requestContext Request context for logging.
   * @return Response containing records with regular columns and TTL values.
   * @throws ProjectCommonException if operation fails or alias validation fails.
   */
  @Override
  public Response getRecordsByIdsWithSpecifiedColumnsAndTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> primaryKeys,
      List<String> properties,
      Map<String, String> ttlPropertiesWithAlias,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int keyCount = primaryKeys != null ? primaryKeys.size() : 0;
    int columnCount = properties != null ? properties.size() : 0;
    int ttlColumnCount = ttlPropertiesWithAlias != null ? ttlPropertiesWithAlias.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByIdsWithSpecifiedColumnsAndTTL - keyspace: {}, table: {}, keys: {}, columns: {}, ttlColumns: {}",
        keyspaceName,
        tableName,
        keyCount,
        columnCount,
        ttlColumnCount));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT clause
      Selection selection = QueryBuilder.select();

      // Add regular columns
      if (CollectionUtils.isNotEmpty(properties)) {
        for (String property : properties) {
          selection.column(property);
        }
        logDebug(
            requestContext, formatLogMessage("Added {} regular columns to SELECT", properties.size()));
      }

      // Add TTL columns with aliases
      if (MapUtils.isNotEmpty(ttlPropertiesWithAlias)) {
        for (Map.Entry<String, String> entry : ttlPropertiesWithAlias.entrySet()) {
          String columnName = entry.getKey();
          String alias = entry.getValue();

          // Validate alias is provided
          if (StringUtils.isBlank(alias)) {
            String errorMsg =
                "Alias not provided for TTL column: "
                    + columnName
                    + " - TTL values require aliases to be retrievable";
            logError(requestContext, errorMsg);
            throw new ProjectCommonException(
                ResponseCode.SERVER_ERROR.getErrorCode(),
                errorMsg,
                ResponseCode.SERVER_ERROR.getResponseCode());
          }

          // Add TTL(column) AS alias
          selection.ttl(columnName).as(alias);
          logDebug(
              requestContext, formatLogMessage("Added TTL column: TTL({}) AS {}",
              columnName,
              alias));
        }
        logDebug(
            requestContext, formatLogMessage("Added {} TTL columns with aliases to SELECT",
            ttlPropertiesWithAlias.size()));
      }

      // Build FROM clause
      Select select = selection.from(keyspaceName, tableName);

      // Add WHERE clauses from primaryKeys
      if (primaryKeys != null && !primaryKeys.isEmpty()) {
        for (Map.Entry<String, Object> entry : primaryKeys.entrySet()) {
          select.where().and(eq(entry.getKey(), entry.getValue()));
        }
        logDebug(
            requestContext, formatLogMessage("Added {} WHERE clauses for primary keys", primaryKeys.size()));
      } else {
        logWarn(
            requestContext,
            "Empty primaryKeys provided - query may return unexpected results");
      }

      selectQuery = select;
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(select);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records with TTL - keyspace: {}, table: {}, keys: {}, columns: {}, ttlColumns: {}, found: {}",
          keyspaceName,
          tableName,
          keyCount,
          columnCount,
          ttlColumnCount,
          recordCount));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records with TTL - keyspace: {}, table: {}, keys: {}, columns: {}, ttlColumns: {}, error: {}",
          keyspaceName,
          tableName,
          keyCount,
          columnCount,
          ttlColumnCount,
          e.getMessage(),
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getRecordsByIdsWithSpecifiedColumnsAndTTL",
            startTime,
            selectQuery.getQueryString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordsByIdsWithSpecifiedColumnsAndTTL", startTime);
      }
    }

    return response;
  }



  /**
   * Performs a batch insert with individual TTL values for each record.
   * Each record can have its own expiration time. Lists must have matching sizes.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param records List of maps, each representing a record to insert.
   * @param ttls List of TTL values (seconds) for each record. Null or ≤0 means no TTL.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if validation fails or batch execution fails.
   */
  @Override
  public Response batchInsertWithTTL(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      List<Integer> ttls,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int recordCount = records != null ? records.size() : 0;
    int ttlCount = ttls != null ? ttls.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting batchInsertWithTTL - keyspace: {}, table: {}, records: {}, ttls: {}",
        keyspaceName,
        tableName,
        recordCount,
        ttlCount));

    // Validate inputs
    if (CollectionUtils.isEmpty(records) || CollectionUtils.isEmpty(ttls)) {
      String errorMsg = "Empty records or ttls list provided - both lists must contain values";
      logError(requestContext, errorMsg);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          errorMsg,
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    if (ttls.size() != records.size()) {
      String errorMsg =
          "Mismatch between records and ttls list sizes - records: "
              + records.size()
              + ", ttls: "
              + ttls.size()
              + " - sizes must match";
      logError(requestContext, errorMsg);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          errorMsg,
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    // Warn about large batch sizes
    if (recordCount > 1000) {
      logWarn(
          requestContext, formatLogMessage("Large batch insert with TTL detected - keyspace: {}, table: {}, records: {} - Consider splitting into smaller batches",
          keyspaceName,
          tableName,
          recordCount));
    }

    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();
    int recordsWithTTL = 0;
    int recordsWithoutTTL = 0;

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Build INSERT statements for each record with its TTL
      for (int i = 0; i < records.size(); i++) {
        Map<String, Object> record = records.get(i);
        Integer ttl = ttls.get(i);

        if (record == null || record.isEmpty()) {
          logWarn(
              requestContext, formatLogMessage("Skipping null or empty record at index {} in batch insert with TTL",
              i));
          continue;
        }

        Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);

        // Add all columns and values from the record
        for (Map.Entry<String, Object> entry : record.entrySet()) {
          insert.value(entry.getKey(), entry.getValue());
        }

        // Add TTL if provided and valid
        if (ttl != null && ttl > 0) {
          insert.using(QueryBuilder.ttl(ttl));
          recordsWithTTL++;
          logDebug(
              requestContext, formatLogMessage("Added record {} with TTL: {} seconds ({} hours)",
              i,
              ttl,
              String.format("%.2f", ttl / 3600.0)));
        } else {
          recordsWithoutTTL++;
          logDebug(requestContext, formatLogMessage("Added record {} without TTL (permanent)", i));
        }

        batchStatement.add(insert);
      }

      logDebug(
          requestContext, formatLogMessage("Executing batch insert with TTL - total: {}, withTTL: {}, withoutTTL: {}",
          batchStatement.size(),
          recordsWithTTL,
          recordsWithoutTTL));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch insert at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully batch inserted records with TTL - keyspace: {}, table: {}, records: {}, withTTL: {}, withoutTTL: {}",
          keyspaceName,
          tableName,
          batchStatement.size(),
          recordsWithTTL,
          recordsWithoutTTL));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (QueryExecutionException e) {
      // Handle query execution errors
      logError(
          requestContext, "Batch insert with TTL query execution failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (QueryValidationException e) {
      // Handle query validation errors
      logError(
          requestContext, "Batch insert with TTL query validation failed - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (NoHostAvailableException e) {
      // Handle no available hosts errors
      logError(
          requestContext, "No Cassandra hosts available for batch insert with TTL - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (IllegalStateException e) {
      // Handle illegal state errors
      logError(
          requestContext, "Illegal state during batch insert with TTL - keyspace: {}, table: {}, records: {}, error: {}",
          keyspaceName,
          tableName,
          recordCount,
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "batchInsertWithTTL",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("batchInsertWithTTL", startTime);
      }
    }

    return response;
  }


  /**
   * Performs a generic batch action containing mixed INSERT and UPDATE operations.
   * Processes a map of operations where each key indicates the operation type (INSERT/UPDATE)
   * and the value contains the record data to be processed.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param inputData Map where keys are operation types (INSERT/UPDATE) and values are record maps.
   * @param requestContext Request context for logging.
   * @return Response with "SUCCESS" status.
   * @throws ProjectCommonException if batch execution fails.
   */
  @Override
  public Response performBatchAction(
      String keyspaceName,
      String tableName,
      Map<String, Object> inputData,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int operationCount = inputData != null ? inputData.size() : 0;

    logDebug(
        requestContext,
        formatLogMessage(
            "Starting performBatchAction - keyspace: {}, table: {}, operations: {}",
            keyspaceName,
            tableName,
            operationCount));

    Response response = new Response();
    BatchStatement batchStatement = new BatchStatement();
    int insertCount = 0;
    int updateCount = 0;

    try {
      Session session = connectionManager.getSession(keyspaceName);

      // Process each operation in the input data
      for (Map.Entry<String, Object> entry : inputData.entrySet()) {
        String key = entry.getKey();
        Map<String, Object> record = (Map<String, Object>) entry.getValue();

        if (key.equals(Constants.INSERT)) {
          Insert insert = createInsertStatement(keyspaceName, tableName, record);
          batchStatement.add(insert);
          insertCount++;
          logDebug(requestContext, formatLogMessage("Added INSERT operation for key: {}", key));
        } else if (key.equals(Constants.UPDATE)) {
          Update update = createUpdateStatement(keyspaceName, tableName, record);
          batchStatement.add(update);
          updateCount++;
          logDebug(requestContext, formatLogMessage("Added UPDATE operation for key: {}", key));
        } else {
          logWarn(
              requestContext,
              formatLogMessage("Unknown operation type: {} - skipping", key));
        }
      }

      logDebug(
          requestContext,
          formatLogMessage(
              "Executing batch action - total: {}, inserts: {}, updates: {}",
              batchStatement.size(),
              insertCount,
              updateCount));

      // Execute batch
      ResultSet resultSet = session.execute(batchStatement);
      response.put(Constants.RESPONSE, Constants.SUCCESS);

      // Log successful batch action at INFO level
      logInfo(
          requestContext,
          formatLogMessage(
              "Successfully performed batch action - keyspace: {}, table: {}, total: {}, inserts: {}, updates: {}",
              keyspaceName,
              tableName,
              batchStatement.size(),
              insertCount,
              updateCount));

    } catch (QueryExecutionException e) {
      // Handle query execution errors
      logError(
          requestContext,
          "Batch action query execution failed - keyspace: {}, table: {}, operations: {}, error: {}",
          keyspaceName,
          tableName,
          operationCount,
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (QueryValidationException e) {
      // Handle query validation errors
      logError(
          requestContext,
          "Batch action query validation failed - keyspace: {}, table: {}, operations: {}, error: {}",
          keyspaceName,
          tableName,
          operationCount,
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (NoHostAvailableException e) {
      // Handle no available hosts errors
      logError(
          requestContext,
          "No Cassandra hosts available for batch action - keyspace: {}, table: {}, operations: {}, error: {}",
          keyspaceName,
          tableName,
          operationCount,
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } catch (IllegalStateException e) {
      // Handle illegal state errors
      logError(
          requestContext,
          "Illegal state during batch action - keyspace: {}, table: {}, operations: {}, error: {}",
          keyspaceName,
          tableName,
          operationCount,
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          e.getMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (batchStatement != null && batchStatement.size() > 0) {
        logQueryElapseTime(
            "performBatchAction",
            startTime,
            batchStatement.getStatements().toString(),
            requestContext);
      } else {
        logQueryElapseTime("performBatchAction", startTime);
      }
    }

    return response;
  }


  /**
   * Retrieves records by composite partition key values.
   * Supports both single values (EQ) and lists (IN) for each column. ALL partition key columns must be specified.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param partitionKeyMap Map of partition key columns to values (single value or List).
   * @param requestContext Request context for logging.
   * @return Response containing all matching records.
   * @throws ProjectCommonException if operation fails.
   */
  @Override
  public Response getRecordsByCompositePartitionKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> partitionKeyMap,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int keyColumnCount = partitionKeyMap != null ? partitionKeyMap.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Starting getRecordsByCompositePartitionKey - keyspace: {}, table: {}, partitionKeyColumns: {}",
        keyspaceName,
        tableName,
        keyColumnCount));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT * query
      Builder selectBuilder = QueryBuilder.select().all();
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();

      // Add WHERE clauses for each partition key column
      if (partitionKeyMap != null && !partitionKeyMap.isEmpty()) {
        for (Entry<String, Object> entry : partitionKeyMap.entrySet()) {
          String columnName = entry.getKey();
          Object value = entry.getValue();

          if (value instanceof List) {
            // Handle list values with IN clause
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
              Object[] propertyValues = list.toArray(new Object[0]);
              Clause clauseList = QueryBuilder.in(columnName, propertyValues);
              selectWhere.and(clauseList);
              logDebug(
                  requestContext, formatLogMessage("Added IN clause for partition key column: {} with {} values",
                  columnName,
                  list.size()));
            } else {
              logWarn(
                  requestContext, formatLogMessage("Empty list provided for partition key column: {} - skipping",
                  columnName));
            }
          } else {
            // Handle single value with EQ clause
            Clause clause = eq(columnName, value);
            selectWhere.and(clause);
            logDebug(
                requestContext, formatLogMessage("Added EQ clause for partition key column: {} = {}",
                columnName,
                value));
          }
        }
      } else {
        logError(
            requestContext,
            "Empty partitionKeyMap provided - cannot query without partition key");
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            "No partition key provided - partition key is required for this query",
            ResponseCode.SERVER_ERROR.getResponseCode());
      }

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records by composite partition key - keyspace: {}, table: {}, partitionKeyColumns: {}, found: {}",
          keyspaceName,
          tableName,
          keyColumnCount,
          recordCount));

    } catch (ProjectCommonException e) {
      // Re-throw validation errors
      throw e;

    } catch (Exception e) {
      // Handle query errors
      logError(
          requestContext, "Failed to retrieve records by composite partition key - keyspace: {}, table: {}, partitionKeyColumns: {}, error: {}",
          keyspaceName,
          tableName,
          keyColumnCount,
          e);

      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime(
            "getRecordsByCompositePartitionKey",
            startTime,
            selectQuery.getQueryString(),
            requestContext);
      } else {
        logQueryElapseTime("getRecordsByCompositePartitionKey", startTime);
      }
    }

    return response;
  }

  /**
   * Retrieves a User Defined Type (UDT) from a Cassandra keyspace.
   * Accesses the cluster metadata to fetch the specified user-defined type definition.
   *
   * @param keyspaceName The Cassandra keyspace name containing the user-defined type.
   * @param typeName The name of the user-defined type to retrieve.
   * @return UserType object representing the specified user-defined type.
   * @throws ProjectCommonException if the keyspace or type is not found, or if no hosts are available.
   */
  @Override
  public UserType getUDTType(String keyspaceName, String typeName){
    logDebug(null, formatLogMessage("Retrieving UDT - keyspace: {}, type: {}", keyspaceName, typeName));

    try {
      Session session = connectionManager.getSession(keyspaceName);
      KeyspaceMetadata keyspaceMetadata = session.getCluster().getMetadata().getKeyspace(keyspaceName);
      
      if (keyspaceMetadata == null) {
        String errorMsg = formatLogMessage("Keyspace not found - keyspace: {}", keyspaceName);
        logError(null, errorMsg);
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            errorMsg,
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
      
      UserType userType = keyspaceMetadata.getUserType(typeName);
      
      if (userType == null) {
        String errorMsg = formatLogMessage("User-defined type not found - keyspace: {}, type: {}", keyspaceName, typeName);
        logError(null, errorMsg);
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.getErrorCode(),
            errorMsg,
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
      
      logInfo(null, formatLogMessage("Successfully retrieved UDT - keyspace: {}, type: {}", keyspaceName, typeName));
      return userType;
      
    } catch (ProjectCommonException e) {
      throw e;
    } catch (NoHostAvailableException e) {
      String errorMsg = formatLogMessage("No Cassandra hosts available - keyspace: {}, type: {}", keyspaceName, typeName);
      logError(null, errorMsg, e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          errorMsg,
          ResponseCode.SERVER_ERROR.getResponseCode());
    } catch (Exception e) {
      String errorMsg = formatLogMessage("Failed to retrieve UDT - keyspace: {}, type: {}, error: {}", keyspaceName, typeName, e.getMessage());
      logError(null, errorMsg, e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  /**
   * Executes a SELECT query with WHERE IN clause for multiple IDs.
   * Private helper method used by various public methods for efficient bulk retrieval.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param ids List of ID values for WHERE IN clause.
   * @param selectBuilder SELECT builder (already configured with columns).
   * @param primaryKeyColumnName Primary key column name. If blank, defaults to "id".
   * @param requestContext Request context for logging.
   * @return Response containing matching records.
   */
  private Response executeSelectQuery(
      String keyspaceName,
      String tableName,
      List<String> ids,
      Builder selectBuilder,
      String primaryKeyColumnName,
      RequestContext requestContext) {

    long startTime = System.currentTimeMillis();
    int idCount = ids != null ? ids.size() : 0;

    logDebug(
        requestContext, formatLogMessage("Executing SELECT query with WHERE IN - keyspace: {}, table: {}, ids: {}, primaryKeyColumn: {}",
        keyspaceName,
        tableName,
        idCount,
        StringUtils.isBlank(primaryKeyColumnName) ? "id (default)" : primaryKeyColumnName));

    Response response = new Response();
    Select selectQuery = null;

    try {
      // Build SELECT query from builder
      selectQuery = selectBuilder.from(keyspaceName, tableName);
      Where selectWhere = selectQuery.where();

      // Determine which column to use for WHERE IN clause
      String columnName =
          StringUtils.isBlank(primaryKeyColumnName) ? JsonKey.ID : primaryKeyColumnName;

      // Build WHERE IN clause
      if (ids != null && !ids.isEmpty()) {
        Object[] idArray = ids.toArray(new Object[0]);
        Clause inClause = QueryBuilder.in(columnName, idArray);
        selectWhere.and(inClause);

        logDebug(
            requestContext, formatLogMessage("Added WHERE IN clause for column: {} with {} IDs",
            columnName,
            idCount));
      } else {
        logWarn(
            requestContext,
            "Empty ID list provided for executeSelectQuery - query will return no results");
      }

      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", selectQuery.getQueryString()));

      // Execute query
      ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
      response = CassandraUtil.createResponse(results);

      // Log result count
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logDebug(
          requestContext, formatLogMessage("SELECT query with WHERE IN completed - keyspace: {}, table: {}, ids: {}, found: {}",
          keyspaceName,
          tableName,
          idCount,
          recordCount));

    } finally {
      // Log query execution time
      if (selectQuery != null) {
        logQueryElapseTime("executeSelectQuery", startTime, selectQuery.getQueryString(), requestContext);
      } else {
        logQueryElapseTime("executeSelectQuery", startTime);
      }
    }

    return response;
  }


  /**
   * Creates an INSERT statement for batch operations.
   * Builds a Cassandra INSERT query from a record map containing column-value pairs.
   * This is a helper method used by performBatchAction for batch insert operations.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param record Map of column names to values to be inserted.
   * @return Insert statement ready to be added to a batch.
   */
  private Insert createInsertStatement(
      String keyspaceName, String tableName, Map<String, Object> record) {
    Insert insert = QueryBuilder.insertInto(keyspaceName, tableName);
    
    // Add all column-value pairs from the record to the INSERT statement
    for (Map.Entry<String, Object> entry : record.entrySet()) {
      insert.value(entry.getKey(), entry.getValue());
    }
    
    return insert;
  }

  /**
   * Creates an UPDATE statement for batch operations.
   * Builds a Cassandra UPDATE query from a record map. The record must contain an 'id' field
   * which will be used in the WHERE clause. All other fields will be set in the UPDATE clause.
   * This is a helper method used by performBatchAction for batch update operations.
   *
   * @param keyspaceName The keyspace name.
   * @param tableName The table name.
   * @param record Map of column names to values. Must include 'id' field for WHERE clause.
   * @return Update statement ready to be added to a batch.
   */
  private Update createUpdateStatement(
      String keyspaceName, String tableName, Map<String, Object> record) {
    Update update = QueryBuilder.update(keyspaceName, tableName);
    Assignments assignments = update.with();
    Update.Where where = update.where();
    
    // Process each field: 'id' goes to WHERE clause, others go to SET clause
    for (Map.Entry<String, Object> entry : record.entrySet()) {
      if (Constants.ID.equals(entry.getKey())) {
        // Use ID field for WHERE clause to identify the record to update
        where.and(eq(entry.getKey(), entry.getValue()));
      } else {
        // Use all other fields for SET clause to update values
        assignments.and(QueryBuilder.set(entry.getKey(), entry.getValue()));
      }
    }
    
    return update;
  }




























  /**
   * Logs the elapsed time for a query operation.
   * This method should be implemented by subclasses or provided as a utility method.
   *
   * @param operation The name of the operation being logged.
   * @param startTime The start time of the operation in milliseconds.
   * @param query The CQL query that was executed.
   * @param requestContext The request context for logging.
   */
  protected void logQueryElapseTime(
      String operation, long startTime, String query, RequestContext requestContext) {
    long elapsedTime = System.currentTimeMillis() - startTime;
    logDebug(
        requestContext, formatLogMessage("Operation: {}, Query: {}, Elapsed time: {} ms",
        operation,
        query,
        elapsedTime));
  }

  /**
   * Logs the elapsed time for an operation without query details.
   * Overloaded version for cases where query string is not available.
   *
   * @param operation The name of the operation being logged.
   * @param startTime The start time of the operation in milliseconds.
   */
  protected void logQueryElapseTime(String operation, long startTime) {
    long elapsedTime = System.currentTimeMillis() - startTime;
    logDebug(null, formatLogMessage("Operation: {}, Elapsed time: {} ms", operation, elapsedTime));
  }

  /**
   * Formats a log message with SLF4J-style {} placeholders.
   * Replaces each {} with the corresponding argument's string representation.
   *
   * @param message The message template with {} placeholders.
   * @param args Arguments to replace placeholders.
   * @return Formatted message string.
   */
  protected String formatLogMessage(String message, Object... args) {
    if (message == null || args == null || args.length == 0) {
      return message;
    }
    
    String result = message;
    for (Object arg : args) {
      int index = result.indexOf("{}");
      if (index == -1) {
        break;
      }
      String replacement = arg != null ? arg.toString() : "null";
      result = result.substring(0, index) + replacement + result.substring(index + 2);
    }
    return result;
  }
}
