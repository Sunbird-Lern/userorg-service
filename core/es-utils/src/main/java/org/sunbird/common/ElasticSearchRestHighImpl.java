package org.sunbird.common;

import org.apache.pekko.dispatch.Futures;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
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
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ConnectionManager;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class will provide all required operation for elastic search.
 *
 * @author github.com/iostream04
 */
public class ElasticSearchRestHighImpl implements ElasticSearchService {
  private static final String ERROR = "ERROR";
  private static final LoggerUtil logger = new LoggerUtil(ElasticSearchRestHighImpl.class);

//  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * This method will put a new data entry inside Elastic search. identifier value becomes _id
   * inside ES, so every time provide a unique value while saving it.
   *
   * @param index String ES index name
   * @param identifier ES column identifier as an String
   * @param data Map<String,Object>
   * @param context
   * @return Future<String> which contains identifier for created data
   */
  @Override
  public Future<String> save(String index, String identifier, Map<String, Object> data, RequestContext context) {
    long startTime = System.currentTimeMillis();
    Promise<String> promise = (Promise<String>) (Promise<?>) Futures.promise();

    logger.debug(context, "ElasticSearchUtilRest:save: method started at ==" + startTime + " for Index " + index);
    if (StringUtils.isBlank(identifier) || StringUtils.isBlank(index)) {
      logger.info(context, "ElasticSearchRestHighImpl:save: "
              + "Identifier or Index value is null or empty, identifier : " + identifier
              + ",index: " + index + ",not able to save data.");
      promise.success(ERROR);
      return promise.future();
    }
    data.put("identifier", identifier);

    IndexRequest indexRequest = new IndexRequest(index, _DOC, identifier).source(data);

    ActionListener<IndexResponse> listener =
        new ActionListener<IndexResponse>() {
          @Override
          public void onResponse(IndexResponse indexResponse) {
            logger.info(context, "ElasticSearchRestHighImpl:save: Success for index : " + index
                    + ", identifier :" + identifier);

            promise.success(indexResponse.getId());
            logger.debug(context, "ElasticSearchRestHighImpl:save: method end at ==" + System.currentTimeMillis()
                    + " for Index " + index
                    + " ,Total time elapsed = " + calculateEndTime(startTime));
          }

          @Override
          public void onFailure(Exception e) {
            promise.failure(e);
            logger.error(context, "ElasticSearchRestHighImpl:save: "
                    + "Error while saving " + index
                    + " id : " + identifier, e);
            logger.debug(context, "ElasticSearchRestHighImpl:save: method end at ==" + System.currentTimeMillis()
                    + " for INdex " + index
                    + " ,Total time elapsed = " + calculateEndTime(startTime));
          }
        };

    ConnectionManager.getRestClient().indexAsync(indexRequest, RequestOptions.DEFAULT, listener);

    return promise.future();
  }

  /**
   * This method will update data entry inside Elastic search, using identifier and provided data .
   *
   * @param index String ES index name
   * @param documentId ES column identifier as an String
   * @param document Map<String,Object>
   * @param context
   * @return true or false
   */
  @Override
  public Future<Boolean> update(String index, String documentId, Map<String, Object> document, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "ElasticSearchRestHighImpl:update: method started at ==" + startTime
            + " for Index " + index);
    Promise<Boolean> promise = (Promise<Boolean>) (Promise<?>) Futures.promise();
    document.put("identifier", documentId);

    if (!StringUtils.isBlank(index) && !StringUtils.isBlank(documentId)) {
//      Map<String, Object> updatedDoc = checkDocStringLength(document);
      IndexRequest indexRequest = new IndexRequest(index).id(documentId).source(document);
      UpdateRequest updateRequest = new UpdateRequest().index(index).id(documentId).doc(document).upsert(indexRequest);

      ActionListener<UpdateResponse> listener =
          new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
              promise.success(true);
              logger.info(context, "ElasticSearchRestHighImpl:update:  Success with " + updateResponse.getResult()
                      + " response from elastic search for index" + index
                      + ",documentId : " + documentId);
              logger.debug(context, "ElasticSearchRestHighImpl:update: method end =="
                      + " for INdex " + index
                      + " ,Total time elapsed = " + calculateEndTime(startTime));
            }

