package org.sunbird.common.inf;

import java.util.List;
import java.util.Map;
import scala.concurrent.Future;
import org.sunbird.request.RequestContext;
import org.sunbird.dto.SearchDTO;

/**
 * Elasticsearch service interface defining operations for document management and search.
 * All methods are asynchronous and return Scala Futures for non-blocking execution.
 */
public interface ElasticSearchService {
  
  /** Elasticsearch document type constant for compatibility. */
  String _DOC = "_doc";

  /**
   * Saves a new document in Elasticsearch.
   * The identifier becomes the document _id in ES.
   *
   * @param index ES index name
   * @param identifier document ID
   * @param data document data
   * @param requestContext request context for logging and tracking
   * @return Future containing the created document identifier
   */
  Future<String> save(
      String index,
      String identifier,
      Map<String, Object> data,
      RequestContext requestContext);

  /**
   * Updates an existing document by merging with new data.
   *
   * @param index ES index name
   * @param identifier document ID
   * @param data update data to merge
   * @param requestContext request context for logging and tracking
   * @return Future containing update success status
   */
  Future<Boolean> update(
      String index,
      String identifier,
      Map<String, Object> data,
      RequestContext requestContext);

  /**
   * Retrieves a document by identifier.
   *
   * @param index ES index name
   * @param identifier document ID
   * @param requestContext request context for logging and tracking
   * @return Future containing document data or null if not found
   */
  Future<Map<String, Object>> getDataByIdentifier(
      String index,
      String identifier,
      RequestContext requestContext);

  /**
   * Deletes a document by identifier.
   *
   * @param index ES index name
   * @param identifier document ID
   * @param requestContext request context for logging and tracking
   * @return Future containing deletion success status
   */
  Future<Boolean> delete(
      String index,
      String identifier,
      RequestContext requestContext);

  /**
   * Performs search based on SearchDTO criteria including filters, facets, sorting, and pagination.
   *
   * @param searchDTO search criteria
   * @param index ES index name
   * @param requestContext request context for logging and tracking
   * @return Future containing search results
   */
  Future<Map<String, Object>> search(
      SearchDTO searchDTO,
      String index,
      RequestContext requestContext);

  /**
   * Performs Elasticsearch health check.
   *
   * @return Future containing health status
   */
  Future<Boolean> healthCheck();

  /**
   * Bulk inserts multiple documents in a single operation.
   *
   * @param index ES index name
   * @param dataList list of documents to insert
   * @param requestContext request context for logging and tracking
   * @return Future containing bulk insert success status
   */
  Future<Boolean> bulkInsert(
      String index,
      List<Map<String, Object>> dataList,
      RequestContext requestContext);

  /**
   * Upserts a document (update if exists, insert if not).
   *
   * @param index ES index name
   * @param identifier document ID
   * @param data document data
   * @param requestContext request context for logging and tracking
   * @return Future containing upsert success status
   */
  Future<Boolean> upsert(
      String index,
      String identifier,
      Map<String, Object> data,
      RequestContext requestContext);

  /**
   * Retrieves multiple documents by IDs with specified fields.
   *
   * @param ids list of document IDs
   * @param fields list of fields to retrieve
   * @param index ES index name
   * @param requestContext request context for logging and tracking
   * @return Future containing map of ID to document data
   */
  Future<Map<String, Map<String, Object>>> getEsResultByListOfIds(
      List<String> ids,
      List<String> fields,
      String index,
      RequestContext requestContext);

}
