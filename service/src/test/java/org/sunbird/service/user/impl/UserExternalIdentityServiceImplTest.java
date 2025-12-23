package org.sunbird.service.user.impl;

import static org.powermock.api.mockito.PowerMockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserExternalIdentityService;

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
public class UserExternalIdentityServiceImplTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
  }

//  @Test
  @Ignore
  public void getExternalIdsTest() {
    Response response = new Response();
    List<Map<String, String>> responseList = new ArrayList<>();
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.ID, "extId");
    externalId.put(JsonKey.ID_TYPE, "idType");
    externalId.put(JsonKey.PROVIDER, "idType");
    externalId.put(JsonKey.ORIGINAL_EXTERNAL_ID, "extId");
    externalId.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_DISTRICT);
    externalId.put(JsonKey.ORIGINAL_PROVIDER, "9999911111");
    externalId.put(JsonKey.DECLARED_EMAIL, "xyz@xyz.com");
    externalId.put(JsonKey.DECLARED_PHONE, "9999911111");
    externalId.put(JsonKey.DECLARED_DISTRICT, "district");
    externalId.put(JsonKey.DECLARED_STATE, "state");
    externalId.put(JsonKey.CODE, "code");

    responseList.add(externalId);
    response.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);

    Response response1 = new Response();
    List<Map<String, String>> responseList1 = new ArrayList<>();
    Map<String, String> location = new HashMap<>();
    location.put(JsonKey.ID, "extId");
    location.put(JsonKey.CODE, "code");
    responseList1.add(location);
    response1.getResult().put(JsonKey.RESPONSE, responseList1);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response1);
    UserExternalIdentityService userExternalIdentityService = new UserExternalIdentityServiceImpl();
    List<Map<String, String>> externalIds =
        userExternalIdentityService.getExternalIds("userId", true, new RequestContext());
    Assert.assertNotNull(externalIds);
  }

  @Test
  public void deleteExternalIdsTest() {
    Map<String, String> userExtIdMap = new HashMap<>();
    userExtIdMap.put("provider", "0132818330295992324");
    userExtIdMap.put("idtype", "teacherId");
    userExtIdMap.put("externalid", "cedd456");
    userExtIdMap.put("userid", "46545665465465");

    List<Map<String, String>> userExtIdRespList = new ArrayList<>();
    userExtIdRespList.add(userExtIdMap);

    doNothing()
        .when(cassandraOperationImpl)
        .deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());

    UserExternalIdentityService userExternalIdentityService = new UserExternalIdentityServiceImpl();
    Assert.assertTrue(
        userExternalIdentityService.deleteUserExternalIds(userExtIdRespList, new RequestContext()));
  }
}
