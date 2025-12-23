package org.sunbird.service.organisation;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  ServiceFactory.class,
  ProjectUtil.class
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
  private ElasticSearchService esService = null;
  private CassandraOperation cassandraOperation = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    PowerMockito.when(
            cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false));
    PowerMockito.when(
            cassandraOperation.updateRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getUpsertRecords());
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false))
        .thenReturn(getRecordsByProperty(false));

    PowerMockito.when(
            cassandraOperation.getRecordsByPrimaryKeys(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString(),
                Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsByProperty(false));
    PowerMockito.when(
            cassandraOperation.getPropertiesValueById(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsByProperty(false));

    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    Map<String, Object> esResponse = new HashMap<>();
    List<Map<String, Object>> orgList = new ArrayList<>();
    Map<String, Object> org = new HashMap<>();
    org.put(JsonKey.ID, "orgId");
    org.put(JsonKey.STATUS, 1);
    orgList.add(org);
    esResponse.put(JsonKey.CONTENT, orgList);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);

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
  public void testGetOrgObjById() {
    OrgService orgService = OrgServiceImpl.getInstance();
    Organisation orgObj = orgService.getOrgObjById("id", new RequestContext());
    Assert.assertNotNull(orgObj);
  }

  @Test
  public void testGetOrgByIds() {
    OrgService orgService = OrgServiceImpl.getInstance();
    List<String> orgIds = new ArrayList<>();
    orgIds.add("id1");
    orgIds.add("id2");
    List<Map<String, Object>> map = orgService.getOrgByIds(orgIds, new RequestContext());
    Assert.assertNotNull(map);
  }

  @Test
  public void testDeleteExternalId() {
    OrgService orgService = OrgServiceImpl.getInstance();
    orgService.deleteOrgExternalIdRecord("channel", "externalId", new RequestContext());
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
    orgMap.put(JsonKey.ID, "orgId");
    orgMap.put(JsonKey.ORG_NAME, "orgName");
    OrgService orgService = OrgServiceImpl.getInstance();
    Response res = orgService.updateOrganisation(orgMap, new RequestContext());
    Assert.assertEquals(res.get(JsonKey.RESPONSE), JsonKey.SUCCESS);
  }

  @Test
  public void testGetOrgIdByChannel() {
    OrgService orgService = OrgServiceImpl.getInstance();
    String rootOrgId = orgService.getRootOrgIdFromChannel("channel", new RequestContext());
    Assert.assertNotNull(rootOrgId);
  }

  @Test
  public void testOrganisationObjSearch() {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, "orgId");
    filters.put(JsonKey.ORG_NAME, "orgName");
    OrgService orgService = OrgServiceImpl.getInstance();
    List<Organisation> orgList = orgService.organisationObjSearch(filters, new RequestContext());
    Assert.assertNotNull(orgList);
  }

  @Test
  public void checkOrgStatusTransition() {
    OrgService orgService = OrgServiceImpl.getInstance();
    boolean bool =
        orgService.checkOrgStatusTransition(
            ProjectUtil.OrgStatus.ACTIVE.getValue(), ProjectUtil.OrgStatus.INACTIVE.getValue());
    Assert.assertTrue(bool);
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
      map.put(JsonKey.ORG_ID, "orgId");
      map.put(
          JsonKey.ORG_LOCATION,
          "[{\"id\":\"1\",\"type\":\"state\"},{\"id\":\"2\",\"type\":\"district\"}]");

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

  @Test
  public void testRegisterChannel() {
    OrgService orgService = OrgServiceImpl.getInstance();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "ch");
    map.put(JsonKey.DESCRIPTION, "desc");
    map.put(JsonKey.ID, "id");
    Boolean bool = orgService.registerChannel(map, JsonKey.CREATE, new RequestContext());
    Assert.assertNotNull(bool);
  }

  @Test
  public void testUpdateChannel() {
    OrgService orgService = OrgServiceImpl.getInstance();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "ch");
    map.put(JsonKey.DESCRIPTION, "desc");
    map.put(JsonKey.ID, "id");
    Boolean bool = orgService.registerChannel(map, JsonKey.UPDATE, new RequestContext());
    Assert.assertNotNull(bool);
  }

  @Test
  public void testUpdateChannelV2() {
    PowerMockito.mockStatic(ProjectUtil.class);

    when(ProjectUtil.getConfigValue(JsonKey.CHANNEL_REGISTRATION_DISABLED)).thenReturn("true");
    OrgService orgService = OrgServiceImpl.getInstance();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "ch");
    map.put(JsonKey.DESCRIPTION, "desc");
    map.put(JsonKey.ID, "id");
    Boolean bool = orgService.registerChannel(map, JsonKey.UPDATE, new RequestContext());
    Assert.assertNotNull(bool);
  }
}
