package org.sunbird.actor.role;

import java.time.Duration;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.pattern.PipeToSupport;
import org.apache.pekko.testkit.javadsl.TestKit;
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
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.role.RoleService;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.PropertiesCache;
import org.sunbird.util.Util;

import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        RoleService.class,
        DataCacheHandler.class,
        PropertiesCache.class,
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

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
public class UserRoleActorTestV2 {
    private ActorSystem system = ActorSystem.create("system");
    private static final Props props = Props.create(UserRoleActor.class);
    private CassandraOperationImpl cassandraOperation;
    private ElasticSearchRestHighImpl esService;
    private PropertiesCache propertiesCache;

    @Before
    public void beforeEachTest() {
        PowerMockito.mockStatic(DataCacheHandler.class);
        PowerMockito.mockStatic(ServiceFactory.class);
        PowerMockito.mockStatic(RoleDaoImpl.class);
        PowerMockito.mockStatic(Util.class);
        PowerMockito.mockStatic(UserOrgDaoImpl.class);
        PowerMockito.mockStatic(EsClientFactory.class);
        propertiesCache = PowerMockito.mock(PropertiesCache.class);
        PowerMockito.mockStatic(PropertiesCache.class);


        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("anyRole4", "anyRole4");
        roleMap.put("EDITOR","Editor");
        roleMap.put("PUBLIC","Public");
        PowerMockito.when(DataCacheHandler.getRoleMap()).thenReturn(roleMap);
        PowerMockito.when(PropertiesCache.getInstance()).thenReturn(propertiesCache);

        cassandraOperation = mock(CassandraOperationImpl.class);
        RoleDaoImpl roleDao = Mockito.mock(RoleDaoImpl.class);
        when(RoleDaoImpl.getInstance()).thenReturn(roleDao);
        UserOrgDao userOrgDao = Mockito.mock(UserOrgDaoImpl.class);
        when(UserOrgDaoImpl.getInstance()).thenReturn(userOrgDao);
        when(userOrgDao.updateUserOrg(Mockito.anyObject(), Mockito.any()))
                .thenReturn(getSuccessResponse());
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

    }

    @Test
    public void testAssignRoleFailure() {
        PowerMockito.when(propertiesCache.getProperty(JsonKey.DISABLE_MULTIPLE_ORG_ROLE)).thenReturn("true");
        Map<String, Object> req = getAssignRoleRequest();
        Request request = getRequest(req, ActorOperations.ASSIGN_ROLES.getValue());
        boolean result = testScenario(request, ResponseCode.roleProcessingInvalidOrgError);
        assertTrue(result);
    }

    private boolean testScenario(Request request, ResponseCode errorCode) {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        subject.tell(request, probe.getRef());

        if (errorCode == null) {
            Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
            return null != res && res.getResponseCode() == ResponseCode.OK;
        } else {
            ProjectCommonException res =
                    probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
            return res.getResponseCode().name().equals(errorCode.name())
                    || res.getErrorResponseCode() == errorCode.getResponseCode();
        }
    }

    private Map<String, Object> getAssignRoleRequest() {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.ORGANISATION_ID, "ORGANISATION_ID");
        map.put(JsonKey.USER_ID, "USER_ID");
        map.put(JsonKey.ROLES, Arrays.asList("anyRole4"));
        return map;
    }

    private Request getRequest(Map<String, Object> requestData, String actorOperation) {
        Request reqObj = new Request();
        reqObj.setRequest(requestData);
        reqObj.setOperation(actorOperation);
        return reqObj;
    }

    private Response getSuccessResponse() {
        Response response = new Response();
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
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
}
