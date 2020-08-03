package org.sunbird.learner.actors.bulkupload;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.HashMap;
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
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.util.UserUploadUtil;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DeclaredExternalIdActor.class,
  UserUploadUtil.class
})
@PowerMockIgnore("javax.management.*")
public class DeclaredExternalIdActorTest {
  private static final Props props = Props.create(DeclaredExternalIdActor.class);
  private static ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;
  private static UserUploadUtil userUploadUtil;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(UserUploadUtil.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Test
  public void testUploadDeclaredUser() throws Exception {
    when(UserUploadUtil.getFullRecordFromProcessId(Mockito.anyString())).thenReturn(recordData());
    doNothing()
        .when(
            UserUploadUtil.class,
            "updateStatusInUserBulkTable",
            Mockito.anyString(),
            Mockito.anyInt());
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.USER_BULK_MIGRATION), null);
  }

  Map<String, Object> recordData() {
    Map<String, Object> recordMap = new HashMap<>();
    recordMap.put("status", "");
    recordMap.put(
        "data",
        "PYOeYF3BCS4Xv8duenjraA46kuN5J96XQMHyZz9NRDdLiOkPtBMYGuj6CRytyGwVrvNknVJ8Kzt7\\ndRBNmR5A7IPfuGm0X1XEb/YQjXzgR5G/A6fNKJKMiTYyYHeR9vJyPGJ5jf5zDC/wiAxk80kD3X9b\\nixZo4RN6k3/PPHvaU7iVxSPc1+F74JD2WV72fSbHEuabJAYAH4inbvCRiYq37/KvH9u2bhTV7TPV\\neI6rVIkaYzD07JPNMeVXJqIGbpBCOof9I/yVoczuDRCCPma3fz549diFEVWmUDkKppTGBIRlA1AP\\njIxTkX0DPSEQ5HRHvcKEMtiO6w/S/NNkyP7n6ddkjr75U0hM1uswr4HoeJh9SQeSxMGsvJpgrITO\\nEsI7rp1gID/xTOxz7eIsu7GG+ZDmfECggGC9pAiEsO3dsQZqhOeU8Ge2WAw+a4pvnGIhk478k98e\\nDtATCCcOlJt8zX4hRNuZp0gX9lKAZh5Zb1PfAaYl+wfqRl5fT12EqVdwPoMxO4DeiztONbe65fy0\\n52WWZlBorSgzxjA3qH5xUV4/EuRPcnI3DypooVZgmxDKNKm7AeAABPUNMzgH4spfsFOf1n0XnOut\\nJ2ZNY6vCaduqZluhXE9eTGPqvIzlJDKAEPH+gxPt6LzdndcUbALegmGD7YmpdY8iggsVOM0vtb4=");
    return recordMap;
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
