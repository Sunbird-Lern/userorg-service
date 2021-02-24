package org.sunbird.notification.utils;

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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;

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
public class DataCacheHandlerTest {

  private static CassandraOperationImpl cassandraOperationImpl;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    String templateConfig =
        "[{\\\"OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\\\":\\\"1\\\"},{\\\"OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\\\":\\\"2\\\"},{\\\"Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.\\\":\\\"3\\\"}]";
    Response response = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.SMS_TEMPLATE_CONFIG, templateConfig);
    responseList.add(result);
    response.getResult().put(JsonKey.RESPONSE, responseList);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getAllRecords(
            JsonKey.SUNBIRD, JsonKey.SYSTEM_SETTINGS_DB, new RequestContext()))
        .thenReturn(response);
  }

  @Test
  public void testDataSettings() {
    List<Map<String, String>> settings = DataCacheHandler.getSmsTemplateConfigList();
    Assert.assertNotNull(settings);
  }
}
