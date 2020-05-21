package org.sunbird.location.dao.impl;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.dto.SearchDTO;

public class LocationDaoImplTest {

  @Test
  public void addSortBySuccess() {
    LocationDaoImpl dao = new LocationDaoImpl();
    SearchDTO searchDto = createSearchDtoObj();
    searchDto = dao.addSortBy(searchDto);
    Assert.assertTrue(searchDto.getSortBy().size() == 1);
  }

  @Test
  public void sortByNotAddedInCaseFilterWontHaveTypeKey() {
    LocationDaoImpl dao = new LocationDaoImpl();
    SearchDTO searchDto = createSearchDtoObj();
    ((Map<String, Object>) searchDto.getAdditionalProperties().get(JsonKey.FILTERS))
        .remove(JsonKey.TYPE);
    searchDto = dao.addSortBy(searchDto);
    Assert.assertTrue(searchDto.getSortBy().size() == 0);
  }

  @Test
  public void sortByNotAddedInCasePresent() {
    LocationDaoImpl dao = new LocationDaoImpl();
    SearchDTO searchDto = createSearchDtoObj();
    searchDto.getSortBy().put("some key", "DESC");
    searchDto = dao.addSortBy(searchDto);
    Assert.assertTrue(searchDto.getSortBy().size() == 1);
  }

  private SearchDTO createSearchDtoObj() {
    SearchDTO searchDto = new SearchDTO();
    Map<String, Object> propertyMap = new HashMap<String, Object>();
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(JsonKey.TYPE, "state");
    propertyMap.put(JsonKey.FILTERS, filterMap);
    searchDto.setAdditionalProperties(propertyMap);
    return searchDto;
  }
}
