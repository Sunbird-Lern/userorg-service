package org.sunbird.util;

import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
  CassandraOperation.class,
  CassandraUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class SMSTemplateProviderTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    List<String> idList = new ArrayList<>();
    idList.add("welcomeTemplate");
    Response response = new Response();
    List<Map<String, Object>> orgList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(
        "template",
        "Welcome to $instanceName. Your user account has now been created. Click on the link below to #if ($setPasswordLink) set a password #else verify your email ID #end and start using your account:$newline$link");
    orgList.add(map);
    response.put(JsonKey.RESPONSE, orgList);
    when(cassandraOperationImpl.getRecordsByPrimaryKeys(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyString(),
            Mockito.any()))
        .thenReturn(response);
  }

  @Test
  public void testGetSMSBody() {
    Map<String, String> templateMap = new HashMap<>();
    templateMap.put(JsonKey.NAME, "userName");
    templateMap.put(JsonKey.ORG_NAME, "orgName");
    templateMap.put(JsonKey.NAME, "firstName");
    templateMap.put(JsonKey.SIGNATURE, "signature");
    String sms =
        SMSTemplateProvider.getSMSBody("welcomeTemplate", templateMap, new RequestContext());
    assertNotNull(sms);
  }
}
