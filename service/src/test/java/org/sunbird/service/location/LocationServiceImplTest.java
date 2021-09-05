package org.sunbird.service.location;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.Constants;
import org.sunbird.dao.location.LocationDao;
import org.sunbird.dao.location.impl.LocationDaoFactory;
import org.sunbird.dao.location.impl.LocationDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocationDaoImpl.class, LocationDaoFactory.class, DataCacheHandler.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationServiceImplTest {
  @Before
  public void setUp() {
    PowerMockito.mockStatic(LocationDaoFactory.class);
    LocationDao locationDao = mock(LocationDaoImpl.class);
    when(LocationDaoFactory.getInstance()).thenReturn(locationDao);
    PowerMockito.when(locationDao.search(Mockito.any(), Mockito.any()))
        .thenReturn(getLocationRecords());
    PowerMockito.when(locationDao.read(Mockito.any(), Mockito.any()))
        .thenReturn(getLocationRecords());
    PowerMockito.when(
            locationDao.getLocationsByIds(Mockito.anyList(), Mockito.anyList(), Mockito.any()))
        .thenReturn(getLocationRecords());
    PowerMockito.when(locationDao.create(Mockito.any(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    PowerMockito.when(locationDao.update(Mockito.any(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    PowerMockito.when(locationDao.delete(Mockito.any(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(DataCacheHandler.getLocationOrderMap()).thenReturn(getLocationOrderMap());
  }

  @Test
  public void getLocationByIdTest() {
    LocationService locationService = LocationServiceImpl.getInstance();
    Location location = locationService.getLocationById("locationId", new RequestContext());
    Assert.assertNotNull(location);
  }

  @Test
  public void getLocationByIdsTest() {
    LocationService locationService = LocationServiceImpl.getInstance();
    List<String> locationIds = new ArrayList<>();
    locationIds.add("locationId");
    List<Map<String, Object>> locationList =
        locationService.getLocationsByIds(locationIds, new ArrayList<>(), new RequestContext());
    Assert.assertNotNull(locationList);
  }

  @Test
  public void testGetValidatedRelatedLocationIdAndType() {

    LocationService locationService = LocationServiceImpl.getInstance();
    List<String> codeList = getCodeList();
    List<Map<String, String>> locationIdType =
        locationService.getValidatedRelatedLocationIdAndType(codeList, new RequestContext());
    assertEquals(result(), locationIdType);
  }

  @Test
  public void testGetValidatedRelatedLocationIds() {
    LocationService locationService = LocationServiceImpl.getInstance();
    List<String> codeList = getCodeList();
    List<String> locationIdType =
        locationService.getValidatedRelatedLocationIds(codeList, new RequestContext());
    assertEquals(resultIdList(), locationIdType);
  }

  @Test
  public void testCreateLocation() {
    LocationService locationService = LocationServiceImpl.getInstance();
    Location loc = new Location();
    loc.setId("locId1");
    loc.setCode("locCode1");
    loc.setName("locName1");
    loc.setType("state");
    Response response = locationService.createLocation(loc, new RequestContext());
    Assert.assertNotNull(response.getResult().get(JsonKey.ID));
  }

  @Test
  public void updateLocation() {
    LocationService locationService = LocationServiceImpl.getInstance();
    Location loc = new Location();
    loc.setId("locId1");
    loc.setCode("locCode1");
    loc.setName("locName1");
    loc.setType("state");
    Response response = locationService.updateLocation(loc, new RequestContext());
    Assert.assertNotNull(response.getResult().get(JsonKey.ID));
  }

  @Test
  public void deleteLocation() {
    LocationService locationService = LocationServiceImpl.getInstance();
    Response response = locationService.deleteLocation("locId1", new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void searchLocation() {
    Map<String, Object> filter = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filter.put(JsonKey.ID, "locId1");
    searchRequestMap.put(JsonKey.FILTERS, filter);
    LocationService locationService = LocationServiceImpl.getInstance();
    Response response = locationService.searchLocation(searchRequestMap, new RequestContext());
    Assert.assertNotNull(response);
  }

  public List<String> getCodeList() {
    List<String> codeList = new ArrayList<>();
    codeList.add("code1");
    codeList.add("code2");
    return codeList;
  }

  private Response getLocationRecords() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("type", "state");
    map.put("id", "id1");
    map.put("code", "code1");
    Map<String, Object> map1 = new HashMap<>();
    map1.put("type", "district");
    map1.put("id", "id2");
    map1.put("code", "code2");
    map1.put("parentId", "id1");
    list.add(map);
    list.add(map1);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  public static Map<String, Integer> getLocationOrderMap() {
    Map<String, Integer> orderMap = new HashMap<>();
    orderMap.put("state", 1);
    orderMap.put("district", 2);
    return orderMap;
  }

  public static List<Map<String, String>> result() {
    List<Map<String, String>> result = new ArrayList<>();
    Map<String, String> idType1 = new HashMap<>();
    idType1.put("id", "id2");
    idType1.put("type", "district");
    result.add(idType1);
    Map<String, String> idType = new HashMap<>();
    idType.put("id", "id1");
    idType.put("type", "state");
    result.add(idType);
    return result;
  }

  public static List<String> resultIdList() {
    List<String> result = new ArrayList<>();
    result.add("id2");
    result.add("id1");
    return result;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.ID, "locId1");
    return response;
  }
}
