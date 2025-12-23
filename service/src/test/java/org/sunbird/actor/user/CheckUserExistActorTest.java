package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
        CassandraOperationImpl.class,
  UserService.class,
  UserServiceImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class CheckUserExistActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private Props props = Props.create(CheckUserExistActor.class);
  private CassandraOperationImpl cassandraOperation;
  private ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Response response1 = new Response();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.USER_ID, "123456790-789456-741258");
    userMap.put(JsonKey.ID, "123456790-789456-741258");
    userMap.put(JsonKey.FIRST_NAME, "Name");
    userMap.put(JsonKey.LAST_NAME, "Name");
    List<Map<String, Object>> responseList = new ArrayList<>();
    responseList.add(userMap);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
        .thenReturn(response1);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response1);
    setEsSearchResponse(getUserExistsSearchResponseMap());
  }

  @Test
  public void testCheckUserExistenceV1WithEmail() {
    Map<String, Object> reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, "xyz@xyz.com");
    boolean result = testScenario(getRequest(reqMap, "checkUserExistence"), null);
    assertTrue(result);
  }

  @Test
  public void testCheckUserExistenceV2WithEmail() {
    Map<String, Object> reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, "xyz@xyz.com\"");
    boolean result = testScenario(getRequest(reqMap, "checkUserExistenceV2"), null);
    assertTrue(result);
  }

  @Test
  public void testCheckUserExistenceV2WithLoginid() {
    Map<String, Object> reqMap = getUserProfileByKeyRequest(JsonKey.LOGIN_ID, "amit@ch");
    boolean result = testScenario(getRequest(reqMap, "checkUserExistenceV2"), null);
    assertTrue(result);
  }

  private Map<String, Object> getUserProfileByKeyRequest(String key, String value) {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.KEY, key);
    reqMap.put(JsonKey.VALUE, value);
    return reqMap;
  }

  private static Map<String, Object> getUserExistsSearchResponseMap() {
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> response = new HashMap<>();
    response.put(JsonKey.EXISTS, "true");
    response.put(JsonKey.FIRST_NAME, "Name");
    response.put(JsonKey.LAST_NAME, "Name");
    List contentList = new ArrayList<>();
    contentList.add(response);
    map.put(JsonKey.CONTENT, contentList);
    return map;
  }

  public void setEsSearchResponse(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
  }

  private Request getRequest(Map<String, Object> reqMap, String actorOperation) {
    Request reqObj = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, "requestedBy");
    innerMap.put(JsonKey.PRIVATE, false);
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(actorOperation);
    return reqObj;
  }

  private boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {

      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  @Test
  public void testWithInvalidRequest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request request = new Request();
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }
}
