package org.sunbird.common;

import static org.sunbird.common.ProjectUtil.isNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.pekko.util.Timeout;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.SortOrder;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.dto.SearchDTO;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * Helper class for Elasticsearch operations.
 * Provides utility methods for query construction, response parsing, and aggregation handling.
 */
public class ElasticSearchHelper {

  /** Less than or equal to operator constant. */
  public static final String LTE = "<=";
  
  /** Less than operator constant. */
  public static final String LT = "<";
  
  /** Greater than or equal to operator constant. */
  public static final String GTE = ">=";
  
  /** Greater than operator constant. */
  public static final String GT = ">";
  
  /** Ascending sort order constant. */
  public static final String ASC_ORDER = "ASC";
  
  /** Starts with string operation constant. */
  public static final String STARTS_WITH = "startsWith";
  
  /** Ends with string operation constant. */
  public static final String ENDS_WITH = "endsWith";
  
  /** Soft mode constant for constraints. */
  public static final String SOFT_MODE = "soft";
  
  /** Suffix for raw field access in Elasticsearch. */
  public static final String RAW_APPEND = ".raw";
  
  /** Cache for verifying index existence. */
  protected static Map<String, Boolean> indexMap = new HashMap<>();
  
  /** Cache for verifying type existence. */
  protected static Map<String, Boolean> typeMap = new HashMap<>();
  
  /** Default wait time in seconds for async operations. */
  public static final int WAIT_TIME = 5;
  
  /** Timeout configuration for async operations. */
  public static Timeout timeout = new Timeout(WAIT_TIME, TimeUnit.SECONDS);
  
  /** Valid results for upsert operations. */
  public static final List<String> upsertResults =
      new ArrayList<>(Arrays.asList("CREATED", "UPDATED", "NOOP"));
      
  /** Default document type for Elasticsearch 6.x/7.x compatibility. */
  private static final String _DOC = "_doc";
  
  private static final LoggerUtil logger = new LoggerUtil(ElasticSearchHelper.class);

  /** Private constructor to prevent instantiation of utility class. */
  private ElasticSearchHelper() {}

  /**
   * Waits for and returns the result from a Scala Future.
   *
   * @param future The Scala Future to wait for
   * @return The result object from the future, or null if an error occurs
   */
  @SuppressWarnings("unchecked")
  public static Object getResponseFromFuture(Future future) {
    try {
      if (future != null) {
        return Await.result(future, timeout.duration());
      }
    } catch (Exception e) {
      logger.error(null, "ElasticSearchHelper:getResponseFromFuture: Error occurred while waiting for future result", e);
    }
    return null;
  }

