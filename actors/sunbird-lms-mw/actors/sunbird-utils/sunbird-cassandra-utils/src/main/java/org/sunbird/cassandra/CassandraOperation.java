/** */
package org.sunbird.cassandra;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.FutureCallback;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;

/**
 * @desc this interface will hold functions for cassandra db interaction
 * @author Amit Kumar
 */
public interface CassandraOperation {

  /**
   * @desc This method is used to insert/update record in cassandra db (if primary key exist in
   *     request ,it will update else will insert the record in cassandra db. By default cassandra
   *     insert operation does upsert operation. Upsert means that Cassandra will insert a row if a
   *     primary key does not exist already otherwise if primary key already exists, it will update
   *     that row.)
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param request Map<String,Object>(i.e map of column name and their value)
   * @param context
   * @return Response Response
   */
  public Response upsertRecord(
      String keyspaceName, String tableName, Map<String, Object> request, RequestContext context);

  /**
   * @desc This method is used to insert record in cassandra db
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param request Map<String,Object>(i.e map of column name and their value)
   * @param context
   * @return Response Response
   */
  public Response insertRecord(
      String keyspaceName, String tableName, Map<String, Object> request, RequestContext context);

  /**
   * @desc This method is used to update record in cassandra db
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param request Map<String,Object>(i.e map of column name and their value)
   * @param context
   * @return Response Response
   */
  public Response updateRecord(
      String keyspaceName, String tableName, Map<String, Object> request, RequestContext context);

  /**
   * @desc This method is used to delete record in cassandra db by their primary key(identifier)
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param identifier String
   * @param context
   * @return Response Response
   */
  public Response deleteRecord(
      String keyspaceName, String tableName, String identifier, RequestContext context);

  /**
   * @desc This method is used to delete record in cassandra db by their primary composite key
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param compositeKeyMap Column map for composite primary key
   * @param context
   */
  public void deleteRecord(
      String keyspaceName,
      String tableName,
      Map<String, String> compositeKeyMap,
      RequestContext context);

  /**
   * @desc This method is used to delete one or more records from Cassandra DB corresponding to
   *     given list of primary keys
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param identifierList List of primary keys of records to be deleted
   * @param context
   * @return Status of delete records operation
   */
  public boolean deleteRecords(
      String keyspaceName, String tableName, List<String> identifierList, RequestContext context);

  /**
   * @desc This method is used to fetch record based on given parameter and it's list of value (for
   *     In Query , for example : SELECT * FROM mykeyspace.mytable WHERE id IN (‘A’,’B’,C’) )
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param propertyName String
   * @param propertyValueList List<Object>
   * @param context
   * @return Response Response
   */
  public Response getRecordsByProperty(
      String keyspaceName,
      String tableName,
      String propertyName,
      List<Object> propertyValueList,
      RequestContext context);

