package org.sunbird.actor.bulkupload;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.nullable;
import static org.powermock.api.mockito.PowerMockito.*;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessDaoImpl;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.model.bulkupload.BulkUploadProcessTask;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.telemetry.util.TelemetryWriter;
import org.sunbird.util.ProjectUtil;

@PrepareForTest({
  ServiceFactory.class,
  TelemetryWriter.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  BulkUploadProcessDaoImpl.class,
  BulkUploadProcess.class,
  BulkUploadProcessTaskDaoImpl.class,
  BaseActor.class,
  ActorRef.class,
  ActorSelection.class
})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserBulkUploadBackgroundJobActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(UserBulkUploadBackgroundJobActor.class);
  private static EncryptionService encryptionService;
  private static ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);

    encryptionService = PowerMockito.mock(EncryptionService.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    when(org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance())
        .thenReturn(encryptionService);
    system = ActorSystem.create("system");
  }

  @Test
  public void testSelfUserBulkUploadWithProperCsv() throws Exception {
    PowerMockito.mockStatic(BulkUploadProcessDaoImpl.class);
    BulkUploadProcessDaoImpl bulkUploadProcessDao = mock(BulkUploadProcessDaoImpl.class);
    when(BulkUploadProcessDaoImpl.getInstance()).thenReturn(bulkUploadProcessDao);
    PowerMockito.mockStatic(BulkUploadProcessTaskDaoImpl.class);
    BulkUploadProcessTaskDaoImpl bulkUploadProcessTaskDao =
        mock(BulkUploadProcessTaskDaoImpl.class);
    when(BulkUploadProcessTaskDaoImpl.getInstance()).thenReturn(bulkUploadProcessTaskDao);
    BulkUploadProcess bulkUploadProcess = new BulkUploadProcess();
    bulkUploadProcess.setId("processId");
    bulkUploadProcess.setObjectType("objectType");
    bulkUploadProcess.setUploadedBy("requestedBy");
    bulkUploadProcess.setUploadedDate(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setCreatedBy("requestedBy");
    bulkUploadProcess.setCreatedOn(new Timestamp(Calendar.getInstance().getTime().getTime()));
    bulkUploadProcess.setProcessStartTime(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setTaskCount(3);
    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    bulkUploadProcess.setOrganisationId("someOrgId");
    when(bulkUploadProcessDao.read(nullable(String.class), Mockito.any()))
        .thenReturn(bulkUploadProcess);
    when(bulkUploadProcessDao.update(Mockito.any(), Mockito.any())).thenReturn(new Response());
    when(bulkUploadProcessTaskDao.readByPrimaryKeys(Mockito.anyMap(), Mockito.any()))
        .thenReturn(createBulkUploadProcessTasks());
    when(bulkUploadProcessTaskDao.updateBatchRecord(Mockito.anyList(), Mockito.any()))
        .thenReturn("updated");
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_BULK_UPLOAD_BACKGROUND_JOB.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertNotNull(res);
    Assert.assertEquals(ResponseCode.OK.getResponseCode(), res.getResponseCode().getResponseCode());
    // Assert.assertEquals(ResponseCode.mandatoryParamsMissing.getErrorMessage(), res.getMessage());
  }

  public List getBulkuploadProcessMap() {
    List<Map<String, Object>> bulkUploadProcessList = new ArrayList<>();
    Map<String, Object> bulkuploadProcessMap = new HashMap<>();
    bulkuploadProcessMap.put("status", ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    bulkUploadProcessList.add(bulkuploadProcessMap);
    return bulkUploadProcessList;
  }

  public Map createUserMap() {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("organisationId", "someOrgId");
    userMap.put("roles", "CONTENT_CREATOR");
    userMap.put("userId", "someUserId");
    return userMap;
  }

  public Organisation getOrganisation() {
    Organisation organisation = new Organisation();
    organisation.setId("someOrgId");
    organisation.setStatus(1);
    organisation.setExternalId("someExternalId");
    return organisation;
  }

  public List<BulkUploadProcessTask> createBulkUploadProcessTasks() throws JsonProcessingException {
    List<BulkUploadProcessTask> bulkUploadProcessTasksLst = new ArrayList<>();
    BulkUploadProcessTask bulkUploadProcessTask = new BulkUploadProcessTask();
    bulkUploadProcessTask.setData(objectMapper.writeValueAsString(createUserMap()));
    bulkUploadProcessTask.setStatus(ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    bulkUploadProcessTasksLst.add(bulkUploadProcessTask);
    return bulkUploadProcessTasksLst;
  }
}
