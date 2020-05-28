/** */
package org.sunbird.service.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.services.ProfileCompletenessService;
import org.sunbird.common.services.impl.ProfileCompletenessFactory;

/**
 * This test class have the assumption that each profile attribute have the same weighted.
 * for more details look at profilecompleteness.properties.
 *
 * @author Manzarul
 */
public class ProfileCompletenessTest {

  private ProfileCompletenessService service = ProfileCompletenessFactory.getInstance();

  @Test
  public void allCompleteProfilePercentageTest() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.FIRST_NAME, "test");
    requestMap.put(JsonKey.LAST_NAME, "dsj");
    requestMap.put(JsonKey.EMAIL, "test@test.com");
    requestMap.put(JsonKey.PHONE, "3455556656");
    requestMap.put(JsonKey.PROFILE_SUMMARY, "profile is completed");
    requestMap.put(JsonKey.SUBJECT, "Math,Physics");
    requestMap.put(JsonKey.LANGUAGE, "Hindi");
    requestMap.put(JsonKey.DOB, "1995-08-09");
    requestMap.put("avatar", "some img url");
    requestMap.put(JsonKey.GRADE, "5th,6th,7th");
    requestMap.put(JsonKey.GENDER, "MALE");
    requestMap.put(JsonKey.LOCATION, "hdsvdjdjsfkf");
    requestMap.put(JsonKey.USERNAME, "test@test");
    Map<String, Object> address = new HashMap<>();
    address.put(JsonKey.CITY, "Bangalore");
    address.put(JsonKey.STATE, "sdkjdfjks");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(address);
    requestMap.put(JsonKey.ADDRESS, list);
    Map<String, Object> edu = new HashMap<>();
    edu.put(JsonKey.COURSE, "M.C.A");
    edu.put(JsonKey.PERCENTAGE, 98);
    List<Map<String, Object>> eduList = new ArrayList<>();
    eduList.add(edu);
    requestMap.put(JsonKey.EDUCATION, eduList);
    Map<String, Object> job = new HashMap<>();
    job.put(JsonKey.JOB_NAME, "teacher");
    List<Map<String, Object>> jobList = new ArrayList<>();
    jobList.add(job);
    requestMap.put(JsonKey.JOB_PROFILE, jobList);
    Map<String, Object> response = service.computeProfile(requestMap);
    int val = (int) response.get(JsonKey.COMPLETENESS);
    if(val>100) {val =100;}
    Assert.assertEquals(100, val);
  }

  @Test
  public void allCompleteProfileErrorFieldTest() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.FIRST_NAME, "test");
    requestMap.put(JsonKey.LAST_NAME, "dsj");
    requestMap.put(JsonKey.EMAIL, "test@test.com");
    requestMap.put(JsonKey.PHONE, "3455556656");
    requestMap.put(JsonKey.PROFILE_SUMMARY, "profile is completed");
    requestMap.put(JsonKey.SUBJECT, "Math,Physics");
    requestMap.put(JsonKey.LANGUAGE, "Hindi");
    requestMap.put(JsonKey.DOB, "1995-08-09");
    requestMap.put("avatar", "some img url");
    requestMap.put(JsonKey.GRADE, "5th,6th,7th");
    requestMap.put(JsonKey.GENDER, "MALE");
    requestMap.put(JsonKey.LOCATION, "hdsvdjdjsfkf");
    requestMap.put(JsonKey.USERNAME, "test@test");
    Map<String, Object> address = new HashMap<>();
    address.put(JsonKey.CITY, "Bangalore");
    address.put(JsonKey.STATE, "sdkjdfjks");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(address);
    requestMap.put(JsonKey.ADDRESS, list);
    Map<String, Object> edu = new HashMap<>();
    edu.put(JsonKey.COURSE, "M.C.A");
    edu.put(JsonKey.PERCENTAGE, 98);
    List<Map<String, Object>> eduList = new ArrayList<>();
    eduList.add(edu);
    requestMap.put(JsonKey.EDUCATION, eduList);
    Map<String, Object> job = new HashMap<>();
    job.put(JsonKey.JOB_NAME, "teacher");
    List<Map<String, Object>> jobList = new ArrayList<>();
    jobList.add(job);
    requestMap.put(JsonKey.JOB_PROFILE, jobList);
    Map<String, Object> response = service.computeProfile(requestMap);
    List<String> val = (List) response.get(JsonKey.MISSING_FIELDS);
    Assert.assertEquals(0, val.size());
  }

  @Test
  public void zeroPercentageTest() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> response = service.computeProfile(requestMap);
    int val =
        (int) (response.get(JsonKey.COMPLETENESS) != null ? response.get(JsonKey.COMPLETENESS) : 0);
    Assert.assertEquals(0, val);
  }

  @Test
  public void zeroPercentageErrorTest() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> response = service.computeProfile(requestMap);
    List<String> val = (List) response.get(JsonKey.MISSING_FIELDS);
    Assert.assertEquals(14, val.size());
  }

  @Test
  public void basicProfilePercentageTest() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.FIRST_NAME, "test");
    requestMap.put(JsonKey.LAST_NAME, "dsj");
    requestMap.put(JsonKey.EMAIL, "test@test.com");
    requestMap.put(JsonKey.PHONE, "3455556656");
    requestMap.put(JsonKey.PROFILE_SUMMARY, "profile is completed");
    requestMap.put(JsonKey.SUBJECT, "Math,Physics");
    requestMap.put(JsonKey.LANGUAGE, "Hindi");
    requestMap.put(JsonKey.DOB, "1995-08-09");
    requestMap.put("avatar", "some img url");
    requestMap.put(JsonKey.GRADE, "5th,6th,7th");
    requestMap.put(JsonKey.GENDER, "MALE");
    requestMap.put(JsonKey.LOCATION, "hdsvdjdjsfkf");
    requestMap.put(JsonKey.USERNAME, "test@test");
    Map<String, Object> response = service.computeProfile(requestMap);
    int val = (int) response.get(JsonKey.COMPLETENESS);
    Assert.assertEquals(79, val);
    requestMap.remove("avatar");
    response = service.computeProfile(requestMap);
    val = (int) response.get(JsonKey.COMPLETENESS);
    Assert.assertEquals(72, val);
    requestMap.put("avatar", "some value");
    response = service.computeProfile(requestMap);
    val = (int) response.get(JsonKey.COMPLETENESS);
    Assert.assertEquals(79, val);
    Map<String, Object> address = new HashMap<>();
    address.put(JsonKey.CITY, "Bangalore");
    address.put(JsonKey.STATE, "sdkjdfjks");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(address);
    requestMap.put(JsonKey.ADDRESS, list);
    response = service.computeProfile(requestMap);
    val = (int) response.get(JsonKey.COMPLETENESS);
    Assert.assertEquals(86, val);
  }

  @Test
  public void profileCompletenessWithNullAttribute() {
    Map<String, Object> requestMap = null;
    Map<String, Object> response = service.computeProfile(requestMap);
    int val =
        (int) (response.get(JsonKey.COMPLETENESS) != null ? response.get(JsonKey.COMPLETENESS) : 0);
    Assert.assertEquals(0, val);
  }

  @Test
  public void profileCompletenessWithList() {
    Map<String, Object> requestMap = new HashMap<>();
    List<String> attribute = new ArrayList<>();
    attribute.add("pro");
    requestMap.put("list", attribute);
    Map<String, Object> response = service.computeProfile(requestMap);
    int val =
        (int) (response.get(JsonKey.COMPLETENESS) != null ? response.get(JsonKey.COMPLETENESS) : 0);
    Assert.assertEquals(0, val);
  }

  @Test
  public void profileCompletenessWithMap() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> attribute = new HashMap<>();
    attribute.put("pro", "test");
    requestMap.put("list", attribute);
    Map<String, Object> response = service.computeProfile(requestMap);
    int val =
        (int) (response.get(JsonKey.COMPLETENESS) != null ? response.get(JsonKey.COMPLETENESS) : 0);
    Assert.assertEquals(0, val);
  }
}
