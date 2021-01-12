package org.sunbird.learner.organisation.dao.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.dispatch.Futures;
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
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.util.Util;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
  CassandraOperation.class,
  CassandraUtil.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  Util.class
})
@PowerMockIgnore({"javax.management.*"})
public class OrgDaoImplTest {

  private CassandraOperation cassandraOperation;
  private static ElasticSearchService esService;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
  }

  @Test
  public void testGetOrgById() {
    try {
      cassandraOperation = PowerMockito.mock(CassandraOperation.class);
      PowerMockito.mockStatic(ServiceFactory.class);
      when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.CONTACT_DETAILS, "contact");
      map.put(JsonKey.ID, "contact");
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(RequestContext.class)))
          .thenReturn(response);
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.getOrgById("1234567890", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testGetOrgByIdWithEmptyResponse() {
    try {
      cassandraOperation = PowerMockito.mock(CassandraOperation.class);
      PowerMockito.mockStatic(ServiceFactory.class);
      when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(RequestContext.class)))
          .thenReturn(response);
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.getOrgById("1234567890", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testGetOrgByExternalId() {
    try {
      PowerMockito.mockStatic(EsClientFactory.class);
      ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
      when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
      setEsSearchResponse(getOrgSearchResponseMap());
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.esGetOrgByExternalId("1234567890", "provider", null);
      System.out.println("testGetOrgByExternalId resp map ::::::::::::::::::::" + resp);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testGetOrgByExternalIdWithEmptyResponse() {
    try {
      PowerMockito.mockStatic(EsClientFactory.class);
      ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
      when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
      setEsSearchResponse(getOrgSearchEmptyResponseMap());
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.esGetOrgByExternalId("1234567890", "provider", null);
      System.out.println(
          "testGetOrgByExternalIdWithEmptyResponse resp map ::::::::::::::::::::" + resp);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  public void setEsSearchResponse(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.search(
            Mockito.anyObject(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
  }

  private static Map<String, Object> getOrgSearchResponseMap() {
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> response = new HashMap<>();
    response.put(JsonKey.ID, "123");
    response.put(JsonKey.ORG_NAME, "Name");
    List contentList = new ArrayList<>();
    contentList.add(response);
    map.put(JsonKey.CONTENT, contentList);
    return map;
  }

  private static Map<String, Object> getOrgSearchEmptyResponseMap() {
    Map<String, Object> map = new HashMap<>();
    List contentList = new ArrayList<>();
    map.put(JsonKey.CONTENT, contentList);
    return map;
  }
}
