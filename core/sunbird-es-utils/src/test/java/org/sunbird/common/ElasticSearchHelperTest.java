package org.sunbird.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.sunbird.dto.SearchDTO;
import org.sunbird.keys.JsonKey;

/**
 * Unit tests for ElasticSearchHelper.
 */
public class ElasticSearchHelperTest {

  /**
   * Test getSortOrder method.
   * Verifies correct SortOrder enum is returned for valid strings and default is DESC.
   */
  @Test
  public void testGetSortOrder() {
    assertEquals(SortOrder.ASC, ElasticSearchHelper.getSortOrder("ASC"));
    assertEquals(SortOrder.ASC, ElasticSearchHelper.getSortOrder("asc"));
    assertEquals(SortOrder.DESC, ElasticSearchHelper.getSortOrder("DESC"));
    assertEquals(SortOrder.DESC, ElasticSearchHelper.getSortOrder("desc"));
    assertEquals(SortOrder.DESC, ElasticSearchHelper.getSortOrder("invalid"));
  }

  /**
   * Test createMatchQuery method with boost.
   */
  @Test
  public void testCreateMatchQuery() {
    MatchQueryBuilder query = ElasticSearchHelper.createMatchQuery("fieldName", "value", 1.5f);
    assertNotNull(query);
    assertEquals("fieldName", query.fieldName());
    assertEquals("value", query.value());
  }

  /**
   * Test createMatchQuery method without boost.
   */
  @Test
  public void testCreateMatchQueryWithoutBoost() {
    MatchQueryBuilder query = ElasticSearchHelper.createMatchQuery("fieldName", "value", null);
    assertNotNull(query);
    assertEquals("fieldName", query.fieldName());
    assertEquals("value", query.value());
  }

  /**
   * Test getConstraints method with valid constraints.
   */
  @Test
  public void testGetConstraints() {
    SearchDTO searchDTO = new SearchDTO();
    Map<String, Integer> softConstraints = new HashMap<>();
    softConstraints.put("field1", 10);
    softConstraints.put("field2", 5);
    searchDTO.setSoftConstraints(softConstraints);

    Map<String, Float> constraints = ElasticSearchHelper.getConstraints(searchDTO);
    assertNotNull(constraints);
    assertEquals(2, constraints.size());
    assertEquals(10.0f, constraints.get("field1"), 0.001);
    assertEquals(5.0f, constraints.get("field2"), 0.001);
  }

  /**
   * Test getConstraints method with empty constraints.
   */
  @Test
  public void testGetConstraintsEmpty() {
    SearchDTO searchDTO = new SearchDTO();
    Map<String, Float> constraints = ElasticSearchHelper.getConstraints(searchDTO);
    assertNotNull(constraints);
    assertTrue(constraints.isEmpty());
  }

  /**
   * Test calculateEndTime method.
   */
  @Test
  public void testCalculateEndTime() {
    long startTime = System.currentTimeMillis();
    long endTime = ElasticSearchHelper.calculateEndTime(startTime);
    assertTrue(endTime >= 0);
  }

  /**
   * Test createSearchDTO method with standard Integer limit/offset.
   */
  @Test
  public void testCreateSearchDTO() {
    Map<String, Object> searchQueryMap = new HashMap<>();
    searchQueryMap.put(JsonKey.QUERY, "test query");
    searchQueryMap.put(JsonKey.LIMIT, 20);
    searchQueryMap.put(JsonKey.OFFSET, 5);

    List<String> fields = new ArrayList<>();
    fields.add("field1");
    searchQueryMap.put(JsonKey.FIELDS, fields);

    SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(searchQueryMap);

    assertNotNull(searchDTO);
    assertEquals("test query", searchDTO.getQuery());
    assertEquals((Integer) 20, searchDTO.getLimit());
    assertEquals((Integer) 5, searchDTO.getOffset());
    assertEquals(fields, searchDTO.getFields());
  }

  /**
   * Test createSearchDTO method with BigInteger limit/offset.
   */
  @Test
  public void testCreateSearchDTOWithBigInteger() {
      Map<String, Object> searchQueryMap = new HashMap<>();
      searchQueryMap.put(JsonKey.LIMIT, BigInteger.valueOf(20));
      searchQueryMap.put(JsonKey.OFFSET, BigInteger.valueOf(5));

      SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(searchQueryMap);

      assertNotNull(searchDTO);
      assertEquals((Integer) 20, searchDTO.getLimit());
      assertEquals((Integer) 5, searchDTO.getOffset());
  }

