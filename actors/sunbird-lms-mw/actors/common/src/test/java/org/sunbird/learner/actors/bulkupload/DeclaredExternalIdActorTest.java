package org.sunbird.learner.actors.bulkupload;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.*;

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
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.util.UserUploadUtil;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  UserUploadUtil.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,
  SunbirdMWService.class
})
@PowerMockIgnore("javax.management.*")
public class DeclaredExternalIdActorTest {
  private static final Props props = Props.create(DeclaredExternalIdActor.class);
  private static ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;
  private static DecryptionService decryptionService;
  private static SunbirdMWService SunbirdMWService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    decryptionService = mock(DecryptionService.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createDeclaredBulkUploadData());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(updateData(true));
    when(decryptionService.decryptData(Mockito.anyString()))
        .thenReturn(
            "[{\"email\":null,\"phone\":null,\"name\":null,\"userExternalId\":\"\",\"orgExternalId\":null,\"channel\":\"\",\"inputStatus\":\"VALIDATED\",\"schoolName\":null,\"schoolId\":null,\"userId\":\"\",\"subOrgId\":null}]");
  }

  private Response updateData(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "userId");
      map.put(JsonKey.IS_DELETED, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response createDeclaredBulkUploadData() {
    Response response = new Response();
    Map<String, Object> declaredUserUploadMap = new HashMap<>();
    declaredUserUploadMap.put(JsonKey.ID, "0130752540065218560");
    declaredUserUploadMap.put(
        JsonKey.DATA,
        "PYOeYF3BCS4Xv8duenjraA46kuN5J96XQMHyZz9NRDdLiOkPtBMYGuj6CRytyGwVrvNknVJ8Kzt7\\ndRBNmR5A7IPfuGm0X1XEb/YQjXzgR5G/A6fNKJKMiTYyYHeR9vJyPGJ5jf5zDC/wiAxk80kD3X9b\\nixZo4RN6k3/PPHvaU7iVxSPc1+F74JD2WV72fSbHEuabJAYAH4inbvCRiYq37/KvH9u2bhTV7TPV\\neI6rVIkaYzD07JPNMeVXJqIGbpBCOof9I/yVoczuDRCCPma3fz549diFEVWmUDkKppTGBIRlA1AP\\njIxTkX0DPSEQ5HRHvcKEMtiO6w/S/NNkyP7n6ddkjr75U0hM1uswr4HoeJh9SQeSxMGsvJpgrITO\\nEsI7rp1gID/xTOxz7eIsu7GG+ZDmfECggGC9pAiEsO3dsQZqhOeU8Ge2WAw+a4pvnGIhk478k98e\\nDtATCCcOlJt8zX4hRNuZp0gX9lKAZh5Zb1PfAaYl+wfqRl5fT12EqVdwPoMxO4DeiztONbe65fy0\\n52WWZlBorSgzxjA3qH5xUV4/EuRPcnI3DypooVZgmxDKNKm7AeAABPUNMzgH4spfsFOf1n0XnOut\\nJ2ZNY6vCaduqZluhXE9eTGPqvIzlJDKAEPH+gxPt6LzdndcUbALegmGD7YmpdY8iggsVOM0vtb4=");
    List<Map<String, Object>> result = new ArrayList<>();
    result.add(declaredUserUploadMap);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  @Test
  public void testUploadDeclaredUser() {
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.USER_BULK_MIGRATION), null);
    assertTrue(result);
  }

  Request createRequest(BulkUploadActorOperation actorOperation) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.PROCESS_ID, "anyProcessId");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("100 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.REDIRECTION_REQUIRED;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }
}
