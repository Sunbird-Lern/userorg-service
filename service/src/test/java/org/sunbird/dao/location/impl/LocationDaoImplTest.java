package org.sunbird.dao.location.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationDaoImplTest {
  @Before
  public void setUp() {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchRestHighImpl esSearch = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esSearch);
    Map<String, Object> esRespone = new HashMap<>();
    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    esRespone.put(JsonKey.LOCATION_TYPE, "STATE");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esRespone);
    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());

    Promise<String> promiseL = Futures.promise();
    promiseL.success("4654546-879-54656");
    when(esSearch.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(promiseL.future());

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSuccessResponse());
  }

  @Test
  public void getRecordByPropertyTest() {
    LocationDaoImpl dao = new LocationDaoImpl();
    Map<String, Object> search = new HashMap<>();
    search.put(JsonKey.CODE, "code");
    dao.getRecordByProperty(search, new RequestContext());
    Assert.assertTrue(true);
  }

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

  @Test
  public void create() {
    LocationDaoImpl dao = new LocationDaoImpl();
    Location loc = new Location();
    loc.setId("locId1");
    loc.setCode("locCode1");
    loc.setName("locName1");
    loc.setType("state");
    Response response = dao.create(loc, new RequestContext());
    Assert.assertNotNull(response.getResult().get(JsonKey.ID));
  }

  @Test
  public void update() {
    LocationDaoImpl dao = new LocationDaoImpl();
    Location loc = new Location();
    loc.setId("locId1");
    loc.setCode("locCode1");
    loc.setName("locName1");
    loc.setType("state");
    Response response = dao.update(loc, new RequestContext());
    Assert.assertNotNull(response.getResult().get(JsonKey.ID));
  }

  @Test
  public void delete() {
    LocationDaoImpl dao = new LocationDaoImpl();
    Response response = dao.delete("locId1", new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void read() {
    LocationDaoImpl dao = new LocationDaoImpl();
    Response response = dao.read("locId1", new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void search() {
    Map<String, Object> filter = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filter.put(JsonKey.ID, "locId1");
    searchRequestMap.put(JsonKey.FILTERS, filter);
    LocationDaoImpl dao = new LocationDaoImpl();
    Response response = dao.search(searchRequestMap, new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void saveToEs() {
    Map<String, Object> data = new HashMap<>();
    data.put(JsonKey.ID, "546546-6787-5476");
    data.put(JsonKey.LOCATION_CODES, "code");
    LocationDaoImpl dao = new LocationDaoImpl();
    String response = dao.saveLocationToEs("546546-6787-5476", data, new RequestContext());
    Assert.assertNotNull(response);
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.ID, "locId1");
    return response;
  }
}