  /**
   * Test createLexicalQuery method for STARTS_WITH operation.
   */
  @Test
  public void testCreateLexicalQueryStartsWith() {
    Map<String, Object> operation = new HashMap<>();
    operation.put(ElasticSearchHelper.STARTS_WITH, "prefix");
    QueryBuilder query = ElasticSearchHelper.createLexicalQuery("field", operation, null);
    assertNotNull(query);
    assertTrue(query.toString().contains("prefix"));
  }

  /**
   * Test createLexicalQuery method for ENDS_WITH operation.
   */
  @Test
  public void testCreateLexicalQueryEndsWith() {
    Map<String, Object> operation = new HashMap<>();
    operation.put(ElasticSearchHelper.ENDS_WITH, "suffix");
    QueryBuilder query = ElasticSearchHelper.createLexicalQuery("field", operation, null);
    assertNotNull(query);
    assertTrue(query.toString().contains("~suffix"));
  }

  /**
   * Test addAdditionalProperties method for FILTERS.
   */
  @Test
  public void testAddAdditionalPropertiesFilters() {
      BoolQueryBuilder query = QueryBuilders.boolQuery();
      Map<String, Object> entryValue = new HashMap<>();
      entryValue.put("status", "active");

      Map.Entry<String, Object> entry = new java.util.AbstractMap.SimpleEntry<>(JsonKey.FILTERS, entryValue);
      Map<String, Float> constraints = new HashMap<>();

      ElasticSearchHelper.addAdditionalProperties(query, entry, constraints);

      String queryString = query.toString();
      assertTrue(queryString.contains("status.raw"));
      assertTrue(queryString.contains("active"));
  }

  /**
   * Test addAdditionalProperties method for EXISTS.
   */
  @Test
  public void testAddAdditionalPropertiesExists() {
      BoolQueryBuilder query = QueryBuilders.boolQuery();
      List<String> fields = Arrays.asList("field1", "field2");

      Map.Entry<String, Object> entry = new java.util.AbstractMap.SimpleEntry<>(JsonKey.EXISTS, fields);
      Map<String, Float> constraints = new HashMap<>();

      ElasticSearchHelper.addAdditionalProperties(query, entry, constraints);

      String queryString = query.toString();
      assertTrue(queryString.contains("exists"));
      assertTrue(queryString.contains("field1"));
      assertTrue(queryString.contains("field2"));
  }

  /**
   * Test addAdditionalProperties method for NOT_EXISTS.
   */
  @Test
  public void testAddAdditionalPropertiesNotExists() {
      BoolQueryBuilder query = QueryBuilders.boolQuery();
      List<String> fields = Arrays.asList("field1");

      Map.Entry<String, Object> entry = new java.util.AbstractMap.SimpleEntry<>(JsonKey.NOT_EXISTS, fields);
      Map<String, Float> constraints = new HashMap<>();

      ElasticSearchHelper.addAdditionalProperties(query, entry, constraints);

      String queryString = query.toString();
      assertTrue(queryString.contains("must_not"));
      assertTrue(queryString.contains("exists"));
      assertTrue(queryString.contains("field1"));
  }

  /**
   * Test createFuzzyMatchQuery method.
   */
  @Test
  public void testCreateFuzzyMatchQuery() {
      BoolQueryBuilder query = QueryBuilders.boolQuery();
      ElasticSearchHelper.createFuzzyMatchQuery(query, "name", "sunbird");

      String queryString = query.toString();
      assertTrue(queryString.contains("fuzziness"));
      assertTrue(queryString.contains("AUTO"));
  }

  /**
   * Test addAdditionalProperties method for NESTED_EXISTS.
   */
  @Test
  public void testAddAdditionalPropertiesNestedExists() {
      BoolQueryBuilder query = QueryBuilders.boolQuery();
      Map<String, String> nestedFields = new HashMap<>();
      nestedFields.put("nestedField", "nestedPath");

      Map.Entry<String, Object> entry = new java.util.AbstractMap.SimpleEntry<>(JsonKey.NESTED_EXISTS, nestedFields);
      Map<String, Float> constraints = new HashMap<>();

      ElasticSearchHelper.addAdditionalProperties(query, entry, constraints);

      String queryString = query.toString();
      assertTrue(queryString.contains("nested"));
      assertTrue(queryString.contains("nestedPath"));
      assertTrue(queryString.contains("exists"));
      assertTrue(queryString.contains("nestedField"));
  }
}