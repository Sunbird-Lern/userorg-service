package org.sunbird.learner.actors.bulkupload;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.telemetry.util.TelemetryWriter;
import scala.concurrent.Promise;
import org.sunbird.common.request.RequestContext;

@PrepareForTest({
  ServiceFactory.class,
  TelemetryWriter.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class UserBulkMigrationActorTest {
  private static ActorSystem system;
  private static final Props props = Props.create(UserBulkMigrationActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static EncryptionService encryptionService;

  @Before
  public void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response response = getCassandraRecordById();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());
    encryptionService = PowerMockito.mock(EncryptionService.class);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    TelemetryWriter.write(Mockito.any());
    system = ActorSystem.create("system");

  }

  @Test
  public void testSelfUserBulkUploadWithOutInputInCsv() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkSelfDeclaredUserUploadEmpty.csv");
    Request reqObj = new Request();
    reqObj.setOperation(BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, "anyUserID");
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(res);
    Assert.assertEquals(ResponseCode.noDataForConsumption.getErrorCode(), res.getCode());
    Assert.assertEquals(ResponseCode.noDataForConsumption.getErrorMessage(), res.getMessage());
  }

  @Test
  public void testSelfUserBulkUploadWithImproperHeadersInCsv() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkSelfDeclaredUserUploadImproperSample.csv");
    Request reqObj = new Request();
    reqObj.setOperation(BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, "anyUserID");
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
    Assert.assertNotNull(res);
    Assert.assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), res.getCode());
    Assert.assertEquals(
        ProjectUtil.formatMessage(
            ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.PERSONA),
        res.getMessage());
  }

  @Test
  public void testSelfUserBulkUploadWithProperCsv() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    byte[] bytes = getFileAsBytes("BulkSelfDeclaredUserUploadSample.csv");
    Request reqObj = new Request();
    reqObj.setOperation(BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CREATED_BY, "anyUserID");
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    innerMap.put(JsonKey.FILE, bytes);
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertNotNull(res);
    Assert.assertEquals(ResponseCode.OK.getResponseCode(), res.getResponseCode().getResponseCode());
    Assert.assertNotNull(res.getResult().get("processId"));
    // Assert.assertEquals(ResponseCode.mandatoryParamsMissing.getErrorMessage(), res.getMessage());
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
    } catch (FileNotFoundException e) {
      ProjectLogger.log(e.getMessage(), e);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return bytes;
  }

  private static Response getCassandraRecordById() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> bulkUploadProcessMap = new HashMap<>();
    bulkUploadProcessMap.put(JsonKey.ID, "anyId");
    bulkUploadProcessMap.put(JsonKey.CHANNEL, "anyChannel");
    bulkUploadProcessMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    list.add(bulkUploadProcessMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
