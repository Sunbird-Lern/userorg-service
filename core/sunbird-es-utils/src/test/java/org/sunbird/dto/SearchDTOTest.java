package org.sunbird.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for SearchDTO.
 */
public class SearchDTOTest {

  /**
   * Test the default constructor of SearchDTO.
   * Verifies that lists and maps are initialized and default values are set.
   */
  @Test
  public void testSearchDTODefaultConstructor() {
    SearchDTO searchDTO = new SearchDTO();
    assertNotNull(searchDTO);
    assertNotNull(searchDTO.getFacets());
    assertNotNull(searchDTO.getSortBy());
    assertNotNull(searchDTO.getAdditionalProperties());
    assertNotNull(searchDTO.getSoftConstraints());
    assertNotNull(searchDTO.getFuzzy());
    assertNotNull(searchDTO.getGroupQuery());
    assertNotNull(searchDTO.getMode());
    assertEquals((Integer) 1000, searchDTO.getLimit());
    assertEquals((Integer) 0, searchDTO.getOffset());
  }

  /**
   * Test the parameterized constructor of SearchDTO.
   * Verifies that properties, operation, and limit are set correctly.
   */
  @Test
  public void testSearchDTOParameterizedConstructor() {
    List<Map> properties = new ArrayList<>();
    String operation = "AND";
    int limit = 50;
    SearchDTO searchDTO = new SearchDTO(properties, operation, limit);
    assertEquals(properties, searchDTO.getProperties());
    assertEquals(operation, searchDTO.getOperation());
    assertEquals((Integer) limit, searchDTO.getLimit());
  }

  /**
   * Test all getters and setters of SearchDTO.
   */
  @Test
  public void testGettersAndSetters() {
    SearchDTO searchDTO = new SearchDTO();

    List<Map> properties = new ArrayList<>();
    searchDTO.setProperties(properties);
    assertEquals(properties, searchDTO.getProperties());

    List<Map<String, String>> facets = new ArrayList<>();
    searchDTO.setFacets(facets);
    assertEquals(facets, searchDTO.getFacets());

    List<String> fields = new ArrayList<>();
    searchDTO.setFields(fields);
    assertEquals(fields, searchDTO.getFields());

    List<String> excludedFields = new ArrayList<>();
    searchDTO.setExcludedFields(excludedFields);
    assertEquals(excludedFields, searchDTO.getExcludedFields());

    Map<String, Object> sortBy = new HashMap<>();
    searchDTO.setSortBy(sortBy);
    assertEquals(sortBy, searchDTO.getSortBy());

    String operation = "OR";
    searchDTO.setOperation(operation);
    assertEquals(operation, searchDTO.getOperation());

    String query = "test query";
    searchDTO.setQuery(query);
    assertEquals(query, searchDTO.getQuery());

    List<String> queryFields = new ArrayList<>();
    searchDTO.setQueryFields(queryFields);
    assertEquals(queryFields, searchDTO.getQueryFields());

    Integer limit = 100;
    searchDTO.setLimit(limit);
    assertEquals(limit, searchDTO.getLimit());

    Integer offset = 10;
    searchDTO.setOffset(offset);
    assertEquals(offset, searchDTO.getOffset());

    boolean fuzzySearch = true;
    searchDTO.setFuzzySearch(fuzzySearch);
    assertTrue(searchDTO.isFuzzySearch());

    Map<String, Object> additionalProperties = new HashMap<>();
    searchDTO.setAdditionalProperties(additionalProperties);
    assertEquals(additionalProperties, searchDTO.getAdditionalProperties());

    Map<String, Integer> softConstraints = new HashMap<>();
    searchDTO.setSoftConstraints(softConstraints);
    assertEquals(softConstraints, searchDTO.getSoftConstraints());

    Map<String, String> fuzzy = new HashMap<>();
    searchDTO.setFuzzy(fuzzy);
    assertEquals(fuzzy, searchDTO.getFuzzy());

    List<Map<String, Object>> groupQuery = new ArrayList<>();
    searchDTO.setGroupQuery(groupQuery);
    assertEquals(groupQuery, searchDTO.getGroupQuery());

    List<String> mode = new ArrayList<>();
    searchDTO.setMode(mode);
    assertEquals(mode, searchDTO.getMode());
  }

  /**
   * Test addAdditionalProperty and getAdditionalProperty methods.
   */
  @Test
  public void testAdditionalPropertyMethods() {
    SearchDTO searchDTO = new SearchDTO();
    String key = "testKey";
    String value = "testValue";

    searchDTO.addAdditionalProperty(key, value);
    assertEquals(value, searchDTO.getAdditionalProperty(key));
  }

  /**
   * Test setting and getting GroupQuery.
   */
  @Test
  public void testGroupQueryUsage() {
      SearchDTO searchDTO = new SearchDTO();
      List<Map<String, Object>> groupQuery = new ArrayList<>();
      Map<String, Object> query1 = new HashMap<>();
      query1.put("field", "value");
      groupQuery.add(query1);

      searchDTO.setGroupQuery(groupQuery);
      assertEquals(1, searchDTO.getGroupQuery().size());
      assertEquals("value", searchDTO.getGroupQuery().get(0).get("field"));
  }
}