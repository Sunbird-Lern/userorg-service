package org.sunbird.learner.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;

public class UserUtilityTest {

  @Test
  public void encryptSpecificUserDataSuccess() {
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
  public void encryptUserAddressDataSuccess() {
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
  public void decryptUserDataSuccess() {
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
}
