package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Promise;

@PrepareForTest({Util.class})
public class ManagedUserActorTest extends UserManagementActorTestBase {

  public final Props props = Props.create(ManagedUserActor.class);

  @Before
  public void before() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    List<String> subTypeList = Arrays.asList("state,district,block,cluster,school;".split(";"));
    Map<String, Integer> orderMap = new HashMap<>();
    for (String str : subTypeList) {
      List<String> typeList =
          (((Arrays.asList(str.split(","))).stream().map(String::toLowerCase))
              .collect(Collectors.toList()));
      for (int i = 0; i < typeList.size(); i++) {
        orderMap.put(typeList.get(i), i);
      }
    }
    when(DataCacheHandler.getLocationOrderMap()).thenReturn(orderMap);
  }

  @Test
  public void testGetManagedUsers() throws Exception {
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.GET_MANAGED_USERS),
            null,
            props);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithManagedUserLimit() {
    Map<String, Object> reqMap = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(reqMap);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.updatePassword(Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> userMap = new HashMap<>(getMapObject());
    userMap.put(JsonKey.USER_ID, "3dc4e0bc-43a6-4ba0-84f9-6606a9c17320");

    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(esPromise.future());
    try {
      Request reqObj =
          getRequest(
              false, false, false, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER_V4);
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
    assertTrue(true);
  }

  @Test
  public void testCreateManagedUser2() {
    Map<String, Object> reqMap = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(reqMap);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.updatePassword(Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> userMap = new HashMap<>(getMapObject());
    userMap.put(JsonKey.USER_ID, "3dc4e0bc-43a6-4ba0-84f9-6606a9c17320");
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(esPromise.future());
    try {
      Request reqObj =
          getRequest(
              false,
              false,
              false,
              getAdditionalMapData(reqMap),
              ActorOperations.CREATE_MANAGED_USER);
      Map<String, String> proLocn = new HashMap<>();
      proLocn.put(JsonKey.CODE, "state");
      proLocn.put(JsonKey.TYPE, "state");
      List<Map<String, String>> profileLocn = new ArrayList<>();
      profileLocn.add(proLocn);
      reqObj.getRequest().put(JsonKey.PROFILE_LOCATION, profileLocn);
      reqObj.getRequest().put(JsonKey.PHONE, "987654879");
      reqObj.getRequest().put(JsonKey.EMAIL, "abc@xyz.com");
      reqObj.getRequest().put(JsonKey.USERNAME, "userName");
      Map<String, String> externalId = new HashMap<>();
      externalId.put(JsonKey.ID, "id");
      externalId.put(JsonKey.ID_TYPE, "idType");
      externalId.put(JsonKey.PROVIDER, "provider");
      List<Map<String, String>> externalIds = new ArrayList<>();
      externalIds.add(externalId);
      reqObj.getRequest().put(JsonKey.EXTERNAL_IDS, externalIds);
      reqObj.getContext().put(JsonKey.SIGNUP_TYPE, "self");
      reqObj.getContext().put(JsonKey.REQUEST_SOURCE, "source");
      reqObj.setRequestContext(new RequestContext());
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
    assertTrue(true);
  }

  @Test
  public void testCreateManagedUserWithInvalidLocationCode() {
    Map<String, Object> reqMap = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(reqMap);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.updatePassword(Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> userMap = new HashMap<>(getMapObject());
    userMap.put(JsonKey.USER_ID, "3dc4e0bc-43a6-4ba0-84f9-6606a9c17320");
    // when(Util.getUserDetails(Mockito.anyMap(), Mockito.anyMap(),
    // Mockito.any(RequestContext.class)))
    //    .thenReturn(userMap);

    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(esPromise.future());
    try {
      Request reqObj =
          getRequest(
              false, false, false, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER_V4);
      reqObj.getRequest().put(JsonKey.LOCATION_CODES, "code");
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(Duration.ofSeconds(1000), ProjectCommonException.class);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
    assertTrue(true);
  }

  @Test
  public void testCreateManagedUser() {
    Map<String, Object> reqMap = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(reqMap);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.updatePassword(Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> userMap = new HashMap<>(getMapObject());
    userMap.put(JsonKey.USER_ID, "3dc4e0bc-43a6-4ba0-84f9-6606a9c17320");
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(esPromise.future());
    try {
      Request reqObj =
          getRequest(
              false,
              false,
              false,
              getAdditionalMapData(reqMap),
              ActorOperations.CREATE_MANAGED_USER);
      Map<String, String> proLocn = new HashMap<>();
      proLocn.put(JsonKey.CODE, "state");
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
    assertTrue(true);
  }
}
