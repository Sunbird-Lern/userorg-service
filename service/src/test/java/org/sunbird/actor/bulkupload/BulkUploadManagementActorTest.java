package org.sunbird.actor.bulkupload;

import java.time.Duration;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;

/** @author arvind. Junit test cases for bulk upload - user, org */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, Util.class, BulkUploadManagementActor.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class BulkUploadManagementActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(BulkUploadManagementActor.class);
  private static final String USER_ID = "bcic783gfu239";
  private static final String refOrgId = "id34fy";
  private static CassandraOperationImpl cassandraOperation;
  private static final String PROCESS_ID = "process-13647-fuzzy";

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    system = ActorSystem.create("system");
  }

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void checkTelemetryKeyFailure() throws Exception {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    String telemetryEnvKey = "user";
    PowerMockito.mockStatic(Util.class);
    PowerMockito.doNothing()
        .when(
            Util.class,
            "initializeContext",
            Mockito.any(Request.class),
            Mockito.eq(telemetryEnvKey));

    byte[] bytes = getFileAsBytes("BulkOrgUploadSample.csv");

    Response response = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(!(telemetryEnvKey.charAt(0) >= 65 && telemetryEnvKey.charAt(0) <= 90));
  }

  @Test
  public void testOrgBulkUploadCreateOrgSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkOrgUploadSample.csv");

    Response response = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    String uploadProcessId = (String) res.get(JsonKey.PROCESS_ID);
    Assert.assertTrue(null != uploadProcessId);
  }

  @Test
  public void testOrgBulkUploadCreateOrgEmptyCsvFile() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkOrgUploadEmptyFile.csv");
    Response response = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(res);
    Assert.assertEquals("UOS_BLKUPLD" + ResponseCode.csvError.getErrorCode(), res.getErrorCode());
    Assert.assertEquals(ResponseCode.csvError.getErrorMessage(), res.getMessage());
  }

  @Test
  public void testOrgBulkUploadCreateOrgWithInvalidHeaders() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    String headerLine = "batchId,orgName,isRootOrg,channel";
    String firstLine = "batch78575ir8478,hello001,false,,1119";
    StringBuilder builder = new StringBuilder();
    builder.append(headerLine).append("\n").append(firstLine);

    Response response = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    innerMap.put(JsonKey.FILE, builder.toString().getBytes());
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertTrue(null != res);
  }

  @Test
  public void testBulkUploadGetStatus() {
    Response response = getCassandraRecordByIdForBulkUploadResponse();
    when(cassandraOperation.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(response);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_BULK_OP_STATUS.getValue());
    reqObj.getRequest().put(JsonKey.PROCESS_ID, PROCESS_ID);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    List<Map<String, Object>> list = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
    if (!list.isEmpty()) {
      Map<String, Object> map = list.get(0);
      String processId = (String) map.get(JsonKey.PROCESS_ID);
      Assert.assertTrue(null != processId);
    }
  }

  @Ignore
  public void testUserBulkUploadCreateUserSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkUploadUserSample.csv");
    Response response = getCassandraRecordByIdForOrgResponse();
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), null))
        .thenReturn(response);
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    when(cassandraOperation.getRecordById(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), refOrgId, null))
        .thenReturn(response);
    Response insertResponse = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), null))
        .thenReturn(insertResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.ORGANISATION_ID, refOrgId);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    String processId = (String) res.get(JsonKey.PROCESS_ID);
    Assert.assertTrue(null != processId);
  }

  @Ignore
  public void testUserBulkUploadCreateUserWithOrgExtId() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkUploadUserSample.csv");
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), null))
        .thenReturn(getCassandraRecordByIdForOrgResponse());
    Response insertResponse = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), null))
        .thenReturn(insertResponse);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.ORG_PROVIDER, "provider");
    innerMap.put(JsonKey.ORG_EXTERNAL_ID, "externalId");
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    String processId = (String) res.get(JsonKey.PROCESS_ID);
    Assert.assertTrue(null != processId);
  }

  @Test
  public void userBulkUploadWithInvalidHeaders() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkUploadUserWithInvalidHeaders.csv");
    Response response = getCassandraRecordByIdForOrgResponse();
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    Response insertResponse = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(insertResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.ORGANISATION_ID, refOrgId);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException ex =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertTrue(null != ex);
    Assert.assertEquals(
        "UOS_BLKUPLD" + ResponseCode.invalidColumns.getErrorCode(), ex.getErrorCode());
    Assert.assertEquals(
        "Invalid column: password. Valid columns are: firstName, lastName, phone, countryCode, email, userName, roles, position, location, dob, language, profileSummary, subject, externalIdProvider, externalId, externalIdType, externalIds.",
        ex.getMessage());
  }

  @Test
  public void testUserBulkUploadCreateUserWithInvalidHeaders() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    String headerLine = "batchId,firstName,lastName,phone";
    String firstLine = "batch78575ir8478,xyz1234516,Kumar15,9000000011";
    StringBuilder builder = new StringBuilder();
    builder.append(headerLine).append("\n").append(firstLine);

    Response response = getCassandraRecordByIdForOrgResponse();
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    Response insertResponse = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(insertResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();

    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.ORGANISATION_ID, refOrgId);

    innerMap.put(JsonKey.FILE, builder.toString().getBytes());
    reqObj.getRequest().put(JsonKey.DATA, innerMap);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertTrue(null != res);
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Response getCassandraRecordByIdForBulkUploadResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> bulkUploadProcessMap = new HashMap<>();
    bulkUploadProcessMap.put(JsonKey.ID, "123");
    bulkUploadProcessMap.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    bulkUploadProcessMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    list.add(bulkUploadProcessMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response getCassandraRecordByIdForOrgResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ORGANISATION_ID, refOrgId);
    orgMap.put(JsonKey.IS_TENANT, true);
    orgMap.put(JsonKey.EXTERNAL_ID, "externalId");
    orgMap.put(JsonKey.PROVIDER, "provider");
    orgMap.put(JsonKey.ID, refOrgId);
    orgMap.put(JsonKey.CHANNEL, "channel");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private byte[] getFileAsBytes(String fileName) {
    File file = null;
    byte[] bytes = null;
    try {
      file =
          new File(
              BulkUploadManagementActorTest.class.getClassLoader().getResource(fileName).getFile());
      Path path = Paths.get(file.getPath());
      bytes = Files.readAllBytes(path);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return bytes;
  }
}
