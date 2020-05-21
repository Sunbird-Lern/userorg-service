/** */
package org.sunbird.common.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.services.ProfileCompletenessService;

/** @author Manzarul */
public class ProfileCompletenessServiceImpl implements ProfileCompletenessService {

  @Override
  public Map<String, Object> computeProfile(Map<String, Object> profileData) {
    Map<String, Object> response = new HashMap<>();
    float completedCount = 0;
    if (profileData == null || profileData.size() == 0) {
      response.put(JsonKey.COMPLETENESS, (int) Math.ceil(completedCount));
      response.put(JsonKey.MISSING_FIELDS, findMissingAttribute(profileData));
      return response;
    }
    Iterator<Entry<String, Object>> itr = profileData.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, Object> entry = itr.next();
      Object value = entry.getValue();
      if (value instanceof List) {
        List list = (List) value;
        if (list.size() > 0) {
          completedCount = completedCount + getValue(entry.getKey());
        }
      } else if (value instanceof Map) {
        Map map = (Map) value;
        if (map != null && map.size() > 0) {
          completedCount = completedCount + getValue(entry.getKey());
        }
      } else {
        if (value != null && !StringUtils.isBlank(value.toString())) {
          completedCount = completedCount + getValue(entry.getKey());
        }
      }
    }
    response.put(JsonKey.COMPLETENESS, (int) Math.ceil(completedCount));
    response.put(JsonKey.MISSING_FIELDS, findMissingAttribute(profileData));
    return response;
  }

  /**
   * This method will provide weighted value for particular attribute
   *
   * @param key String
   * @return float
   */
  private float getValue(String key) {
    return PropertiesCache.getInstance().attributePercentageMap.get(key) != null
        ? PropertiesCache.getInstance().attributePercentageMap.get(key)
        : 0;
  }

  /**
   * This method will provide all the missing filed list
   *
   * @param profileData Map<String, Object>
   * @return List<String>
   */
  private List<String> findMissingAttribute(Map<String, Object> profileData) {
    List<String> attribute = new ArrayList<>();
    Iterator<Entry<String, Float>> itr =
        PropertiesCache.getInstance().attributePercentageMap.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, Float> entry = itr.next();
      if (profileData == null || !profileData.containsKey(entry.getKey())) {
        attribute.add(entry.getKey());
      } else {
        Object val = profileData.get(entry.getKey());
        if (val == null) {
          attribute.add(entry.getKey());
        } else if (val instanceof List) {
          List list = (List) val;
          if (list.size() == 0) {
            attribute.add(entry.getKey());
          }
        } else if (val instanceof Map) {
          Map map = (Map) val;
          if (map == null || map.size() == 0) {
            attribute.add(entry.getKey());
          }
        } else {
          if (StringUtils.isBlank(val.toString())) {
            attribute.add(entry.getKey());
          }
        }
      }
    }
    return attribute;
  }
}
