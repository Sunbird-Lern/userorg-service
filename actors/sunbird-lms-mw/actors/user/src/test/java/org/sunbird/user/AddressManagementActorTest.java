package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.actors.AddressManagementActor;
import org.sunbird.user.util.UserActorOperations;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class AddressManagementActorTest {

  private static final ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(AddressManagementActor.class);

  @Before
  public void beforeEachTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = Mockito.mock(EncryptionService.class);
    Mockito.when(
            org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
                .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);

    try {
      Mockito.when(encryptionService.encryptData(Mockito.anyString())).thenReturn("encrptUserId");
    } catch (Exception e) {
      fail(
          "AddressManagementActorTest initialization failed, with exception message: "
              + e.getMessage());
    }
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSuccessResponse());
  }

  @Test
  public void testInsertUserAddressSuccess() {
    boolean result = testScenario(UserActorOperations.INSERT_USER_ADDRESS, true, true, true);
    assertTrue(result);
  }

  @Test
  public void testUpdateAddressSuccessWithDeleteAddress() {
    boolean result = testScenario(UserActorOperations.UPDATE_USER_ADDRESS, true, true, false);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserAddressSuccessWithAddNewAddress() {
    boolean result = testScenario(UserActorOperations.UPDATE_USER_ADDRESS, false, true, false);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserAddressSuccessWithUpdateExistingAddress() {
    boolean result = testScenario(UserActorOperations.UPDATE_USER_ADDRESS, false, true, true);
    assertTrue(result);
  }

  @Test
  public void testInsertUserAddressFailureWithoutReqParams() {
    boolean result = testScenario(UserActorOperations.INSERT_USER_ADDRESS, false, false, false);
    assertTrue(result);
  }

  private boolean testScenario(
      UserActorOperations actorOperation, boolean isDelete, boolean success, boolean isIdReq) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(getRequestObject(actorOperation, isDelete, success, isIdReq), probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    if (success) {
      return res != null && "SUCCESS".equals(res.getResult().get(JsonKey.RESPONSE));
    } else {
      return res != null && res.getResult().get(JsonKey.ERROR_MSG) != null;
    }
  }

  private Request getRequestObject(
      UserActorOperations actorOperation, boolean isDelete, boolean success, boolean isIdReq) {
    Request reqObj = new Request();
    reqObj.setOperation(actorOperation.getValue());
    if (success) {
      reqObj.put(JsonKey.ADDRESS, getAddressList(isDelete, isIdReq));
    }
    reqObj.put(JsonKey.CREATED_BY, "createdBy");
    return reqObj;
  }

  private List<Map<String, Object>> getAddressList(boolean isDelete, boolean isIdReq) {
    List<Map<String, Object>> lst = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ADDRESS, "anyAddress");
    if (isIdReq) {
      map.put(JsonKey.ID, "someUserId");
    }
    map.put(JsonKey.IS_DELETED, isDelete);
    lst.add(map);
    return lst;
  }

  private Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, "SUCCESS");
    return response;
  }
}