  /**
   * Fetch records with specified columns (select all if null) for given column map (name, value
   * pairs).
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param propertyMap Map describing columns to be used in where clause of select query.
   * @param fields List of columns to be returned in each record
   * @param context
   * @return Response consisting of fetched records
   */
  Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      List<String> fields,
      RequestContext context);

  /**
   * @desc This method is used to fetch record based on given parameter list and their values
   *     pairs). without using allow filtering
   * @param keyspaceName
   * @param tableName
   * @param propertyMap
   * @param context
   * @return
   */
  Response getRecordsByProperties(
      String keyspaceName,
      String tableName,
      Map<String, Object> propertyMap,
      RequestContext context);

  /**
   * @desc This method is used to fetch properties value based on id
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param id String
   * @param properties String varargs
   * @param context
   * @return Response.
   */
  public Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      String id,
      List<String> properties,
      RequestContext context);

  /**
   * @desc This method is used to fetch properties value based on list of id
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param ids String
   * @param properties String varargs
   * @param context
   * @return Response.
   */
  public Response getPropertiesValueById(
      String keyspaceName,
      String tableName,
      List<String> ids,
      List<String> properties,
      RequestContext context);

  /**
   * @desc This method is used to fetch all records for table(i.e Select * from tableName)
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param context
   * @return Response Response
   */
  public Response getAllRecords(String keyspaceName, String tableName, RequestContext context);

  /**
   * @desc This method is used to fetch all records for table(i.e Select * from tableName)
   * @param keyspaceName String (data base keyspace name)
   * @param tableName String
   * @param context
   * @param fields List of columns to be returned in each record
   * @return Response Response
   */
  public Response getAllRecords(
      String keyspaceName, String tableName, List<String> fields, RequestContext context);

  /**
   * Method to update the record on basis of composite primary key.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param updateAttributes Column map to be used in set clause of update query
   * @param compositeKey Column map for composite primary key
   * @param context
   * @return Response consisting of update query status
   */
  Response updateRecord(
      String keyspaceName,
      String tableName,
      Map<String, Object> updateAttributes,
      Map<String, Object> compositeKey,
      RequestContext context);

  /**
   * Method to get record by primary key.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param key Primary key
   * @param context
   * @return Response consisting of matched record
   */
  Response getRecordById(String keyspaceName, String tableName, String key, RequestContext context);

  /**
   * Method to get record by composite primary key.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param key Column map representing composite primary key
   * @param context
   * @return Response consisting of matched record
   */
  Response getRecordById(
      String keyspaceName, String tableName, Map<String, Object> key, RequestContext context);

  /**
   * Method to get record by primary key consisting of only specified fields (return all if null).
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param key Primary key
   * @param fields List of columns to be returned in each record
   * @param context
   * @return Response consisting of matched record
   */
  Response getRecordById(
      String keyspaceName,
      String tableName,
      String key,
      List<String> fields,
      RequestContext context);

  /**
   * Method to get record by composity primary key consisting of only specified fields (return all
   * if null).
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param key Column map representing composite primary key
   * @param fields List of columns to be returned in each record
   * @param context
   * @return Response consisting of matched record
   */
  Response getRecordById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> fields,
      RequestContext context);

  /**
   * Method to get record by primary key consisting of only specified fields (return all if null).
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param key Primary key
   * @param ttlFields List of columns to be returned in each record with ttl
   * @param fields List of columns to be returned in each record
   * @param context
   * @return Response consisting of matched record
   */
  Response getRecordWithTTLById(
      String keyspaceName,
      String tableName,
      Map<String, Object> key,
      List<String> ttlFields,
      List<String> fields,
      RequestContext context);

  /**
   * Method to perform batch insert operation.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param records List of records in the batch insert operation
   * @param context
   * @return Response indicating status of operation
   */
  Response batchInsert(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext context);

  /**
   * Method to perform batch update operation.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param records List of map consisting of two maps with exactly two keys: PK: Column map for
   *     primary key, NonPK: Column map for properties with new values to be updated
   * @param context
   * @return Response indicating status of operation
   */
  Response batchUpdate(
      String keyspaceName,
      String tableName,
      List<Map<String, Map<String, Object>>> records,
      RequestContext context);

  Response batchUpdateById(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      RequestContext context);

  /**
   * Fetch records with composite key.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param compositeKeyMap Column map for composite primary key
   * @param context
   * @return Response consisting of fetched records
   */
  Response getRecordsByCompositeKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> compositeKeyMap,
      RequestContext context);

  /**
   * Fetch records with specified columns for given identifiers.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param properties List of columns to be returned in each record
   * @param ids List of identifiers
   * @param context
   * @return Response consisting of fetched records
   */
  Response getRecordsByIdsWithSpecifiedColumns(
      String keyspaceName,
      String tableName,
      List<String> properties,
      List<String> ids,
      RequestContext context);

  /**
   * Fetch records for given primary keys.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param primaryKeys List of primary key values
   * @param primaryKeyColumnName Name of the primary key column
   * @param context
   * @return Response consisting of fetched records
   */
  Response getRecordsByPrimaryKeys(
      String keyspaceName,
      String tableName,
      List<String> primaryKeys,
      String primaryKeyColumnName,
      RequestContext context);

  /**
   * Insert record with TTL expiration
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param request Map consisting of column name and value
   * @param ttl Time to live after which inserted record will be auto deleted
   * @param context
   * @return Response indicating status of operation
   */
  public Response insertRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      int ttl,
      RequestContext context);

  /**
   * Update record with TTL expiration
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param request Map consisting of column name and value
   * @param compositeKey Column map for composite primary key
   * @param ttl Time to live after which inserted record will be auto deleted
   * @param context
   * @return Response indicating status of operation
   */
  public Response updateRecordWithTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> request,
      Map<String, Object> compositeKey,
      int ttl,
      RequestContext context);
  /**
   * Fetch records with specified columns that match given partition / primary key. Multiple records
   * would be fetched in case partition key is specified.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param primaryKeys Column and value map for partition / primary key
   * @param properties List of columns to be returned in each record
   * @param ttlPropertiesWithAlias Map containing TTL column as key and alias as value.
   * @param context
   * @return Response consisting of fetched records
   */
  Response getRecordsByIdsWithSpecifiedColumnsAndTTL(
      String keyspaceName,
      String tableName,
      Map<String, Object> primaryKeys,
      List<String> properties,
      Map<String, String> ttlPropertiesWithAlias,
      RequestContext context);

  /**
   * Perform batch insert with different TTL values
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param records List of records in the batch insert operation
   * @param ttls TTL (in seconds) for each record to be inserted. TTL is ignored if value is not a
   *     positive number.
   * @param context
   * @return Response indicating status of operation
   */
  Response batchInsertWithTTL(
      String keyspaceName,
      String tableName,
      List<Map<String, Object>> records,
      List<Integer> ttls,
      RequestContext context);

  public Response getRecords(
      String keyspace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      RequestContext context);

  /**
   * Apply callback on cassandra async read call.
   *
   * @param keySpace Keyspace name
   * @param table Table name
   * @param filters Column and value map for filtering
   * @param fields List of columns to be returned in each record
   * @param callback action callback to be applied on resultset when it is returned.
   * @param context
   */
  public void applyOperationOnRecordsAsync(
      String keySpace,
      String table,
      Map<String, Object> filters,
      List<String> fields,
      FutureCallback<ResultSet> callback,
      RequestContext context);

  public Response performBatchAction(
      String keyspaceName, String tableName, Map<String, Object> inputData, RequestContext context);

  /**
   * this method will be used to do CONTAINS query in list
   *
   * @param keyspace
   * @param tableName
   * @param key
   * @param Value
   * @param context
   * @return
   */
  Response searchValueInList(
      String keyspace, String tableName, String key, String Value, RequestContext context);

  /**
   * this method will be used to do CONTAINS query in list with the AND operations
   *
   * @param keyspace
   * @param tableName
   * @param key
   * @param Value
   * @param propertyMap
   * @param context
   * @return
   */
  Response searchValueInList(
      String keyspace,
      String tableName,
      String key,
      String Value,
      Map<String, Object> propertyMap,
      RequestContext context);

  /**
   * @param keySpace
   * @param table
   * @param primaryKey
   * @param column
   * @param key
   * @param value
   * @param context
   * @return
   */
  public Response updateAddMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      Object value,
      RequestContext context);

  /**
   * @param keySpace
   * @param table
   * @param primaryKey
   * @param column
   * @param key
   * @param context
   * @return
   */
  public Response updateRemoveMapRecord(
      String keySpace,
      String table,
      Map<String, Object> primaryKey,
      String column,
      String key,
      RequestContext context);

  /**
   * Fetch records from user lookup.
   *
   * @param keyspaceName Keyspace name
   * @param tableName Table name
   * @param partitionKeyMap Column map for partition key
   * @param context
   * @return Response consisting of fetched records
   */
  Response getRecordsByCompositePartitionKey(
      String keyspaceName,
      String tableName,
      Map<String, Object> partitionKeyMap,
      RequestContext context);
}
