package org.sunbird.util.search;

import org.apache.pekko.util.Timeout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.dto.SearchDTO;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.telemetry.util.TelemetryWriter;
import org.sunbird.util.PropertiesCache;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class SearchTelemetryGenerator extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase("generateSearchTelemetry")) {
      generateTelemetry(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void generateTelemetry(Request request) {
    Future<Response> response = (Future<Response>) request.getRequest().get("searchFResponse");
    String indexType = (String) request.getRequest().get("indexType");
    SearchDTO searchDto = (SearchDTO) request.getRequest().get("searchDto");
    Map<String, Object> telemetryContext =
        (Map<String, Object>) request.getRequest().get("context");
    Response orgSearchResponse = null;
    try {
      Timeout timeout = new Timeout(30, TimeUnit.SECONDS);
      orgSearchResponse = Await.result(response, timeout.duration());
      String[] types = new String[] {indexType};
      Map<String, Object> contentMap = new HashMap<>();
      List<Object> contentList = new ArrayList<>();
      if (orgSearchResponse != null
          && MapUtils.isNotEmpty(orgSearchResponse.getResult())
          && MapUtils.isNotEmpty(
              (Map<String, Object>) orgSearchResponse.getResult().get(JsonKey.RESPONSE))) {
        HashMap<String, Object> contentListMap =
            (HashMap<String, Object>) orgSearchResponse.getResult().get(JsonKey.RESPONSE);
        contentList.add(contentListMap.get(JsonKey.CONTENT));
        if (CollectionUtils.isNotEmpty(contentList)) {
          contentMap.put(JsonKey.CONTENT, contentList.get(0));
          contentMap.put(
              JsonKey.COUNT,
              contentListMap.get(JsonKey.COUNT) != null ? contentListMap.get(JsonKey.COUNT) : 0);
          generateSearchTelemetryEvent(searchDto, types, contentMap, telemetryContext);
        }
      }
    } catch (Exception e) {
      logger.error(
          "SearchTelemetryGenerator:generateTelemetry: Error occured in generating Telemetry for orgSearch  ",
          e);
    }
  }

  private void generateSearchTelemetryEvent(
      SearchDTO searchDto,
      String[] types,
      Map<String, Object> result,
      Map<String, Object> telemetryContext) {

    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.TYPE, String.join(",", types));
    params.put(JsonKey.QUERY, searchDto.getQuery());
    params.put(JsonKey.FILTERS, searchDto.getAdditionalProperties().get(JsonKey.FILTERS));
    params.put(JsonKey.SORT, searchDto.getSortBy());
    params.put(JsonKey.SIZE, result.get(JsonKey.COUNT));
    params.put(JsonKey.TOPN, generateTopnResult(result)); // need to get topn value from
    // response
    Request req = new Request();
    req.setRequest(telemetryRequestForSearch(telemetryContext, params));
    TelemetryWriter.write(req);
  }

  private List<Map<String, Object>> generateTopnResult(Map<String, Object> result) {

    List<Map<String, Object>> userMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Integer topN =
        Integer.parseInt(PropertiesCache.getInstance().getProperty(JsonKey.SEARCH_TOP_N));

    List<Map<String, Object>> list = new ArrayList<>();
    if (topN < userMapList.size()) {
      for (int i = 0; i < topN; i++) {
        Map<String, Object> m = new HashMap<>();
        m.put(JsonKey.ID, userMapList.get(i).get(JsonKey.ID));
        list.add(m);
      }
    } else {

      for (int i = 0; i < userMapList.size(); i++) {
        Map<String, Object> m = new HashMap<>();
        m.put(JsonKey.ID, userMapList.get(i).get(JsonKey.ID));
        list.add(m);
      }
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
