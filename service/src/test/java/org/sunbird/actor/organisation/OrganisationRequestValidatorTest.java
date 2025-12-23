package org.sunbird.actor.organisation;

import static org.powermock.api.mockito.PowerMockito.*;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.sunbird.actor.organisation.validator.OrganisationRequestValidator;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  ServiceFactory.class,
  CassandraOperationImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class OrganisationRequestValidatorTest {
  private static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(false));
    PowerMockito.when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getOrgListResponse());
  }

  @Test
  public void isTenantValidTest() {
    OrganisationRequestValidator validator = new OrganisationRequestValidator();
    try {
      validator.isTenantIdValid("orgId", new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  @Test
  public void validateOrgLocationWithLocationCodeListTest() {
    OrganisationRequestValidator validator = new OrganisationRequestValidator();
    List codeList = getLocationCodesLists();
    Map requestMap = new HashMap<String, Object>();
    requestMap.put(JsonKey.LOCATION_CODE, codeList);
    validator.validateOrgLocation(requestMap, new RequestContext());
  }

  @Test
  public void validateChannelInvalidChannelTest() {

    OrganisationRequestValidator validator = new OrganisationRequestValidator();
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.CHANNEL, "channel");
    try {
      validator.validateChannel(requestMap, new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.invalidParameter.getErrorCode());
    }
  }

  @Test
  public void validateChannelValidChannelInactiveOrgTest() {

    OrganisationRequestValidator validator = new OrganisationRequestValidator();
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.CHANNEL, "channel");
    try {
      validator.validateChannel(requestMap, new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.errorInactiveOrg.getErrorCode());
    }
  }

  @Test
  public void validateOrgLocationWithInvalidOrgLocationTest() {
    OrganisationRequestValidator validator = new OrganisationRequestValidator();
    List codeList = getInvalidOrgLocationLists();
    Map requestMap = new HashMap<String, Object>();
    requestMap.put(JsonKey.ORG_LOCATION, codeList);
    try {
      validator.validateOrgLocation(requestMap, new RequestContext());
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
      Assert.assertEquals(ex.getErrorCode(), ResponseCode.invalidParameterValue.getErrorCode());
    }
  }

  @Test
  public void validateOrgLocationWithOrgLocationTest() {
    OrganisationRequestValidator validator = new OrganisationRequestValidator();
    List codeList = getOrgLocationLists();
    Map requestMap = new HashMap<String, Object>();
    requestMap.put(JsonKey.ORG_LOCATION, codeList);
    validator.validateOrgLocation(requestMap, new RequestContext());
  }

  public static List<String> getLocationIdsLists() {
    List<String> locationIds = new ArrayList<>();
    locationIds.add("location1");
    locationIds.add("location2");
    return locationIds;
  }

  public static List<String> getLocationCodesLists() {
    List<String> locationCodes = new ArrayList<>();
    locationCodes.add("code1");
    locationCodes.add("code2");
    return locationCodes;
  }

  public static List<Map<String, String>> getInvalidOrgLocationLists() {
    List<Map<String, String>> locationCodes = new ArrayList<>();
    Map map = new HashMap<String, Object>();
    map.put(JsonKey.ID, "location3");
    map.put(JsonKey.TYPE, "state");
    locationCodes.add(map);
    Map map1 = new HashMap<String, Object>();
    map1.put(JsonKey.ID, "location5");
    map1.put(JsonKey.TYPE, "district");
    map1.put(JsonKey.PARENT_ID, "location4");
    locationCodes.add(map1);
    return locationCodes;
  }

  public static List<Map<String, String>> getOrgLocationLists() {
    List<Map<String, String>> locationCodes = new ArrayList<>();
    Map map = new HashMap<String, Object>();
    map.put(JsonKey.ID, "location1");
    map.put(JsonKey.TYPE, "state");
    locationCodes.add(map);
    Map map1 = new HashMap<String, Object>();
    map1.put(JsonKey.ID, "location2");
    map1.put(JsonKey.TYPE, "district");
    map1.put(JsonKey.PARENT_ID, "location1");
    locationCodes.add(map1);
    return locationCodes;
  }

  private static Map<String, Object> getEsResponse(boolean empty) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, String>> locationCodes = new ArrayList<>();
    Map map = new HashMap<String, Object>();
    map.put(JsonKey.ID, "location1");
    map.put(JsonKey.TYPE, "state");
    locationCodes.add(map);
    Map map1 = new HashMap<String, Object>();
    map1.put(JsonKey.ID, "location2");
    map1.put(JsonKey.TYPE, "district");
    map1.put(JsonKey.PARENT_ID, "location1");
    locationCodes.add(map1);
    response.put(JsonKey.CONTENT, locationCodes);
    return response;
  }

  private Response getOrgListResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.IS_TENANT, true);
    orgMap.put(JsonKey.ID, "location1");
    orgMap.put(JsonKey.TYPE, "state");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
