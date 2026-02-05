package org.sunbird.util.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

public class FuzzySearchManager {
  private static final LoggerUtil logger = new LoggerUtil(FuzzySearchManager.class);

  private Map<String, Object> fuzzySearchMap;
  private List<Map<String, Object>> searchMap;

  private FuzzySearchManager(
      Map<String, Object> fuzzySearchMap, List<Map<String, Object>> searchMap) {
    this.fuzzySearchMap = fuzzySearchMap;
    this.searchMap = searchMap;
  }

  public static FuzzySearchManager getInstance(
      Map<String, Object> fuzzySearchMap, List<Map<String, Object>> searchMap) {
    return new FuzzySearchManager(fuzzySearchMap, searchMap);
  }

  public List<Map<String, Object>> startFuzzySearch() {
    HashSet<String> resultSet = new HashSet<>();
    fuzzySearchMap
        .entrySet()
        .forEach(
            map -> {
              validateKeyInFuzzyMap(map.getKey());
              String[] splittedName = map.getValue().toString().split(" ");
              for (int i = 0; i < splittedName.length; i++) {
                resultSet.addAll(
                    FuzzyMatcher.matchDoc(
                        splittedName[i].trim(), getFuzzyAttributeFromMap(map.getKey())));
              }
            });
    logger.info(
        String.format(
            "%s:%s:the size of resultSet i.e number of searches found is %s",
            this.getClass().getSimpleName(), "startFuzzySearch", resultSet.size()));
    return prepareResponseList(resultSet);
  }

  private void validateKeyInFuzzyMap(String key) {
    Map<String, Object> resultMap = searchMap.get(0);
    if (!resultMap.keySet().contains(key)) {
      logger.info(
          String.format(
              "%s:%s:key not found in searchMap %s",
              this.getClass().getSimpleName(), "getFuzzyAttributeFromMap", key));
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
    }
  }

  private Map<String, String> getFuzzyAttributeFromMap(String key) {
    Map<String, String> attributesValueMap = new HashMap<>();
    searchMap
        .stream()
        .forEach(
            result -> {
              Map<String, Object> resultMap = (Map<String, Object>) result;
              attributesValueMap.put(
                  (String) resultMap.get(JsonKey.ID), (String) resultMap.get(key));
            });
    logger.info(
        String.format(
            "%s:%s:the prepared Map for fuzzy search  %s",
            this.getClass().getSimpleName(),
            "getFuzzyAttributeFromMap",
            Collections.singleton(attributesValueMap.toString())));
    return attributesValueMap;
  }

  private List<Map<String, Object>> fetchResultSetFromSearchMap(HashSet<String> fuzzyResultSet) {

    List<Map<String, Object>> responseList = new ArrayList<>();

    searchMap
        .stream()
        .forEach(
            result -> {
              Map<String, Object> resultMap = (Map<String, Object>) result;
              if (fuzzyResultSet.contains(resultMap.get(JsonKey.ID))) {
                responseList.add(resultMap);
              }
            });
    logger.info(
        String.format(
            "%s:%s:returning only fuzzy matched result, the size of List of map is   %s",
            this.getClass().getSimpleName(), "fetchResultSetFromSearchMap", responseList.size()));
    return responseList;
  }

  private List<Map<String, Object>> prepareResponseList(HashSet<String> fuzzyResultSet) {
    return fuzzyResultSet.size() != 0
        ? fetchResultSetFromSearchMap(fuzzyResultSet)
        : new ArrayList<>();
  }
}
