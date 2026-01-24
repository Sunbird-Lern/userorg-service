package org.sunbird.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Elasticsearch query operations.
 * Encapsulates all parameters required for building and executing ES queries including
 * search criteria, pagination, sorting, facets, and fuzzy search options.
 */
public class SearchDTO {

  /** List of property filters for the search query. */
  @SuppressWarnings("rawtypes")
  private List<Map> properties;

  /** List of facet aggregations to compute. */
  private List<Map<String, String>> facets = new ArrayList<>();

  /** Fields to include in search results. */
  private List<String> fields;

  /** Fields to exclude from search results. */
  private List<String> excludedFields;

  /** Sorting criteria as field-order pairs. */
  private Map<String, Object> sortBy = new HashMap<>();

  /** Logical operation for combining search criteria (AND/OR). */
  private String operation;

  /** Free-text search query string. */
  private String query;

  /** Specific fields to search within for the query. */
  private List<String> queryFields;

  /** Maximum number of results to return. Default: 1000 */
  private Integer limit = 1000;

  /** Number of results to skip for pagination. Default: 0 */
  private Integer offset = 0;

  /** Enable fuzzy matching for search queries. */
  private boolean fuzzySearch = false;

  /** Additional filter properties including filters, exists, and not-exists conditions. */
  private Map<String, Object> additionalProperties = new HashMap<>();

  /** Soft constraints with priority weights for ranking. */
  private Map<String, Integer> softConstraints = new HashMap<>();

  /** Fuzzy search configuration parameters. */
  private Map<String, String> fuzzy = new HashMap<>();

  /** Grouped query clauses for complex boolean queries. */
  private List<Map<String, Object>> groupQuery = new ArrayList<>();

  /** Query execution modes. */
  private List<String> mode = new ArrayList<>();

  /** Default constructor. */
  public SearchDTO() {
    super();
  }

  /**
   * Constructor with basic search parameters.
   *
   * @param properties list of property filters
   * @param operation logical operation (AND/OR)
   * @param limit maximum results to return
   */
  @SuppressWarnings("rawtypes")
  public SearchDTO(List<Map> properties, String operation, int limit) {
    super();
    this.properties = properties;
    this.operation = operation;
    this.limit = limit;
  }

  @SuppressWarnings("rawtypes")
  public List<Map> getProperties() {
    return properties;
  }

  @SuppressWarnings("rawtypes")
  public void setProperties(List<Map> properties) {
    this.properties = properties;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public List<Map<String, String>> getFacets() {
    return facets;
  }

  public void setFacets(List<Map<String, String>> facets) {
    this.facets = facets;
  }

  public Map<String, Object> getSortBy() {
    return sortBy;
  }

  public void setSortBy(Map<String, Object> sortBy) {
    this.sortBy = sortBy;
  }

  public boolean isFuzzySearch() {
    return fuzzySearch;
  }

  public void setFuzzySearch(boolean fuzzySearch) {
    this.fuzzySearch = fuzzySearch;
  }

  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  /**
   * Retrieves a specific additional property by key.
   *
   * @param key property key
   * @return property value or null if not found
   */
  public Object getAdditionalProperty(String key) {
    return additionalProperties.get(key);
  }

  /**
   * Adds a single additional property.
   *
   * @param key property key
   * @param value property value
   */
  public void addAdditionalProperty(String key, Object value) {
    this.additionalProperties.put(key, value);
  }

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public Map<String, Integer> getSoftConstraints() {
    return softConstraints;
  }

  public void setSoftConstraints(Map<String, Integer> softConstraints) {
    this.softConstraints = softConstraints;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public List<String> getMode() {
    return mode;
  }

  public void setMode(List<String> mode) {
    this.mode = mode;
  }

  public List<String> getExcludedFields() {
    return excludedFields;
  }

  public void setExcludedFields(List<String> excludedFields) {
    this.excludedFields = excludedFields;
  }

  public List<String> getQueryFields() {
    return queryFields;
  }

  public void setQueryFields(List<String> queryFields) {
    this.queryFields = queryFields;
  }

  public Map<String, String> getFuzzy() {
    return fuzzy;
  }

  public void setFuzzy(Map<String, String> fuzzy) {
    this.fuzzy = fuzzy;
  }

  public List<Map<String, Object>> getGroupQuery() {
    return groupQuery;
  }

  public void setGroupQuery(List<Map<String, Object>> groupQuery) {
    this.groupQuery = groupQuery;
  }
}
