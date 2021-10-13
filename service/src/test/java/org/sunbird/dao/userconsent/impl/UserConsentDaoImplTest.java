package org.sunbird.dao.userconsent.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.dao.feed.impl.FeedDaoImpl;
import org.sunbird.dao.userconsent.UserConsentDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserConsentDaoImplTest {

  private static CassandraOperation cassandraOperation = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(FeedDaoImpl.getCassandraInstance()).thenReturn(cassandraOperation);
    initCassandraForSuccess();
  }

  private static void initCassandraForSuccess() {
    Response response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getConsentMap()));
    response.getResult().putAll(responseMap);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);

    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);

    when(cassandraOperation.upsertRecord(
            Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(upsertResponse);
  }

  private static Map<String, Object> getConsentMap() {
    Map<String, Object> consentMap = new HashMap<>();
    consentMap.put("userId", "5a8a3f2b-3409-42e0-9001-f913bc0fde31");
    consentMap.put("consumerId", "0126632859575746566");
    consentMap.put("status", "ACTIVE");
    consentMap.put("objectType", "Collection");
    consentMap.put("objectId", "do_31313966505806233613406");
    return consentMap;
  }

  @Test
  public void testUpdateConsent() {
    UserConsentDao userConsentDao = new UserConsentDaoImpl();
    Response res = userConsentDao.updateConsent(getConsentMap(), null);
    Assert.assertTrue(
        ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
  }

  @Test
  public void testGetConsent() {
    Map<String, Object> props = new HashMap<>();
    props.put(JsonKey.USER_ID, "5a8a3f2b-3409-42e0-9001-f913bc0fde39");
    UserConsentDao userConsentDao = new UserConsentDaoImpl();
    List<Map<String, Object>> res = userConsentDao.getConsent(props, null);
    Assert.assertNotNull(res);
  }
}
