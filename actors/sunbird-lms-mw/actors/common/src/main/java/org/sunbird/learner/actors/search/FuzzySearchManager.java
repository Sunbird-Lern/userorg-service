package org.sunbird.learner.actors.search;

import java.util.*;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

public class FuzzySearchManager {
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

  protected List<Map<String, Object>> startFuzzySearch() {
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
    ProjectLogger.log(
        String.format(
            "%s:%s:the size of resultSet i.e number of searches found is %s",
            this.getClass().getSimpleName(), "startFuzzySearch", resultSet.size()),
        LoggerEnum.INFO.name());
    return prepareResponseList(resultSet);
  }

  private void validateKeyInFuzzyMap(String key) {
    Map<String, Object> resultMap = searchMap.get(0);
    if (!resultMap.keySet().contains(key)) {
      ProjectLogger.log(
          String.format(
              "%s:%s:key not found in searchMap %s",
              this.getClass().getSimpleName(), "getFuzzyAttributeFromMap", key),
          LoggerEnum.ERROR.name());
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
    ProjectLogger.log(
        String.format(
            "%s:%s:the prepared Map for fuzzy search  %s",
            this.getClass().getSimpleName(),
            "getFuzzyAttributeFromMap",
            Collections.singleton(attributesValueMap.toString())),
        LoggerEnum.INFO.name());
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
    ProjectLogger.log(
        String.format(
            "%s:%s:returning only fuzzy matched result, the size of List of map is   %s",
            this.getClass().getSimpleName(), "fetchResultSetFromSearchMap", responseList.size()),
        LoggerEnum.INFO.name());
    return responseList;
  }

  private List<Map<String, Object>> prepareResponseList(HashSet<String> fuzzyResultSet) {
    return fuzzyResultSet.size() != 0
        ? fetchResultSetFromSearchMap(fuzzyResultSet)
        : new ArrayList<>();
  }
}
