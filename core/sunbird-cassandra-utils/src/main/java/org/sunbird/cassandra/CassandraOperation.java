package org.sunbird.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.UserType;
import com.google.common.util.concurrent.FutureCallback;
import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

/**
 * Interface defining core CRUD operations for Cassandra database interactions.
 * Provides methods for inserting, updating, deleting, and upserting records in Cassandra tables.
 * All operations support request context for tracking and logging purposes.
 */
public interface CassandraOperation {

  /**
   * Performs an upsert operation (insert or update) on a Cassandra table.
   * If the primary key exists in the request, the record will be updated;
   * otherwise, a new record will be inserted. Cassandra's insert operation
   * inherently performs upsert by design.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where the record will be upserted.
   * @param request A map of column names to their values.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response upsertRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      RequestContext requestContext);

  /**
   * Inserts a new record into a Cassandra table.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where the record will be inserted.
   * @param request A map of column names to their values.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response insertRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      RequestContext requestContext);

  /**
   * Updates an existing record in a Cassandra table.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where the record will be updated.
   * @param request A map of column names to their updated values.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      RequestContext requestContext);

  /**
   * Deletes a record from a Cassandra table using a single primary key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name from which the record will be deleted.
   * @param identifier The primary key identifier of the record to delete.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response deleteRecord(
      String keyspaceName,
      String tableName,
      String identifier,
      RequestContext requestContext);

  /**
   * Deletes a record from a Cassandra table using a composite primary key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name from which the record will be deleted.
   * @param compositeKeyMap A map containing the composite primary key column names and values.
   * @param requestContext The request context for tracking and logging.
   */
  void deleteRecord(
      String keyspaceName,
      String tableName,
      Map<String, String> compositeKeyMap,
      RequestContext requestContext);

  /**
   * Deletes multiple records from a Cassandra table using a list of primary keys.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name from which the records will be deleted.
   * @param identifierList A list of primary key identifiers for the records to delete.
   * @param requestContext The request context for tracking and logging.
   * @return {@code true} if the delete operation was successful, {@code false} otherwise.
   */
  boolean deleteRecords(
      String keyspaceName,
      String tableName,
      List<String> identifierList,
      RequestContext requestContext);

