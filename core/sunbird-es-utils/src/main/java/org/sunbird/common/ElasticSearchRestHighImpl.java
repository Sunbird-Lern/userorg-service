package org.sunbird.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.dispatch.Futures;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortMode;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.PropertiesCache;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ConnectionManager;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Implementation of the ElasticSearchService using the RestHighLevelClient.
 * This class provides methods to interact with Elasticsearch for indexing,
 * updating, deleting, and searching documents.
 */
public class ElasticSearchRestHighImpl implements ElasticSearchService {

  private static final String ERROR = "ERROR";
  private static final LoggerUtil logger = new LoggerUtil(ElasticSearchRestHighImpl.class);


  /**
   * Saves a document to Elasticsearch.
   *
   * @param index        The name of the index.
   * @param identifier   The unique identifier for the document.
   * @param data         The data to be saved (as a Map).
   * @param requestContext      The RequestContext for logging and tracing.
   * @return A Future containing the identifier of the saved document, or "ERROR" if validation fails.
   */
  @Override
  public Future<String> save(String index, String identifier, Map<String, Object> data, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<String> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:save: method started at ==" + startTime + " for Index " + index);
    
    if (StringUtils.isBlank(identifier) || StringUtils.isBlank(index)) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:save: Identifier or Index value is null or empty, identifier : " 
          + identifier + ", index: " + index + ", not able to save data.");
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidData));
      return promise.future();
    }
    
    try {
      data.put(JsonKey.IDENTIFIER, identifier);

      IndexRequest indexRequest = new IndexRequest(index, _DOC, identifier).source(data);

      ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse indexResponse) {
          logger.info(requestContext, "ElasticSearchRestHighImpl:save: Success for index : " + index + ", identifier :" + identifier);
          promise.success(indexResponse.getId());
          logEndTime(startTime, index, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:save: Error while saving " + index + " id : " + identifier, e);
          promise.failure(e);
          logEndTime(startTime, index, requestContext);
        }
      };

      ConnectionManager.getRestClient().indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
      
    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:save: Failed to prepare/submit save request for index: " 
          + index + ", identifier: " + identifier, e);
      promise.failure(e);
      logEndTime(startTime, index, requestContext);
    }

    return promise.future();
  }

  /**
   * Updates an existing document in Elasticsearch.
   *
   * @param index          The name of the index.
   * @param identifier     The unique identifier for the document.
   * @param data           The data to update (as a Map).
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing true if update succeeds, or failure if validation/update fails.
   */
  @Override
  public Future<Boolean> update(String index, String identifier, Map<String, Object> data, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:update: method started at ==" + startTime + " for Index " + index);

    if (StringUtils.isBlank(index) || StringUtils.isBlank(identifier) || data == null) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:update: Invalid parameters - index: " + index 
          + ", identifier: " + identifier + ", data: " + (data == null ? "null" : "present"));
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidData));
      return promise.future();
    }

    try {
      data.put(JsonKey.IDENTIFIER, identifier);
      UpdateRequest updateRequest = new UpdateRequest(index, _DOC, identifier).doc(data);

      ActionListener<UpdateResponse> listener = new ActionListener<UpdateResponse>() {
        @Override
        public void onResponse(UpdateResponse updateResponse) {
          logger.info(requestContext, "ElasticSearchRestHighImpl:update: Success with " + updateResponse.getResult()
              + " response from Elasticsearch for index: " + index + ", identifier: " + identifier);
          promise.success(true);
          logUpdateEndTime(startTime, index, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:update: Failed to update document in index: " 
              + index + ", identifier: " + identifier, e);
          promise.failure(e);
          logUpdateEndTime(startTime, index, requestContext);
        }
      };

      ConnectionManager.getRestClient().updateAsync(updateRequest, RequestOptions.DEFAULT, listener);

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:update: Failed to prepare/submit update request for index: " 
          + index + ", identifier: " + identifier, e);
      promise.failure(e);
      logUpdateEndTime(startTime, index, requestContext);
    }

    return promise.future();
  }

  /**
   * Retrieves a document from Elasticsearch by its identifier.
   *
   * @param index          The name of the index.
   * @param identifier     The unique identifier for the document.
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing the document as a Map, or an empty Map if not found.
   */
  @Override
  public Future<Map<String, Object>> getDataByIdentifier(String index, String identifier, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<Map<String, Object>> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: method started at ==" + startTime 
        + " for Index " + index);

    if (StringUtils.isBlank(index) || StringUtils.isBlank(identifier)) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: Invalid parameters - index: " 
          + index + ", identifier: " + identifier);
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidData));
      return promise.future();
    }

    try {
      GetRequest getRequest = new GetRequest(index, _DOC, identifier);

      ActionListener<GetResponse> listener = new ActionListener<GetResponse>() {
        @Override
        public void onResponse(GetResponse getResponse) {
          if (getResponse.isExists()) {
            Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
            if (MapUtils.isNotEmpty(sourceAsMap)) {
              logger.debug(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: Document found for index: " 
                  + index + ", identifier: " + identifier);
              promise.success(sourceAsMap);
            } else {
              logger.debug(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: Document exists but source is empty for index: " 
                  + index + ", identifier: " + identifier);
              promise.success(new HashMap<>());
            }
          } else {
            logger.debug(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: Document not found for index: " 
                + index + ", identifier: " + identifier);
            promise.success(new HashMap<>());
          }
          logGetEndTime(startTime, index, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: Failed to retrieve document from index: " 
              + index + ", identifier: " + identifier, e);
          promise.failure(e);
          logGetEndTime(startTime, index, requestContext);
        }
      };

      ConnectionManager.getRestClient().getAsync(getRequest, RequestOptions.DEFAULT, listener);

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: Failed to prepare/submit get request for index: " 
          + index + ", identifier: " + identifier, e);
      promise.failure(e);
      logGetEndTime(startTime, index, requestContext);
    }

    return promise.future();
  }

  /**
   * Deletes a document from Elasticsearch by its identifier.
   *
   * @param index          The name of the index.
   * @param identifier     The unique identifier for the document to delete.
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing true if deletion succeeds, false if document not found, or failure on error.
   */
  @Override
  public Future<Boolean> delete(String index, String identifier, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:delete: method started at ==" + startTime);

    if (StringUtils.isBlank(index) || StringUtils.isBlank(identifier)) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:delete: Invalid parameters - index: " 
          + index + ", identifier: " + identifier);
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidData));
      return promise.future();
    }

    try {
      DeleteRequest delRequest = new DeleteRequest(index, _DOC, identifier);
      
      ActionListener<DeleteResponse> listener = new ActionListener<DeleteResponse>() {
        @Override
        public void onResponse(DeleteResponse deleteResponse) {
          if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            logger.info(requestContext, "ElasticSearchRestHighImpl:delete: Document not found for index: " 
                + index + ", identifier: " + identifier);
            promise.success(false);
          } else {
            logger.info(requestContext, "ElasticSearchRestHighImpl:delete: Successfully deleted document from index: " 
                + index + ", identifier: " + identifier);
            promise.success(true);
          }
          logDeleteEndTime(startTime, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:delete: Failed to delete document from index: " 
              + index + ", identifier: " + identifier, e);
          promise.failure(e);
          logDeleteEndTime(startTime, requestContext);
        }
      };

      ConnectionManager.getRestClient().deleteAsync(delRequest, RequestOptions.DEFAULT, listener);

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:delete: Failed to prepare/submit delete request for index: " 
          + index + ", identifier: " + identifier, e);
      promise.failure(e);
      logDeleteEndTime(startTime, requestContext);
    }

    return promise.future();
  }

  /**
   * Performs an Elasticsearch search based on SearchDTO criteria.
   * Supports filters, facets, sorting, pagination, fuzzy search, and field selection.
   *
   * @param searchDTO      The search criteria containing filters, facets, sort, pagination, etc.
   * @param index          The name of the index to search.
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing search results as a Map with content, count, and facets.
   */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Future<Map<String, Object>> search(SearchDTO searchDTO, String index, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<Map<String, Object>> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:search: method started at ==" + startTime);

    try {
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      SearchRequest searchRequest = new SearchRequest(index);
      // Note: types() is deprecated in Elasticsearch 7.x, document type is now always "_doc"

      // Check mode and set constraints
      Map<String, Float> constraintsMap = ElasticSearchHelper.getConstraints(searchDTO);
      BoolQueryBuilder query = new BoolQueryBuilder();

      // Add channel field as mandatory
      String channel = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_ES_CHANNEL);
      if (!(StringUtils.isBlank(channel) || JsonKey.SUNBIRD_ES_CHANNEL.equals(channel))) {
        query.must(ElasticSearchHelper.createMatchQuery(JsonKey.CHANNEL, channel, constraintsMap.get(JsonKey.CHANNEL)));
      }

      // Apply simple query string
      if (!StringUtils.isBlank(searchDTO.getQuery())) {
        SimpleQueryStringBuilder sqsb = QueryBuilders.simpleQueryStringQuery(searchDTO.getQuery());
        if (CollectionUtils.isEmpty(searchDTO.getQueryFields())) {
          query.must(sqsb.field("all_fields"));
        } else {
          Map<String, Float> searchFields = searchDTO.getQueryFields().stream()
              .collect(Collectors.<String, String, Float>toMap(s -> s, v -> 1.0f));
          query.must(sqsb.fields(searchFields));
        }
      }

      // Apply sorting
      if (searchDTO.getSortBy() != null && !searchDTO.getSortBy().isEmpty()) {
        for (Map.Entry<String, Object> entry : searchDTO.getSortBy().entrySet()) {
          if (!entry.getKey().contains(".")) {
            searchSourceBuilder.sort(entry.getKey() + ElasticSearchHelper.RAW_APPEND,
                ElasticSearchHelper.getSortOrder((String) entry.getValue()));
          } else {
            Map<String, Object> map = (Map<String, Object>) entry.getValue();
            Map<String, String> dataMap = (Map) map.get(JsonKey.TERM);
            for (Map.Entry<String, String> dateMapEntry : dataMap.entrySet()) {
              FieldSortBuilder mySort = new FieldSortBuilder(entry.getKey() + ElasticSearchHelper.RAW_APPEND)
                  .setNestedFilter(new TermQueryBuilder(dateMapEntry.getKey(), dateMapEntry.getValue()))
                  .sortMode(SortMode.MIN)
                  .order(ElasticSearchHelper.getSortOrder((String) map.get(JsonKey.ORDER)));
              searchSourceBuilder.sort(mySort);
            }
          }
        }
      }

      // Apply field filters
      searchSourceBuilder.fetchSource(
          searchDTO.getFields() != null ? searchDTO.getFields().stream().toArray(String[]::new) : null,
          searchDTO.getExcludedFields() != null ? searchDTO.getExcludedFields().stream().toArray(String[]::new) : null);

      // Set offset
      if (searchDTO.getOffset() != null) {
        searchSourceBuilder.from(searchDTO.getOffset());
      }

      // Set limit
      if (searchDTO.getLimit() != null) {
        searchSourceBuilder.size(searchDTO.getLimit());
      }

      // Apply additional properties
      if (searchDTO.getAdditionalProperties() != null && !searchDTO.getAdditionalProperties().isEmpty()) {
        for (Map.Entry<String, Object> entry : searchDTO.getAdditionalProperties().entrySet()) {
          ElasticSearchHelper.addAdditionalProperties(query, entry, constraintsMap);
        }
      }

      // Apply fuzzy search
      if (MapUtils.isNotEmpty(searchDTO.getFuzzy())) {
        Map.Entry<String, String> entry = searchDTO.getFuzzy().entrySet().iterator().next();
        ElasticSearchHelper.createFuzzyMatchQuery(query, entry.getKey(), entry.getValue());
      }

      // Set final query
      searchSourceBuilder.query(query);
      List finalFacetList = new ArrayList();

      // Add aggregations
      if (searchDTO.getFacets() != null && !searchDTO.getFacets().isEmpty()) {
        searchSourceBuilder = addAggregations(searchSourceBuilder, searchDTO.getFacets(), requestContext);
      }

      logger.info(requestContext, "ElasticSearchRestHighImpl:search: calling search for index " + index 
          + ", with query = " + searchSourceBuilder.toString());

      searchRequest.source(searchSourceBuilder);

      ActionListener<SearchResponse> listener = new ActionListener<SearchResponse>() {
        @Override
        public void onResponse(SearchResponse response) {
          logger.debug(requestContext, "ElasticSearchRestHighImpl:search: onResponse received");
          
          if (response.getHits() == null || response.getHits().getTotalHits().value == 0) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put(JsonKey.CONTENT, new ArrayList<>());
            responseMap.put(JsonKey.COUNT, 0);
            promise.success(responseMap);
          } else {
            Map<String, Object> responseMap = ElasticSearchHelper.getSearchResponseMap(response, searchDTO, finalFacetList);
            promise.success(responseMap);
          }
          logSearchEndTime(startTime, index, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:search: Search failed for index: " + index, e);
          promise.failure(e);
          logSearchEndTime(startTime, index, requestContext);
        }
      };

      ConnectionManager.getRestClient().searchAsync(searchRequest, RequestOptions.DEFAULT, listener);

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:search: Failed to prepare/submit search request for index: " + index, e);
      promise.failure(e);
      logSearchEndTime(startTime, index, requestContext);
    }

    return promise.future();
  }

  /**
   * Performs a health check on Elasticsearch by verifying index existence.
   *
   * @return A Future containing true if Elasticsearch is healthy, false otherwise.
   */
  @Override
  public Future<Boolean> healthCheck() {
    Promise<Boolean> promise = Futures.promise();
    
    try {
      GetIndexRequest indexRequest = new GetIndexRequest()
          .indices(ProjectUtil.EsType.courseBatch.getTypeName(), ProjectUtil.EsType.user.getTypeName())
          .indicesOptions(IndicesOptions.fromOptions(true, true, true, false));
      
      ActionListener<Boolean> listener = new ActionListener<Boolean>() {
        @Override
        public void onResponse(Boolean getResponse) {
          promise.success(getResponse != null ? getResponse : false);
          logger.info("ElasticSearchRestHighImpl:healthCheck: Health check successful, index exists: " + getResponse);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error("ElasticSearchRestHighImpl:healthCheck: Health check failed", e);
          promise.failure(e);
        }
      };
      
      ConnectionManager.getRestClient().indices().existsAsync(indexRequest, RequestOptions.DEFAULT, listener);
      
    } catch (Exception e) {
      logger.error("ElasticSearchRestHighImpl:healthCheck: Failed to prepare health check request", e);
      promise.failure(e);
    }

    return promise.future();
  }

  /**
   * Performs bulk insertion of documents into Elasticsearch.
   *
   * @param index          The name of the index.
   * @param dataList       List of documents to insert.
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing true if bulk insert succeeds, false otherwise.
   */
  @Override
  public Future<Boolean> bulkInsert(String index, List<Map<String, Object>> dataList, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:bulkInsert: method started at ==" + startTime + " for Index " + index);

    if (StringUtils.isBlank(index) || dataList == null || dataList.isEmpty()) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:bulkInsert: Invalid parameters - index: " + index 
          + ", dataList size: " + (dataList == null ? "null" : dataList.size()));
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidData));
      return promise.future();
    }

    try {
      BulkRequest request = new BulkRequest();
      
      for (Map<String, Object> data : dataList) {
        String id = (String) data.get(JsonKey.ID);
        if (StringUtils.isNotBlank(id)) {
          data.put(JsonKey.IDENTIFIER, id);
          request.add(new IndexRequest(index, _DOC, id).source(data));
        } else {
          logger.warn(requestContext, "ElasticSearchRestHighImpl:bulkInsert: Skipping document without ID", null);
        }
      }

      ActionListener<BulkResponse> listener = new ActionListener<BulkResponse>() {
        @Override
        public void onResponse(BulkResponse bulkResponse) {
          boolean hasFailures = false;
          Iterator<BulkItemResponse> responseItr = bulkResponse.iterator();
          
          while (responseItr.hasNext()) {
            BulkItemResponse bResponse = responseItr.next();
            if (bResponse.isFailed()) {
              hasFailures = true;
              logger.warn(requestContext, "ElasticSearchRestHighImpl:bulkInsert: Failed to index document - ID: " 
                  + bResponse.getId() + ", Failure: " + bResponse.getFailureMessage(), null);
            }
          }
          
          if (!hasFailures) {
            logger.info(requestContext, "ElasticSearchRestHighImpl:bulkInsert: Successfully inserted " 
                + dataList.size() + " documents into index: " + index);
          }
          
          promise.success(true);
          logBulkInsertEndTime(startTime, index, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:bulkInsert: Bulk upload failed for index: " + index, e);
          promise.success(false);
          logBulkInsertEndTime(startTime, index, requestContext);
        }
      };

      ConnectionManager.getRestClient().bulkAsync(request, RequestOptions.DEFAULT, listener);

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:bulkInsert: Failed to prepare/submit bulk request for index: " + index, e);
      promise.success(false);
      logBulkInsertEndTime(startTime, index, requestContext);
    }

    return promise.future();
  }

  /**
   * Adds aggregations to the SearchSourceBuilder based on facet configurations.
   * Supports date histogram and terms aggregations.
   *
   * @param searchSourceBuilder The SearchSourceBuilder to add aggregations to.
   * @param facets              List of facet configurations (map of field names to aggregation types).
   * @param requestContext      The RequestContext for logging (can be null).
   * @return The updated SearchSourceBuilder with aggregations added.
   */
  private static SearchSourceBuilder addAggregations(SearchSourceBuilder searchSourceBuilder, 
                                                     List<Map<String, String>> facets, 
                                                     RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    logger.debug(requestContext, "ElasticSearchRestHighImpl:addAggregations: method started at ==" + startTime);
    
    if (CollectionUtils.isNotEmpty(facets)) {
      Map<String, String> map = facets.get(0);
      for (Map.Entry<String, String> entry : map.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        
        if (JsonKey.DATE_HISTOGRAM.equalsIgnoreCase(value)) {
          searchSourceBuilder.aggregation(
              AggregationBuilders.dateHistogram(key)
                  .field(key + ElasticSearchHelper.RAW_APPEND)
                  .dateHistogramInterval(DateHistogramInterval.days(1)));
        } else if (null == value) {
          searchSourceBuilder.aggregation(
              AggregationBuilders.terms(key).field(key + ElasticSearchHelper.RAW_APPEND));
        }
      }
    }
    
    logger.debug(requestContext, "ElasticSearchRestHighImpl:addAggregations: method end, Total time elapsed = " 
        + ElasticSearchHelper.calculateEndTime(startTime));
    return searchSourceBuilder;
  }

  /**
   * Performs an upsert operation (update if exists, insert if not) on Elasticsearch.
   *
   * @param index          The name of the index.
   * @param identifier     The unique identifier for the document.
   * @param data           The data to upsert.
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing true if upsert succeeds, or failure on error.
   */
  @Override
  public Future<Boolean> upsert(String index, String identifier, Map<String, Object> data, RequestContext requestContext) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:upsert: method started at ==" + startTime + " for Index " + index);

    if (StringUtils.isBlank(index) || StringUtils.isBlank(identifier) || data == null || data.isEmpty()) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:upsert: Invalid parameters - index: " + index 
          + ", identifier: " + identifier + ", data: " + (data == null ? "null" : "size=" + data.size()));
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidData));
      return promise.future();
    }

    try {
      data.put(JsonKey.IDENTIFIER, identifier);
      IndexRequest indexRequest = new IndexRequest(index, _DOC, identifier).source(data);
      UpdateRequest updateRequest = new UpdateRequest(index, _DOC, identifier).upsert(indexRequest).doc(indexRequest);

      ActionListener<UpdateResponse> listener = new ActionListener<UpdateResponse>() {
        @Override
        public void onResponse(UpdateResponse updateResponse) {
          logger.info(requestContext, "ElasticSearchRestHighImpl:upsert: Success with result: " + updateResponse.getResult()
              + " for index: " + index + ", identifier: " + identifier);
          promise.success(true);
          logUpsertEndTime(startTime, index, requestContext);
        }

        @Override
        public void onFailure(Exception e) {
          logger.error(requestContext, "ElasticSearchRestHighImpl:upsert: Failed to upsert document in index: " 
              + index + ", identifier: " + identifier, e);
          promise.failure(e);
          logUpsertEndTime(startTime, index, requestContext);
        }
      };

      ConnectionManager.getRestClient().updateAsync(updateRequest, RequestOptions.DEFAULT, listener);

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:upsert: Failed to prepare/submit upsert request for index: " 
          + index + ", identifier: " + identifier, e);
      promise.failure(e);
      logUpsertEndTime(startTime, index, requestContext);
    }

    return promise.future();
  }

  /**
   * Retrieves multiple documents by their IDs with specified fields.
   *
   * @param ids            List of document IDs to retrieve.
   * @param fields         List of fields to include in the results.
   * @param index          The name of the index.
   * @param requestContext The RequestContext for logging and tracing.
   * @return A Future containing a map of document ID to document data.
   */
  @Override
  public Future<Map<String, Map<String, Object>>> getEsResultByListOfIds(List<String> ids, List<String> fields, 
                                                                          String index, RequestContext requestContext) {
    Promise<Map<String, Map<String, Object>>> promise = Futures.promise();

    logger.debug(requestContext, "ElasticSearchRestHighImpl:getEsResultByListOfIds: method started for index " + index);

    if (ids == null || ids.isEmpty() || StringUtils.isBlank(index)) {
      logger.info(requestContext, "ElasticSearchRestHighImpl:getEsResultByListOfIds: Invalid parameters - index: " + index 
          + ", ids size: " + (ids == null ? "null" : ids.size()));
      promise.success(new HashMap<>());
      return promise.future();
    }

    try {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.ID, ids);

      SearchDTO searchDTO = new SearchDTO();
      searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
      searchDTO.setFields(fields);

      Future<Map<String, Object>> resultF = search(searchDTO, index, requestContext);
      Map<String, Object> result = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
      List<Map<String, Object>> esContent = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);

      if (esContent != null && !esContent.isEmpty()) {
        Map<String, Map<String, Object>> resultMap = esContent.stream()
            .collect(Collectors.toMap(
                obj -> (String) obj.get(JsonKey.ID),
                val -> val
            ));
        promise.success(resultMap);
        logger.info(requestContext, "ElasticSearchRestHighImpl:getEsResultByListOfIds: Retrieved " 
            + resultMap.size() + " documents for index " + index);
      } else {
        promise.success(new HashMap<>());
        logger.info(requestContext, "ElasticSearchRestHighImpl:getEsResultByListOfIds: No documents found for index " + index);
      }

    } catch (Exception e) {
      logger.error(requestContext, "ElasticSearchRestHighImpl:getEsResultByListOfIds: Failed to retrieve documents for index: " + index, e);
      promise.success(new HashMap<>());
    }

    return promise.future();
  }

  private void logUpsertEndTime(long startTime, String index, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:upsert: method end for Index " + index 
        + ", Total time elapsed = " + ElasticSearchHelper.calculateEndTime(startTime));
  }

   private void logEndTime(long startTime, String index, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:save: method end at ==" + System.currentTimeMillis()
        + " for Index " + index + " ,Total time elapsed = " + ElasticSearchHelper.calculateEndTime(startTime));
  }

  private void logUpdateEndTime(long startTime, String index, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:update: method end for Index " + index 
        + ", Total time elapsed = " + ElasticSearchHelper.calculateEndTime(startTime));
  }

  private void logGetEndTime(long startTime, String index, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:getDataByIdentifier: method end for Index " + index 
        + ", Total time elapsed = " + ElasticSearchHelper.calculateEndTime(startTime));
  }

    private void logDeleteEndTime(long startTime, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:delete: method end, Total time elapsed = " 
        + ElasticSearchHelper.calculateEndTime(startTime));
  }

  private void logSearchEndTime(long startTime, String index, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:search: method end for Index " + index 
        + ", Total time elapsed = " + ElasticSearchHelper.calculateEndTime(startTime));
  }

    private void logBulkInsertEndTime(long startTime, String index, RequestContext requestContext) {
    logger.debug(requestContext, "ElasticSearchRestHighImpl:bulkInsert: method end for Index " + index 
        + ", Total time elapsed = " + ElasticSearchHelper.calculateEndTime(startTime));
  }

}
