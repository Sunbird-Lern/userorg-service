package org.sunbird.actor.location.validator;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectUtil.class, EsClientFactory.class, ElasticSearchRestHighImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationRequestValidatorTest {

  private static ElasticSearchService esService;

  @BeforeClass
  public static void before() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString()))
        .thenReturn("state,district,block,cluster,school;");
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
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
      LocationRequestValidator.isValidParentIdAndCode(request, "create");
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
      LocationRequestValidator.isValidParentIdAndCode(request, "create");
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
      LocationRequestValidator.isValidParentIdAndCode(request, "create");
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.alreadyExists.getErrorMessage(), JsonKey.CODE, request.getCode());
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
      LocationRequestValidator.isValidParentIdAndCode(request, "update");
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      String msg =
          ProjectUtil.formatMessage(
              ResponseCode.alreadyExists.getErrorMessage(), JsonKey.CODE, request.getCode());
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
      LocationRequestValidator.isValidParentIdAndCode(request, "create");
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
      LocationRequestValidator.isValidParentIdAndCode(request, "create");
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

    boolean isValid = LocationRequestValidator.isValidParentIdAndCode(request, "update");
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
      LocationRequestValidator.isLocationHasChild("stateid");
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(
          ResponseCode.invalidLocationDeleteRequest.getErrorMessage(), ex.getMessage());
    }
  }


}
