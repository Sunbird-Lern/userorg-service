package org.sunbird.common;

import static org.sunbird.common.models.util.ProjectUtil.isNotNull;

import akka.dispatch.Futures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.ConfigUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ConnectionManager;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * This class will provide all required operation for elastic search.
 *
 * @author arvind
 * @author Manzarul
 */
public class ElasticSearchUtil {

  private static final String LTE = "<=";
  private static final String LT = "<";
  private static final String GTE = ">=";
  private static final String GT = ">";
  private static final String ASC_ORDER = "ASC";
  public static final String STARTS_WITH = "startsWith";
  private static final String ENDS_WITH = "endsWith";
  private static final List<String> upsertResults =
      new ArrayList<>(Arrays.asList("CREATED", "UPDATED", "NOOP"));
  private static final String SOFT_MODE = "soft";
  private static final String RAW_APPEND = ".raw";
  protected static Map<String, Boolean> indexMap = new HashMap<>();
  protected static Map<String, Boolean> typeMap = new HashMap<>();
  protected static final String ES_CONFIG_FILE = "elasticsearch.conf";
  private static Config config = ConfigUtil.getConfig(ES_CONFIG_FILE);

  private ElasticSearchUtil() {}

  /**
   * This method will put a new data entry inside Elastic search. identifier value becomes _id
   * inside ES, so every time provide a unique value while saving it.
   *
   * @param index String ES index name
   * @param type String ES type name
   * @param identifier ES column identifier as an String
   * @param data Map<String,Object>
   * @return String identifier for created data
   */
  public static String createData(
      String index, String type, String identifier, Map<String, Object> data) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil createData method started at ==" + startTime + " for Type " + type,
        LoggerEnum.PERF_LOG);
    if (StringUtils.isBlank(identifier)
        || StringUtils.isBlank(type)
        || StringUtils.isBlank(index)) {
      ProjectLogger.log("Identifier value is null or empty ,not able to save data.");
      return "ERROR";
    }
    Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
    try {
      data.put("identifier", identifier);
      IndexResponse response =
          ConnectionManager.getClient()
              .prepareIndex(
                  mappedIndexAndType.get(JsonKey.INDEX),
                  mappedIndexAndType.get(JsonKey.TYPE),
                  identifier)
              .setSource(data)
              .get();
      ProjectLogger.log(
          "Save value==" + response.getId() + " " + response.status(), LoggerEnum.INFO.name());
      ProjectLogger.log(
          "ElasticSearchUtil createData method end at =="
              + System.currentTimeMillis()
              + " for Type "
              + type
              + " ,Total time elapsed = "
              + calculateEndTime(startTime),
          LoggerEnum.PERF_LOG);
      return response.getId();
    } catch (Exception e) {
      ProjectLogger.log("Error while saving " + type + " id : " + identifier, e);
      ProjectLogger.log(
          "ElasticSearchUtil createData method end at =="
              + System.currentTimeMillis()
              + " for Type "
              + type
              + " ,Total time elapsed = "
              + calculateEndTime(startTime),
          LoggerEnum.PERF_LOG);
      return "";
    }
  }

  /**
   * This method will provide data form ES based on incoming identifier. we can get data by passing
   * index and identifier values , or all the three
   *
   * @param type String
   * @param identifier String
   * @return Map<String,Object> or null
   */
  public static Map<String, Object> getDataByIdentifier(
      String index, String type, String identifier) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil getDataByIdentifier method started at =="
            + startTime
            + " for Type "
            + type,
        LoggerEnum.PERF_LOG);
    Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
    GetResponse response = null;
    if (StringUtils.isBlank(index) || StringUtils.isBlank(identifier)) {
      ProjectLogger.log("Invalid request is coming.");
      return new HashMap<>();
    } else if (StringUtils.isBlank(type)) {
      response =
          ConnectionManager.getClient()
              .prepareGet()
              .setIndex(mappedIndexAndType.get(JsonKey.INDEX))
              .setId(identifier)
              .get();
    } else {
      response =
          ConnectionManager.getClient()
              .prepareGet(
                  mappedIndexAndType.get(JsonKey.INDEX),
                  mappedIndexAndType.get(JsonKey.TYPE),
                  identifier)
              .get();
    }
    if (response == null || null == response.getSource()) {
      return new HashMap<>();
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil getDataByIdentifier method end at =="
            + stopTime
            + " for Type "
            + type
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return response.getSource();
  }

  /**
   * This method will do the data search inside ES. based on incoming search data.
   *
   * @param index String
   * @param type String
   * @param searchData Map<String,Object>
   * @return Map<String,Object>
   */
  public static Map<String, Object> searchData(
      String index, String type, Map<String, Object> searchData) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil searchData method started at ==" + startTime + " for Type " + type,
        LoggerEnum.PERF_LOG);
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    Iterator<Entry<String, Object>> itr = searchData.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, Object> entry = itr.next();
      sourceBuilder.query(QueryBuilders.commonTermsQuery(entry.getKey(), entry.getValue()));
    }
    Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
    SearchResponse sr = null;
    try {
      sr =
          ConnectionManager.getClient()
              .search(
                  new SearchRequest(mappedIndexAndType.get(JsonKey.INDEX))
                      .types(mappedIndexAndType.get(JsonKey.TYPE))
                      .source(sourceBuilder))
              .get();
    } catch (InterruptedException e) {
      ProjectLogger.log("Error, interrupted while connecting to Elasticsearch", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      ProjectLogger.log("Error while execution in Elasticsearch", e);
    }
    if (sr.getHits() == null || sr.getHits().getTotalHits() == 0) {
      return new HashMap<>();
    }
    sr.getHits().getAt(0);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil searchData method end at =="
            + stopTime
            + " for Type "
            + type
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return sr.getAggregations().asList().get(0).getMetaData();
  }

  /**
   * This method will update data based on identifier.take the data based on identifier and merge
   * with incoming data then update it.
   *
   * @param index String
   * @param type String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  public static boolean updateData(
      String index, String type, String identifier, Map<String, Object> data) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil updateData method started at ==" + startTime + " for Type " + type,
        LoggerEnum.PERF_LOG);
    if (!StringUtils.isBlank(index)
        && !StringUtils.isBlank(type)
        && !StringUtils.isBlank(identifier)
        && data != null) {
      Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
      try {
        UpdateResponse response =
            ConnectionManager.getClient()
                .prepareUpdate(
                    mappedIndexAndType.get(JsonKey.INDEX),
                    mappedIndexAndType.get(JsonKey.TYPE),
                    identifier)
                .setDoc(data)
                .get();
        ProjectLogger.log(
            "updated response==" + response.getResult().name(), LoggerEnum.INFO.name());
        if (response.getResult().name().equals("UPDATED")) {
          long stopTime = System.currentTimeMillis();
          long elapsedTime = stopTime - startTime;
          ProjectLogger.log(
              "ElasticSearchUtil updateData method end at =="
                  + stopTime
                  + " for Type "
                  + type
                  + " ,Total time elapsed = "
                  + elapsedTime,
              LoggerEnum.PERF_LOG);
          return true;
        } else {
          ProjectLogger.log(
              "ElasticSearchUtil:updateData update was not success:" + response.getResult(),
              LoggerEnum.INFO.name());
        }
      } catch (Exception e) {
        ProjectLogger.log(
            "ElasticSearchUtil:updateData exception occured:" + e.getMessage(),
            LoggerEnum.ERROR.name());
      }
    } else {
      ProjectLogger.log(
          "ElasticSearchUtil:updateData Requested data is invalid.", LoggerEnum.INFO.name());
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil updateData method end at =="
            + stopTime
            + " for Type "
            + type
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return false;
  }

  /**
   * This method will upser data based on identifier.take the data based on identifier and merge
   * with incoming data then update it and if identifier does not exist , it will insert data .
   *
   * @param index String
   * @param type String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  public static boolean upsertData(
      String index, String type, String identifier, Map<String, Object> data) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil upsertData method started at ==" + startTime + " for Type " + type,
        LoggerEnum.PERF_LOG);
    if (!StringUtils.isBlank(index)
        && !StringUtils.isBlank(type)
        && !StringUtils.isBlank(identifier)
        && data != null
        && data.size() > 0) {
      Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
      IndexRequest indexRequest =
          new IndexRequest(
                  mappedIndexAndType.get(JsonKey.INDEX),
                  mappedIndexAndType.get(JsonKey.TYPE),
                  identifier)
              .source(data);
      UpdateRequest updateRequest =
          new UpdateRequest(
                  mappedIndexAndType.get(JsonKey.INDEX),
                  mappedIndexAndType.get(JsonKey.TYPE),
                  identifier)
              .doc(data)
              .upsert(indexRequest);
      UpdateResponse response = null;
      try {
        response = ConnectionManager.getClient().update(updateRequest).get();
      } catch (InterruptedException | ExecutionException e) {
        ProjectLogger.log(e.getMessage(), e);
        return false;
      }
      ProjectLogger.log("updated response==" + response.getResult().name());
      if (upsertResults.contains(response.getResult().name())) {
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        ProjectLogger.log(
            "ElasticSearchUtil upsertData method end at =="
                + stopTime
                + " for Type "
                + type
                + " ,Total time elapsed = "
                + elapsedTime,
            LoggerEnum.PERF_LOG);
        return true;
      }
    } else {
      ProjectLogger.log("Requested data is invalid.");
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil upsertData method end at =="
            + stopTime
            + " for Type "
            + type
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return false;
  }

  /**
   * This method will remove data from ES based on identifier.
   *
   * @param index String
   * @param type String
   * @param identifier String
   */
  public static boolean removeData(String index, String type, String identifier) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil removeData method started at ==" + startTime, LoggerEnum.PERF_LOG);
    DeleteResponse deleteResponse = null;
    if (!StringUtils.isBlank(index)
        && !StringUtils.isBlank(type)
        && !StringUtils.isBlank(identifier)) {
      Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
      try {
        deleteResponse =
            ConnectionManager.getClient()
                .prepareDelete(
                    mappedIndexAndType.get(JsonKey.INDEX),
                    mappedIndexAndType.get(JsonKey.TYPE),
                    identifier)
                .get();
        ProjectLogger.log(
            "delete info ==" + deleteResponse.getResult().name() + " " + deleteResponse.getId());
      } catch (Exception e) {
        ProjectLogger.log(e.getMessage(), e);
      }
    } else {
      ProjectLogger.log("Data can not be deleted due to invalid input.");
      return false;
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil removeData method end at =="
            + stopTime
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);

    return (deleteResponse.getResult().name().equalsIgnoreCase("DELETED"));
  }

  /**
   * Method to perform the elastic search on the basis of SearchDTO . SearchDTO contains the search
   * criteria like fields, facets, sort by , filters etc. here user can pass single type to search
   * or multiple type or null
   *
   * @param type var arg of String
   * @return search result as Map.
   */
  public static Map<String, Object> complexSearch(
      SearchDTO searchDTO, String index, String... type) {
    long startTime = System.currentTimeMillis();
    List<Map<String, String>> indicesAndTypesMapping = getMappedIndexesAndTypes(index, type);
    String[] indices =
        indicesAndTypesMapping
            .stream()
            .map(indexMap -> indexMap.get(JsonKey.INDEX))
            .toArray(String[]::new);
    String[] types =
        indicesAndTypesMapping
            .stream()
            .map(indexMap -> indexMap.get(JsonKey.TYPE))
            .distinct()
            .toArray(String[]::new);
    ProjectLogger.log(
        "ElasticSearchUtil complexSearch method started at ==" + startTime, LoggerEnum.PERF_LOG);
    SearchRequestBuilder searchRequestBuilder =
        getSearchBuilder(ConnectionManager.getClient(), indices, types);
    // check mode and set constraints
    Map<String, Float> constraintsMap = getConstraints(searchDTO);

    BoolQueryBuilder query = new BoolQueryBuilder();

    // add channel field as mandatory
    String channel = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_ES_CHANNEL);
    if (!(StringUtils.isBlank(channel) || JsonKey.SUNBIRD_ES_CHANNEL.equals(channel))) {
      query.must(createMatchQuery(JsonKey.CHANNEL, channel, constraintsMap.get(JsonKey.CHANNEL)));
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
              entry.getKey() + RAW_APPEND, getSortOrder((String) entry.getValue()));
        } else {
          Map<String, Object> map = (Map<String, Object>) entry.getValue();
          Map<String, String> dataMap = (Map) map.get(JsonKey.TERM);
          for (Map.Entry<String, String> dateMapEntry : dataMap.entrySet()) {
            FieldSortBuilder mySort =
                SortBuilders.fieldSort(entry.getKey() + RAW_APPEND)
                    .setNestedFilter(
                        new TermQueryBuilder(dateMapEntry.getKey(), dateMapEntry.getValue()))
                    .sortMode(SortMode.MIN)
                    .order(getSortOrder((String) map.get(JsonKey.ORDER)));
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
        addAdditionalProperties(query, entry, constraintsMap);
      }
    }

    // set final query to search request builder
    searchRequestBuilder.setQuery(query);
    List finalFacetList = new ArrayList();

    if (null != searchDTO.getFacets() && !searchDTO.getFacets().isEmpty()) {
      addAggregations(searchRequestBuilder, searchDTO.getFacets());
    }
    ProjectLogger.log(
        "calling search builder======" + searchRequestBuilder.toString(), LoggerEnum.INFO.name());
    SearchResponse response = null;
    try {
      response = searchRequestBuilder.execute().actionGet();
    } catch (SearchPhaseExecutionException e) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue, e.getRootCause().getMessage());
    }

    List<Map<String, Object>> esSource = new ArrayList<>();
    Map<String, Object> responsemap = new HashMap<>();
    long count = 0;
    if (response != null) {
      SearchHits hits = response.getHits();
      count = hits.getTotalHits();

      for (SearchHit hit : hits) {
        esSource.add(hit.getSourceAsMap());
      }

      // fetch aggregations aggregations
      if (null != searchDTO.getFacets() && !searchDTO.getFacets().isEmpty()) {
        Map<String, String> m1 = searchDTO.getFacets().get(0);
        for (Map.Entry<String, String> entry : m1.entrySet()) {
          String field = entry.getKey();
          String aggsType = entry.getValue();
          List<Object> aggsList = new ArrayList<>();
          Map facetMap = new HashMap();
          if (JsonKey.DATE_HISTOGRAM.equalsIgnoreCase(aggsType)) {
            Histogram agg = response.getAggregations().get(field);
            for (Histogram.Bucket ent : agg.getBuckets()) {
              // DateTime key = (DateTime) ent.getKey(); // Key
              String keyAsString = ent.getKeyAsString(); // Key as String
              long docCount = ent.getDocCount(); // Doc count
              Map internalMap = new HashMap();
              internalMap.put(JsonKey.NAME, keyAsString);
              internalMap.put(JsonKey.COUNT, docCount);
              aggsList.add(internalMap);
            }
          } else {
            Terms aggs = response.getAggregations().get(field);
            for (Bucket bucket : aggs.getBuckets()) {
              Map internalMap = new HashMap();
              internalMap.put(JsonKey.NAME, bucket.getKey());
              internalMap.put(JsonKey.COUNT, bucket.getDocCount());
              aggsList.add(internalMap);
            }
          }
          facetMap.put("values", aggsList);
          facetMap.put(JsonKey.NAME, field);
          finalFacetList.add(facetMap);
        }
      }
    }
    responsemap.put(JsonKey.CONTENT, esSource);
    if (!(finalFacetList.isEmpty())) {
      responsemap.put(JsonKey.FACETS, finalFacetList);
    }
    responsemap.put(JsonKey.COUNT, count);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil complexSearch method end at =="
            + stopTime
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return responsemap;
  }

  private static void addAggregations(
      SearchRequestBuilder searchRequestBuilder, List<Map<String, String>> facets) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil addAggregations method started at ==" + startTime, LoggerEnum.PERF_LOG);
    Map<String, String> map = facets.get(0);
    for (Map.Entry<String, String> entry : map.entrySet()) {

      String key = entry.getKey();
      String value = entry.getValue();
      if (JsonKey.DATE_HISTOGRAM.equalsIgnoreCase(value)) {
        searchRequestBuilder.addAggregation(
            AggregationBuilders.dateHistogram(key)
                .field(key + RAW_APPEND)
                .dateHistogramInterval(DateHistogramInterval.days(1)));

      } else if (null == value) {
        searchRequestBuilder.addAggregation(AggregationBuilders.terms(key).field(key + RAW_APPEND));
      }
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil addAggregations method end at =="
            + stopTime
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
  }

  private static Map<String, Float> getConstraints(SearchDTO searchDTO) {
    if (null != searchDTO.getSoftConstraints() && !searchDTO.getSoftConstraints().isEmpty()) {
      return searchDTO
          .getSoftConstraints()
          .entrySet()
          .stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().floatValue()));
    }
    return Collections.emptyMap();
  }

  private static SearchRequestBuilder getSearchBuilder(
      TransportClient client, String[] index, String... type) {

    if (type == null || type.length == 0) {
      return client.prepareSearch().setIndices(index);
    } else {
      return client.prepareSearch().setIndices(index).setTypes(type);
    }
  }

  /** Method to add the additional search query like range query , exists - not exist filter etc. */
  @SuppressWarnings("unchecked")
  private static void addAdditionalProperties(
      BoolQueryBuilder query, Entry<String, Object> entry, Map<String, Float> constraintsMap) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil addAdditionalProperties method started at ==" + startTime,
        LoggerEnum.PERF_LOG);
    String key = entry.getKey();

    if (key.equalsIgnoreCase(JsonKey.FILTERS)) {

      Map<String, Object> filters = (Map<String, Object>) entry.getValue();
      for (Map.Entry<String, Object> en : filters.entrySet()) {
        createFilterESOpperation(en, query, constraintsMap);
      }
    } else if (key.equalsIgnoreCase(JsonKey.EXISTS) || key.equalsIgnoreCase(JsonKey.NOT_EXISTS)) {
      createESOpperation(entry, query, constraintsMap);
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil addAdditionalProperties method end at =="
            + stopTime
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
  }

  /** Method to create CommonTermQuery , multimatch and Range Query. */
  @SuppressWarnings("unchecked")
  private static void createFilterESOpperation(
      Entry<String, Object> entry, BoolQueryBuilder query, Map<String, Float> constraintsMap) {

    String key = entry.getKey();
    Object val = entry.getValue();
    if (val instanceof List) {
      if (!((List) val).isEmpty()) {
        if (((List) val).get(0) instanceof String) {
          ((List<String>) val).replaceAll(String::toLowerCase);
          query.must(
              createTermsQuery(key + RAW_APPEND, (List<String>) val, constraintsMap.get(key)));
        } else {
          query.must(createTermsQuery(key, (List) val, constraintsMap.get(key)));
        }
      }
    } else if (val instanceof Map) {
      Map<String, Object> value = (Map<String, Object>) val;
      Map<String, Object> rangeOperation = new HashMap<>();
      Map<String, Object> lexicalOperation = new HashMap<>();
      for (Map.Entry<String, Object> it : value.entrySet()) {
        String operation = it.getKey();
        if (operation.startsWith(LT) || operation.startsWith(GT)) {
          rangeOperation.put(operation, it.getValue());
        } else if (operation.startsWith(STARTS_WITH) || operation.startsWith(ENDS_WITH)) {
          lexicalOperation.put(operation, it.getValue());
        }
      }
      if (!(rangeOperation.isEmpty())) {
        query.must(createRangeQuery(key, rangeOperation, constraintsMap.get(key)));
      }
      if (!(lexicalOperation.isEmpty())) {
        query.must(createLexicalQuery(key, lexicalOperation, constraintsMap.get(key)));
      }

    } else if (val instanceof String) {
      query.must(
          createTermQuery(key + RAW_APPEND, ((String) val).toLowerCase(), constraintsMap.get(key)));
    } else {
      query.must(createTermQuery(key + RAW_APPEND, val, constraintsMap.get(key)));
    }
  }

  /** Method to create EXISTS and NOT EXIST FILTER QUERY . */
  @SuppressWarnings("unchecked")
  private static void createESOpperation(
      Entry<String, Object> entry, BoolQueryBuilder query, Map<String, Float> constraintsMap) {

    String operation = entry.getKey();
    List<String> existsList = (List<String>) entry.getValue();

    if (operation.equalsIgnoreCase(JsonKey.EXISTS)) {
      for (String name : existsList) {
        query.must(createExistQuery(name, constraintsMap.get(name)));
      }
    } else if (operation.equalsIgnoreCase(JsonKey.NOT_EXISTS)) {
      for (String name : existsList) {
        query.mustNot(createExistQuery(name, constraintsMap.get(name)));
      }
    }
  }

  /** Method to return the sorting order on basis of string param . */
  private static SortOrder getSortOrder(String value) {
    return value.equalsIgnoreCase(ASC_ORDER) ? SortOrder.ASC : SortOrder.DESC;
  }

  private static MatchQueryBuilder createMatchQuery(String name, Object text, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.matchQuery(name, text).boost(boost);
    } else {
      return QueryBuilders.matchQuery(name, text);
    }
  }

  private static TermsQueryBuilder createTermsQuery(String key, List values, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.termsQuery(key, (values).stream().toArray(Object[]::new)).boost(boost);
    } else {
      return QueryBuilders.termsQuery(key, (values).stream().toArray(Object[]::new));
    }
  }

  private static RangeQueryBuilder createRangeQuery(
      String name, Map<String, Object> rangeOperation, Float boost) {

    RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(name + RAW_APPEND);
    for (Map.Entry<String, Object> it : rangeOperation.entrySet()) {
      if (it.getKey().equalsIgnoreCase(LTE)) {
        rangeQueryBuilder.lte(it.getValue());
      } else if (it.getKey().equalsIgnoreCase(LT)) {
        rangeQueryBuilder.lt(it.getValue());
      } else if (it.getKey().equalsIgnoreCase(GTE)) {
        rangeQueryBuilder.gte(it.getValue());
      } else if (it.getKey().equalsIgnoreCase(GT)) {
        rangeQueryBuilder.gt(it.getValue());
      }
    }
    if (isNotNull(boost)) {
      return rangeQueryBuilder.boost(boost);
    }
    return rangeQueryBuilder;
  }

  private static TermQueryBuilder createTermQuery(String name, Object text, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.termQuery(name, text).boost(boost);
    } else {
      return QueryBuilders.termQuery(name, text);
    }
  }

  private static ExistsQueryBuilder createExistQuery(String name, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.existsQuery(name).boost(boost);
    } else {
      return QueryBuilders.existsQuery(name);
    }
  }

  private static QueryBuilder createLexicalQuery(
      String key, Map<String, Object> rangeOperation, Float boost) {
    QueryBuilder queryBuilder = null;
    for (Map.Entry<String, Object> it : rangeOperation.entrySet()) {
      if (it.getKey().equalsIgnoreCase(STARTS_WITH)) {
        String startsWithVal = (String) it.getValue();
        if (StringUtils.isNotBlank(startsWithVal)) {
          startsWithVal = startsWithVal.toLowerCase();
        }
        if (isNotNull(boost)) {
          queryBuilder = QueryBuilders.prefixQuery(key + RAW_APPEND, startsWithVal).boost(boost);
        }
        queryBuilder = QueryBuilders.prefixQuery(key + RAW_APPEND, startsWithVal);
      } else if (it.getKey().equalsIgnoreCase(ENDS_WITH)) {
        String endsWithRegex = "~" + it.getValue();
        if (isNotNull(boost)) {
          queryBuilder = QueryBuilders.regexpQuery(key + RAW_APPEND, endsWithRegex).boost(boost);
        }
        queryBuilder = QueryBuilders.regexpQuery(key + RAW_APPEND, endsWithRegex);
      }
    }
    return queryBuilder;
  }

  /**
   * This method will do the bulk data insertion.
   *
   * @param index String index name
   * @param type String type name
   * @param dataList List<Map<String, Object>>
   * @return boolean
   */
  public static boolean bulkInsertData(
      String index, String type, List<Map<String, Object>> dataList) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchUtil bulkInsertData method started at ==" + startTime + " for Type " + type,
        LoggerEnum.PERF_LOG);
    boolean response = true;
    Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
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
                              "Bulk insert api response==="
                                  + bResponse.getId()
                                  + " "
                                  + bResponse.isFailed());
                        }
                      }
                    }

                    @Override
                    public void afterBulk(
                        long executionId, BulkRequest request, Throwable failure) {
                      ProjectLogger.log("Bulk upload error block", failure);
                    }
                  })
              .setBulkActions(10000)
              .setConcurrentRequests(0)
              .build();

      for (Map<String, Object> map : dataList) {
        map.put(JsonKey.IDENTIFIER, map.get(JsonKey.ID));
        IndexRequest request =
            new IndexRequest(
                    mappedIndexAndType.get(JsonKey.INDEX),
                    mappedIndexAndType.get(JsonKey.TYPE),
                    (String) map.get(JsonKey.IDENTIFIER))
                .source(map);
        bulkProcessor.add(request);
      }
      // Flush any remaining requests
      bulkProcessor.flush();

      // Or close the bulkProcessor if you don't need it anymore
      bulkProcessor.close();

      // Refresh your indices
      ConnectionManager.getClient().admin().indices().prepareRefresh().get();
    } catch (Exception e) {
      response = false;
      ProjectLogger.log(e.getMessage(), e);
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil bulkInsertData method end at =="
            + stopTime
            + " for Type "
            + type
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return response;
  }

  /**
   * This method will do the health check of elastic search.
   *
   * @return boolean
   */
  public static boolean healthCheck() {
    boolean indexResponse = false;
    Map<String, String> mappedIndexAndType =
        getMappedIndexAndType(
            ProjectUtil.EsIndex.sunbird.getIndexName(), ProjectUtil.EsType.user.getTypeName());
    try {
      indexResponse =
          ConnectionManager.getClient()
              .admin()
              .indices()
              .exists(Requests.indicesExistsRequest(mappedIndexAndType.get(JsonKey.INDEX)))
              .get()
              .isExists();
    } catch (Exception e) {
      ProjectLogger.log("ElasticSearchUtil:healthCheck error " + e.getMessage(), e);
    }
    return indexResponse;
  }

  /**
   * Method to execute ES query with the limitation of size set to 0 Currently this is a rest call
   *
   * @param index ES indexName
   * @param type ES type
   * @param rawQuery actual query to be executed
   * @return ES response for the query
   */
  @SuppressWarnings("unchecked")
  public static Response searchMetricsData(String index, String type, String rawQuery) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log("Metrics search method started at ==" + startTime, LoggerEnum.PERF_LOG);
    String baseUrl = null;
    if (!StringUtils.isBlank(System.getenv(JsonKey.SUNBIRD_ES_IP))) {
      String envHost = System.getenv(JsonKey.SUNBIRD_ES_IP);
      String[] host = envHost.split(",");
      baseUrl =
          "http://"
              + host[0]
              + ":"
              + PropertiesCache.getInstance().getProperty(JsonKey.ES_METRICS_PORT);
    } else {
      ProjectLogger.log("ES URL from Properties file");
      baseUrl = PropertiesCache.getInstance().getProperty(JsonKey.ES_URL);
    }
    Map<String, String> mappedIndexAndType = getMappedIndexAndType(index, type);
    String requestURL =
        baseUrl
            + "/"
            + mappedIndexAndType.get(JsonKey.INDEX)
            + "/"
            + mappedIndexAndType.get(JsonKey.TYPE)
            + "/"
            + "_search";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    Map<String, Object> responseData = new HashMap<>();
    try {
      // TODO:Currently this is making a rest call but needs to be modified to make
      // the call using
      // ElasticSearch client
      String responseStr = HttpUtil.sendPostRequest(requestURL, rawQuery, headers);
      ObjectMapper mapper = new ObjectMapper();
      responseData = mapper.readValue(responseStr, Map.class);
    } catch (IOException e) {
      throw new ProjectCommonException(
          ResponseCode.unableToConnectToES.getErrorCode(),
          ResponseCode.unableToConnectToES.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.unableToParseData.getErrorCode(),
          ResponseCode.unableToParseData.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, responseData);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "ElasticSearchUtil metrics search method end at == "
            + stopTime
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return response;
  }

  /**
   * this method will take start time and subtract with current time to get the time spent in
   * millis.
   *
   * @param startTime long
   * @return long
   */
  public static long calculateEndTime(long startTime) {
    return System.currentTimeMillis() - startTime;
  }

  public static SearchDTO createSearchDTO(Map<String, Object> searchQueryMap) {
    SearchDTO search = new SearchDTO();
    if (searchQueryMap.containsKey(JsonKey.QUERY)) {
      search.setQuery((String) searchQueryMap.get(JsonKey.QUERY));
    }
    if (searchQueryMap.containsKey(JsonKey.QUERY_FIELDS)) {
      search.setQueryFields((List<String>) searchQueryMap.get(JsonKey.QUERY_FIELDS));
    }
    if (searchQueryMap.containsKey(JsonKey.FACETS)) {
      search.setFacets((List<Map<String, String>>) searchQueryMap.get(JsonKey.FACETS));
    }
    if (searchQueryMap.containsKey(JsonKey.FIELDS)) {
      search.setFields((List<String>) searchQueryMap.get(JsonKey.FIELDS));
    }
    if (searchQueryMap.containsKey(JsonKey.FILTERS)) {
      search.getAdditionalProperties().put(JsonKey.FILTERS, searchQueryMap.get(JsonKey.FILTERS));
    }
    if (searchQueryMap.containsKey(JsonKey.EXISTS)) {
      search.getAdditionalProperties().put(JsonKey.EXISTS, searchQueryMap.get(JsonKey.EXISTS));
    }
    if (searchQueryMap.containsKey(JsonKey.NOT_EXISTS)) {
      search
          .getAdditionalProperties()
          .put(JsonKey.NOT_EXISTS, searchQueryMap.get(JsonKey.NOT_EXISTS));
    }
    if (searchQueryMap.containsKey(JsonKey.SORT_BY)) {
      search
          .getSortBy()
          .putAll((Map<? extends String, ? extends String>) searchQueryMap.get(JsonKey.SORT_BY));
    }
    if (searchQueryMap.containsKey(JsonKey.OFFSET)) {
      if ((searchQueryMap.get(JsonKey.OFFSET)) instanceof Integer) {
        search.setOffset((int) searchQueryMap.get(JsonKey.OFFSET));
      } else {
        search.setOffset(((BigInteger) searchQueryMap.get(JsonKey.OFFSET)).intValue());
      }
    }
    if (searchQueryMap.containsKey(JsonKey.LIMIT)) {
      if ((searchQueryMap.get(JsonKey.LIMIT)) instanceof Integer) {
        search.setLimit((int) searchQueryMap.get(JsonKey.LIMIT));
      } else {
        search.setLimit(((BigInteger) searchQueryMap.get(JsonKey.LIMIT)).intValue());
      }
    }
    if (searchQueryMap.containsKey(JsonKey.GROUP_QUERY)) {
      search
          .getGroupQuery()
          .addAll(
              (Collection<? extends Map<String, Object>>) searchQueryMap.get(JsonKey.GROUP_QUERY));
    }
    if (searchQueryMap.containsKey(JsonKey.SOFT_CONSTRAINTS)) {
      search.setSoftConstraints(
          (Map<String, Integer>) searchQueryMap.get(JsonKey.SOFT_CONSTRAINTS));
    }
    return search;
  }

  /**
   * @param ids List of ids of document
   * @param fields List of fields which needs to captured
   * @param typeToSearch type of ES
   * @return Map<String,Map<String,Objec>> It will return a map with id as key and the data from ES
   *     as value
   */
  public static Map<String, Map<String, Object>> getEsResultByListOfIds(
      List<String> ids, List<String> fields, ProjectUtil.EsType typeToSearch) {

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, ids);

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    searchDTO.setFields(fields);

    Map<String, Object> result =
        complexSearch(
            searchDTO, ProjectUtil.EsIndex.sunbird.getIndexName(), typeToSearch.getTypeName());
    List<Map<String, Object>> esContent = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    return esContent
        .stream()
        .collect(
            Collectors.toMap(
                obj -> {
                  return (String) obj.get("id");
                },
                val -> val));
  }

  private static Map<String, String> getMappedIndexAndType(
      String sunbirdIndex, String sunbirdType) {
    String mappedIndexAndType = "mapping." + sunbirdIndex + "." + sunbirdType;
    Map<String, String> mappedIndexAndTypeResult = new HashMap<>();
    if (config.hasPath(mappedIndexAndType)) {
      mappedIndexAndTypeResult = (Map<String, String>) config.getAnyRef(mappedIndexAndType);
    } else {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    ProjectLogger.log(
        "Elasticsearch input index "
            + sunbirdIndex
            + " types "
            + sunbirdType
            + " output "
            + mappedIndexAndTypeResult,
        LoggerEnum.DEBUG);
    return mappedIndexAndTypeResult;
  }

  private static List<Map<String, String>> getMappedIndexesAndTypes(
      String sunbirdIndex, String... sunbirdTypes) {
    List<Map<String, String>> mappedIndexesAndTypes = new ArrayList<>();
    for (String sunbirdType : sunbirdTypes) {
      mappedIndexesAndTypes.add(getMappedIndexAndType(sunbirdIndex, sunbirdType));
    }
    return mappedIndexesAndTypes;
  }

  public static Future<Map<String, Object>> doAsyncSearch(
      String index, String type, SearchDTO searchDTO) {
    Map<String, String> indexTypeMap = getMappedIndexAndType(index, type);
    Promise<Map<String, Object>> promise = Futures.promise();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
    if (!StringUtils.isBlank(searchDTO.getQuery())) {
      SimpleQueryStringBuilder sqsb = QueryBuilders.simpleQueryStringQuery(searchDTO.getQuery());
      if (CollectionUtils.isEmpty(searchDTO.getQueryFields())) {
        boolQueryBuilder.must(sqsb.field("all_fields"));
      } else {
        Map<String, Float> searchFields =
            searchDTO
                .getQueryFields()
                .stream()
                .collect(Collectors.<String, String, Float>toMap(s -> s, v -> 1.0f));
        boolQueryBuilder.must(sqsb.fields(searchFields));
      }
    }
    sourceBuilder.from(searchDTO.getOffset() != null ? searchDTO.getOffset() : 0);
    sourceBuilder.size(searchDTO.getLimit() != null ? searchDTO.getLimit() : 250);
    // check mode and set constraints
    Map<String, Float> constraintsMap = getConstraints(searchDTO);
    // apply additional properties
    if (searchDTO.getAdditionalProperties() != null
        && searchDTO.getAdditionalProperties().size() > 0) {
      for (Map.Entry<String, Object> entry : searchDTO.getAdditionalProperties().entrySet()) {
        addAdditionalProperties(boolQueryBuilder, entry, constraintsMap);
      }
    }
    sourceBuilder.query(boolQueryBuilder);
    SearchRequest searchRequest = new SearchRequest(indexTypeMap.get(JsonKey.INDEX));
    searchRequest.source(sourceBuilder);
    ActionListener<SearchResponse> listener =
        new ActionListener<SearchResponse>() {
          @Override
          public void onResponse(SearchResponse searchResponse) {
            List<Map<String, Object>> mapList = new ArrayList<>();
            Map<String, Object> responseMap = new HashMap<>();
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits.getHits()) {
              mapList.add(hit.getSourceAsMap());
            }
            responseMap.put(JsonKey.CONTENT, mapList);
            responseMap.put(JsonKey.COUNT, hits.getTotalHits());
            promise.success(responseMap);
          }

          @Override
          public void onFailure(Exception e) {
            promise.failure(e);
          }
        };
    ConnectionManager.getRestClient().searchAsync(searchRequest, listener);
    return promise.future();
  }
}
