package org.sunbird.actor.user;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.user.UserUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class, UserUtil.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserOrgManagementActorTest {
  Props props = Props.create(UserOrgManagementActor.class);
  ActorSystem system = ActorSystem.create("UserOrgManagementActor");
  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void beforeEachTest() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    when(cassandraOperationImpl.updateRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(response);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);

    PowerMockito.mockStatic(UserUtil.class);
    Map<String, Object> userOrg = new HashMap<>();
    userOrg.put(JsonKey.USER_ID, "userId");
    userOrg.put(JsonKey.ORGANISATION_ID, "id");
    List<Map<String, Object>> userOrgListDb = new ArrayList<>();
    userOrgListDb.add(userOrg);
    when(UserUtil.getUserOrgDetails(Mockito.anyBoolean(), Mockito.anyString(), Mockito.any()))
        .thenReturn(userOrgListDb);
  }

  @Test
  public void testInsertUserOrgDetailsSuccess() {
    boolean result = testScenario(getUserOrgInsertRequest(), null);
    Assert.assertTrue(result);
  }

  @Test
  public void testInsertUserOrgDetailsWithInvalidOperations() {
    boolean result = testScenario(getUserOrgInsertFailureRequest(), ResponseCode.CLIENT_ERROR);
    Assert.assertTrue(result);
  }

  @Test
  public void testInsertUserOrgDetailsWithoutCallerIdSuccess() throws Exception {
    Request request = getUserOrgInsertRequest();
    request.getRequest().remove(JsonKey.CALLER_ID);
    boolean result = testScenario(request, null);
    Assert.assertTrue(result);
  }

  // @Test
  public void testUpdateUserOrgDetailsSuccess() throws Exception {
    boolean result = testScenario(getUserOrgUpdateRequest(), null);
    Assert.assertTrue(result);
  }

  private Request getUserOrgInsertRequest() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.CALLER_ID, "anyCallerId");
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    reqMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    request.setRequest(reqMap);
    request.setOperation("insertUserOrgDetails");
    return request;
  }

  private Request getUserOrgUpdateRequest() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.CALLER_ID, "anyCallerId");
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    reqMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    request.setRequest(reqMap);
    request.setOperation("updateUserOrgDetails");
    return request;
  }

  private Request getUserOrgInsertFailureRequest() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.CALLER_ID, "anyCallerId");
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    request.setRequest(reqMap);
    request.setOperation("invalidOperation");
    return request;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }
}
