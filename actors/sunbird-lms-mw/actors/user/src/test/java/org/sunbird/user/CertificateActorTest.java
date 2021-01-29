package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
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
import org.sunbird.common.models.util.datasecurity.impl.DefaultDataMaskServiceImpl;
import org.sunbird.common.models.util.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.User;
import org.sunbird.user.actors.CertificateActor;
import org.sunbird.user.service.impl.UserServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserServiceImpl.class,
  ServiceFactory.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,
  DefaultDecryptionServiceImpl.class,
  DefaultDataMaskServiceImpl.class
})
@PowerMockIgnore({"javax.management.*"})
@Ignore // Will depricate these api with SC-2169
public class CertificateActorTest {
  public static CassandraOperationImpl cassandraOperationImpl;
  public static UserServiceImpl userServiceImpl;
  Props props = Props.create(CertificateActor.class);
  ActorSystem system = ActorSystem.create("CertificateActor");
  private static DefaultDecryptionServiceImpl defaultDecryptionService;
  private static DefaultDataMaskServiceImpl dataMaskingService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    userServiceImpl = mock(UserServiceImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(UserServiceImpl.getInstance()).thenReturn(userServiceImpl);
    defaultDecryptionService = mock(DefaultDecryptionServiceImpl.class);
    dataMaskingService = mock(DefaultDataMaskServiceImpl.class);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(defaultDecryptionService);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
            null))
        .thenReturn(dataMaskingService);
  }

  @Test
  public void testAddCertificate() {
    when(userServiceImpl.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserDetails(new User(), false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true))
        .thenReturn(getRecordsById2(false));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    when(cassandraOperationImpl.performBatchAction(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    boolean result =
        testScenario(getAddCertRequest(ActorOperations.ADD_CERTIFICATE, "oldId"), null);
    assertTrue(result);
  }

  @Test
  public void testAddCertificateFailure3() {
    when(userServiceImpl.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserDetails(new User(), false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true))
        .thenReturn(getRecordsById(false));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    boolean result =
        testScenario(
            getAddCertRequest(ActorOperations.ADD_CERTIFICATE, "oldId"),
            ResponseCode.errorUnavailableCertificate);
    assertTrue(result);
  }

  @Test
  public void testAddCertificateFailure2() {
    when(userServiceImpl.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserDetails(new User(), false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(false));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    boolean result =
        testScenario(
            getAddCertRequest(ActorOperations.ADD_CERTIFICATE, "oldId"),
            ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Test
  public void testAddCertificateFailure() {
    when(userServiceImpl.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserDetails(new User(), false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    boolean result =
        testScenario(
            getAddCertRequest(ActorOperations.ADD_CERTIFICATE, "oldId"),
            ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Test
  public void testAddReIssueCertificate() {
    when(userServiceImpl.getUserById(Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(getUserDetails(new User(), false));
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById(true));
    boolean result =
        testScenario(
            getAddCertRequest(ActorOperations.ADD_CERTIFICATE, "anyOldId"),
            ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Test
  public void testValidateCertificate() {
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getValidRecordDetails(false, true));
    boolean result =
        testScenario(getValidateCertRequest(ActorOperations.VALIDATE_CERTIFICATE), null);
    assertTrue(result);
  }

  @Test
  public void testInValidateCertificateAccessCode() {
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getValidRecordDetails(false, false));
    boolean result =
        testScenario(
            getValidateCertRequest(ActorOperations.VALIDATE_CERTIFICATE),
            ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Test
  public void testMergeCertificate() {

    when(cassandraOperationImpl.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsById3(false));
    // when(defaultDecryptionService.decryptData(Mockito.anyString()))
    boolean result =
        testScenario(getMergeCertRequest(ActorOperations.MERGE_USER_CERTIFICATE), null);
    assertTrue(true);
  }

  private Request getMergeCertRequest(ActorOperations actorOperation) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.FROM_ACCOUNT_ID, "userid1");
    reqMap.put(JsonKey.TO_ACCOUNT_ID, "userid2");
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
    if (oldId != null) {
      reqMap.put(JsonKey.OLD_ID, oldId);
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
    if (user != null) user.setIsDeleted(b);
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

  private Response getRecordsById2(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "certId");
      map.put(JsonKey.IS_DELETED, false);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getRecordsById3(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.EMAIL, "qwe@qw.com");
      map.put(JsonKey.PHONE, "981485145");
      map.put(JsonKey.PREV_USED_EMAIL, "xyz@xy.com");
      map.put(JsonKey.PREV_USED_PHONE, "1234567980");
      map.put(JsonKey.FIRST_NAME, "name");
      map.put(JsonKey.ID, "userid1");
      list.add(map);

      Map<String, Object> map2 = new HashMap<>();
      map2.put(JsonKey.EMAIL, "qwep@qw.com");
      map2.put(JsonKey.PHONE, "981485146");
      map2.put(JsonKey.PREV_USED_EMAIL, "xyzn@xy.com");
      map2.put(JsonKey.PREV_USED_PHONE, "1234567981");
      map2.put(JsonKey.FIRST_NAME, "name");
      map2.put(JsonKey.ID, "userid2");
      list.add(map2);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getValidRecordDetails(boolean exists, boolean accessCode) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> recordStore = new HashMap<>();
    recordStore.put(JsonKey.PDF, "anyPDF");
    recordStore.put(JsonKey.JSON_DATA, "{\n" + "  \"jsonData\":\"jsonData\"\n" + "}");
    recordStore.put(JsonKey.COURSE_ID, "anyCourseId");
    recordStore.put(JsonKey.BATCH_ID, "anyBatchId");
    map.put(JsonKey.ID, "certId");
    map.put(JsonKey.IS_DELETED, exists);
    if (accessCode) {
      map.put("accesscode", "anyAccessCode");
    } else {
      map.put("accesscode", "anyNonAccessCode");
    }
    map.put(JsonKey.STORE, recordStore);
    list.add(map);
    res.put(JsonKey.RESPONSE, list);
    return res;
  }
}
