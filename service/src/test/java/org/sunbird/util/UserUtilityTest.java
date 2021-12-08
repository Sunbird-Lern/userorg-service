package org.sunbird.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserUtilityTest {

  @Test
  public void encryptSpecificUserDataSuccess() {
    String email = "test@test.com";
    String userName = "test_user";
    String city = "Bangalore";
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.FIRST_NAME, "test user");
    userMap.put(JsonKey.EMAIL, email);
    userMap.put(JsonKey.USER_NAME, userName);
    userMap.put(JsonKey.PROFILE_USERTYPE, "teacher");
    userMap.put(JsonKey.PROFILE_LOCATION, "location");
    userMap.put(JsonKey.USER_TYPE, "userType");
    userMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    userMap.put(JsonKey.LOCATION_ID, "locationID");
    userMap.put(JsonKey.LOCATION_TYPE, "type");
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserData(userMap);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(userMap.get(JsonKey.FIRST_NAME), response.get(JsonKey.FIRST_NAME));
    assertNotEquals(email, response.get(JsonKey.EMAIL));
  }

  @Test
  public void encryptUserSearchFilterQueryDataUserType() {
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(JsonKey.USER_TYPE, "userType");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(JsonKey.FILTERS, filterMap);
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserSearchFilterQueryData(map);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(filterMap.get(JsonKey.USER_TYPE), response.get(JsonKey.USER_TYPE));
  }

  @Test
  public void encryptUserSearchFilterQueryDataUserSubType() {
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(JsonKey.FILTERS, filterMap);
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserSearchFilterQueryData(map);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(filterMap.get(JsonKey.USER_SUB_TYPE), response.get(JsonKey.USER_SUB_TYPE));
  }

  @Test
  public void encryptUserSearchFilterQueryDataLocationID() {
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(JsonKey.LOCATION_ID, "locationID");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(JsonKey.FILTERS, filterMap);
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserSearchFilterQueryData(map);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(filterMap.get(JsonKey.LOCATION_ID), response.get(JsonKey.LOCATION_ID));
  }

  @Test
  public void encryptUserSearchFilterQueryDataLocationType() {
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(JsonKey.LOCATION_TYPE, "type");
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(JsonKey.FILTERS, filterMap);
    Map<String, Object> response = null;
    try {
      response = UserUtility.encryptUserSearchFilterQueryData(map);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(filterMap.get(JsonKey.LOCATION_TYPE), response.get(JsonKey.LOCATION_TYPE));
  }

  @Test
  public void decryptUserDataSuccess() {
    String email = "test@test.com";
    String userName = "test_user";
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.FIRST_NAME, "test user");
    userMap.put(JsonKey.EMAIL, email);
    userMap.put(JsonKey.USER_NAME, userName);
    userMap.put(JsonKey.PROFILE_USERTYPE, "teacher");
    userMap.put(JsonKey.PROFILE_LOCATION, "location");
    userMap.put(JsonKey.USER_TYPE, "userType");
    userMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    userMap.put(JsonKey.LOCATION_ID, "locationID");
    userMap.put(JsonKey.LOCATION_TYPE, "type");
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
