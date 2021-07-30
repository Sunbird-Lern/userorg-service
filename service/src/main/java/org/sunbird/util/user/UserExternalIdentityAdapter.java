package org.sunbird.util.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.keys.JsonKey;

public class UserExternalIdentityAdapter {

  public static List<Map<String, String>> convertSelfDeclareFieldsToExternalIds(
      Map<String, Object> selfDeclaredFields) {
    List<Map<String, String>> externalIds = new ArrayList<>();
    if (MapUtils.isNotEmpty(selfDeclaredFields)) {
      Map<String, String> userInfo =
          (Map<String, String>) selfDeclaredFields.get(JsonKey.USER_INFO);
      if (MapUtils.isNotEmpty(userInfo)) {
        for (Map.Entry<String, String> itr : userInfo.entrySet()) {
          Map<String, String> externalIdMap = new HashMap<>();
          externalIdMap.put(JsonKey.USER_ID, (String) selfDeclaredFields.get(JsonKey.USER_ID));
          externalIdMap.put(JsonKey.ID_TYPE, itr.getKey());
          externalIdMap.put(JsonKey.ORIGINAL_ID_TYPE, itr.getKey());
          externalIdMap.put(JsonKey.PROVIDER, (String) selfDeclaredFields.get(JsonKey.ORG_ID));
          externalIdMap.put(
              JsonKey.ORIGINAL_PROVIDER, (String) selfDeclaredFields.get(JsonKey.ORG_ID));
          externalIdMap.put(JsonKey.EXTERNAL_ID, itr.getValue());
          externalIdMap.put(JsonKey.ORIGINAL_EXTERNAL_ID, itr.getValue());
          externalIds.add(externalIdMap);
        }
      }
    }
    return externalIds;
  }

  public static Map<String, Object> convertExternalFieldsToSelfDeclareFields(
      List<Map<String, String>> externalIds) {
    Map<String, Object> selfDeclaredField = new HashMap<>();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      Map<String, String> userInfo = new HashMap<>();
      for (Map<String, String> extIdMap : externalIds) {
        userInfo.put(
            extIdMap.get(JsonKey.ORIGINAL_ID_TYPE), extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID));
      }
      selfDeclaredField.put(JsonKey.USER_ID, externalIds.get(0).get(JsonKey.USER_ID));
      selfDeclaredField.put(JsonKey.ORG_ID, externalIds.get(0).get(JsonKey.ORIGINAL_PROVIDER));
      selfDeclaredField.put(JsonKey.PERSONA, JsonKey.TEACHER_PERSONA);
      selfDeclaredField.put(JsonKey.USER_INFO, userInfo);
    }
    return selfDeclaredField;
  }
}
