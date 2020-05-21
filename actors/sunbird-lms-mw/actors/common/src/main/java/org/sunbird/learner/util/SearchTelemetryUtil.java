package org.sunbird.learner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.dto.SearchDTO;
import org.sunbird.telemetry.util.TelemetryLmaxWriter;
import org.sunbird.telemetry.util.TelemetryUtil;

public class SearchTelemetryUtil {
  private SearchTelemetryUtil() {}

  public static void generateSearchTelemetryEvent(
      SearchDTO searchDto, String[] types, Map<String, Object> result) {
    try {
      Map<String, Object> telemetryContext = TelemetryUtil.getTelemetryContext();
      Map<String, Object> params = new HashMap<>();
      params.put(JsonKey.QUERY, searchDto.getQuery());
      params.put(JsonKey.FILTERS, searchDto.getAdditionalProperties().get(JsonKey.FILTERS));
      params.put(JsonKey.SORT, searchDto.getSortBy());
      params.put(JsonKey.TOPN, generateTopNResult(result));
      params.put(JsonKey.SIZE, result.get(JsonKey.COUNT));
      params.put(JsonKey.TYPE, String.join(",", types));

      Request request = new Request();
      request.setRequest(telemetryRequestForSearch(telemetryContext, params));
      TelemetryLmaxWriter.getInstance().submitMessage(request);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
  }

  private static List<Map<String, Object>> generateTopNResult(Map<String, Object> result) {
    List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Integer topN =
        Integer.parseInt(PropertiesCache.getInstance().getProperty(JsonKey.SEARCH_TOP_N));
    int count = Math.min(topN, dataMapList.size());
    List<Map<String, Object>> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> m = new HashMap<>();
      m.put(JsonKey.ID, dataMapList.get(i).get(JsonKey.ID));
      list.add(m);
    }
    return list;
  }

  private static Map<String, Object> telemetryRequestForSearch(
      Map<String, Object> telemetryContext, Map<String, Object> params) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTEXT, telemetryContext);
    map.put(JsonKey.PARAMS, params);
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, "SEARCH");
    return map;
  }
}
