package org.sunbird.learner.actors.bulkupload;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.operations.BulkUploadActorOperation;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  SunbirdMWService.class,
  OrganisationClientImpl.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class DeclaredExternalIdActorTest {
  private static final Props props = Props.create(DeclaredExternalIdActor.class);
  private static ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;
  private static DecryptionService decryptionService;
  private static SunbirdMWService SunbirdMWService;
  private static OrganisationClient organisationClient;

  @BeforeClass
  public static void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);

    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    decryptionService = mock(DecryptionService.class);
    when(org.sunbird.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(updateData(true));
  }

  private static Response updateData(boolean empty) {
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

  private Response createDeclaredBulkUploadData(String statusType) {
    Response response = new Response();
    Map<String, Object> declaredUserUploadMap = new HashMap<>();
    declaredUserUploadMap.put(JsonKey.ID, "0130752540065218560");
    if (JsonKey.VALIDATED.equals(statusType)) {
      declaredUserUploadMap.put(
          JsonKey.DATA,
          "PYOeYF3BCS4Xv8duenjraA46kuN5J96XQMHyZz9NRDdLiOkPtBMYGuj6CRytyGwVrvNknVJ8Kzt7\\ndRBNmR5A7IPfuGm0X1XEb/YQjXzgR5G/A6fNKJKMiTYyYHeR9vJyPGJ5jf5zDC/wiAxk80kD3X9b\\nixZo4RN6k3/PPHvaU7iVxSPc1+F74JD2WV72fSbHEuabJAYAH4inbvCRiYq37/KvH9u2bhTV7TPV\\neI6rVIkaYzD07JPNMeVXJqIGbpBCOof9I/yVoczuDRCCPma3fz549diFEVWmUDkKppTGBIRlA1AP\\njIxTkX0DPSEQ5HRHvcKEMtiO6w/S/NNkyP7n6ddkjr75U0hM1uswr4HoeJh9SQeSxMGsvJpgrITO\\nEsI7rp1gID/xTOxz7eIsu7GG+ZDmfECggGC9pAiEsO3dsQZqhOeU8Ge2WAw+a4pvnGIhk478k98e\\nDtATCCcOlJt8zX4hRNuZp0gX9lKAZh5Zb1PfAaYl+wfqRl5fT12EqVdwPoMxO4DeiztONbe65fy0\\n52WWZlBorSgzxjA3qH5xUV4/EuRPcnI3DypooVZgmxDKNKm7AeAABPUNMzgH4spfsFOf1n0XnOut\\nJ2ZNY6vCaduqZluhXE9eTGPqvIzlJDKAEPH+gxPt6LzdndcUbALegmGD7YmpdY8iggsVOM0vtb4=");
    } else if (JsonKey.REJECTED.equals(statusType)) {
      declaredUserUploadMap.put(
          JsonKey.DATA,
          "PYOeYF3BCS4Xv8duenjraA46kuN5J96XQMHyZz9NRDdLiOkPtBMYGuj6CRytyGwVrvNknVJ8Kzt7\\ndRBNmR5A7IPfuGm0X1XEb/YQjXzgR5G/A6fNKJKMiTYyYHeR9vJyPGJ5jf5zDC/wiAxk80kD3X9b\\nixZo4RN6k3/PPHvaU7iVxSPc1+F74JD2WV72fSbHEuabJAYAH4inbvCRiYq37/KvH9u2bhTV7TPV\\neI6rVIkaYzD07JPNMeVXJqIGbpBCOof9I/yVoczuDRCCPma3fz549diFEVWmUDkKppTGBIRlA1AP\\njIxTkX0DPSEQ5HRH9GqKI8c53rQKl9OmwLn+Kdi+pJMK3/K/DKq3noKu5jVzytvonHUIid0OFDdk\\nHIqXXYHSajzCw9hzQeoaD5sIrK+YCQklglUYq9QTkXAfFUoBDHq5jTV+qTbtLhApsCyt/H/ExLRd\\ne3W8zHKY/z2jh38VRww7G0B7Pz2xNj4auP09xbIkXRG2NLx9gQk4+pQ8r6PImlxJRkFJlYn6ePQl\\n403NiDZfKefKn4dBLm+ZAiTPT0+QO6gWevtuJgkO6VhRc8aHd/uiEZnRpudFmYu7hGgkVk4qaBJv\\n6ULNlXPpi2LT3pKUFpVaEqVkiU+H2iY1chyf2ZRjlyHabORsk2PKvQzXQ7/CA4/fLbxEU+wvlo8r\\nvNqUr8TCIIf22PnZKxlyv7HWLAL2tEhPYIdBrxUnUbKQo/LFzf4rl6QvL1wdYXAgOeWid21HySz4\\nMHmYxhkFPKboJqKv4eqZ+iijfJzxoT38RMkZtI8alOJ5ePDUmUAsmAW11Lmi7M6+YMZc2Kjg");
    } else if (JsonKey.ERROR.equals(statusType)) {
      declaredUserUploadMap.put(
          JsonKey.DATA,
          "PYOeYF3BCS4Xv8duenjraA46kuN5J96XQMHyZz9NRDdLiOkPtBMYGuj6CRytyGwVrvNknVJ8Kzt7\\ndRBNmR5A7IPfuGm0X1XEb/YQjXzgR5G/A6fNKJKMiTYyYHeR9vJyPGJ5jf5zDC/wiAxk80kD3X9b\\nixZo4RN6k3/PPHvaU7iVxSPc1+F74JD2WV72fSbHEuabJAYAH4inbvCRiYq37/KvH9u2bhTV7TPV\\neI6rVIkaYzD07JPNMeVXJqIGbpBCOof9I/yVoczuDRCCPma3fz549diFEVWmUDkKppTGBIRlA1AP\\njIxTkX0DPSEQ5HRH9GqKI8c53rQKl9OmwLn+Kdi+pJMK3/K/DKq3noKu5jVzytvonHUIid0OFDdk\\nHIqXXYHSajzCw9hzQeoaD5sIrK+YCQklglUYq9QTkXAfFUoBDHq5jTV+qTbtLhApsCyt/H/ExLRd\\ne3W8zHKY/z2jh38VRww7G0B7Pz2xNj4auP09xbIkXRG2NLx9gQk4+pQ8r6PImlxJRkFJlYn6ePQl\\n403NiDZfKefKn4dBLm+ZAiTPT0+QO6gWevtuJgkO6VhRc8aHd/uiEZnRpudFmYu7hGgkVk4qaBJv\\n6ULNlXPpi2LT3pKUFpVaEqVkiU+H2iY1chyf2ZRjlyHabORsk2PKvQzXQ7/CA4/fLbxEU+wvlo8r\\nvNqUr8TCIIf22PnZKxlyv7HWLAL2tEhPYIdBrxUnUbKQo/LFzf4rl6QvL1wdYXAgOeWid21HySz4\\nMHmYxhkFPKboJqKv4eqZ+iijfJzxoT38RMkZtI8alOJ5ePDUmUAsmAW11Lmi7M6+YMZc2Kjg");
    }
    List<Map<String, Object>> result = new ArrayList<>();
    result.add(declaredUserUploadMap);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  @Test
  public void testUploadDeclaredUser() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(createDeclaredBulkUploadData(JsonKey.VALIDATED));
    when(decryptionService.decryptData(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "[{\"email\":null,\"phone\":null,\"name\":null,\"userExternalId\":\"\",\"orgExternalId\":null,\"channel\":\"\",\"inputStatus\":\"VALIDATED\",\"schoolName\":null,\"userId\":\"\",\"persona\":\"teacher\"}]");
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED), null);
    assertTrue(result);
  }

  @Test
  public void testUploadDeclaredUserOFErrorStatus() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(createDeclaredBulkUploadData(JsonKey.ERROR));
    when(decryptionService.decryptData(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "[{\"email\":null,\"phone\":null,\"name\":null,\"userExternalId\":\"\",\"orgExternalId\":null,\"channel\":\"\",\"inputStatus\":\"ERROR\",\"schoolName\":null,\"userId\":\"\",\"persona\":\"teacher\"}]");
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED), null);
    assertTrue(result);
  }

  @Test
  public void testUploadDeclaredUserOFErrorStatusWithErrorType() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(createDeclaredBulkUploadData(JsonKey.ERROR));
    when(decryptionService.decryptData(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "[{\"email\":null,\"phone\":null,\"name\":null,\"userExternalId\":\"\",\"orgExternalId\":null,\"channel\":\"\",\"inputStatus\":\"ERROR\",\"errorType\":\"ERROR_ID\",\"schoolName\":null,\"userId\":\"\",\"persona\":\"teacher\"}]");
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED), null);
    assertTrue(result);
  }

  @Test
  public void testUploadDeclaredUserOfRejectedStatus() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(createDeclaredBulkUploadData(JsonKey.REJECTED));
    when(decryptionService.decryptData(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "[{\"email\":null,\"phone\":null,\"name\":null,\"userExternalId\":\"\",\"orgExternalId\":null,\"channel\":\"\",\"inputStatus\":\"REJECTED\",\"schoolName\":null,\"userId\":\"\",\"persona\":\"teacher\"}]");
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED), null);
    assertTrue(result);
  }

  @Test
  public void testUploadDeclaredUserOfWrongSubOrg() {
    Organisation org = new Organisation();
    org.setRootOrgId("anyOrgId");
    org.setId("anyOrgId");
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(createDeclaredBulkUploadData(JsonKey.VALIDATED));
    when(decryptionService.decryptData(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "[{\"email\":null,\"phone\":null,\"name\":null,\"userExternalId\":\"\",\"subOrgExternalId\":\"anyOrgExternalId\",\"channel\":\"\",\"inputStatus\":\"VALIDATED\",\"userId\":\"\",\"persona\":\"teacher\"}]");
    organisationClient = mock(OrganisationClient.class);
    mockStatic(OrganisationClientImpl.class);
    when(OrganisationClientImpl.getInstance()).thenReturn(organisationClient);
    when(organisationClient.esGetOrgByExternalId(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(org);
    Request request = createRequest(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED);
    request.getRequest().put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    boolean result =
        testScenario(createRequest(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED), null);
    assertTrue(result);
  }

  Request createRequest(BulkUploadActorOperation actorOperation) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.PROCESS_ID, "anyProcessId");
    reqMap.put(JsonKey.ROOT_ORG_ID, "anyOrgId");
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
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }
}
