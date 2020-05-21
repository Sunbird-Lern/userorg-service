package org.sunbird.common.models.util;

import java.util.Map;

public class MapperUtil {
  public static void put(
      Map<String, Object> inMap, String inKey, Map<String, Object> outMap, String outKey) {
    String[] inputKeys = inKey.split("\\.");
    String lastKey = inputKeys[inputKeys.length - 1];

    Map<String, Object> map = inMap;

    for (int i = 0; i < (inputKeys.length - 1); i++) {
      if (map.containsKey(inputKeys[i])) {
        map = (Map<String, Object>) inMap.get(inputKeys[i]);
      }
    }

    if (map.containsKey(lastKey)) {
      outMap.put(outKey, map.get(lastKey));
    }
  }
}