  /**
   * Retrieves records from a Cassandra table where a specific property matches any value in the
   * provided list.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param propertyName The name of the property/column to filter by.
   * @param propertyValueList A list of values to match against the property.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      List<Object> propertyValueList,
      RequestContext requestContext);


  /**
   * Retrieves records from a Cassandra table where a specific property matches a given value.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param propertyName The name of the property/column to filter by.
   * @param propertyValue The value to match against the property.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  @Deprecated
  Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      RequestContext requestContext);

  /**
   * Retrieves specific fields from records in a Cassandra table where a property matches a given
   * value. This method allows selective field retrieval to optimize query performance and reduce
   * data transfer.
   *
   * <p>This is an optimized version of {@link #getRecordsByProperty(String, String, String, List,
   * RequestContext)} that:
   *
   * <ul>
   *   <li>Accepts a single property value instead of a list
   *   <li>Allows specification of which fields to retrieve
   *   <li>Reduces network overhead by fetching only required columns
   * </ul>
   *
   * <p><b>Use Case:</b> Use this method when you need to query by a single property value and only
   * require specific fields from the matching records, rather than retrieving all columns.
   *
   * <p><b>Example:</b>
   *
   * <pre>
   * // Retrieve only userId and courseId from enrollments where status = "active"
   * List&lt;String&gt; fields = Arrays.asList("userId", "courseId", "enrolledDate");
   * Response response = getRecordsByProperty(
   *     "sunbird",
   *     "user_enrollment",
   *     "status",
   *     "active",
   *     fields,
   *     requestContext);
   * </pre>
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param propertyName The name of the property/column to filter by.
   * @param propertyValue The value to match against the property.
   * @param fields A list of field/column names to retrieve. If null or empty, all fields are
   *     retrieved.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records with only the specified fields.
   */
  Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      List<String> fields,
      RequestContext requestContext);


  /**
   * Retrieves records from a Cassandra table using an indexed property for efficient querying.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param propertyName The name of the indexed property/column to filter by.
   * @param propertyValue The value to match against the indexed property.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response getRecordsByIndexedProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      Object propertyValue,
      RequestContext requestContext);

  /**
   * Retrieves records from a Cassandra table matching multiple property criteria.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param propertyMap A map of property names to their values for filtering.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      RequestContext requestContext);

  /**
   * Retrieves specific fields from records in a Cassandra table matching multiple property
   * criteria.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param propertyMap A map of property names to their values for filtering.
   * @param fields A list of field names to retrieve from the matching records.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the specified fields from matching records.
   */
  Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      List<String> fields,
      RequestContext requestContext);

  /**
   * Retrieves specific property values from a record identified by its ID.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param id The primary key identifier of the record.
   * @param requestContext The request context for tracking and logging.
   * @param properties A list of property names to retrieve.
   * @return Response object containing the requested property values.
   */
  Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      String id,
      RequestContext requestContext,
      List<String> properties);

  /**
   * Retrieves specific property values from multiple records identified by their IDs.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param ids A list of primary key identifiers.
   * @param properties A list of property names to retrieve from each record.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the requested property values for all specified records.
   */
  Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      List<String> ids,
      List<String> properties,
      RequestContext requestContext);

  /**
   * Retrieves all records from a Cassandra table.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing all records from the table.
   */
  Response getAllRecords(
      String keyspaceName, String tableName, RequestContext requestContext);

  /**
   * Retrieves specific fields from all records in a Cassandra table.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param fields A list of field names to retrieve from each record.
   * @param context The request context for tracking and logging.
   * @return Response object containing the specified fields from all records.
   */
  Response getAllRecords(
      String keyspaceName, String tableName, List<String> fields, RequestContext context);

  /**
   * Updates a record in a Cassandra table using a composite primary key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where the record will be updated.
   * @param updateAttributes A map of column names to their updated values.
   * @param compositeKey A map representing the composite primary key.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> updateAttributes,
      Map<String, Object> compositeKey,
      RequestContext requestContext);

  /**
   * Retrieves a record by its identifier with specified fields.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The primary key identifier (can be any object type).
   * @param fields A list of field names to retrieve.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record.
   */
  Response getRecordByIdentifier(
      String keyspaceName,
      String tableName,
      Object key,
      List<String> fields,
      RequestContext requestContext);

  /**
   * Retrieves a record by its primary key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The primary key identifier.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record.
   */
  Response getRecordById(
      String keyspaceName, String tableName, String key, RequestContext requestContext);

  /**
   * Retrieves a record by composite primary key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key A map representing the composite primary key.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record.
   */
  Response getRecordById(
      String keyspaceName, String tableName, Map<String, Object> key, RequestContext requestContext);

  /**
   * Retrieves a record by primary key with only specified fields (returns all fields if null).
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The primary key identifier.
   * @param fields A list of column names to retrieve (null returns all columns).
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record with specified fields.
   */
  Response getRecordById(
      String keyspaceName,
      String tableName,
      String key,
      List<String> fields,
      RequestContext requestContext);

  /**
   * Retrieves a record by composite primary key with only specified fields (returns all fields if
   * null).
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key A map representing the composite primary key.
   * @param fields A list of column names to retrieve (null returns all columns).
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record with specified fields.
   */
  Response getRecordById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> fields,
      RequestContext requestContext);

  /**
   * Retrieves a record by primary key with TTL (Time To Live) information for specified fields.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key A map representing the primary key.
   * @param ttlFields A list of column names for which TTL information should be retrieved.
   * @param fields A list of column names to retrieve (null returns all columns).
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record with TTL information.
   */
  Response getRecordWithTTLById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext requestContext);



  /**
   * Retrieves a record by its identifier with TTL (Time To Live) information for specified fields.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key A map representing the primary key identifier.
   * @param ttlFields A list of column names for which TTL information should be retrieved.
   * @param fields A list of column names to retrieve (null returns all columns).
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching record with TTL information.
   */
  Response getRecordWithTTLByIdentifier(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext requestContext);

  /**
   * Performs a batch insert operation to insert multiple records into a Cassandra table.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where records will be inserted.
   * @param records A list of maps, each representing a record to insert.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response batchInsert(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext requestContext);

  /**
   * Performs a batch delete operation to delete multiple records in a single atomic operation.
   * Each record is identified by its composite primary key provided in the list.
   * More efficient than individual delete operations for bulk deletions.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name from which records will be deleted.
   * @param primaryKeys A list of maps, each containing the composite primary key columns and values
   *                    for a record to delete.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response batchDelete(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> primaryKeys,
      RequestContext requestContext);

  /**
   * Retrieves records from a Cassandra table based on filter criteria with specified fields.
   *
   * @param keyspace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters A map of column names to their filter values.
   * @param fields A list of field names to retrieve.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response getRecords(
      String keyspace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      RequestContext requestContext);

  /**
   * Retrieves records from a Cassandra table using a composite primary key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param compositeKeyMap A map representing the composite primary key.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response getRecordsByCompositeKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> compositeKeyMap,
      RequestContext requestContext);

  /**
   * Performs a batch update operation on multiple records in a Cassandra table.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where records will be updated.
   * @param list A list of maps containing the update specifications.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response batchUpdate(
      String keyspaceName,
      String tableName,
      List<Map<String, Map<String, Object>>> list,
      RequestContext requestContext);

  /**
   * Performs a batch update operation on multiple records identified by their IDs.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where records will be updated.
   * @param records A list of maps, each containing the record ID and update values.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response batchUpdateById(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext requestContext);

  /**
   * Applies a callback operation on Cassandra async read call.
   * This method performs asynchronous read operations and applies the provided callback when the
   * result set is returned.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters A map of column names to their filter values.
   * @param fields A list of column names to retrieve.
   * @param callback The callback to be applied on the result set when returned.
   * @param requestContext The request context for tracking and logging.
   */
  void applyOperationOnRecordsAsync(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      FutureCallback<ResultSet> callback,
      RequestContext requestContext);

  /**
   * Performs a CONTAINS query on a list column in Cassandra.
   *
   * @param keyspace The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The column name containing the list.
   * @param value The value to search for in the list.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String value,
      RequestContext requestContext);

  /**
   * Performs a CONTAINS query on a list column in Cassandra with additional AND filter conditions.
   *
   * @param keyspace The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param key The column name containing the list.
   * @param value The value to search for in the list.
   * @param propertyMap Additional filter criteria to apply with AND operation.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records.
   */
  Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String value,
      Map<String, Object> propertyMap,
      RequestContext requestContext);

  /**
   * Adds a key-value pair to a map column in an existing Cassandra record.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the map column to update.
   * @param key The key to add to the map.
   * @param value The value to associate with the key.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response updateAddMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      Object value,
      RequestContext requestContext);

  /**
   * Removes a key from a map column in an existing Cassandra record.
   *
   * @param keySpace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param primaryKey A map representing the primary key of the record to update.
   * @param column The name of the map column to update.
   * @param key The key to remove from the map.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response updateRemoveMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      RequestContext requestContext);

  /**
   * Updates records using QueryBuilder with conditional IF EXISTS clause.
   * This method allows for conditional updates that only execute if the record exists.
   *
   * @param keyspace The Cassandra keyspace name.
   * @param table The table name to update.
   * @param selectMap A map of column names to their values for selecting the record.
   * @param updateMap A map of column names to their new values.
   * @param ifExists If {@code true}, the update only executes if the record exists.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response updateRecordV2(
      String keyspace,
      String table,
      Map<String, Object> selectMap,
      Map<String, Object> updateMap,
      boolean ifExists,
      RequestContext requestContext);

  /**
   * Retrieves records from a Cassandra table with a limit on the number of results.
   *
   * @param keyspace The Cassandra keyspace name.
   * @param table The table name to query.
   * @param filters A map of column names to their filter values.
   * @param fields A list of field names to retrieve.
   * @param limit The maximum number of records to return.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the matching records up to the specified limit.
   */
  Response getRecordsWithLimit(
      String keyspace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      Integer limit,
      RequestContext requestContext);

  /**
   * Performs a logged batch insert operation to insert multiple records into a Cassandra table.
   * Logged batches ensure atomicity across multiple partition keys but have performance overhead.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where records will be inserted.
   * @param records A list of maps, each representing a record to insert.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response batchInsertLogged(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext requestContext);

  /**
   * Retrieves records with specified columns for given identifiers.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param properties A list of field names to retrieve.
   * @param ids A list of primary key identifiers.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the fetched records.
   */
  Response getRecordsByIdsWithSpecifiedColumns(
      String keyspaceName,
      String tableName,
      List<String> properties,
      List<String> ids,
      RequestContext requestContext);

  /**
   * Retrieves records for given primary keys.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param primaryKeys A list of primary key values.
   * @param primaryKeyColumnName The name of the primary key column.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the fetched records.
   */
  Response getRecordsByPrimaryKeys(
      String keyspaceName,
      String tableName,
      List<String> primaryKeys,
      String primaryKeyColumnName,
      RequestContext requestContext);

  /**
   * Inserts a record with a Time-To-Live (TTL) expiration.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where the record will be inserted.
   * @param request A map of column names to their values.
   * @param ttl The time-to-live in seconds after which the record will be automatically deleted.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response insertRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      int ttl,
      RequestContext requestContext);

  /**
   * Updates a record with a Time-To-Live (TTL) expiration.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where the record will be updated.
   * @param request A map of column names to their updated values.
   * @param compositeKey A map representing the composite primary key.
   * @param ttl The time-to-live in seconds after which the record will be automatically deleted.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response updateRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey,
      int ttl,
      RequestContext requestContext);

  /**
   * Retrieves records with specified columns that match a given partition/primary key, including TTL information.
   * Multiple records may be fetched if a partition key is specified.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param primaryKeys A map of partition/primary key columns and their values.
   * @param properties A list of field names to retrieve.
   * @param ttlPropertiesWithAlias A map where the key is the TTL column name and the value is its alias.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the fetched records with TTL info.
   */
  Response getRecordsByIdsWithSpecifiedColumnsAndTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> primaryKeys,
      List<String> properties,
      Map<String, String> ttlPropertiesWithAlias,
      RequestContext requestContext);

  /**
   * Performs a batch insert operation where each record has a different TTL value.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name where records will be inserted.
   * @param records A list of maps, each representing a record to insert.
   * @param ttls A list of TTL values (in seconds) corresponding to each record. TTL is ignored if not positive.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response batchInsertWithTTL(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      List<Integer> ttls,
      RequestContext requestContext);

  /**
   * Performs a generic batch action.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name.
   * @param inputData A map containing input data for the batch action.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the operation result.
   */
  Response performBatchAction(
      String keyspaceName,
      String tableName,
      Map<String, Object> inputData,
      RequestContext requestContext);

  /**
   * Retrieves records using a composite partition key.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param tableName The table name to query.
   * @param partitionKeyMap A map representing the composite partition key.
   * @param requestContext The request context for tracking and logging.
   * @return Response object containing the fetched records.
   */
  Response getRecordsByCompositePartitionKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> partitionKeyMap,
      RequestContext requestContext);

  /**
   * Retrieves a User Defined Type (UDT) from a Cassandra keyspace.
   *
   * @param keyspaceName The Cassandra keyspace name.
   * @param typeName The name of the user-defined type to retrieve.
   * @return UserType object representing the specified user-defined type.
   */
  UserType getUDTType(String keyspaceName, String typeName);
}
