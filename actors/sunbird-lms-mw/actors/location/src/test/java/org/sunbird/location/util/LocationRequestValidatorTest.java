package org.sunbird.location.util;

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
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;
import scala.concurrent.Future;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ProjectUtil.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
})
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
  public void isValidParentIdAndCodeTest() {
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
    Future<Map<String, Object>> test = promise.future();
    SearchDTO searchDTO = new SearchDTO();
    when(Util.createSearchDto(Mockito.anyMap())).thenReturn(searchDTO);
    when(esService.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName(), null))
        .thenReturn(promise.future());
    try {
      LocationRequestValidator.isValidParentIdAndCode(request, "create");
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
    }
  }
}
