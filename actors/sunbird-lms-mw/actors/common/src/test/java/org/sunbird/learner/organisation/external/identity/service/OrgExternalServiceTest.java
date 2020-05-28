package org.sunbird.learner.organisation.external.identity.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class, ServiceFactory.class, CassandraOperation.class, CassandraUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class OrgExternalServiceTest {

    private CassandraOperation cassandraOperation;
    private final String ORG_EXTERNAL_IDENTITY = "org_external_identity";
    private OrgExternalService orgExternalService;

    @Before
    public void setUp()  {
        orgExternalService=new OrgExternalService();
        cassandraOperation= PowerMockito.mock(CassandraOperation.class);
        PowerMockito.mockStatic(ServiceFactory.class);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    }

    @Test
    public void testGetOrgIdFromOrgExtIdFailure() {
        try {

            Map<String, Object> dbRequestMap = new HashMap<>();
            dbRequestMap.put(JsonKey.EXTERNAL_ID, "anyorgextid");
            dbRequestMap.put(JsonKey.PROVIDER, "anyprovider");
            Response response = new Response();
            List<Map<String, Object>> orgList =new ArrayList<>();
            Map<String,Object>map=new HashMap<>();
            orgList.add(map);
            response.put(JsonKey.RESPONSE, orgList);
            when(cassandraOperation.getRecordsByCompositeKey(Util.KEY_SPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap)).thenReturn(response);
            String resp = orgExternalService.getOrgIdFromOrgExternalIdAndProvider("anyOrgExtid", "anyprovider");
            Assert.assertEquals(null, resp);

        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }
    @Test
    public void testGetOrgIdFromOrgExtIdSuccess() {
        try {
            Map<String, Object> dbRequestMap = new HashMap<>();
            dbRequestMap.put(JsonKey.EXTERNAL_ID, "orgextid");
            dbRequestMap.put(JsonKey.PROVIDER, "provider");
            Response response = new Response();
            List<Map<String, Object>> orgList =new ArrayList<>();
            Map<String,Object>map=new HashMap<>();
            map.put(JsonKey.ORG_ID,"anyOrgId");
            orgList.add(map);
            response.put(JsonKey.RESPONSE, orgList);
            when(cassandraOperation.getRecordsByCompositeKey(Util.KEY_SPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap)).thenReturn(response);
            String resp = orgExternalService.getOrgIdFromOrgExternalIdAndProvider("OrgExtid", "provider");
            Assert.assertEquals("anyOrgId", resp);

        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

}