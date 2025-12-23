package org.sunbird.actor.location.validator;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.location.impl.LocationDaoFactory;
import org.sunbird.dao.location.impl.LocationDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.model.location.UpsertLocationRequest;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ProjectUtil.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  LocationDaoImpl.class,
  LocationDaoFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationRequestValidatorTest {

  private LocationRequestValidator validator;
  private ElasticSearchService esService;
  private LocationDaoImpl locationDao;

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString()))
        .thenReturn("state,district,block,cluster,school;");
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    PowerMockito.mockStatic(LocationDaoFactory.class);
    locationDao = mock(LocationDaoImpl.class);
    when(LocationDaoFactory.getInstance()).thenReturn(locationDao);
    validator = new LocationRequestValidator();
  }

  @Test
  public void isValidLocationTypeTestSuccess() {
    boolean bool = LocationRequestValidator.isValidLocationType("state");
    Assert.assertTrue(bool);
  }

  @Test
  public void isValidLocationTypeTestFailure() {
    try {
      LocationRequestValidator.isValidLocationType("state2");
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void isValidParentIdAndCodeParentIdMissingTest() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    try {
      validator.isValidParentIdAndCode(request, "create", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (JsonKey.PARENT_ID + " or " + JsonKey.PARENT_CODE));
      Assert.assertEquals(msg, ex.getMessage());
    }
  }

  @Test
  public void isValidParentIdAndCodeParentNotAllowedTest() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("state");
    request.setParentId("parentId");
    request.setParentCode("parentCode");
    try {
      validator.isValidParentIdAndCode(request, "create", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.parentNotAllowed.getErrorMessage(),
              (JsonKey.PARENT_ID + " or " + JsonKey.PARENT_CODE));
      Assert.assertEquals(msg, ex.getMessage());
    }
  }

  @Test
  public void isValidParentIdAndCodeAlreadyExistTest() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    request.setParentId("parentId");
    request.setParentCode("parentCode");

    Map<String, Object> locMap = new HashMap<>();
    List<Map<String, Object>> locList = new ArrayList<>();

    locMap.put("id", "1234");
    locMap.put("channel", "channel004");
    locList.add(locMap);
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.CONTENT, locList);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(contentMap);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    try {
      validator.isValidParentIdAndCode(request, "create", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.errorDuplicateEntry.getErrorMessage(), request.getCode(), JsonKey.CODE);
      Assert.assertEquals(msg, ex.getMessage());
    }
  }

  @Test
  public void isValidParentIdAndCodeAlreadyExist2Test() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    request.setParentId("parentId");
    request.setParentCode("parentCode");

    Map<String, Object> locMap = new HashMap<>();
    List<Map<String, Object>> locList = new ArrayList<>();

    locMap.put("id", "1234");
    locMap.put("channel", "channel004");
    locList.add(locMap);
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.CONTENT, locList);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(contentMap);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    try {
      validator.isValidParentIdAndCode(request, "update", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.errorDuplicateEntry.getErrorMessage(), request.getCode(), JsonKey.CODE);
      Assert.assertEquals(msg, ex.getMessage());
    }
  }

  @Test
  public void isValidParentIdAndCodeInvalidParamTest() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    request.setParentId("parentId");
    request.setParentCode("parentCode");

    Map<String, Object> emptyContentMap = new HashMap<>();
    emptyContentMap.put(JsonKey.CONTENT, new ArrayList<>());

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(emptyContentMap);

    /*Map<String, Object> locMap = new HashMap<>();
    List<Map<String, Object>> locList = new ArrayList<>();
    locMap.put("id", "1234");
    locMap.put("channel", "channel004");
    locList.add(locMap);*/
    Map<String, Object> emptyContentMap2 = new HashMap<>();
    emptyContentMap2.put(JsonKey.CONTENT, new ArrayList<>());

    Promise<Map<String, Object>> promise2 = Futures.promise();
    promise2.success(emptyContentMap2);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future())
        .thenReturn(promise2.future());
    try {
      validator.isValidParentIdAndCode(request, "create", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.invalidParameter.getErrorMessage(), JsonKey.PARENT_CODE);
      Assert.assertEquals(msg, ex.getMessage());
    }
  }

  @Test
  public void isValidParentIdAndCodeTest() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    request.setParentId("parentId");
    request.setParentCode("parentCode");

    Map<String, Object> emptyContentMap = new HashMap<>();
    emptyContentMap.put(JsonKey.CONTENT, new ArrayList<>());

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(emptyContentMap);

    Map<String, Object> locMap = new HashMap<>();
    List<Map<String, Object>> locList = new ArrayList<>();
    locMap.put("id", "1234");
    locMap.put("name", "locName");
    locMap.put("code", "locCode");
    locList.add(locMap);
    Map<String, Object> emptyContentMap2 = new HashMap<>();
    emptyContentMap2.put(JsonKey.CONTENT, locList);

    Promise<Map<String, Object>> promise2 = Futures.promise();
    promise2.success(emptyContentMap2);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future())
        .thenReturn(promise2.future());

    Map<String, Object> loc = new HashMap<>();

    Promise<Map<String, Object>> locPromise = Futures.promise();
    locPromise.success(loc);

    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(locPromise.future());
    try {
      validator.isValidParentIdAndCode(request, "create", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(ResponseCode.invalidParameter.getErrorMessage(), "parentCode");
      Assert.assertEquals(msg, ex.getMessage());
    }
  }

  @Test
  public void isValidParentIdAndCode2Test() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    request.setParentId("parentId");
    request.setParentCode("parentCode");

    Map<String, Object> emptyContentMap = new HashMap<>();
    emptyContentMap.put(JsonKey.CONTENT, new ArrayList<>());

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(emptyContentMap);

    Map<String, Object> locMap = new HashMap<>();
    List<Map<String, Object>> locList = new ArrayList<>();
    locMap.put("id", "1234");
    locMap.put("name", "locName");
    locMap.put("code", "locCode");
    locMap.put("type", "state");
    locList.add(locMap);
    Map<String, Object> emptyContentMap2 = new HashMap<>();
    emptyContentMap2.put(JsonKey.CONTENT, locList);

    Promise<Map<String, Object>> promise2 = Futures.promise();
    promise2.success(emptyContentMap2);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future())
        .thenReturn(promise2.future());

    Map<String, Object> loc = new HashMap<>();
    loc.put("id", "1234");
    loc.put("name", "locName");
    loc.put("code", "locCode");
    loc.put("type", "state");
    Promise<Map<String, Object>> locPromise = Futures.promise();
    locPromise.success(loc);

    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(locPromise.future());
    boolean isValid = validator.isValidParentIdAndCode(request, "update", new RequestContext());
    Assert.assertTrue(isValid);
  }

  @Test
  public void isLocationHasChildTest() {
    UpsertLocationRequest request = new UpsertLocationRequest();
    request.setCode("code");
    request.setId("id");
    request.setName("name");
    request.setType("district");
    request.setParentId("parentId");
    request.setParentCode("parentCode");

    Map<String, Object> locMap = new HashMap<>();
    List<Map<String, Object>> locList = new ArrayList<>();
    locMap.put("id", "1234");
    locMap.put("name", "locName");
    locMap.put("code", "locCode");
    locMap.put("type", "state");
    locList.add(locMap);
    Map<String, Object> emptyContentMap2 = new HashMap<>();
    emptyContentMap2.put(JsonKey.CONTENT, locList);

    Promise<Map<String, Object>> promise2 = Futures.promise();
    promise2.success(emptyContentMap2);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise2.future());

    Map<String, Object> loc = new HashMap<>();
    loc.put("id", "1234");
    loc.put("name", "locName");
    loc.put("code", "locCode");
    loc.put("type", "state");
    Promise<Map<String, Object>> locPromise = Futures.promise();
    locPromise.success(loc);

    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(locPromise.future());

    try {
      validator.isLocationHasChild("stateid", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(
          ResponseCode.invalidLocationDeleteRequest.getErrorMessage(), ex.getMessage());
    }
  }

  @Test
  public void getValidatedLocationSetTest() {
    LocationRequestValidator validator = new LocationRequestValidator();
    Location location = new Location();
    location.setCode("code");
    location.setId("id");
    location.setName("Name");
    location.setType("state");
    List<Location> locList = new ArrayList<>();
    locList.add(location);
    Set<String> loc = validator.getValidatedLocationSet(locList, null);
    Assert.assertNotNull(loc);
  }

  @Test
  public void getValidatedLocationIdsTest() {

    PowerMockito.when(locationDao.search(Mockito.any(), Mockito.any()))
        .thenReturn(getLocationRecords());
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("type", "state");
    map.put("id", "id1");
    map.put("code", "code1");
    list.add(map);
    response.put(Constants.RESPONSE, list);
    PowerMockito.when(locationDao.read(Mockito.any(), Mockito.any())).thenReturn(response);
    LocationRequestValidator validator = new LocationRequestValidator();
    List<String> locList = new ArrayList<>();
    locList.add("code1");
    locList.add("code2");
    List<String> loc = validator.getValidatedLocationIds(locList, new RequestContext());
    Assert.assertNotNull(loc);
  }

  @Test
  public void getHierarchyLocationIdsTest() {

    PowerMockito.when(locationDao.search(Mockito.any(), Mockito.any()))
        .thenReturn(getLocationRecords());
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("type", "state");
    map.put("id", "id1");
    map.put("code", "code1");
    list.add(map);
    response.put(Constants.RESPONSE, list);
    PowerMockito.when(locationDao.read(Mockito.any(), Mockito.any())).thenReturn(response);
    LocationRequestValidator validator = new LocationRequestValidator();
    List<String> locList = new ArrayList<>();
    locList.add("id1");
    locList.add("id2");
    List<String> loc = validator.getHierarchyLocationIds(locList, new RequestContext());
    Assert.assertNotNull(loc);
  }

  @Test
  public void validateSearchLocationRequestTest() {
    List<String> filters = new ArrayList<>();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.FILTERS, filters);
    Request req = new Request();
    req.setRequest(reqMap);
    try {
      validator.validateSearchLocationRequest(req);
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  @Test
  public void validateSearchLocationEmptyRequestTest() {
    Map<String, Object> reqMap = new HashMap<>();
    Request req = new Request();
    req.setRequest(reqMap);
    try {
      validator.validateSearchLocationRequest(req);
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  @Test
  public void validateDeleteLocationRequestTest() {
    try {
      validator.validateDeleteLocationRequest("");
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.mandatoryParamsMissing.getErrorCode());
    }
  }

  private static Response getLocationRecords() {
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
}
