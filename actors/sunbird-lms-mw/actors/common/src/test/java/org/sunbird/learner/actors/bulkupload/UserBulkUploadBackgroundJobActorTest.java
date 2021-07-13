package org.sunbird.learner.actors.bulkupload;

import static akka.testkit.JavaTestKit.duration;
import static org.mockito.ArgumentMatchers.nullable;
import static org.powermock.api.mockito.PowerMockito.*;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
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
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.operations.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessDaoImpl;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.telemetry.util.TelemetryWriter;
import org.sunbird.validator.user.UserRequestValidator;

@PrepareForTest({
  ServiceFactory.class,
  TelemetryWriter.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  UserClientImpl.class,
  OrganisationClientImpl.class,
  SystemSettingClientImpl.class,
  BulkUploadProcessDaoImpl.class,
  BulkUploadProcess.class,
  BulkUploadProcessTaskDaoImpl.class,
  RequestRouter.class,
  BaseActor.class,
  ActorRef.class,
  ActorSelection.class,
  BaseMWService.class
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

  private UserClientImpl userClient;
  private OrganisationClientImpl organisationClient;
  private SystemSettingClientImpl systemSettingClient;
  private UserRequestValidator userRequestValidator;

  private static ActorSystem system;
  private static final Props props = Props.create(UserBulkUploadBackgroundJobActor.class);
  private static EncryptionService encryptionService;
  private static ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);

    encryptionService = PowerMockito.mock(EncryptionService.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    when(org.sunbird.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    system = ActorSystem.create("system");
    PowerMockito.mockStatic(UserClientImpl.class);
    userClient = mock(UserClientImpl.class);
    when(UserClientImpl.getInstance()).thenReturn(userClient);
    PowerMockito.mockStatic(OrganisationClientImpl.class);
    organisationClient = mock(OrganisationClientImpl.class);
    when(OrganisationClientImpl.getInstance()).thenReturn(organisationClient);
    PowerMockito.mockStatic(SystemSettingClientImpl.class);
    systemSettingClient = mock(SystemSettingClientImpl.class);
    when(SystemSettingClientImpl.getInstance()).thenReturn(systemSettingClient);
    userRequestValidator = new UserRequestValidator();
    ActorSelection selection = PowerMockito.mock(ActorSelection.class);
    PowerMockito.mockStatic(BaseMWService.class);
    when(BaseMWService.getRemoteRouter(Mockito.anyString())).thenReturn(selection);
    when(systemSettingClient.getSystemSettingByFieldAndKey(
            Mockito.any(ActorRef.class),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.any()))
        .thenReturn(new ArrayList<>().toArray());
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getOrganisation());
    doNothing()
        .when(userClient)
        .updateUser(Mockito.any(ActorRef.class), Mockito.anyMap(), Mockito.any());
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
    PowerMockito.when(bulkUploadProcessDao.read(nullable(String.class), Mockito.any()))
        .thenReturn(bulkUploadProcess);
    PowerMockito.when(bulkUploadProcessDao.update(Mockito.any(), Mockito.any()))
        .thenReturn(new Response());
    PowerMockito.when(bulkUploadProcessTaskDao.readByPrimaryKeys(Mockito.anyMap(), Mockito.any()))
        .thenReturn(createBulkUploadProcessTasks());
    PowerMockito.when(bulkUploadProcessTaskDao.updateBatchRecord(Mockito.anyList(), Mockito.any()))
        .thenReturn("updated");
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(BulkUploadActorOperation.USER_BULK_UPLOAD_BACKGROUND_JOB.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    reqObj.getRequest().put(JsonKey.DATA, innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
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
