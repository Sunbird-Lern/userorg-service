package org.sunbird.cassandraimpl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.Response;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ResponseCode;

/**
 * Extended Cassandra Data Access Component (DAC) implementation.
 * Provides additional specialized operations beyond the base CassandraOperationImpl,
 * including advanced filtering, async operations, and collection manipulation.
 *
 * <p>This class extends CassandraOperationImpl and adds methods for:
 * <ul>
 *   <li>Filtered record retrieval with field selection
 *   <li>Asynchronous operations with callbacks
 *   <li>Map column operations (add/remove key-value pairs)
 *   <li>Set column operations (add/remove values)
 *   <li>Limited result queries
 * </ul>
 */
public class CassandraDACImpl extends CassandraOperationImpl {

  /**
   * Retrieves records from a Cassandra table based on filter criteria with specified fields.
   * Overrides the base implementation to provide consistent error handling and logging.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters A map of column names to their filter values (supports both single values and Lists for IN clauses).
   * @param fields A list of field names to retrieve (null or empty for all fields).
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   * @throws ProjectCommonException if the operation fails.
   */
  @Override
  public Response getRecords(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Response response = new Response();
    Session session = connectionManager.getSession(keySpace);
    Select select = null;

    try {
      // Build SELECT clause with specified fields or all fields
      if (CollectionUtils.isNotEmpty(fields)) {
        select = QueryBuilder.select(fields.toArray(new String[0])).from(keySpace, table);
      } else {
        select = QueryBuilder.select().all().from(keySpace, table);
      }

      // Build WHERE clause with filters
      if (MapUtils.isNotEmpty(filters)) {
        Select.Where where = select.where();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
          Object value = filter.getValue();
          if (value instanceof List) {
            where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
          } else {
            where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
          }
        }
      }

      // Log and execute query
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", select.getQueryString()));
      ResultSet results = session.execute(select);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records - keyspace: {}, table: {}, filters: {}, fields: {}, count: {}",
          keySpace,
          table,
          filters != null ? filters.size() : 0,
          fields != null ? fields.size() : "all",
          recordCount));

    } catch (Exception e) {
      logger.error(
          requestContext,
          Constants.EXCEPTION_MSG_FETCH + table + " : " + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != select) {
        logQueryElapseTime("getRecords", startTime, select.getQueryString(), requestContext);
      }
    }
    return response;
  }

  /**
   * Applies an asynchronous operation on records matching the filter criteria.
   * The provided callback will be invoked when the result set is available.
   *
   * @param requestContext The request context for tracking and logging.
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters A map of column names to their filter values (supports both single values and Lists for IN clauses).
   * @param fields A list of field names to retrieve (null or empty for all fields).
   * @param callback The callback to be applied on the result set when returned.
   * @throws ProjectCommonException if the operation fails.
   */
  @Override
  public void applyOperationOnRecordsAsync(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      FutureCallback<ResultSet> callback,
      RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Session session = connectionManager.getSession(keySpace);
    Select select = null;

    try {
      // Build SELECT clause with specified fields or all fields
      if (CollectionUtils.isNotEmpty(fields)) {
        select = QueryBuilder.select(fields.toArray(new String[0])).from(keySpace, table);
      } else {
        select = QueryBuilder.select().all().from(keySpace, table);
      }

      // Build WHERE clause with filters
      if (MapUtils.isNotEmpty(filters)) {
        Select.Where where = select.where();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
          Object value = filter.getValue();
          if (value instanceof List) {
            where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
          } else {
            where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
          }
        }
      }

      // Log and execute async query
      logDebug(requestContext, formatLogMessage("Executing async CQL query: {}", select.getQueryString()));
      ResultSetFuture future = session.executeAsync(select);
      Futures.addCallback(future, callback, Executors.newFixedThreadPool(1));
      
      // Log async operation initiation at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully initiated async query - keyspace: {}, table: {}, filters: {}, fields: {}",
          keySpace,
          table,
          filters != null ? filters.size() : 0,
          fields != null ? fields.size() : "all"));

    } catch (Exception e) {
      logger.error(
          requestContext,
          Constants.EXCEPTION_MSG_FETCH + table + " : " + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != select) {
        logQueryElapseTime(
            "applyOperationOnRecordsAsync", startTime, select.getQueryString(), requestContext);
      }
    }
  }

  /**
   * Adds a key-value pair to a map column in an existing Cassandra record.
   * Convenience method that delegates to updateMapRecord with add=true.
   *
   * @param requestContext The request context for tracking and logging.
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the map column to update.
   * @param key The key to add to the map.
   * @param value The value to associate with the key.
   * @return Response object containing the operation result.
   * @throws ProjectCommonException if the operation fails.
   */
  @Override
  public Response updateAddMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      Object value,
      RequestContext requestContext) {
    return updateMapRecord(keySpace, table, primaryKey, column, key, value, true, requestContext);
  }

  /**
   * Removes a key from a map column in an existing Cassandra record.
   * Convenience method that delegates to updateMapRecord with add=false.
   *
   * @param requestContext The request context for tracking and logging.
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the map column to update.
   * @param key The key to remove from the map.
   * @return Response object containing the operation result.
   * @throws ProjectCommonException if the operation fails.
   */
  @Override
  public Response updateRemoveMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      RequestContext requestContext) {
    return updateMapRecord(keySpace, table, primaryKey, column, key, null, false, requestContext);
  }

  /**
   * Updates a map column in a Cassandra record by adding or removing a key-value pair.
   * This is a low-level method used by updateAddMapRecord and updateRemoveMapRecord.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the map column to update.
   * @param key The key to add or remove from the map.
   * @param value The value to associate with the key (ignored if add=false).
   * @param add If true, adds the key-value pair; if false, removes the key.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   * @throws ProjectCommonException if the operation fails or primary key is empty.
   */
  public Response updateMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      Object value,
      boolean add,
      RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Update update = QueryBuilder.update(keySpace, table);

    // Build UPDATE clause for map operation
    if (add) {
      update.with(QueryBuilder.put(column, key, value));
    } else {
      update.with(QueryBuilder.remove(column, key));
    }

    // Validate primary key
    if (MapUtils.isEmpty(primaryKey)) {
      String errorMsg =
          Constants.EXCEPTION_MSG_FETCH + table + " : primary key is a must for update call";
      logError(requestContext, "Primary key validation failed for map operation - table: {}, error: {}",
          table,
          errorMsg);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    // Build WHERE clause with primary key
    Update.Where where = update.where();
    for (Map.Entry<String, Object> filter : primaryKey.entrySet()) {
      Object filterValue = filter.getValue();
      if (filterValue instanceof List) {
        where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
      } else {
        where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
      }
    }

    try {
      Response response = new Response();
      logDebug(requestContext, formatLogMessage("Executing map operation query: {}", update.getQueryString()));
      connectionManager.getSession(keySpace).execute(update);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
      
      // Log successful map operation at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully {} map column - keyspace: {}, table: {}, column: {}, key: {}, operation: {}",
          add ? "added to" : "removed from",
          keySpace,
          table,
          column,
          key,
          add ? "add" : "remove"));
      
      return response;

    } catch (Exception e) {
      logError(
          requestContext,
          "Map operation failed - keyspace: {}, table: {}, column: {}, key: {}, operation: {}, error: {}",
          keySpace,
          table,
          column,
          key,
          add ? "add" : "remove",
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != update) {
        logQueryElapseTime("updateMapRecord", startTime, update.getQueryString(), requestContext);
      }
    }
  }

  /**
   * Adds a value to a set column in an existing Cassandra record.
   * Convenience method that delegates to updateSetRecord with add=true.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the set column to update.
   * @param value The value to add to the set.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   * @throws ProjectCommonException if the operation fails.
   */
  public Response updateAddSetRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      Object value,
      RequestContext requestContext) {
    logDebug(
        requestContext,
        formatLogMessage("Starting updateAddSetRecord - keyspace: {}, table: {}, column: {}, value: {}",
            keySpace,
            table,
            column,
            value));
    return updateSetRecord(keySpace, table, primaryKey, column, value, true, requestContext);
  }

  /**
   * Removes a value from a set column in an existing Cassandra record.
   * Convenience method that delegates to updateSetRecord with add=false.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the set column to update.
   * @param value The value to remove from the set.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   * @throws ProjectCommonException if the operation fails.
   */
  public Response updateRemoveSetRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      Object value,
      RequestContext requestContext) {
    logDebug(
        requestContext,
        formatLogMessage("Starting updateRemoveSetRecord - keyspace: {}, table: {}, column: {}, value: {}",
            keySpace,
            table,
            column,
            value));
    return updateSetRecord(keySpace, table, primaryKey, column, value, false, requestContext);
  }

  /**
   * Updates a set column in a Cassandra record by adding or removing a value.
   * This is a low-level method used by updateAddSetRecord and updateRemoveSetRecord.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the set column to update.
   * @param value The value to add or remove from the set.
   * @param add If true, adds the value; if false, removes the value.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   * @throws ProjectCommonException if the operation fails or primary key is empty.
   */
  public Response updateSetRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      Object value,
      boolean add,
      RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext,
        formatLogMessage("Starting updateSetRecord - keyspace: {}, table: {}, column: {}, value: {}, operation: {}",
            keySpace,
            table,
            column,
            value,
            add ? "add" : "remove"));

    Update update = QueryBuilder.update(keySpace, table);

    // Build UPDATE clause for set operation
    if (add) {
      update.with(QueryBuilder.add(column, value));
    } else {
      update.with(QueryBuilder.remove(column, value));
    }

    // Validate primary key
    if (MapUtils.isEmpty(primaryKey)) {
      String errorMsg =
          Constants.EXCEPTION_MSG_FETCH + table + " : primary key is a must for update call";
      logError(requestContext, "Primary key validation failed for set operation - table: {}, error: {}",
          table,
          errorMsg);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    // Build WHERE clause with primary key
    Update.Where where = update.where();
    for (Map.Entry<String, Object> filter : primaryKey.entrySet()) {
      Object filterValue = filter.getValue();
      if (filterValue instanceof List) {
        where = where.and(QueryBuilder.in(filter.getKey(), filterValue));
      } else {
        where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
      }
    }

    try {
      Response response = new Response();
      logDebug(requestContext, formatLogMessage("Executing set operation query: {}", update.getQueryString()));
      connectionManager.getSession(keySpace).execute(update);
      response.put(Constants.RESPONSE, Constants.SUCCESS);
      
      // Log successful set operation at INFO level
      logInfo(
          requestContext, formatLogMessage("Successfully {} set column - keyspace: {}, table: {}, column: {}, value: {}, operation: {}",
          add ? "added to" : "removed from",
          keySpace,
          table,
          column,
          value,
          add ? "add" : "remove"));
      
      return response;

    } catch (Exception e) {
      logError(
          requestContext,
          "Set operation failed - keyspace: {}, table: {}, column: {}, value: {}, operation: {}, error: {}",
          keySpace,
          table,
          column,
          value,
          add ? "add" : "remove",
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != update) {
        logQueryElapseTime("updateSetRecord", startTime, update.getQueryString(), requestContext);
      }
    }
  }

  /**
   * Retrieves records from a Cassandra table with a limit on the number of results.
   * Overrides the base implementation to provide consistent error handling and logging.
   *
   * @param requestContext The request context for tracking and logging.
   * @param keyspace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters A map of column names to their filter values (supports both single values and Lists for IN clauses).
   * @param fields A list of field names to retrieve (null or empty for all fields).
   * @param limit The maximum number of records to return.
   * @return Response object containing the matching records up to the specified limit.
   * @throws ProjectCommonException if the operation fails.
   */
  @Override
  public Response getRecordsWithLimit(
      String keyspace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      Integer limit,
      RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    logDebug(
        requestContext,
        formatLogMessage("Starting getRecordsWithLimit - keyspace: {}, table: {}, limit: {}, fields: {}, filters: {}",
            keyspace,
            table,
            limit,
            fields != null ? fields.size() : "all",
            filters != null ? filters.size() : 0));

    Response response = new Response();
    Session session = connectionManager.getSession(keyspace);
    Select select = null;

    try {
      // Build SELECT clause with specified fields or all fields
      if (CollectionUtils.isNotEmpty(fields)) {
        select = QueryBuilder.select(fields.toArray(new String[0])).from(keyspace, table);
      } else {
        select = QueryBuilder.select().all().from(keyspace, table);
      }

      // Build WHERE clause with filters
      if (MapUtils.isNotEmpty(filters)) {
        Select.Where where = select.where();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
          Object value = filter.getValue();
          if (value instanceof List) {
            where = where.and(QueryBuilder.in(filter.getKey(), ((List) filter.getValue())));
          } else {
            where = where.and(QueryBuilder.eq(filter.getKey(), filter.getValue()));
          }
        }
      }

      // Apply limit
      select.limit(limit);

      // Log and execute query
      logDebug(requestContext, formatLogMessage("Executing CQL query: {}", select.getQueryString()));
      ResultSet results = session.execute(select);
      response = CassandraUtil.createResponse(results);

      // Log successful query at INFO level
      int recordCount =
          response.getResult() != null
              ? ((List<?>) response.getResult().get(Constants.RESPONSE)).size()
              : 0;

      logInfo(
          requestContext, formatLogMessage("Successfully retrieved records with limit - keyspace: {}, table: {}, filters: {}, fields: {}, limit: {}, count: {}",
          keyspace,
          table,
          filters != null ? filters.size() : 0,
          fields != null ? fields.size() : "all",
          limit,
          recordCount));

    } catch (Exception e) {
      logError(
          requestContext,
          "Failed to retrieve records with limit - keyspace: {}, table: {}, limit: {}, error: {}",
          keyspace,
          table,
          limit,
          e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      if (null != select) {
        logQueryElapseTime(
            "getRecordsWithLimit", startTime, select.getQueryString(), requestContext);
      }
    }
    return response;
  }
}
