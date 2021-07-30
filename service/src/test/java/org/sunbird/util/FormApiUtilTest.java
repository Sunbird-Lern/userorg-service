package org.sunbird.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.keys.JsonKey;

public class FormApiUtilTest {

  @Test
  public void testGetLocationTypeConfigMap() {
    List<String> locationType = FormApiUtil.getLocationTypeConfigMap(getFormApiConfig());
    Assert.assertTrue(locationType.contains(JsonKey.LOCATION_TYPE_SCHOOL));
  }

  public Map<String, Object> getFormApiConfig() {
    Map<String, Object> formData = new HashMap<>();
    Map<String, Object> formMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    List<Map<String, Object>> fieldsList = new ArrayList<>();
    Map<String, Object> field = new HashMap<>();

    Map<String, Object> children = new HashMap<>();
    List<Map<String, Object>> userTypeConfigList = new ArrayList<>();
    Map<String, Object> schoolConfig = new HashMap<>();
    schoolConfig.put(JsonKey.CODE, JsonKey.LOCATION_TYPE_SCHOOL);
    userTypeConfigList.add(schoolConfig);
    children.put("teacher", userTypeConfigList);
    field.put(JsonKey.CODE, JsonKey.PERSONA);
    field.put(JsonKey.CHILDREN, children);
    fieldsList.add(field);
    dataMap.put(JsonKey.FIELDS, fieldsList);
    formMap.put(JsonKey.DATA, dataMap);
    formData.put(JsonKey.FORM, formMap);
    return formData;
  }
}
