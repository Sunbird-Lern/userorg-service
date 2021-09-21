package org.sunbird.service.user.impl;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.TenantMigrationService;
import org.sunbird.service.user.UserService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        CassandraOperationImpl.class,
        UserServiceImpl.class,
        UserService.class,
        OrgServiceImpl.class,
        OrgService.class,
        OrgExternalService.class
})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
public class TenantMigrationServiceImplTest {

    private CassandraOperation cassandraOperation = null;
    private static OrgExternalServiceImpl orgExternalService;

    @Before
    public void beforeEachTest() throws Exception {
        PowerMockito.mockStatic(ServiceFactory.class);
        PowerMockito.mockStatic(UserServiceImpl.class);
        PowerMockito.mockStatic(OrgServiceImpl.class);
        PowerMockito.mockStatic(OrgExternalServiceImpl.class);
        cassandraOperation = mock(CassandraOperationImpl.class);
        PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
        Response updateResponse = new Response();
        updateResponse.getResult().put(JsonKey.RESPONSE, "SUCCESS");
        UserService userService = mock(UserServiceImpl.class);
        PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
        when(userService.getRootOrgIdFromChannel(Mockito.anyObject(), Mockito.anyObject()))
                .thenReturn("anyRootOrgId");
        OrgService orgService = mock(OrgServiceImpl.class);
        PowerMockito.when(OrgServiceImpl.getInstance()).thenReturn(orgService);
        when(orgService.getOrgById(Mockito.anyString(), Mockito.any(RequestContext.class)))
                .thenReturn(getOrgandLocation());
        orgExternalService = PowerMockito.mock(OrgExternalServiceImpl.class);
        whenNew(OrgExternalServiceImpl.class).withNoArguments().thenReturn(orgExternalService);
        when(orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
                .thenReturn("anyRootOrgId");
        PowerMockito.when(
                cassandraOperation.updateRecord(
                        Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(updateResponse);
        PowerMockito.when(
                cassandraOperation.getRecordsByCompositeKey(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getRecordsByProperty(true));
    }

    @Test
    public void migrateUser() {
        List<Map<String, Object>> listMap = new ArrayList<>();
        listMap.add(new HashMap<String, Object>());
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put(JsonKey.ROOT_ORG_ID, "");
        userDetails.put(JsonKey.ORGANISATIONS, listMap);
        TenantMigrationService tenantMigrationService = TenantMigrationServiceImpl.getInstance();
        tenantMigrationService.migrateUser(getSelfDeclaredMigrateReq(), userDetails);
    }

    public Request getSelfDeclaredMigrateReq() {
        Request reqObj = new Request();
        Map<String, Object> requestMap = new HashMap();
        Map<String, String> externalIdMap = new HashMap();
        List<Map<String, String>> externalIdLst = new ArrayList();
        requestMap.put(JsonKey.USER_ID, "anyUserID");
        requestMap.put(JsonKey.CHANNEL, "anyChannel");
        requestMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
        externalIdMap.put(JsonKey.ID, "anyID");
        externalIdMap.put(JsonKey.ID_TYPE, "anyIDtype");
        externalIdMap.put(JsonKey.PROVIDER, "anyProvider");
        externalIdLst.add(externalIdMap);
        requestMap.put(JsonKey.EXTERNAL_IDS, externalIdLst);
        requestMap.put(JsonKey.ORG_EXTERNAL_ID, "anyOrgId");
        reqObj.setRequest(requestMap);
        return reqObj;
    }

    public Map<String, Object> getOrgandLocation() {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.ORG_ID, "anyOrgId");
        map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
        return map;
    }

    private Response getRecordsByProperty(boolean empty) {
        Response res = new Response();
        List<Map<String, Object>> list = new ArrayList<>();
        if (empty) {
            Map<String, Object> map = new HashMap<>();
            map.put(JsonKey.ID, "orgId");
            map.put(JsonKey.ORG_ID, "orgId");
            map.put(JsonKey.IS_DELETED, true);
            map.put(JsonKey.CHANNEL, "channel1");
            map.put(JsonKey.IS_TENANT, true);
            list.add(map);
        }
        res.put(JsonKey.RESPONSE, list);
        return res;
    }
}