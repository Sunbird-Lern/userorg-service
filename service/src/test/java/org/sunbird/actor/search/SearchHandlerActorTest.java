package org.sunbird.actor.search;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.time.Duration;
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
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  OrgTypeValidator.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class SearchHandlerActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(SearchHandlerActor.class);

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");
  }

  private static Map<String, Object> createResponseGet(boolean isResponseRequired) {
    HashMap<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    HashMap<String, Object> innerMap = new HashMap<>();
    List<Map<String, Object>> orgList = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    orgList.add(orgMap);
    List<Map<String, Object>> roles = new ArrayList<>();
    Map<String, Object> role = new HashMap<>();
    role.put(JsonKey.ROLE, "COURSE_CREATOR");
    List<Map<String, String>> scopes = new ArrayList<>();
    Map<String, String> scope = new HashMap<>();
    scope.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    scopes.add(scope);
    role.put(JsonKey.SCOPE, scopes);
    roles.add(role);
    innerMap.put(JsonKey.ROLES, roles);

    innerMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    innerMap.put(JsonKey.ORGANISATIONS, orgList);
    innerMap.put(JsonKey.HASHTAGID, "HASHTAGID");
    innerMap.put(JsonKey.ORGANISATION_TYPE, 2);
    Map<String, Object> userType = new HashMap<>();
    userType.put(JsonKey.TYPE, "type");
    userType.put(JsonKey.SUB_TYPE, "subType");
    innerMap.put(JsonKey.PROFILE_USERTYPE, userType);
    List<Map<String, String>> locList = new ArrayList<>();
    Map<String, String> locn = new HashMap<>();
    locn.put(JsonKey.ID, "456465464");
    locn.put(JsonKey.TYPE, "type");
    locList.add(locn);
    innerMap.put(JsonKey.PROFILE_LOCATION, locList);
    content.add(innerMap);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  @Test
  public void searchOrg() {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    OrgTypeValidator orgTypeValidator = mock(OrgTypeValidator.class);
    PowerMockito.mockStatic(OrgTypeValidator.class);
    when(OrgTypeValidator.getInstance()).thenReturn(orgTypeValidator);
    when(orgTypeValidator.getValueByType(JsonKey.ORG_TYPE_SCHOOL)).thenReturn(2);
    promise.success(createResponseGet(true));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ORG_SEARCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.QUERY, "");
    Map<String, Object> filters = new HashMap<>();
    List<String> objectType = new ArrayList<String>();
    objectType.add("org");
    filters.put(JsonKey.ID, "ORG_001");
    filters.put(JsonKey.IS_SCHOOL, true);
    filters.put(JsonKey.IS_ROOT_ORG, true);
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.LIMIT, 1);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.ORG_NAME);
    fields.add(JsonKey.HASHTAGID);
    innerMap.put(JsonKey.FIELDS, fields);
    Map<String, Object> contextMap = new HashMap<>();
    contextMap.put(JsonKey.FIELDS, JsonKey.ORG_NAME);
    reqObj.setContext(contextMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    try {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void searchUser() {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createResponseGet(true));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_SEARCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.QUERY, "");
    Map<String, Object> filters = new HashMap<>();
    List<String> objectType = new ArrayList<String>();
    objectType.add("user");
    filters.put(JsonKey.OBJECT_TYPE, objectType);
    filters.put(JsonKey.PROFILE_USERTYPE, "teacher");
    filters.put(JsonKey.PROFILE_LOCATION, "location");
    filters.put(JsonKey.USER_TYPE, "userType");
    filters.put(JsonKey.USER_SUB_TYPE, "userSubType");
    filters.put(JsonKey.LOCATION_ID, "locationID");
    filters.put(JsonKey.LOCATION_TYPE, "type");
    filters.put(JsonKey.ROOT_ORG_ID, "ORG_001");
    filters.put(JsonKey.ORGANISATIONS + "." + JsonKey.ROLES, "COURSE_CREATOR");
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.LIMIT, 1);
    Map<String, Object> contextMap = new HashMap<>();
    contextMap.put(JsonKey.FIELDS, JsonKey.ORG_NAME);
    reqObj.setContext(contextMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    try {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void searchUser2() {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> resp = createResponseGet(true);
    List<Map<String, Object>> content = (List<Map<String, Object>>) resp.get(JsonKey.CONTENT);
    content.get(0).put(JsonKey.PROFILE_USERTYPE, null);
    content.get(0).put(JsonKey.PROFILE_LOCATION, null);
    promise.success(resp);
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_SEARCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.QUERY, "");
    Map<String, Object> filters = new HashMap<>();
    List<String> objectType = new ArrayList<String>();
    objectType.add("user");
    filters.put(JsonKey.OBJECT_TYPE, objectType);
    filters.put(JsonKey.PROFILE_USERTYPE, "teacher");
    filters.put(JsonKey.PROFILE_LOCATION, "location");
    filters.put(JsonKey.USER_TYPE, "userType");
    filters.put(JsonKey.USER_SUB_TYPE, "userSubType");
    filters.put(JsonKey.LOCATION_ID, "locationID");
    filters.put(JsonKey.LOCATION_TYPE, "type");
    filters.put(JsonKey.ROOT_ORG_ID, "ORG_001");
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.LIMIT, 1);
    Map<String, Object> contextMap = new HashMap<>();
    contextMap.put(JsonKey.FIELDS, JsonKey.ORG_NAME);
    reqObj.setContext(contextMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    try {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void searchUserWithObjectTypeAsOrg() {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createResponseGet(true));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_SEARCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.QUERY, "");
    Map<String, Object> filters = new HashMap<>();
    List<String> objectType = new ArrayList<String>();
    objectType.add("org");
    filters.put(JsonKey.OBJECT_TYPE, objectType);
    filters.put(JsonKey.ROOT_ORG_ID, "ORG_001");
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.LIMIT, 1);

    Map<String, Object> contextMap = new HashMap<>();
    contextMap.put(JsonKey.FIELDS, JsonKey.ORG_NAME);
    reqObj.setContext(contextMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    try {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void testInvalidOperation() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation("INVALID_OPERATION");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }
}
