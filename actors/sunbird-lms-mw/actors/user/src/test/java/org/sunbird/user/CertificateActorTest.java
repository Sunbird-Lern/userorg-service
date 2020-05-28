package org.sunbird.user;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.User;
import org.sunbird.user.actors.CertificateActor;
import org.sunbird.user.service.impl.UserServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        UserServiceImpl.class,
        ServiceFactory.class

})
@PowerMockIgnore({"javax.management.*"})
public class CertificateActorTest {
  public static CassandraOperationImpl cassandraOperationImpl;
  public static UserServiceImpl userServiceImpl;
  Props props = Props.create(CertificateActor.class);
  ActorSystem system = ActorSystem.create("CertificateActor");

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    userServiceImpl = mock(UserServiceImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(UserServiceImpl.getInstance()).thenReturn(userServiceImpl);


  }

  @Test
  public void testAddCertificate() {
    when(userServiceImpl.getUserById(Mockito.anyString())).thenReturn(getUserDetails(new User(),false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(getRecordsById(true));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
            .thenReturn(getRecordsById(true));
    boolean result = testScenario(getAddCertRequest(ActorOperations.ADD_CERTIFICATE,null), null);
    assertTrue(result);
  }

  @Ignore
  @Test
  public void testAddReIssueCertificate() {
    when(userServiceImpl.getUserById(Mockito.anyString())).thenReturn(getUserDetails(new User(),false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(getRecordsById(true));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
            .thenReturn(getRecordsById(true));
    boolean result = testScenario(getAddCertRequest(ActorOperations.ADD_CERTIFICATE, "anyOldId"), null);
    assertTrue(result);
  }

  @Test
  public void testValidateCertificate() {
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(getValidRecordDetails(false,true));
    boolean result = testScenario(getValidateCertRequest(ActorOperations.VALIDATE_CERTIFICATE), null);
    assertTrue(result);
  }

  @Test
  public void testInValidateCertificateAccessCode() {
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(getValidRecordDetails(false,false));
    boolean result = testScenario(getValidateCertRequest(ActorOperations.VALIDATE_CERTIFICATE), ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Ignore
  @Test
  public void testMergeCertificate() {
    when(cassandraOperationImpl.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(getRecordsById(false));
    boolean result = testScenario(getMergeCertRequest(ActorOperations.MERGE_USER_CERTIFICATE), null);
    assertTrue(result);
  }

  private Request getMergeCertRequest(ActorOperations actorOperation) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.FROM_ACCOUNT_ID, "anyUserId");
    reqMap.put(JsonKey.TO_ACCOUNT_ID, "anyUserId");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
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

  Request getAddCertRequest(ActorOperations actorOperation, String oldId) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "anyId");
    reqMap.put(JsonKey.ACCESS_CODE, "anyAccessCode");
    reqMap.put(JsonKey.JSON_DATA, "anyJsonDate");
    reqMap.put(JsonKey.PDF_URL, "anyPdfUrl");
    if(oldId!=null) {
      reqMap.put(JsonKey.OLD_ID,oldId);
    }
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  Request getValidateCertRequest(ActorOperations actorOperation) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.CERT_ID, "anyId");
    reqMap.put(JsonKey.ACCESS_CODE, "anyAccessCode");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  private User getUserDetails(User user, boolean b) {
   if(user != null)
    user.setIsDeleted(b);
    user.setRootOrgId("AnyRootOrgId");
    return user;
  }

  private Response getRecordsById(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "certId");
      map.put(JsonKey.IS_DELETED, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getValidRecordDetails(boolean exists, boolean accessCode) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> recordStore = new HashMap<>();
    recordStore.put(JsonKey.PDF,"anyPDF");
    recordStore.put(JsonKey.JSON_DATA,"{\n" +
            "  \"jsonData\":\"jsonData\"\n" +
            "}");
    recordStore.put(JsonKey.COURSE_ID,"anyCourseId");
    recordStore.put(JsonKey.BATCH_ID,"anyBatchId");
    map.put(JsonKey.ID, "certId");
    map.put(JsonKey.IS_DELETED, exists);
    if(accessCode) {
      map.put("accesscode", "anyAccessCode");
    } else {
      map.put("accesscode", "anyNonAccessCode");
    }
    map.put(JsonKey.STORE,recordStore);
    list.add(map);
    res.put(JsonKey.RESPONSE, list);
    return res;
  }



}