  /**
   * Adds aggregations to the SearchRequestBuilder based on the provided facets.
   *
   * @param searchRequestBuilder The builder to add aggregations to
   * @param facets List of facets configuration
   * @return The updated SearchRequestBuilder
   */
  public static SearchRequestBuilder addAggregations(
      SearchRequestBuilder searchRequestBuilder, List<Map<String, String>> facets) {
    long startTime = System.currentTimeMillis();
    logger.debug(null, "ElasticSearchHelper:addAggregations: method started at " + startTime);

    if (searchRequestBuilder != null && CollectionUtils.isNotEmpty(facets)) {
      Map<String, String> map = facets.get(0);
      if (MapUtils.isNotEmpty(map)) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();

          if (JsonKey.DATE_HISTOGRAM.equalsIgnoreCase(value)) {
            searchRequestBuilder.addAggregation(
                AggregationBuilders.dateHistogram(key)
                    .field(key + RAW_APPEND)
                    .dateHistogramInterval(DateHistogramInterval.days(1)));
          } else if (value == null) {
            searchRequestBuilder.addAggregation(
                AggregationBuilders.terms(key).field(key + RAW_APPEND));
          }
        }
      }
    }

    long elapsedTime = calculateEndTime(startTime);
    logger.debug(null, "ElasticSearchHelper:addAggregations: method ended. Total time elapsed = " + elapsedTime);
    return searchRequestBuilder;
  }

  /**
   * Extracts soft constraints from the SearchDTO.
   *
   * @param searchDTO The search object containing constraints
   * @return Map of constraints where key is the field and value is the boost/weight
   */
  public static Map<String, Float> getConstraints(SearchDTO searchDTO) {
    if (searchDTO != null && MapUtils.isNotEmpty(searchDTO.getSoftConstraints())) {
      return searchDTO.getSoftConstraints().entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().floatValue()));
    }
    return Collections.emptyMap();
  }

  /**
   * Prepares a SearchRequestBuilder for the TransportClient (Legacy support).
   *
   * @param client The TransportClient instance
   * @param index Array of index names to search
   * @return A configured SearchRequestBuilder
   */
  public static SearchRequestBuilder getTransportSearchBuilder(
      TransportClient client, String[] index) {
    return client.prepareSearch().setIndices(index).setTypes(_DOC);
  }

  /**
   * Adds additional search criteria such as filters, exists, nested filters to the query.
   *
   * @param query The BoolQueryBuilder to update
   * @param entry Map entry containing the query key and value
   * @param constraintsMap Map of constraints for boost values
   */
  @SuppressWarnings("unchecked")
  public static void addAdditionalProperties(
      BoolQueryBuilder query, Entry<String, Object> entry, Map<String, Float> constraintsMap) {
    long startTime = System.currentTimeMillis();
    logger.debug(null, "ElasticSearchHelper:addAdditionalProperties: method started at " + startTime);

    String key = entry.getKey();
    Object value = entry.getValue();

    if (JsonKey.FILTERS.equalsIgnoreCase(key)) {
      if (value instanceof Map) {
        Map<String, Object> filters = (Map<String, Object>) value;
        for (Map.Entry<String, Object> en : filters.entrySet()) {
          query = createFilterESOperation(en, query, constraintsMap);
        }
      }
    } else if (JsonKey.EXISTS.equalsIgnoreCase(key) || JsonKey.NOT_EXISTS.equalsIgnoreCase(key)) {
      query = createESOperation(entry, query, constraintsMap);
    } else if (JsonKey.NESTED_EXISTS.equalsIgnoreCase(key)
        || JsonKey.NESTED_NOT_EXISTS.equalsIgnoreCase(key)) {
      query = createNestedESOperation(entry, query, constraintsMap);
    } else if (JsonKey.NESTED_KEY_FILTER.equalsIgnoreCase(key)) {
      if (value instanceof Map) {
        Map<String, Object> nestedFilters = (Map<String, Object>) value;
        for (Map.Entry<String, Object> en : nestedFilters.entrySet()) {
          query = createNestedFilterESOperation(en, query, constraintsMap);
        }
      }
    }

    long elapsedTime = calculateEndTime(startTime);
    logger.debug(null, "ElasticSearchHelper:addAdditionalProperties: method ended. Total time elapsed = " + elapsedTime);
  }

  /**
   * Creates filter operations including Terms, Term, Range, and Match queries.
   *
   * @param entry Map entry with field name and value/condition
   * @param query The BoolQueryBuilder to append to
   * @param constraintsMap Map of boost constraints
   * @return The updated BoolQueryBuilder
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder createFilterESOperation(
      Entry<String, Object> entry, BoolQueryBuilder query, Map<String, Float> constraintsMap) {
    String key = entry.getKey();
    Object val = entry.getValue();

    if (val != null) {
      if (val instanceof List) {
        query = getTermQueryFromList(val, key, query, constraintsMap);
      } else if (val instanceof Map) {
        if (key.equalsIgnoreCase(JsonKey.ES_OR_OPERATION)) {
          query.must(createEsORFilterQuery((Map<String, Object>) val));
        } else {
          query = getTermQueryFromMap(val, key, query, constraintsMap);
        }
      } else if (val instanceof String) {
        query.must(
            createTermQuery(key + RAW_APPEND, ((String) val).toLowerCase(), constraintsMap.get(key)));
      } else {
        query.must(createTermQuery(key + RAW_APPEND, val, constraintsMap.get(key)));
      }
    }
    return query;
  }

  /**
   * Creates nested filter operations for the given entry, updating the query builder.
   * Handles List, Map, and String values for nested properties.
   *
   * @param entry The map entry containing the key (dot-separated path) and value for the filter.
   * @param query The BoolQueryBuilder to update with the new nested query.
   * @param constraintsMap A map of constraints (boost values) for keys.
   * @return The updated BoolQueryBuilder.
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder createNestedFilterESOperation(
      Entry<String, Object> entry, BoolQueryBuilder query, Map<String, Float> constraintsMap) {
    String key = entry.getKey();
    Object val = entry.getValue();
    String path = key.split("\\.")[0];

    if (val instanceof List && CollectionUtils.isNotEmpty((List) val)) {
      List<Object> valueList = (List<Object>) val;
      if (valueList.get(0) instanceof String) {
        List<String> stringList = (List<String>) val;
        stringList.replaceAll(String::toLowerCase);
        query.must(
            QueryBuilders.nestedQuery(
                path,
                createTermsQuery(key + RAW_APPEND, stringList, constraintsMap.get(key)),
                ScoreMode.None));
      } else {
        query.must(
            QueryBuilders.nestedQuery(
                path, createTermsQuery(key, valueList, constraintsMap.get(key)), ScoreMode.None));
      }
    } else if (val instanceof Map) {
      query = getNestedTermQueryFromMap(val, key, path, query, constraintsMap);
    } else if (val instanceof String) {
      query.must(
          QueryBuilders.nestedQuery(
              path,
              createTermQuery(
                  key + RAW_APPEND, ((String) val).toLowerCase(), constraintsMap.get(key)),
              ScoreMode.None));
    } else {
      query.must(
          QueryBuilders.nestedQuery(
              path,
              createTermQuery(key + RAW_APPEND, val, constraintsMap.get(key)),
              ScoreMode.None));
    }
    return query;
  }

  /**
   * Generates a term query or range/lexical query from a Map value.
   *
   * @param val The value map containing operation keys (e.g., LT, GT, startsWith).
   * @param key The field key for the query.
   * @param query The BoolQueryBuilder to update.
   * @param constraintsMap Map of boost constraints.
   * @return The updated BoolQueryBuilder.
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder getTermQueryFromMap(
      Object val, String key, BoolQueryBuilder query, Map<String, Float> constraintsMap) {
    Map<String, Object> value = (Map<String, Object>) val;
    Map<String, Object> rangeOperation = new HashMap<>();
    Map<String, Object> lexicalOperation = new HashMap<>();

    for (Map.Entry<String, Object> entry : value.entrySet()) {
      String operation = entry.getKey();
      if (operation.startsWith(LT) || operation.startsWith(GT)) {
        rangeOperation.put(operation, entry.getValue());
      } else if (operation.startsWith(STARTS_WITH) || operation.startsWith(ENDS_WITH)) {
        lexicalOperation.put(operation, entry.getValue());
      }
    }

    if (!rangeOperation.isEmpty()) {
      query.must(createRangeQuery(key, rangeOperation, constraintsMap.get(key)));
    }
    if (!lexicalOperation.isEmpty()) {
      query.must(createLexicalQuery(key, lexicalOperation, constraintsMap.get(key)));
    }

    return query;
  }

  /**
   * Creates a boolean OR query for multiple term filters.
   *
   * @param orFilters Map of field names to values for the OR condition.
   * @return A new BoolQueryBuilder with SHOULD clauses.
   */
  private static BoolQueryBuilder createEsORFilterQuery(Map<String, Object> orFilters) {
    BoolQueryBuilder query = new BoolQueryBuilder();
    for (Map.Entry<String, Object> entry : orFilters.entrySet()) {
      query.should(
          QueryBuilders.termQuery(
              entry.getKey() + RAW_APPEND, ((String) entry.getValue()).toLowerCase()));
    }
    return query;
  }

  /**
   * Generates a nested term query (range or lexical) from a Map value.
   *
   * @param val The value map containing operation keys.
   * @param key The field key for the query.
   * @param path The nested path.
   * @param query The BoolQueryBuilder to update.
   * @param constraintsMap Map of boost constraints.
   * @return The updated BoolQueryBuilder.
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder getNestedTermQueryFromMap(
      Object val,
      String key,
      String path,
      BoolQueryBuilder query,
      Map<String, Float> constraintsMap) {
    Map<String, Object> value = (Map<String, Object>) val;
    Map<String, Object> rangeOperation = new HashMap<>();
    Map<String, Object> lexicalOperation = new HashMap<>();

    for (Map.Entry<String, Object> entry : value.entrySet()) {
      String operation = entry.getKey();
      if (operation.startsWith(LT) || operation.startsWith(GT)) {
        rangeOperation.put(operation, entry.getValue());
      } else if (operation.startsWith(STARTS_WITH) || operation.startsWith(ENDS_WITH)) {
        lexicalOperation.put(operation, entry.getValue());
      }
    }

    if (!rangeOperation.isEmpty()) {
      query.must(
          QueryBuilders.nestedQuery(
              path,
              createRangeQuery(key, rangeOperation, constraintsMap.get(key)),
              ScoreMode.None));
    }
    if (!lexicalOperation.isEmpty()) {
      query.must(
          QueryBuilders.nestedQuery(
              path,
              createLexicalQuery(key, lexicalOperation, constraintsMap.get(key)),
              ScoreMode.None));
    }
    return query;
  }

  /**
   * Generates a terms query from a List value.
   *
   * @param val The value list for the terms query.
   * @param key The field key for the query.
   * @param query The BoolQueryBuilder to update.
   * @param constraintsMap Map of boost constraints.
   * @return The updated BoolQueryBuilder.
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder getTermQueryFromList(
      Object val, String key, BoolQueryBuilder query, Map<String, Float> constraintsMap) {
    if (val instanceof List && !((List<?>) val).isEmpty()) {
      if (((List<?>) val).get(0) instanceof String) {
        List<String> stringList = (List<String>) val;
        stringList.replaceAll(String::toLowerCase);
        query.must(
            createTermsQuery(key + RAW_APPEND, stringList, constraintsMap.get(key)));
      } else {
        query.must(createTermsQuery(key, (List<?>) val, constraintsMap.get(key)));
      }
    }
    return query;
  }

  /**
   * Creates filter operations for EXISTS and NOT_EXISTS conditions.
   *
   * @param entry The map entry containing the operation key and list of fields.
   * @param query The BoolQueryBuilder to update.
   * @param constraintsMap Map of boost constraints.
   * @return The updated BoolQueryBuilder.
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder createESOperation(
      Entry<String, Object> entry, BoolQueryBuilder query, Map<String, Float> constraintsMap) {

    String operation = entry.getKey();
    if (entry.getValue() instanceof List) {
      List<String> existsList = (List<String>) entry.getValue();

      if (JsonKey.EXISTS.equalsIgnoreCase(operation)) {
        for (String name : existsList) {
          query.must(createExistQuery(name, constraintsMap.get(name)));
        }
      } else if (JsonKey.NOT_EXISTS.equalsIgnoreCase(operation)) {
        for (String name : existsList) {
          query.mustNot(createExistQuery(name, constraintsMap.get(name)));
        }
      }
    }
    return query;
  }

  /**
   * Creates nested filter operations for NESTED_EXISTS and NESTED_NOT_EXISTS conditions.
   *
   * @param entry The map entry containing the operation key and map of nested paths/fields.
   * @param query The BoolQueryBuilder to update.
   * @param constraintsMap Map of boost constraints.
   * @return The updated BoolQueryBuilder.
   */
  @SuppressWarnings("unchecked")
  private static BoolQueryBuilder createNestedESOperation(
      Entry<String, Object> entry, BoolQueryBuilder query, Map<String, Float> constraintsMap) {

    String operation = entry.getKey();
    if (entry.getValue() instanceof Map) {
      Map<String, String> existsMap = (Map<String, String>) entry.getValue();

      if (JsonKey.NESTED_EXISTS.equalsIgnoreCase(operation)) {
        for (Map.Entry<String, String> nameByPath : existsMap.entrySet()) {
          query.must(
              QueryBuilders.nestedQuery(
                  nameByPath.getValue(),
                  createExistQuery(nameByPath.getKey(), constraintsMap.get(nameByPath.getKey())),
                  ScoreMode.None));
        }
      } else if (JsonKey.NESTED_NOT_EXISTS.equalsIgnoreCase(operation)) {
        for (Map.Entry<String, String> nameByPath : existsMap.entrySet()) {
          query.mustNot(
              QueryBuilders.nestedQuery(
                  nameByPath.getValue(),
                  createExistQuery(nameByPath.getKey(), constraintsMap.get(nameByPath.getKey())),
                  ScoreMode.None));
        }
      }
    }
    return query;
  }

  /**
   * returns the sorting order based on the string parameter.
   *
   * @param value The sort order string ("ASC" or "DESC").
   * @return The SortOrder enum.
   */
  public static SortOrder getSortOrder(String value) {
    return ASC_ORDER.equalsIgnoreCase(value) ? SortOrder.ASC : SortOrder.DESC;
  }

  /**
   * Creates a MatchQueryBuilder with an optional boost.
   *
   * @param name The attribute/field name.
   * @param value The value to match.
   * @param boost The optional boost value (can be null).
   * @return A MatchQueryBuilder instance.
   */
  public static MatchQueryBuilder createMatchQuery(String name, Object value, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.matchQuery(name, value).boost(boost);
    } else {
      return QueryBuilders.matchQuery(name, value);
    }
  }

  /**
   * Creates a TermsQueryBuilder with an optional boost.
   *
   * @param key The field name.
   * @param values The list of values for the terms query.
   * @param boost The optional boost value (can be null).
   * @return A TermsQueryBuilder instance.
   */
  private static TermsQueryBuilder createTermsQuery(String key, List<?> values, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.termsQuery(key, values.stream().toArray(Object[]::new)).boost(boost);
    } else {
      return QueryBuilders.termsQuery(key, values.stream().toArray(Object[]::new));
    }
  }

  /**
   * Creates a RangeQueryBuilder based on the provided operations and optional boost.
   *
   * @param name The field name.
   * @param rangeOperation Map containing range operators (LTE, LT, GTE, GT) and values.
   * @param boost The optional boost value (can be null).
   * @return A RangeQueryBuilder instance.
   */
  private static RangeQueryBuilder createRangeQuery(
      String name, Map<String, Object> rangeOperation, Float boost) {

    RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(name + RAW_APPEND);
    for (Map.Entry<String, Object> entry : rangeOperation.entrySet()) {
      switch (entry.getKey()) {
        case LTE:
          rangeQueryBuilder.lte(entry.getValue());
          break;
        case LT:
          rangeQueryBuilder.lt(entry.getValue());
          break;
        case GTE:
          rangeQueryBuilder.gte(entry.getValue());
          break;
        case GT:
          rangeQueryBuilder.gt(entry.getValue());
          break;
      }
    }
    if (isNotNull(boost)) {
      return rangeQueryBuilder.boost(boost);
    }
    return rangeQueryBuilder;
  }

  /**
   * Creates a TermQueryBuilder with an optional boost.
   *
   * @param name The field name.
   * @param value The value to search for.
   * @param boost The optional boost value (can be null).
   * @return A TermQueryBuilder instance.
   */
  private static TermQueryBuilder createTermQuery(String name, Object value, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.termQuery(name, value).boost(boost);
    } else {
      return QueryBuilders.termQuery(name, value);
    }
  }

  /**
   * Creates an ExistsQueryBuilder with an optional boost.
   *
   * @param name The field name to check for existence.
   * @param boost The optional boost value (can be null).
   * @return An ExistsQueryBuilder instance.
   */
  private static ExistsQueryBuilder createExistQuery(String name, Float boost) {
    if (isNotNull(boost)) {
      return QueryBuilders.existsQuery(name).boost(boost);
    } else {
      return QueryBuilders.existsQuery(name);
    }
  }

  /**
   * Creates a lexical query (Prefix or Regexp) with optional boosts.
   *
   * @param key The field key.
   * @param rangeOperation Map containing lexical operators (STARTS_WITH, ENDS_WITH) and values.
   * @param boost The optional boost value (can be null).
   * @return A QueryBuilder instance (PrefixQueryBuilder or RegexpQueryBuilder).
   */
  public static QueryBuilder createLexicalQuery(
      String key, Map<String, Object> rangeOperation, Float boost) {
    QueryBuilder queryBuilder = null;
    for (Map.Entry<String, Object> entry : rangeOperation.entrySet()) {
      switch (entry.getKey()) {
        case STARTS_WITH:
          {
            String startsWithVal = (String) entry.getValue();
            if (StringUtils.isNotBlank(startsWithVal)) {
              startsWithVal = startsWithVal.toLowerCase();
            }
            if (isNotNull(boost)) {
              queryBuilder =
                  QueryBuilders.prefixQuery(key + RAW_APPEND, startsWithVal).boost(boost);
            } else {
              queryBuilder = QueryBuilders.prefixQuery(key + RAW_APPEND, startsWithVal);
            }
            break;
          }
        case ENDS_WITH:
          {
            String endsWithRegex = "~" + entry.getValue();
            if (isNotNull(boost)) {
              queryBuilder =
                  QueryBuilders.regexpQuery(key + RAW_APPEND, endsWithRegex).boost(boost);
            } else {
              queryBuilder = QueryBuilders.regexpQuery(key + RAW_APPEND, endsWithRegex);
            }
            break;
          }
      }
    }
    return queryBuilder;
  }

  /**
   * Calculates the elapsed time in milliseconds.
   *
   * @param startTime The start time in milliseconds
   * @return The elapsed time in milliseconds
   */
  public static long calculateEndTime(long startTime) {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * Adds a fuzzy match query to the BoolQueryBuilder.
   *
   * @param query The BoolQueryBuilder to update.
   * @param name The field name to match against.
   * @param value The value to fuzzy match.
   */
  public static void createFuzzyMatchQuery(BoolQueryBuilder query, String name, Object value) {
    if (value != null) {
      query.must(
          QueryBuilders.matchQuery(name, value)
              .fuzziness(Fuzziness.AUTO)
              .fuzzyTranspositions(true));
    }
  }

  /**
   * Creates a SearchDTO from a search query map.
   *
   * @param searchQueryMap Map containing query parameters.
   * @return SearchDTO configured with the provided parameters.
   */
  @SuppressWarnings("unchecked")
  public static SearchDTO createSearchDTO(Map<String, Object> searchQueryMap) {
    SearchDTO search = new SearchDTO();
    search = getBasicBuilders(search, searchQueryMap);
    search = setOffset(search, searchQueryMap);
    search = getLimits(search, searchQueryMap);
    
    if (searchQueryMap.containsKey(JsonKey.GROUP_QUERY)) {
      search
          .getGroupQuery()
          .addAll(
              (Collection<? extends Map<String, Object>>) searchQueryMap.get(JsonKey.GROUP_QUERY));
    }
    
    search = getSoftConstraints(search, searchQueryMap);
    
    // Handle fuzzy search if present
    Map<String, String> fuzzy = (Map<String, String>) searchQueryMap.get(JsonKey.SEARCH_FUZZY);
    if (MapUtils.isNotEmpty(fuzzy)) {
      search.setFuzzy(fuzzy);
    }
    
    return search;
  }

  /**
   * Adds soft constraints from the search query map to the SearchDTO.
   *
   * @param search The SearchDTO to update.
   * @param searchQueryMap Map containing soft constraints.
   * @return The updated SearchDTO.
   */
  @SuppressWarnings("unchecked")
  private static SearchDTO getSoftConstraints(
      SearchDTO search, Map<String, Object> searchQueryMap) {
    if (searchQueryMap.containsKey(JsonKey.SOFT_CONSTRAINTS)) {
      search.setSoftConstraints(
          (Map<String, Integer>) searchQueryMap.get(JsonKey.SOFT_CONSTRAINTS));
    }
    return search;
  }

  /**
   * Adds limit parameter from the search query map to the SearchDTO.
   *
   * @param search The SearchDTO to update.
   * @param searchQueryMap Map containing the limit parameter.
   * @return The updated SearchDTO.
   */
  private static SearchDTO getLimits(SearchDTO search, Map<String, Object> searchQueryMap) {
    if (searchQueryMap.containsKey(JsonKey.LIMIT)) {
      if (searchQueryMap.get(JsonKey.LIMIT) instanceof Integer) {
        search.setLimit((int) searchQueryMap.get(JsonKey.LIMIT));
      } else {
        search.setLimit(((BigInteger) searchQueryMap.get(JsonKey.LIMIT)).intValue());
      }
    }
    return search;
  }

  /**
   * Adds offset parameter from the search query map to the SearchDTO.
   *
   * @param search The SearchDTO to update.
   * @param searchQueryMap Map containing the offset parameter.
   * @return The updated SearchDTO.
   */
  private static SearchDTO setOffset(SearchDTO search, Map<String, Object> searchQueryMap) {
    if (searchQueryMap.containsKey(JsonKey.OFFSET)) {
      if (searchQueryMap.get(JsonKey.OFFSET) instanceof Integer) {
        search.setOffset((int) searchQueryMap.get(JsonKey.OFFSET));
      } else {
        search.setOffset(((BigInteger) searchQueryMap.get(JsonKey.OFFSET)).intValue());
      }
    }
    return search;
  }

  /**
   * Adds basic query parameters to the SearchDTO.
   *
   * @param search The SearchDTO to update.
   * @param searchQueryMap Map containing basic query parameters.
   * @return The updated SearchDTO.
   */
  @SuppressWarnings("unchecked")
  private static SearchDTO getBasicBuilders(SearchDTO search, Map<String, Object> searchQueryMap) {
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
    return search;
  }

  /**
   * Converts Elasticsearch SearchResponse to a response map.
   *
   * @param response The Elasticsearch SearchResponse.
   * @param searchDTO The SearchDTO used for the query.
   * @param finalFacetList List to populate with facet aggregations.
   * @return Map containing search results, facets, and count.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getSearchResponseMap(
      SearchResponse response, SearchDTO searchDTO, List finalFacetList) {
    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> esSource = new ArrayList<>();
    long count = 0;
    
    if (response != null) {
      SearchHits hits = response.getHits();
      count = hits.getTotalHits().value;

      for (SearchHit hit : hits) {
        esSource.add(hit.getSourceAsMap());
      }

      // Fetch aggregations
      finalFacetList = getFinalFacetList(response, searchDTO, finalFacetList);
    }
    
    responseMap.put(JsonKey.CONTENT, esSource);
    if (!finalFacetList.isEmpty()) {
      responseMap.put(JsonKey.FACETS, finalFacetList);
    }
    responseMap.put(JsonKey.COUNT, count);
    return responseMap;
  }

  /**
   * Extracts facet aggregations from the Elasticsearch response.
   *
   * @param response The Elasticsearch SearchResponse.
   * @param searchDTO The SearchDTO containing facet configuration.
   * @param finalFacetList List to populate with facet results.
   * @return The populated facet list.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List getFinalFacetList(
      SearchResponse response, SearchDTO searchDTO, List finalFacetList) {
    if (searchDTO.getFacets() != null && !searchDTO.getFacets().isEmpty()) {
      Map<String, String> facetConfig = searchDTO.getFacets().get(0);
      
      for (Map.Entry<String, String> entry : facetConfig.entrySet()) {
        String field = entry.getKey();
        String aggsType = entry.getValue();
        List<Object> aggsList = new ArrayList<>();
        Map facetMap = new HashMap();
        
        if (JsonKey.DATE_HISTOGRAM.equalsIgnoreCase(aggsType)) {
          Histogram agg = response.getAggregations().get(field);
          for (Histogram.Bucket bucket : agg.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
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
    return finalFacetList;
  }
}
