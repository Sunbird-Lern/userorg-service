package org.sunbird.service.tenantpreference;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
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
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.dao.tenantpreference.impl.TenantPreferenceDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class, TenantPreferenceDaoImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class TenantPreferenceServiceTest {

  private CassandraOperation cassandraOperation = null;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(cassandraGetRecordByProperty());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(createCassandraInsertSuccessResponse());
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Test(expected = ProjectCommonException.class)
  public void validateAndGetTenantPreferencesByIdForCreateFailureTest() {
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    preferenceService.validateAndGetTenantPreferencesById(
        "45456464682", "someKey", JsonKey.CREATE, new RequestContext());
  }

  @Test
  public void validateAndGetTenantPreferencesByIdByUpdateFailureTest() {
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Map<String, Object> preference =
        preferenceService.validateAndGetTenantPreferencesById(
            "45456464682", "someKey", JsonKey.UPDATE, new RequestContext());
    Assert.assertTrue(MapUtils.isNotEmpty(preference));
  }

  @Test
  public void createPreferenceSuccess() {
    Map<String, Object> data = new HashMap<>();
    data.put("someKey", "key");
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Response response =
        preferenceService.createPreference(
            "87986546549", "someKey", data, "1245-4654-8454", new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void updatePreferenceFailure() {
    Map<String, Object> data = new HashMap<>();
    data.put("someKey", "key");
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Response response =
        preferenceService.updatePreference(
            "87986546549", "someKey", data, "1245-4654-8454", new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void validateDataSecurityPolicySuccess() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPref());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PLAIN_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    boolean validationResult =
        preferenceService.validateTenantDataSecurityPolicy(
            "45456464682", "dataSecurityPolicy", inputData, new RequestContext());
    Assert.assertTrue(validationResult);
  }

  @Test
  public void validateDataSecurityPolicyConfigSuccess() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPref());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PLAIN_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    boolean validationResult = preferenceService.validateDataSecurityPolicyConfig(inputData);
    Assert.assertTrue(validationResult);
  }

  @Test
  public void validateGetDataSecurityPolicyPrefSuccess() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPref());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Map<String, Object> tenantPref =
        preferenceService.getDataSecurityPolicyPref(
            "34234234", JsonKey.DATA_SECURITY_POLICY, new RequestContext());
    Assert.assertTrue(!tenantPref.isEmpty());
  }

  @Test
  public void validateGetDataSecurityPolicyPrefException() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(null);
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    try {
      preferenceService.getDataSecurityPolicyPref(
          "34234234", JsonKey.DATA_SECURITY_POLICY, new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.ERROR_PARAM_EXISTS));
    }
  }

  @Test
  public void validateTenantDataSecurityPolicyException1() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(null);
    TenantPreferenceService preferenceService = new TenantPreferenceService();

    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "34234234", JsonKey.DATA_SECURITY_POLICY, getInputData(), new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.RESOURCE_NOT_FOUND));
    }
  }

  @Test
  public void validateTenantDataSecurityPolicyException2() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPrefPASSWORD_PROTECTED_DATASET());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PLAIN_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "34234234", JsonKey.DATA_SECURITY_POLICY, inputData, new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(
          pe.getErrorCode()
              .equalsIgnoreCase(ResponseMessage.Key.INVALID_TENANT_SECURITY_LEVEL_LOWER));
    }
  }

  @Test
  public void validateTenantDataSecurityPolicyException3() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPrefPASSWORD_PROTECTED_DATASET());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "34234234", JsonKey.DATA_SECURITY_POLICY, inputData, new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(
          pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.INVALID_REQUESTED_DATA));
    }
  }

  @Test
  public void validateTenantDataSecurityPolicyException4() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPrefPASSWORD_PROTECTED_DATASET());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PLAIN_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "34234234", JsonKey.DATA_SECURITY_POLICY, inputData, new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(
          pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.INVALID_SECURITY_LEVEL_LOWER));
    }
  }

  @Test
  public void validateTenantDataSecurityPolicyException5() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(missingSystemSecurityLevelPref());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "34234234", JsonKey.DATA_SECURITY_POLICY, inputData, new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(
          pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.MISSING_DEFAULT_SECURITY_LEVEL));
    }
  }

  @Test
  public void validateDataSecurityPolicyException() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPref());
    TenantPreferenceService preferenceService = new TenantPreferenceService();

    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "45456464682", "dataSecurityPolicy", getInputData(), new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(
          pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.INVALID_SECURITY_LEVEL));
    }
  }

  @Test
  public void validateDataSecurityPolicyPEException() {
    PowerMockito.mockStatic(TenantPreferenceDaoImpl.class);
    TenantPreferenceDao daoService = mock(TenantPreferenceDaoImpl.class);
    when(TenantPreferenceDaoImpl.getInstance()).thenReturn(daoService);
    when(daoService.getTenantPreferenceById(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(defaultDataSecurityPref());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"L0\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"TEXT_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }

    try {
      preferenceService.validateTenantDataSecurityPolicy(
          "45456464682", "dataSecurityPolicy", inputData, new RequestContext());
    } catch (ProjectCommonException pe) {
      Assert.assertTrue(
          pe.getErrorCode().equalsIgnoreCase(ResponseMessage.Key.INVALID_SECURITY_LEVEL));
    }
  }

  private static Response cassandraGetRecordByProperty() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    map.put(JsonKey.ORG_ID, "45456464682");
    map.put(
        JsonKey.DATA,
        "{\"default\":{\"action\":\"volunteer\",\"templateName\":\"volunteer\",\"fields\":[[{\"title\":\"Please confirm that ALL the following items are verified (by ticking the check-boxes) before you can publish:\",\"contents\":[{\"name\":\"Appropriateness\",\"checkList\":[\"No Hate speech, Abuse, Violence, Profanity\",\"No Discrimination or Defamation\",\"Is suitable for children\"]}]}]]}}");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response cassandraGetRecordByPropertiesEmptyResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static List<Map<String, Object>> defaultDataSecurityPref() {
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    map.put(JsonKey.ORG_ID, "default");
    map.put(
        JsonKey.DATA,
        "{\n"
            + "           \"level\": \"PLAIN_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PLAIN_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }");
    list.add(map);

    return list;
  }

  private static List<Map<String, Object>> defaultDataSecurityPrefPASSWORD_PROTECTED_DATASET() {
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    map.put(JsonKey.ORG_ID, "default");
    map.put(
        JsonKey.DATA,
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"PUBLIC_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }");
    list.add(map);

    return list;
  }

  private static List<Map<String, Object>> missingSystemSecurityLevelPref() {
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    map.put(JsonKey.ORG_ID, "default");
    map.put(
        JsonKey.DATA,
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }");
    list.add(map);

    return list;
  }

  private Map<String, Object> getInputData() {
    ObjectMapper mapper = new ObjectMapper();
    String inputDataStr =
        "{\n"
            + "           \"level\": \"PASSWORD_PROTECTED_DATASET\",\n"
            + "           \"dataEncrypted\": \"No\",\n"
            + "           \"comments\": \"Data is not encrypted\",\n"
            + "            \"job\": {\n"
            + "                    \"admin-geo-reports\": {\n"
            + "                        \"level\": \"L0\",\n"
            + "                        \"dataEncrypted\": \"No\",\n"
            + "                        \"comments\": \"Unprotected file.\"\n"
            + "                    },\n"
            + "                    \"userinfo-exhaust\": {\n"
            + "                        \"level\": \"TEXT_KEY_ENCRYPTED_DATASET\",\n"
            + "                        \"dataEncrypted\": \"Yes\",\n"
            + "                        \"comments\": \"Decryption tool link need to be downloaded to decrypt the encrypted file.\"\n"
            + "                    }\n"
            + "                },\n"
            + "            \"securityLevels\": {\n"
            + "                \"PLAIN_DATASET\": \"Data is present in plain text/zip. Generally applicable to open datasets.\",\n"
            + "                \"PASSWORD_PROTECTED_DATASET\": \"Password protected zip file. Generally applicable to non PII data sets but can contain sensitive information which may not be considered open.\",\n"
            + "                \"TEXT_KEY_ENCRYPTED_DATASET\": \"Data encrypted with a user provided encryption key. Generally applicable to non PII data but can contain sensitive information which may not be considered open.\",\n"
            + "                \"PUBLIC_KEY_ENCRYPTED_DATASET\": \"Data encrypted via an org provided public/private key. Generally applicable to all PII data exhaust.\"\n"
            + "            }\n"
            + "        }";

    Map<String, Object> inputData = null;
    try {
      inputData = mapper.readValue(inputDataStr, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return inputData;
  }
}
