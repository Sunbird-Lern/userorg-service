package org.sunbird.notification.utils;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;

/**
 * Tests for the {@link SmsTemplateUtil} class to ensure SMS template configurations are fetched and
 * parsed correctly from the database.
 */
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
public class SmsTemplateUtilTest {

  /** Initializes mock data for SMS template configurations in Cassandra. */
  public void before() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    String templateConfig =
        "{\"OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\":\"1\",\"OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\":\"2\",\"Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.\":\"3\",\"Welcome to $instanceName. Your user account has now been created. Click on the link below to  set a password  and start using your account: $link\":\"4\",\"You can now access your diksha state teacher account using $phone. Please log out and login once again to see updated details.\":\"5\",\"VidyaDaan: Your nomination for $content has not been accepted. Thank you for your interest. Please login to https:\\/\\/vdn.diksha.gov.in for details.\":\"6\",\"VidyaDaan: Your nomination for $content is accepted. Please login to https:\\/\\/vdn.diksha.gov.in to start contributing content.\":\"7\",\"VidyaDaan: Your Content $content has not been approved by the project owner. Please login to https:\\/\\/vdn.diksha.gov.in for details.\":\"8\",\"VidyaDaan: Your Content $content has been approved by the project owner.\":\"9\"}";
    Response response = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.FIELD, JsonKey.SMS_TEMPLATE_CONFIG);
    result.put(JsonKey.VALUE, templateConfig);
    responseList.add(result);
    response.getResult().put(JsonKey.RESPONSE, responseList);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
  }

  /**
   * Tests the retrieval and parsing of SMS template configuration mappings from the database.
   */
  @Test
  public void testDataSettings() {
    before();
    Map<String, Map<String, String>> settings = SmsTemplateUtil.getSmsTemplateConfigMap();
    Assert.assertNotNull(settings);
    Assert.assertTrue(MapUtils.isNotEmpty(settings));
  }
}
