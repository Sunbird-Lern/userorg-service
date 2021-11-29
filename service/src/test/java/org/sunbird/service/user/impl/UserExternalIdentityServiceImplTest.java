package org.sunbird.service.user.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;
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
  ObjectMapper mapper = new ObjectMapper();
  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
  }

  @Test
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
  public void testConvertSelfDeclareFieldsToExternalIds() {
    Map<String, Object> selfDeclaredFields = getSelfDeclareFields();
    List<Map<String, String>> externalIds =
            UserExternalIdentityServiceImpl.convertSelfDeclareFieldsToExternalIds(selfDeclaredFields);
    String declaredEmail = "";
    String declaredPhone = "";
    for (Map<String, String> extIdMap : externalIds) {
      if (JsonKey.DECLARED_EMAIL.equals((String) extIdMap.get(JsonKey.ORIGINAL_ID_TYPE))) {
        declaredEmail = (String) extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID);
      }
      if (JsonKey.DECLARED_PHONE.equals((String) extIdMap.get(JsonKey.ORIGINAL_ID_TYPE))) {
        declaredPhone = (String) extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID);
      }
    }

    Assert.assertEquals("abc@tenant.com", declaredEmail);
    Assert.assertEquals("999999999", declaredPhone);
  }

  @Test
  public void testConvertExternalFieldsToSelfDeclareFields() {
    Map<String, Object> declaredFeilds = getSelfDeclareFields();
    List<Map<String, String>> externalIds =
            UserExternalIdentityServiceImpl.convertSelfDeclareFieldsToExternalIds(declaredFeilds);
    Map<String, Object> resultDeclaredFields =
            UserExternalIdentityServiceImpl.convertExternalFieldsToSelfDeclareFields(externalIds);
    Assert.assertEquals(
            declaredFeilds.get(JsonKey.USER_ID), resultDeclaredFields.get(JsonKey.USER_ID));
    Assert.assertEquals(
            ((Map<String, Object>) declaredFeilds.get(JsonKey.USER_INFO)).get(JsonKey.DECLARED_EMAIL),
            ((Map<String, Object>) resultDeclaredFields.get(JsonKey.USER_INFO))
                    .get(JsonKey.DECLARED_EMAIL));
  }

  private Map<String, Object> getSelfDeclareFields() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setUserId("userid");
    userDeclareEntity.setOrgId("org");
    userDeclareEntity.setPersona(JsonKey.TEACHER_PERSONA);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "abc@tenant.com");
    userInfo.put(JsonKey.DECLARED_PHONE, "999999999");
    userDeclareEntity.setUserInfo(userInfo);
    Map<String, Object> selfDeclaredMap = mapper.convertValue(userDeclareEntity, Map.class);
    return selfDeclaredMap;
  }
}
