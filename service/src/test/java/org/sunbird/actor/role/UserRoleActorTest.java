package org.sunbird.actor.role;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.pattern.PipeToSupport;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.dao.role.impl.RoleDaoImpl;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.util.Util;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  RoleDaoImpl.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  Util.class,
  UserOrgDaoImpl.class,
  DecryptionService.class,
  Patterns.class,
  PipeToSupport.PipeableFuture.class,
  OrgServiceImpl.class,
  OrgService.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserRoleActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(UserRoleActor.class);
  private final Response response = Mockito.mock(Response.class);
  private CassandraOperationImpl cassandraOperation;
  private ElasticSearchRestHighImpl esService;

  private Response getRecordByPropertyResponse() {

    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.IS_DELETED, false);
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Before
  public void beforeEachTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(RoleDaoImpl.class);
    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(UserOrgDaoImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ServiceFactory.class);

    cassandraOperation = mock(CassandraOperationImpl.class);
    RoleDaoImpl roleDao = Mockito.mock(RoleDaoImpl.class);
    when(RoleDaoImpl.getInstance()).thenReturn(roleDao);
    UserOrgDao userOrgDao = Mockito.mock(UserOrgDaoImpl.class);
    when(UserOrgDaoImpl.getInstance()).thenReturn(userOrgDao);
    when(userOrgDao.updateUserOrg(Mockito.anyObject(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    CompletionStage completionStage = Mockito.mock(CompletionStage.class);
    ActorSelection actorSelection = Mockito.mock(ActorSelection.class);
    when(actorSelection.resolveOneCS(Duration.create(Mockito.anyLong(), "seconds")))
        .thenReturn(completionStage);

    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getCassandraResponse());
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordByPropertyResponse());
    when(cassandraOperation.batchInsert(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    cassandraOperation.deleteRecord(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getCassandraUserRoleResponse());
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    PowerMockito.mockStatic(Patterns.class);
    Future<Object> future = Futures.future(() -> response, system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
  }

  @Test
  public void testGetUserRoleSuccess() {
    assertTrue(testScenario(true, true, null, false));
  }

  @Test
  public void testAssignRolesV2SuccessWithValidOrgId() {
    PowerMockito.mockStatic(OrgServiceImpl.class);
    OrgService orgService = PowerMockito.mock(OrgService.class);
    when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ORGANISATION_ID, "1234567890");
    orgMap.put(JsonKey.HASHTAGID, "1234567890");
    when(orgService.getOrgById(Mockito.anyString(), Mockito.any())).thenReturn(orgMap);
    when(orgService.getOrgByExternalIdAndProvider(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(orgMap);
    assertTrue(testScenario(true, null, true));
  }

  @Test
  public void testAssignRolesSuccessWithValidOrgId() {
    PowerMockito.mockStatic(OrgServiceImpl.class);
    OrgService orgService = PowerMockito.mock(OrgService.class);

    when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ORGANISATION_ID, "1234567890");
    orgMap.put(JsonKey.HASHTAGID, "1234567890");
    when(orgService.getOrgById(Mockito.anyString(), Mockito.any())).thenReturn(orgMap);
    when(orgService.getOrgByExternalIdAndProvider(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(orgMap);
    assertTrue(testScenario(true, null, false));
  }

  private boolean testScenario(boolean isOrgIdReq, ResponseCode errorResponse, boolean isV2) {
    return testScenario(false, isOrgIdReq, errorResponse, isV2);
  }

  private boolean testScenario(
      boolean isGetUserRoles, boolean isOrgIdReq, ResponseCode errorResponse, boolean isV2) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    if (isGetUserRoles) {

      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ROLES.getValue());
      subject.tell(reqObj, probe.getRef());
    } else if (isV2) {
      DecryptionService decryptionService = Mockito.mock(DecryptionService.class);
      when(decryptionService.decryptData(Mockito.anyMap(), Mockito.any()))
          .thenReturn(getOrganisationsMap());

      if (errorResponse == null) {
        when(response.get(Mockito.anyString())).thenReturn(new HashMap<>());
        mockGetOrgResponse(true);
      } else {
        mockGetOrgResponse(false);
      }
      subject.tell(createUserRoleRequestV2(true, true, true, true), probe.getRef());
    } else {
      DecryptionService decryptionService = Mockito.mock(DecryptionService.class);
      when(decryptionService.decryptData(Mockito.anyMap(), Mockito.any()))
          .thenReturn(getOrganisationsMap());

      if (errorResponse == null) {
        when(response.get(Mockito.anyString())).thenReturn(new HashMap<>());
        mockGetOrgResponse(true);
      } else {
        mockGetOrgResponse(false);
      }
      subject.tell(getRequestObj(isOrgIdReq), probe.getRef());
    }
    if (errorResponse == null) {
      Response res = probe.expectMsgClass(java.time.Duration.ofSeconds(100), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      Response res1 = probe.expectMsgClass(java.time.Duration.ofSeconds(100), Response.class);
      if (res1.getResult().size() <= 1) {
        return false;
      }
      ProjectCommonException res =
          probe.expectMsgClass(java.time.Duration.ofSeconds(100), ProjectCommonException.class);
      return res.getErrorResponseCode() == errorResponse.getResponseCode();
    }
  }

  private Map<String, Object> getOrganisationsMap() {

    Map<String, Object> orgMap = new HashMap<>();
    List<Map<String, Object>> list = new ArrayList<>();
    orgMap.put(JsonKey.ORGANISATION_ID, "ORGANISATION_ID");
    list.add(orgMap);
    orgMap.put(JsonKey.ORGANISATIONS, list);
    return orgMap;
  }

  private Map<String, Object> createResponseGet(boolean isResponseRequired) {
    HashMap<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CONTACT_DETAILS, "CONTACT_DETAILS");
    innerMap.put(JsonKey.ID, "ORGANISATION_ID");
    innerMap.put(JsonKey.HASHTAGID, "HASHTAGID");
    HashMap<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ORGANISATION_ID, "ORGANISATION_ID");
    List<Map<String, Object>> orgList = new ArrayList<>();
    orgList.add(orgMap);
    innerMap.put(JsonKey.ORGANISATIONS, orgList);
    if (isResponseRequired) {
      content.add(innerMap);
    }
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  private Object getRequestObj(boolean isOrgIdReq) {
    Request reqObj = new Request();
    List roleLst = new ArrayList();
    roleLst.add("anyRole");
    roleLst.add("anyRole1");
    reqObj.put(JsonKey.ROLES, roleLst);
    reqObj.put(JsonKey.EXTERNAL_ID, "EXTERNAL_ID");
    reqObj.put(JsonKey.USER_ID, "USER_ID");
    reqObj.put(JsonKey.HASHTAGID, "HASHTAGID");
    reqObj.put(JsonKey.PROVIDER, "PROVIDER");
    reqObj.put(JsonKey.ROLE_OPERATION, "assignRole");
    if (isOrgIdReq) {
      reqObj.put(JsonKey.ORGANISATION_ID, "ORGANISATION_ID");
    }
    reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
    return reqObj;
  }

  private void mockGetOrgResponse(boolean isResponseRequired) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createResponseGet(isResponseRequired));
    when(esService.search(Mockito.any(SearchDTO.class), Mockito.anyVararg(), Mockito.any()))
        .thenReturn(promise.future());
  }

  private Response getCassandraUserRoleResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole1");
    orgMap.put(
        JsonKey.SCOPE,
        "[{\"organisationId\":\"ORGANISATION_ID1\"},{\"organisationId\":\"ORGANISATION_ID\"}]");
    list.add(orgMap);
    orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole2");
    orgMap.put(JsonKey.SCOPE, "[{\"organisationId\":\"ORGANISATION_ID\"}]");
    list.add(orgMap);
    orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole3");
    orgMap.put(
        JsonKey.SCOPE,
        "[{\"organisationId\":\"ORGANISATION_ID1\"},{\"organisationId\":\"ORGANISATION_ID\"}]");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response getCassandraResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Request createUserRoleRequestV2(
      boolean isUserIdReq, boolean isScopeReq, boolean isRoleReq, boolean isOpReq) {
    Request reqObj = new Request();

    if (isUserIdReq) reqObj.put(JsonKey.USER_ID, "USER_ID");
    if (isRoleReq) {
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scopeMap = new HashMap<>();
      scopeMap.put(JsonKey.ORGANISATION_ID, "ORGANISATION_ID");
      scopeList.add(scopeMap);

      List<Map<String, Object>> rolesList = new ArrayList<>();
      Map<String, Object> roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "anyRole");
      if (isOpReq) roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      if (isScopeReq) roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "anyRole1");
      roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "anyRole2");
      roleMap.put(JsonKey.OPERATION, JsonKey.REMOVE);
      roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "anyRole3");
      roleMap.put(JsonKey.OPERATION, JsonKey.REMOVE);
      roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);
      reqObj.put(JsonKey.ROLES, rolesList);
    }
    reqObj.setOperation(ActorOperations.ASSIGN_ROLES_V2.getValue());

    return reqObj;
  }
}