            @Override
            public void onFailure(Exception e) {
              logger.error(context, "ElasticSearchRestHighImpl:update: exception occured:" + e.getMessage(), e);
              promise.failure(e);
            }
          };
      ConnectionManager.getRestClient().updateAsync(updateRequest, RequestOptions.DEFAULT, listener);

    } else {
      logger.info(context, "ElasticSearchRestHighImpl:update: Requested data is invalid.");
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidRequestData));
    }
    return promise.future();
  }

  /**
   * This method will provide data form ES based on incoming identifier. we can get data by passing
   * index and identifier values , or all the three
   *
   * @param identifier String
   * @param context
   * @return Map<String,Object> or empty map
   */
  @Override
  public Future<Map<String, Object>> getDataByIdentifier(String index, String identifier, RequestContext context) {
    long startTime = System.currentTimeMillis();
    Promise<Map<String, Object>> promise = (Promise<Map<String, Object>>) (Promise<?>) Futures.promise();
    if (StringUtils.isNotEmpty(identifier) && StringUtils.isNotEmpty(index)) {
      logger.debug(context, "ElasticSearchRestHighImpl:getDataByIdentifier: method started at ==" + startTime
              + " for Index " + index);

      GetRequest getRequest = new GetRequest(index, _DOC, identifier);

      ActionListener<GetResponse> listener =
          new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
              if (getResponse.isExists()) {
                Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
                if (MapUtils.isNotEmpty(sourceAsMap)) {
                  promise.success(sourceAsMap);
                  logger.debug(context, "ElasticSearchRestHighImpl:getDataByIdentifier: method end == for Index " + index
                          + " ,Total time elapsed = " + calculateEndTime(startTime));
                } else {
                  promise.success(new HashMap<>());
                }
              } else {
                promise.success(new HashMap<>());
              }
            }

            @Override
            public void onFailure(Exception e) {
              logger.error(context, "ElasticSearchRestHighImpl:getDataByIdentifier: method Failed with error == ", e);
              promise.failure(e);
            }
          };

      ConnectionManager.getRestClient().getAsync(getRequest, RequestOptions.DEFAULT, listener);
    } else {
      logger.info(context, "ElasticSearchRestHighImpl:getDataByIdentifier:  "
              + "provided index or identifier is null, index = " + index
              + ", identifier = " + identifier);
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidRequestData));
    }

    return promise.future();
  }

  /**
   * This method will remove data from ES based on identifier.
   *
   * @param index String
   * @param identifier String
   * @param context
   */
  @Override
  public Future<Boolean> delete(String index, String identifier, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "ElasticSearchRestHighImpl:delete: method started at ==" + startTime);
    Promise<Boolean> promise = (Promise<Boolean>) (Promise<?>) Futures.promise();
    if (StringUtils.isNotEmpty(identifier) && StringUtils.isNotEmpty(index)) {
      DeleteRequest delRequest = new DeleteRequest(index, _DOC, identifier);
      ActionListener<DeleteResponse> listener =
          new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
              if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                logger.info(context,
                    "ElasticSearchRestHighImpl:delete:OnResponse: Document  not found for index : " + index
                        + " , identifier : " + identifier);
                promise.success(false);
              } else {
                promise.success(true);
              }
            }

            @Override
            public void onFailure(Exception e) {
              logger.error(context, "ElasticSearchRestHighImpl:delete: Async Failed due to error :", e);
              promise.failure(e);
            }
          };

      ConnectionManager.getRestClient().deleteAsync(delRequest, RequestOptions.DEFAULT, listener);
    } else {
      logger.info(context, "ElasticSearchRestHighImpl:delete:  "
              + "provided index or identifier is null, index = " + index
              + ", identifier = " + identifier);
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidRequestData));
    }

    logger.debug(context, "ElasticSearchRestHighImpl:delete: method end =="
            + " ,Total time elapsed = " + calculateEndTime(startTime));
    return promise.future();
  }

  /**
   * Method to perform the elastic search on the basis of SearchDTO . SearchDTO contains the search
   * criteria like fields, facets, sort by , filters etc. here user can pass single type to search
   * or multiple type or null
   *
   * @param context
   * @return search result as Map.
   */
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Future<Map<String, Object>> search(SearchDTO searchDTO, String index, RequestContext context) {
    long startTime = System.currentTimeMillis();

    logger.debug(context, "ElasticSearchRestHighImpl:search: method started at ==" + startTime);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    SearchRequest searchRequest = new SearchRequest(index);
    searchRequest.types(_DOC);

    // check mode and set constraints
    Map<String, Float> constraintsMap = ElasticSearchHelper.getConstraints(searchDTO);

    BoolQueryBuilder query = new BoolQueryBuilder();

    // add channel field as mandatory
    String channel = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_ES_CHANNEL);
    if (!(StringUtils.isBlank(channel) || JsonKey.SUNBIRD_ES_CHANNEL.equals(channel))) {
      query.must(QueryBuilders.matchQuery(JsonKey.CHANNEL, channel));
    }

    // apply simple query string
    if (!StringUtils.isBlank(searchDTO.getQuery())) {
      SimpleQueryStringBuilder sqsb = QueryBuilders.simpleQueryStringQuery(searchDTO.getQuery());
      query.must(sqsb);
      if (CollectionUtils.isNotEmpty(searchDTO.getQueryFields())) {
        Map<String, Float> searchFields =
            searchDTO.getQueryFields()
                .stream()
                .collect(Collectors.<String, String, Float>toMap(s -> s, v -> 1.0f));
        query.must(sqsb.fields(searchFields));
      }
    }
    // apply the sorting
    if (searchDTO.getSortBy() != null && searchDTO.getSortBy().size() > 0) {
      for (Map.Entry<String, Object> entry : searchDTO.getSortBy().entrySet()) {
        if (!entry.getKey().contains(".")) {
          searchSourceBuilder.sort(entry.getKey() + ElasticSearchHelper.RAW_APPEND,
              ElasticSearchHelper.getSortOrder((String) entry.getValue()));
        } else {
          Map<String, Object> map = (Map<String, Object>) entry.getValue();
          Map<String, String> dataMap = (Map) map.get(JsonKey.TERM);
          for (Map.Entry<String, String> dateMapEntry : dataMap.entrySet()) {
            FieldSortBuilder mySort =
                new FieldSortBuilder(entry.getKey() + ElasticSearchHelper.RAW_APPEND)
                    .setNestedFilter(new TermQueryBuilder(dateMapEntry.getKey(), dateMapEntry.getValue()))
                    .sortMode(SortMode.MIN)
                    .order(ElasticSearchHelper.getSortOrder((String) map.get(JsonKey.ORDER)));
            searchSourceBuilder.sort(mySort);
          }
        }
      }
    }

    // apply the fields filter
    searchSourceBuilder.fetchSource(
        searchDTO.getFields() != null
            ? searchDTO.getFields().stream().toArray(String[]::new)
            : null,
        searchDTO.getExcludedFields() != null
            ? searchDTO.getExcludedFields().stream().toArray(String[]::new)
            : null);

    // setting the offset
    if (searchDTO.getOffset() != null) {
      searchSourceBuilder.from(searchDTO.getOffset());
    }

    // setting the limit
    if (searchDTO.getLimit() != null) {
      searchSourceBuilder.size(searchDTO.getLimit());
    }
    // apply additional properties
    if (searchDTO.getAdditionalProperties() != null && searchDTO.getAdditionalProperties().size() > 0) {
      for (Map.Entry<String, Object> entry : searchDTO.getAdditionalProperties().entrySet()) {
        ElasticSearchHelper.addAdditionalProperties(query, entry, constraintsMap);
      }
    }

    // do fuzzy search
    if (MapUtils.isNotEmpty(searchDTO.getFuzzy())) {
      Map.Entry<String, String> entry = searchDTO.getFuzzy().entrySet().iterator().next();
      ElasticSearchHelper.createFuzzyMatchQuery(query, entry.getKey(), entry.getValue());
    }
    // set final query to search request builder
    searchSourceBuilder.query(query);

    List finalFacetList = new ArrayList();

    if (null != searchDTO.getFacets() && !searchDTO.getFacets().isEmpty()) {
      searchSourceBuilder = addAggregations(searchSourceBuilder, searchDTO.getFacets());
    }
    logger.info(context, "ElasticSearchRestHighImpl:search: calling search for index " + index
            + ", with query = " + searchSourceBuilder.toString());

    searchRequest.source(searchSourceBuilder);
    Promise<Map<String, Object>> promise = (Promise<Map<String, Object>>) (Promise<?>) Futures.promise();

    ActionListener<SearchResponse> listener =
        new ActionListener<SearchResponse>() {
          @Override
          public void onResponse(SearchResponse response) {
            logger.debug(context, "ElasticSearchRestHighImpl:search:onResponse  response1 = " + response);
            if (response.getHits() == null || response.getHits().getTotalHits().value == 0) {

              Map<String, Object> responseMap = new HashMap<>();
              List<Map<String, Object>> esSource = new ArrayList<>();
              responseMap.put(JsonKey.CONTENT, esSource);
              responseMap.put(JsonKey.COUNT, 0);
              promise.success(responseMap);
            } else {
              Map<String, Object> responseMap = ElasticSearchHelper.getSearchResponseMap(response, searchDTO, finalFacetList);
              logger.debug(context, "ElasticSearchRestHighImpl:search: method end "
                      + " ,Total time elapsed = " + calculateEndTime(startTime));
              promise.success(responseMap);
            }
          }

          @Override
          public void onFailure(Exception e) {
            promise.failure(e);

            logger.debug(context, "ElasticSearchRestHighImpl:search: method end   for Index " + index
                    + " ,Total time elapsed = " + calculateEndTime(startTime));
            logger.error(context, "ElasticSearchRestHighImpl:search: method Failed with error :", e);
          }
        };

    ConnectionManager.getRestClient().searchAsync(searchRequest, RequestOptions.DEFAULT, listener);
    return promise.future();
  }

  /**
   * This method will do the health check of elastic search.
   *
   * @return boolean
   */
  @Override
  public Future<Boolean> healthCheck() {
    GetIndexRequest indexRequest = new GetIndexRequest().indices(ProjectUtil.EsType.user.getTypeName());
    Promise<Boolean> promise = (Promise<Boolean>) (Promise<?>) Futures.promise();
    ActionListener<Boolean> listener =
        new ActionListener<Boolean>() {
          @Override
          public void onResponse(Boolean getResponse) {
            if (getResponse) {
              promise.success(getResponse);
            } else {
              promise.success(false);
            }
          }

          @Override
          public void onFailure(Exception e) {
            promise.failure(e);
            logger.error("ElasticSearchRestHighImpl:healthCheck: error " + e.getMessage(), e);
          }
        };
    ConnectionManager.getRestClient().indices().existsAsync(indexRequest, RequestOptions.DEFAULT, listener);

    return promise.future();
  }

  /**
   * This method will do the bulk data insertion.
   *
   * @param index String index name
   * @param dataList List<Map<String, Object>>
   * @param context
   * @return boolean
   */
  @Override
  public Future<Boolean> bulkInsert(String index, List<Map<String, Object>> dataList, RequestContext context) {
    long startTime = System.currentTimeMillis();
    logger.debug(context, "ElasticSearchRestHighImpl:bulkInsert: method started at ==" + startTime
            + " for Index " + index);
    BulkRequest request = new BulkRequest();
    Promise<Boolean> promise = (Promise<Boolean>) (Promise<?>) Futures.promise();
    for (Map<String, Object> data : dataList) {
      data.put("identifier", data.get(JsonKey.ID));
      request.add(new IndexRequest(index, _DOC, (String) data.get(JsonKey.ID)).source(data));
    }
    ActionListener<BulkResponse> listener =
        new ActionListener<BulkResponse>() {
          @Override
          public void onResponse(BulkResponse bulkResponse) {
            Iterator<BulkItemResponse> responseItr = bulkResponse.iterator();
            if (responseItr != null) {
              promise.success(true);
              while (responseItr.hasNext()) {

                BulkItemResponse bResponse = responseItr.next();

                if (bResponse.isFailed()) {
                  logger.info(context, "ElasticSearchRestHighImpl:bulkinsert: api response===" + bResponse.getId()
                          + " " + bResponse.getFailureMessage());
                }
              }
            }
          }

          @Override
          public void onFailure(Exception e) {
            logger.error(context, "ElasticSearchRestHighImpl:bulkinsert: Bulk upload error block", e);
            promise.success(false);
          }
        };
    ConnectionManager.getRestClient().bulkAsync(request, RequestOptions.DEFAULT, listener);

    logger.debug(context, "ElasticSearchRestHighImpl:bulkInsert: method end == for Index " + index
            + " ,Total time elapsed = " + calculateEndTime(startTime));
    return promise.future();
  }

  private static long calculateEndTime(long startTime) {
    return System.currentTimeMillis() - startTime;
  }

  private static SearchSourceBuilder addAggregations(SearchSourceBuilder searchSourceBuilder, List<Map<String, String>> facets) {
    long startTime = System.currentTimeMillis();
    logger.debug(null, "ElasticSearchRestHighImpl:addAggregations: method started at ==" + startTime);
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
    logger.debug(null, "ElasticSearchRestHighImpl:addAggregations: method end =="
            + " ,Total time elapsed = " + calculateEndTime(startTime));
    return searchSourceBuilder;
  }

  /**
   * This method will update data based on identifier.take the data based on identifier and merge
   * with incoming data then update it.
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @param context
   * @return boolean
   */
  @Override
  public Future<Boolean> upsert(String index, String identifier, Map<String, Object> data, RequestContext context) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = (Promise<Boolean>) (Promise<?>) Futures.promise();
    logger.debug(context, "ElasticSearchRestHighImpl:upsert: method started at ==" + startTime
            + " for INdex " + index);
    if (!StringUtils.isBlank(index)
        && !StringUtils.isBlank(identifier)
        && data != null
        && data.size() > 0) {
      data.put("identifier", identifier);
      IndexRequest indexRequest = new IndexRequest(index, _DOC, identifier).source(data);

      UpdateRequest updateRequest = new UpdateRequest(index, _DOC, identifier).upsert(indexRequest);
      updateRequest.doc(indexRequest);
      ActionListener<UpdateResponse> listener =
          new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
              promise.success(true);
              logger.info(context, "ElasticSearchRestHighImpl:upsert:  Response for index : " + updateResponse.getResult()
                      + "," + index
                      + ",identifier : " + identifier);
              logger.debug(context, "ElasticSearchRestHighImpl:upsert: method end == for Index " + index
                      + " ,Total time elapsed = " + calculateEndTime(startTime));
            }

            @Override
            public void onFailure(Exception e) {
              logger.error(context, "ElasticSearchRestHighImpl:upsert: exception occured:" + e.getMessage(), e);
              promise.failure(e);
            }
          };
      ConnectionManager.getRestClient().updateAsync(updateRequest, RequestOptions.DEFAULT, listener);
      return promise.future();
    } else {
      logger.info(context, "ElasticSearchRestHighImpl:upsert: Requested data is invalid.");
      promise.failure(ProjectUtil.createClientException(ResponseCode.invalidRequestData));
      return promise.future();
    }
  }

  /**
   * This method will return map of objects on the basis of ids provided.
   *
   * @param ids List of String
   * @param fields List of String
   * @param index index of elasticserach for query
   * @param context
   * @return future of requested data in the form of map
   */
  @Override
  public Future<Map<String, Map<String, Object>>> getEsResultByListOfIds(List<String> ids, List<String> fields, String index, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, ids);

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    searchDTO.setFields(fields);

    Future<Map<String, Object>> resultF = search(searchDTO, index, null);
    Map<String, Object> result = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    List<Map<String, Object>> esContent = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Promise<Map<String, Map<String, Object>>> promise = (Promise<Map<String, Map<String, Object>>>) (Promise<?>) Futures.promise();
    promise.success(esContent.stream().collect(Collectors.toMap(
                    obj -> {
                      return (String) obj.get("id");
                    },
                    val -> val)));
    logger.debug(context, "ElasticSearchRestHighImpl:getEsResultByListOfIds: method ended for index " + index);

    return promise.future();
  }
}
