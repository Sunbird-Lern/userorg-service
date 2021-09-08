package org.sunbird.service.user.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserSelfDeclarationService;

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
public class UserSelfDeclarationServiceImplTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> res = new HashMap<>();
    Map<String, String> declaredFields = new HashMap<>();
    declaredFields.put(JsonKey.DECLARED_EMAIL, "xyz@xyz.com");
    declaredFields.put(JsonKey.DECLARED_PHONE, "9999911111");
    res.put(JsonKey.USER_INFO, declaredFields);
    responseList.add(res);
    response.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
  }

  @Test
  public void fetchUserDeclarationsTest() {
    UserSelfDeclarationService userSelfDeclarationService =
        UserSelfDeclarationServiceImpl.getInstance();
    List<Map<String, Object>> declarations =
        userSelfDeclarationService.fetchUserDeclarations("userId", new RequestContext());
    Assert.assertNotNull(declarations);
  }
}
