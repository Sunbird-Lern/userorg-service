package org.sunbird.learner.util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.security.*;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.ServiceFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DataCacheHandler.class, ServiceFactory.class})
@PowerMockIgnore({"javax.management.*"})
public class UserUtilityTest {

  EncryptionService encryptionService;
  DecryptionService decryptionService;

  public void beforeEachTest() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    encryptionService = mock(EncryptionService.class);
    decryptionService = mock(DecryptionService.class);

    PowerMockito.when(ServiceFactory.getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    PowerMockito.when(ServiceFactory.getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);

    Mockito.when(encryptionService.encryptData(Mockito.anyString()))
        .thenAnswer(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return Base64.getEncoder().encodeToString(((String) args[0]).getBytes());
              }
            });
    Mockito.when(decryptionService.decryptData(Mockito.anyString()))
        .thenAnswer(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return new String(Base64.getDecoder().decode(((String) args[0])));
              }
            });

    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.when(DataCacheHandler.getTenantConfigMap()).thenReturn(getTenantEmptyConfig());
  }

  @Test
  public void encryptSpecificUserDataSuccess() throws Exception {
    beforeEachTest();
    String email = "test@test.com";
    String userName = "test_user";
    String city = "Bangalore";
    String addressLine1 = "xyz";
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.FIRST_NAME, "test user");
    userMap.put(JsonKey.EMAIL, email);
    userMap.put(JsonKey.USER_NAME, userName);
    List<Map<String, Object>> addressList = new ArrayList<Map<String, Object>>();
    Map<String, Object> address = new HashMap<String, Object>();
    address.put(JsonKey.COUNTRY, "India");
    address.put(JsonKey.CITY, city);
    address.put(JsonKey.ADDRESS_LINE1, addressLine1);
    addressList.add(address);
    userMap.put(JsonKey.ADDRESS, addressList);
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserData(userMap);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(userMap.get(JsonKey.FIRST_NAME), response.get(JsonKey.FIRST_NAME));
    assertNotEquals(email, response.get(JsonKey.EMAIL));
    assertNotEquals(
        "India",
        ((List<Map<String, Object>>) response.get(JsonKey.ADDRESS)).get(0).get(JsonKey.COUNTRY));
    assertNotEquals(
        addressLine1,
        ((List<Map<String, Object>>) response.get(JsonKey.ADDRESS))
            .get(0)
            .get(JsonKey.ADDRESS_LINE1));
  }

  @Test
  public void encryptUserAddressDataSuccess() throws Exception {
    beforeEachTest();
    String city = "Bangalore";
    String addressLine1 = "xyz";
    String state = "Karnataka";
    List<Map<String, Object>> addressList = new ArrayList<Map<String, Object>>();
    Map<String, Object> address = new HashMap<String, Object>();
    address.put(JsonKey.COUNTRY, "India");
    address.put(JsonKey.CITY, city);
    address.put(JsonKey.ADDRESS_LINE1, addressLine1);
    address.put(JsonKey.STATE, state);
    addressList.add(address);
    List<Map<String, Object>> response = null;
    try {
      response = UserUtility.encryptUserAddressData(addressList);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertNotEquals("India", response.get(0).get(JsonKey.COUNTRY));
    assertNotEquals(addressLine1, response.get(0).get(JsonKey.ADDRESS_LINE1));
    assertNotEquals(state, response.get(0).get(JsonKey.STATE));
  }

  @Test
  public void decryptUserDataSuccess() throws Exception {
    beforeEachTest();
    String email = "test@test.com";
    String userName = "test_user";
    String city = "Bangalore";
    String addressLine1 = "xyz";
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.FIRST_NAME, "test user");
    userMap.put(JsonKey.EMAIL, email);
    userMap.put(JsonKey.USER_NAME, userName);
    List<Map<String, Object>> addressList = new ArrayList<Map<String, Object>>();
    Map<String, Object> address = new HashMap<String, Object>();
    address.put(JsonKey.COUNTRY, "India");
    address.put(JsonKey.CITY, city);
    address.put(JsonKey.ADDRESS_LINE1, addressLine1);
    addressList.add(address);
    userMap.put(JsonKey.ADDRESS, addressList);
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserData(userMap);
      response = UserUtility.decryptUserData(response);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(userMap.get(JsonKey.FIRST_NAME), response.get(JsonKey.FIRST_NAME));
    assertEquals(email, response.get(JsonKey.EMAIL));
    assertEquals(userName, response.get(JsonKey.USER_NAME));
  }

  @Test
  public void testGetTenantMandatoryFields() throws Exception {
    beforeEachTest();
    List<String> manadatoryFields =
        UserUtility.getTenantMandatoryFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS);
    Assert.assertTrue(manadatoryFields.isEmpty());
  }

  @Test
  public void testGetTenantOptionalFields() throws Exception {
    beforeEachTest();
    List<String> optionalFields =
        UserUtility.getTenantOptionalFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS);
    Assert.assertTrue(optionalFields.isEmpty());
  }

  @Test
  public void testGetTenantAliasFields() throws Exception {
    beforeEachTest();
    Map<String, String> aliasFields =
        UserUtility.getTenantAliasFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS);
    Assert.assertTrue(aliasFields.isEmpty());
  }

  private Map<String, Map<String, Object>> getTenantEmptyConfig() {
    Map<String, Map<String, Object>> tenantConfig = new HashMap<>();
    Map<String, Object> keyConfig = new HashMap<>();
    String data =
        "{\"templateName\":\"defaultTemplate\",\"action\":\"update\",\"private\":[\"declared-phone\",\"declared-email\"],"
            + "\"aliases\":{\"Diksha UUID\":\"userId\",\"Status\":\"input status\",\"State provided ext. ID\":\"userExternalId\",\"Channel\":\"channel\","
            + "\"Org ID\":\"orgId\",\"Persona\":\"persona\",\"Phone number\":\"phone\",\"Email ID\":\"email\",\"School Name\":\"schoolName\","
            + "\"School UDISE ID\":\"schoolUdiseId\",\"Diksha Sub-Org ID\":\"subOrgId\",\"Error Type\":\"errorType\"},\"mandatoryFields\":[\"Diksha UUID\","
            + "\"Status\",\"State provided ext. ID\",\"Channel\",\"Org ID\",\"Persona\"],\"optionalFields\":[\"School Name\",\"School UDISE ID\",\"Email ID\","
            + "\"Phone number\",\"Diksha Sub-Org ID\"\"Error Type\"]}";
    keyConfig.put(JsonKey.SELF_DECLARATIONS, data);
    tenantConfig.put(JsonKey.ALL, keyConfig);
    return tenantConfig;
  }
}
