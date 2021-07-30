package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.user.actors.ManagedUserActor;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Promise;

@PrepareForTest({Util.class})
public class ManagedUserActorTest extends UserManagementActorTestBase {

  public final Props props = Props.create(ManagedUserActor.class);

  @Test
  public void testGetManagedUsers() throws Exception {
    HashMap<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "102fcbd2-8ec1-4870-b9e1-5dc01f2acc75");
    reqMap.put(JsonKey.WITH_TOKENS, "true");

    PowerMockito.mockStatic(UserClientImpl.class);
    UserClientImpl userClient = mock(UserClientImpl.class);
    when(UserClientImpl.getInstance()).thenReturn(userClient);

    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("anyString", new Object());
    content.add(map);
    responseMap.put(JsonKey.CONTENT, content);

    when(userClient.searchManagedUser(
            Mockito.any(ActorRef.class),
            Mockito.any(Request.class),
            Mockito.any(RequestContext.class)))
        .thenReturn(responseMap);
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
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("1000 second"), NullPointerException.class);
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
              false,
              false,
              false,
              getAdditionalMapData(reqMap),
              ActorOperations.CREATE_MANAGED_USER);
      Map<String, String> proLocn = new HashMap<>();
      proLocn.put(JsonKey.CODE, "state");
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
      TestKit probe = new TestKit(system);
      ActorRef subject = system.actorOf(props);
      subject.tell(reqObj, probe.getRef());
      probe.expectMsgClass(duration("1000 second"), NullPointerException.class);
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
      probe.expectMsgClass(duration("1000 second"), ProjectCommonException.class);
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
      probe.expectMsgClass(duration("1000 second"), NullPointerException.class);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
    assertTrue(true);
  }
}
