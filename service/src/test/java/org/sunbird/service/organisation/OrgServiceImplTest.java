package org.sunbird.service.organisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.dispatch.Futures;
import org.junit.Assert;
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
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import scala.concurrent.Promise;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ElasticSearchRestHighImpl.class,
        ElasticSearchHelper.class,
        EsClientFactory.class,
        CassandraOperationImpl.class,
        ServiceFactory.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
public class OrgServiceImplTest {

  @Before
  public void setUp() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    PowerMockito.when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(true));
    PowerMockito.when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getUpsertRecords());
    PowerMockito.when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(getRecordsByProperty(true));

    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    Map<String, Object> esRespone = new HashMap<>();
    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    esRespone.put(JsonKey.ID, "orgId");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esRespone);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(promise.future());
  }

  @Test
  public void testGetOrgById() {
    OrgService orgService = OrgServiceImpl.getInstance();
    Map<String, Object> map = orgService.getOrgById("id", new RequestContext());
    Assert.assertNotNull(map);
  }

  @Test
  public void testDeleteExternalId() {
    OrgService orgService = OrgServiceImpl.getInstance();
    orgService.deleteOrgExternalIdRecord("channel","externalId", new RequestContext());

  }

  @Test
  public void testGetOrgByExternalIdAndProvider() {
    OrgService orgService = OrgServiceImpl.getInstance();
    Map<String, Object> map =
        orgService.getOrgByExternalIdAndProvider("extId", "provider", new RequestContext());
    Assert.assertNotNull(map);
  }

  @Test
  public void testGetOrgIdFromSlug() {
    OrgService orgService = OrgServiceImpl.getInstance();
    String orgId = orgService.getOrgIdFromSlug("slug", new RequestContext());
    Assert.assertNotNull(orgId);
  }

  @Test
  public void testUpdateOrganisation() {
    HashMap<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID,"orgId");
    orgMap.put(JsonKey.ORG_NAME,"orgName");
    OrgService orgService = OrgServiceImpl.getInstance();
    Response res = orgService.updateOrganisation(orgMap, new RequestContext());
    Assert.assertEquals(res.get(JsonKey.RESPONSE),JsonKey.SUCCESS);
  }

  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "orgId");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getUpsertRecords() {
    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return res;
  }
}
