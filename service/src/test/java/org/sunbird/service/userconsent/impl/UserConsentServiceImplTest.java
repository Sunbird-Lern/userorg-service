package org.sunbird.service.userconsent.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sunbird.common.Constants;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.dao.userconsent.UserConsentDao;
import org.sunbird.dao.userconsent.impl.UserConsentDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.userconsent.UserConsentService;
import org.sunbird.util.DataCacheHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  UserConsentDaoImpl.class,
  UserConsentDao.class,
  OrgDaoImpl.class,
  OrgDao.class,
  DataCacheHandler.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserConsentServiceImplTest {

  private static UserConsentService userConsentService;
  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> orgList = new ArrayList<>();
    response.getResult().put(JsonKey.RESPONSE, orgList);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> config = new HashMap<>();
    config.put(JsonKey.CUSTODIAN_ORG_ID, "custodianOrgId");
    PowerMockito.when(DataCacheHandler.getConfigSettings()).thenReturn(config);
    PowerMockito.mockStatic(UserConsentDaoImpl.class);
    UserConsentDao userConsentDao = PowerMockito.mock(UserConsentDaoImpl.class);
    PowerMockito.when(UserConsentDaoImpl.getInstance()).thenReturn(userConsentDao);
    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(
        JsonKey.ID,
        "usr-consent:529a57aa-6365-4145-9a35-e8cfc934eb4e:0130107621805015045:do_31313966505806233613406");
    responseMap.put(JsonKey.CONSENT_CONSUMERTYPE, "ORGANISATION");
    responseMap.put(JsonKey.CONSENT_CONSUMERID, "0130107621805015045");
    responseMap.put(JsonKey.USER_ID, "529a57aa-6365-4145-9a35-e8cfc934eb4e");
    responseMap.put(JsonKey.CONSENT_OBJECTID, "do_31313966505806233613406");
    responseMap.put(JsonKey.CONSENT_OBJECTTYPE, "Collection");
    responseMap.put(JsonKey.STATUS, "ACTIVE");
    PowerMockito.when(userConsentDao.getConsent(Mockito.anyMap(), Mockito.any()))
        .thenReturn(Arrays.asList(responseMap));
    PowerMockito.when(userConsentDao.updateConsent(Mockito.anyMap(), Mockito.any()))
        .thenReturn(upsertResponse);
    userConsentService = UserConsentServiceImpl.getInstance();
  }

  @Test
  public void testUpdateConsent() {
    Response response = userConsentService.updateConsent(consentUpdateMap(), new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateConsumerId() {
    userConsentService.validateConsumerId("custodianOrgId", new RequestContext());
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateConsumerIdV2() {
    userConsentService.validateConsumerId("OrgId", new RequestContext());
  }

  @Test
  public void testGetConsent() {
    List<Map<String, Object>> consentResponse = userConsentService.getConsent(getConsentRequest());
    Assert.assertNotNull(consentResponse);
  }

  private Map<String, Object> consentUpdateMap() {
    Map<String, Object> consentMap = new HashMap<>();
    consentMap.put("userId", "529a57aa-6365-4145-9a35-e8cfc934eb4e");
    consentMap.put("consumerId", "0130107621805015045");
    consentMap.put("objectId", "do_31313966505806233613406");

    return consentMap;
  }

  private Request getConsentRequest() {
    Map<String, String> filterMap = new HashMap();
    Map<String, Object> consentMap = new HashMap<>();
    Map<String, Object> requestMap = new HashMap<>();
    filterMap.put("userId", "529a57aa-6365-4145-9a35-e8cfc934eb4e");
    filterMap.put("consumerId", "0130107621805015045");
    filterMap.put("objectId", "do_31313966505806233613406");
    consentMap.put("filters", filterMap);
    requestMap.put("consent", consentMap);
    Request request = new Request();
    request.getRequest().putAll(requestMap);
    return request;
  }
}
