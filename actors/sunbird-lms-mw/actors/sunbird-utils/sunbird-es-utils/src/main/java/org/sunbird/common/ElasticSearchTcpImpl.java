package org.sunbird.common;

import akka.dispatch.Futures;
import akka.util.Timeout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ConnectionManager;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * This class will provide all required operation for elastic search.
 *
 * @author github.com/iostream04
 */
public class ElasticSearchTcpImpl implements ElasticSearchService {
  public static final int WAIT_TIME = 30;
  public static Timeout timeout = new Timeout(WAIT_TIME, TimeUnit.SECONDS);

  /**
   * This method will put a new data entry inside Elastic search. identifier value becomes _id
   * inside ES, so every time provide a unique value while saving it.
   *
   * @param index String ES index name
   * @param identifier ES column identifier as an String
   * @param data Map<String,Object>
   * @return identifier for created data
   */
  @Override
  public Future<String> save(String index, String identifier, Map<String, Object> data) {
    long startTime = System.currentTimeMillis();
    Promise<String> promise = Futures.promise();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:save: method started at ==" + startTime + " for Index " + index,
        LoggerEnum.PERF_LOG.name());
    if (StringUtils.isBlank(identifier) || StringUtils.isBlank(index)) {
      ProjectLogger.log(
          "ElasticSearchTcpImpl:save: Identifier value is null or empty ,not able to save data.",
          LoggerEnum.ERROR.name());
      promise.success("ERROR");
      return promise.future();
    }
    try {
      data.put("identifier", identifier);
      IndexResponse response =
          ConnectionManager.getClient().prepareIndex(index, _DOC, identifier).setSource(data).get();
      ProjectLogger.log(
          "ElasticSearchTcpImpl:save: "
              + "Save value=="
              + response.getId()
              + " "
              + response.status(),
          LoggerEnum.INFO.name());
      ProjectLogger.log(
          "ElasticSearchTcpImpl:save: method end at =="
              + System.currentTimeMillis()
              + " for INdex "
              + index
              + " ,Total time elapsed = "
              + ElasticSearchHelper.calculateEndTime(startTime),
          LoggerEnum.PERF_LOG.name());
      promise.success(response.getId());
      return promise.future();
    } catch (Exception e) {
      ProjectLogger.log(
          "ElasticSearchTcpImpl:save: Error while saving index "
              + index
              + " id : "
              + identifier
              + " with error :"
              + e,
          LoggerEnum.INFO.name());
      ProjectLogger.log(
          "ElasticSearchTcpImpl:save: method end at =="
              + System.currentTimeMillis()
              + " for Index "
              + index
              + " ,Total time elapsed = "
              + ElasticSearchHelper.calculateEndTime(startTime),
          LoggerEnum.PERF_LOG.name());
      promise.success("");
    }
    return promise.future();
  }

  /**
   * This method will provide data form ES based on incoming identifier. we can get data by passing
   * index and identifier values , or all the three
   *
   * @param identifier String
   * @return Map<String,Object> or empty map
   */
  @Override
  public Future<Map<String, Object>> getDataByIdentifier(String index, String identifier) {
    long startTime = System.currentTimeMillis();
    Promise<Map<String, Object>> promise = Futures.promise();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:getDataByIdentifier: method started at =="
            + startTime
            + " for index "
            + index,
        LoggerEnum.PERF_LOG.name());
    GetResponse response = null;
    if (StringUtils.isBlank(index) || StringUtils.isBlank(identifier)) {
      ProjectLogger.log(
          "ElasticSearchTcpImpl:getDataByIdentifier: Invalid request is coming.",
          LoggerEnum.INFO.name());
      promise.success(new HashMap<>());
      return promise.future();
    } else {
      response = ConnectionManager.getClient().prepareGet(index, _DOC, identifier).get();
    }
    if (response == null || null == response.getSource()) {
      promise.success(new HashMap<>());
      return promise.future();
    }
    ProjectLogger.log(
        "ElasticSearchTcpImpl:getDataByIdentifier: method "
            + " for index "
            + index
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG.name());
    promise.success(response.getSource());
    return promise.future();
  }

  /**
   * This method will update data based on identifier.take the data based on identifier and merge
   * with incoming data then update it.
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  @Override
  public Future<Boolean> update(String index, String identifier, Map<String, Object> data) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:update: method started at ==" + startTime + " for index " + index,
        LoggerEnum.PERF_LOG.name());
    if (!StringUtils.isBlank(index) && !StringUtils.isBlank(identifier) && data != null) {
      try {
        UpdateResponse response =
            ConnectionManager.getClient().prepareUpdate(index, _DOC, identifier).setDoc(data).get();
        ProjectLogger.log(
            "ElasticSearchTcpImpl:update: " + "updated response==" + response.getResult().name(),
            LoggerEnum.INFO.name());
        if (response.getResult().name().equals("UPDATED")) {
          ProjectLogger.log(
              "ElasticSearchTcpImpl:update: method end =="
                  + " for index "
                  + index
                  + " ,Total time elapsed = "
                  + ElasticSearchHelper.calculateEndTime(startTime),
              LoggerEnum.PERF_LOG.name());
          promise.success(true);
          return promise.future();
        } else {
          ProjectLogger.log(
              "ElasticSearchTcpImpl:update: update was not success:" + response.getResult(),
              LoggerEnum.INFO.name());
        }
      } catch (Exception e) {
        ProjectLogger.log(
            "ElasticSearchTcpImpl:update: exception occured:" + e.getMessage(),
            LoggerEnum.ERROR.name());
        promise.failure(e);
      }
    } else {
      ProjectLogger.log(
          "ElasticSearchTcpImpl:update: Requested data is invalid.", LoggerEnum.INFO.name());
    }
    ProjectLogger.log(
        "ElasticSearchTcpImpl:update: method end  =="
            + " for Index "
            + index
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG.name());
    promise.success(false);
    return promise.future();
  }

  /**
   * This method will upsert data based on identifier.take the data based on identifier and merge
   * with incoming data then update it and if identifier does not exist , it will insert data .
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  @Override
  public Future<Boolean> upsert(String index, String identifier, Map<String, Object> data) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:upsert: method started at ==" + startTime + " for INdex " + index,
        LoggerEnum.PERF_LOG.name());
    if (!StringUtils.isBlank(index)
        && !StringUtils.isBlank(identifier)
        && data != null
        && data.size() > 0) {
      IndexRequest indexRequest = new IndexRequest(index, _DOC, identifier).source(data);
      UpdateRequest updateRequest =
          new UpdateRequest(index, _DOC, identifier).doc(data).upsert(indexRequest);
      UpdateResponse response = null;
      try {
        response = ConnectionManager.getClient().update(updateRequest).get();
      } catch (InterruptedException | ExecutionException e) {
        ProjectLogger.log("ElasticSearchTcpImpl:upsert: error occured == " + e.getMessage(), e);
        promise.success(false);
        return promise.future();
      }
      ProjectLogger.log(
          "ElasticSearchTcpImpl:upsert: updated response==" + response.getResult().name(),
          LoggerEnum.INFO.name());
      if (ElasticSearchHelper.upsertResults.contains(response.getResult().name())) {
        ProjectLogger.log(
            "ElasticSearchTcpImpl:upsert: method end =="
                + " for index "
                + index
                + " ,Total time elapsed = "
                + ElasticSearchHelper.calculateEndTime(startTime),
            LoggerEnum.PERF_LOG.name());
        promise.success(true);
        return promise.future();
      }
    } else {
      ProjectLogger.log(
          "ElasticSearchTcpImpl:upsert: Requested data is invalid.", LoggerEnum.INFO.name());
      promise.success(false);
    }
    ProjectLogger.log(
        "ElasticSearchTcpImpl:upsert: method end =="
            + " for index "
            + index
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG.name());
    return promise.future();
  }

  /**
   * This method will remove data from ES based on identifier.
   *
   * @param index String
   * @param identifier String
   */
  @Override
  public Future<Boolean> delete(String index, String identifier) {
    long startTime = System.currentTimeMillis();
    Promise<Boolean> promise = Futures.promise();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:delete: method started at ==" + startTime,
        LoggerEnum.PERF_LOG.name());
    DeleteResponse deleteResponse = null;
    if (!StringUtils.isBlank(index) && !StringUtils.isBlank(identifier)) {
      try {
        deleteResponse = ConnectionManager.getClient().prepareDelete(index, _DOC, identifier).get();
        ProjectLogger.log(
            "ElasticSearchTcpImpl:delete: info =="
                + deleteResponse.getResult().name()
                + " "
                + deleteResponse.getId(),
            LoggerEnum.INFO.name());
      } catch (Exception e) {
        promise.failure(e);
        ProjectLogger.log(
            "ElasticSearchTcpImpl:delete: error occured for index and identifier  == "
                + index
                + " and "
                + identifier
                + " with error "
                + e.getMessage(),
            e);
      }
    } else {
      ProjectLogger.log(
          "ElasticSearchTcpImpl:delete: Data can not be deleted due to invalid input.",
          LoggerEnum.INFO.name());
      promise.success(false);
      return promise.future();
    }
    ProjectLogger.log(
        "ElasticSearchTcpImpl:delete: method end =="
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG.name());
    promise.success(deleteResponse.getResult().name().equalsIgnoreCase("DELETED"));
    return promise.future();
  }

  /**
   * Method to perform the elastic search on the basis of SearchDTO . SearchDTO contains the search
   * criteria like fields, facets, sort by ,range, filters etc.
   *
   * @return search result as Map.
   */
  @Override
  public Future<Map<String, Object>> search(SearchDTO searchDTO, String index) {

    long startTime = System.currentTimeMillis();
    Promise<Map<String, Object>> promise = Futures.promise();
    String[] indices = {index};

    ProjectLogger.log(
        "ElasticSearchTcpImpl:search: method started at ==" + startTime,
        LoggerEnum.PERF_LOG.name());
    SearchRequestBuilder searchRequestBuilder =
        ElasticSearchHelper.getTransportSearchBuilder(ConnectionManager.getClient(), indices);
    // check mode and set constraints
    Map<String, Float> constraintsMap = ElasticSearchHelper.getConstraints(searchDTO);

    BoolQueryBuilder query = new BoolQueryBuilder();

    // add channel field as mandatory
    String channel = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_ES_CHANNEL);
    if (!(StringUtils.isBlank(channel) || JsonKey.SUNBIRD_ES_CHANNEL.equals(channel))) {
      query.must(
          ElasticSearchHelper.createMatchQuery(
              JsonKey.CHANNEL, channel, constraintsMap.get(JsonKey.CHANNEL)));
    }

    // apply simple query string
    if (!StringUtils.isBlank(searchDTO.getQuery())) {
      SimpleQueryStringBuilder sqsb = QueryBuilders.simpleQueryStringQuery(searchDTO.getQuery());
      if (CollectionUtils.isEmpty(searchDTO.getQueryFields())) {
        query.must(sqsb.field("all_fields"));
      } else {
        Map<String, Float> searchFields =
            searchDTO
                .getQueryFields()
                .stream()
                .collect(Collectors.<String, String, Float>toMap(s -> s, v -> 1.0f));
        query.must(sqsb.fields(searchFields));
      }
    }
    // apply the sorting
    if (searchDTO.getSortBy() != null && searchDTO.getSortBy().size() > 0) {
      for (Map.Entry<String, Object> entry : searchDTO.getSortBy().entrySet()) {
        if (!entry.getKey().contains(".")) {
          searchRequestBuilder.addSort(
              entry.getKey() + ElasticSearchHelper.RAW_APPEND,
              ElasticSearchHelper.getSortOrder((String) entry.getValue()));
        } else {
          Map<String, Object> map = (Map<String, Object>) entry.getValue();
          Map<String, String> dataMap = (Map) map.get(JsonKey.TERM);
          for (Map.Entry<String, String> dateMapEntry : dataMap.entrySet()) {
            FieldSortBuilder mySort =
                SortBuilders.fieldSort(entry.getKey() + ElasticSearchHelper.RAW_APPEND)
                    .setNestedFilter(
                        new TermQueryBuilder(dateMapEntry.getKey(), dateMapEntry.getValue()))
                    .sortMode(SortMode.MIN)
                    .order(ElasticSearchHelper.getSortOrder((String) map.get(JsonKey.ORDER)));
            searchRequestBuilder.addSort(mySort);
          }
        }
      }
    }

    // apply the fields filter
    searchRequestBuilder.setFetchSource(
        searchDTO.getFields() != null
            ? searchDTO.getFields().stream().toArray(String[]::new)
            : null,
        searchDTO.getExcludedFields() != null
            ? searchDTO.getExcludedFields().stream().toArray(String[]::new)
            : null);

    // setting the offset
    if (searchDTO.getOffset() != null) {
      searchRequestBuilder.setFrom(searchDTO.getOffset());
    }

    // setting the limit
    if (searchDTO.getLimit() != null) {
      searchRequestBuilder.setSize(searchDTO.getLimit());
    }
    // apply additional properties
    if (searchDTO.getAdditionalProperties() != null
        && searchDTO.getAdditionalProperties().size() > 0) {
      for (Map.Entry<String, Object> entry : searchDTO.getAdditionalProperties().entrySet()) {
        ElasticSearchHelper.addAdditionalProperties(query, entry, constraintsMap);
      }
    }

    // set final query to search request builder
    searchRequestBuilder.setQuery(query);
    List finalFacetList = new ArrayList();

    if (null != searchDTO.getFacets() && !searchDTO.getFacets().isEmpty()) {
      searchRequestBuilder =
          ElasticSearchHelper.addAggregations(searchRequestBuilder, searchDTO.getFacets());
    }
    ProjectLogger.log(
        "ElasticSearchTcpImpl:search: calling search builder ==" + searchRequestBuilder.toString(),
        LoggerEnum.INFO.name());
    SearchResponse response = null;
    try {
      response = searchRequestBuilder.execute().actionGet();
    } catch (SearchPhaseExecutionException e) {
      promise.failure(e);
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue, e.getRootCause().getMessage());
    }

    Map<String, Object> responseMap =
        ElasticSearchHelper.getSearchResponseMap(response, searchDTO, finalFacetList);
    ProjectLogger.log(
        "ElasticSearchTcpImpl:search: method end"
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG.name());
    promise.success(responseMap);
    return promise.future();
  }
  /**
   * @param List of document's ids
   * @param fields List of fields which needs to captured
   * @param index of elastic search in which search need to be done
   * @return Map<String,Map<String,Object>> It will return a map with id as key and the data from ES
   *     as value
   */
  @Override
  public Future<Map<String, Map<String, Object>>> getEsResultByListOfIds(
      List<String> ids, List<String> fields, String index) {

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, ids);

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    searchDTO.setFields(fields);

    Future<Map<String, Object>> resultF = search(searchDTO, index);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    List<Map<String, Object>> esContent = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Promise<Map<String, Map<String, Object>>> promise = Futures.promise();
    promise.success(
        esContent
            .stream()
            .collect(
                Collectors.toMap(
                    obj -> {
                      return (String) obj.get("id");
                    },
                    val -> val)));
    ProjectLogger.log(
        "ElasticSearchTcpImpl:getEsResultByListOfIds: method complete for  for index " + index,
        LoggerEnum.INFO.name());
    return promise.future();
  }

  /**
   * This method will do the bulk data insertion.
   *
   * @param index String index name
   * @param dataList List<Map<String, Object>>
   * @return boolean
   */
  @Override
  public Future<Boolean> bulkInsert(String index, List<Map<String, Object>> dataList) {
    Promise<Boolean> promise = Futures.promise();
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:bulkInsert: method started at ==" + startTime + " for index " + index,
        LoggerEnum.PERF_LOG.name());
    promise.success(true);
    try {
      BulkProcessor bulkProcessor =
          BulkProcessor.builder(
                  ConnectionManager.getClient(),
                  new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId, BulkRequest request) {}

                    @Override
                    public void afterBulk(
                        long executionId, BulkRequest request, BulkResponse response) {
                      Iterator<BulkItemResponse> bulkResponse = response.iterator();
                      if (bulkResponse != null) {
                        while (bulkResponse.hasNext()) {
                          BulkItemResponse bResponse = bulkResponse.next();
                          ProjectLogger.log(
                              "ElasticSearchTcpImpl:bulkInsert: "
                                  + "Bulk insert api response==="
                                  + bResponse.getId()
                                  + " "
                                  + bResponse.isFailed(),
                              LoggerEnum.INFO.name());
                        }
                      }
                    }

                    @Override
                    public void afterBulk(
                        long executionId, BulkRequest request, Throwable failure) {
                      ProjectLogger.log(
                          "ElasticSearchTcpImpl:bulkInsert: Bulk upload error block with error "
                              + failure,
                          LoggerEnum.INFO.name());
                    }
                  })
              .setBulkActions(10000)
              .setConcurrentRequests(0)
              .build();

      for (Map<String, Object> map : dataList) {
        map.put(JsonKey.IDENTIFIER, map.get(JsonKey.ID));
        IndexRequest request =
            new IndexRequest(index, _DOC, (String) map.get(JsonKey.IDENTIFIER)).source(map);
        bulkProcessor.add(request);
      }
      // Flush any remaining requests
      bulkProcessor.flush();

      // Or close the bulkProcessor if you don't need it anymore
      bulkProcessor.close();

      // Refresh your indices
      ConnectionManager.getClient().admin().indices().prepareRefresh().get();
    } catch (Exception e) {
      promise.success(false);
      ProjectLogger.log("ElasticSearchTcpImpl:bulkInsert: Bulk upload error " + e.getMessage(), e);
    }
    ProjectLogger.log(
        "ElasticSearchTcpImpl:bulkInsert: method end  at =="
            + System.currentTimeMillis()
            + " for index "
            + index
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG.name());
    return promise.future();
  }

  /**
   * This method will do the health check of elastic search.
   *
   * @return boolean
   */
  @Override
  public Future<Boolean> healthCheck() {
    Promise<Boolean> promise = Futures.promise();

    boolean indexResponse = false;
    try {
      indexResponse =
          ConnectionManager.getClient()
              .admin()
              .indices()
              .exists(Requests.indicesExistsRequest(ProjectUtil.EsType.user.getTypeName()))
              .get()
              .isExists();
    } catch (Exception e) {
      ProjectLogger.log("ElasticSearchTcpImpl:healthCheck: error " + e.getMessage(), e);
      promise.failure(e);
    }
    promise.success(indexResponse);
    return promise.future();
  }
}
