package org.sunbird.common.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import scala.concurrent.Future;

public interface ElasticSearchService {
  public static final String _DOC = "_doc";

  /**
   * This method will put a new data entry inside Elastic search. identifier value becomes _id
   * inside ES, so every time provide a unique value while saving it.
   *
   * @param index String ES index name
   * @param identifier ES column identifier as an String
   * @param data Map<String,Object>
   * @return String identifier for created data
   */
  public Future<String> save(String index, String identifier, Map<String, Object> data);

  /**
   * This method will update data based on identifier.take the data based on identifier and merge
   * with incoming data then update it.
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  public Future<Boolean> update(String index, String identifier, Map<String, Object> data);

  /**
   * This method will provide data form ES based on incoming identifier. we can get data by passing
   * index and identifier values , or all the three index, identifier and type
   *
   * @param index String
   * @param identifier String
   * @return Map<String,Object> or null
   */
  public Future<Map<String, Object>> getDataByIdentifier(String index, String identifier);

  /**
   * This method will remove data from ES based on identifier.
   *
   * @param index String
   * @param identifier String
   */
  public Future<Boolean> delete(String index, String identifier);

  /**
   * Method to perform the elastic search on the basis of SearchDTO . SearchDTO contains the search
   * criteria like fields, facets, sort by , filters etc. here user can pass single type to search
   * or multiple type or null
   *
   * @param type var arg of String
   * @return search result as Map.
   */
  public Future<Map<String, Object>> search(SearchDTO searchDTO, String index);

  /**
   * This method will do the health check of elastic search.
   *
   * @return boolean
   */
  public Future<Boolean> healthCheck();

  /**
   * This method will do the bulk data insertion.
   *
   * @param index String index name
   * @param dataList List<Map<String, Object>>
   * @return boolean
   */
  public Future<Boolean> bulkInsert(String index, List<Map<String, Object>> dataList);

  /**
   * This method will upsert data based on identifier.take the data based on identifier and merge
   * with incoming data then update it or if not present already will create it.
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  public Future<Boolean> upsert(String index, String identifier, Map<String, Object> data);

  /**
   * @param ids List of ids of document
   * @param fields List of fields which needs to captured
   * @param index elastic search index in which search should be done
   * @return Map<String,Map<String,Object>> It will return a map with id as key and the data from ES
   *     as value
   */
  public Future<Map<String, Map<String, Object>>> getEsResultByListOfIds(
      List<String> organisationIds, List<String> fields, String index);

  /**
   * Method to execute ES raw query with the limitation of size set to 0 Currently, This is a not a
   * tcp call.
   *
   * @param index ES indexName
   * @param rawQuery actual query to be executed
   * @return Response Object from elastic Search
   */
  default Response searchMetricsData(String index, String rawQuery) {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "ElasticSearchTcpImpl:searchMetricsData: "
            + "Metrics search method started at =="
            + startTime,
        LoggerEnum.PERF_LOG);
    String baseUrl = null;
    if (!StringUtils.isBlank(System.getenv(JsonKey.SUNBIRD_ES_IP))) {
      String envHost = System.getenv(JsonKey.SUNBIRD_ES_IP);
      String[] host = envHost.split(",");
      baseUrl =
          "http://"
              + host[0]
              + ":"
              + PropertiesCache.getInstance().getProperty(JsonKey.ES_METRICS_PORT);
    } else {
      ProjectLogger.log("ElasticSearchTcpImpl:searchMetricsData:" + " ES URL from Properties file");
      baseUrl = PropertiesCache.getInstance().getProperty(JsonKey.ES_URL);
    }
    String requestURL = baseUrl + "/" + index + "/" + "_doc" + "/" + "_search";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    Map<String, Object> responseData = new HashMap<>();
    try {
      // TODO:Currently this is making a rest call but needs to be modified to make
      // the call using
      // ElasticSearch client
      String responseStr = HttpUtil.sendPostRequest(requestURL, rawQuery, headers);
      ObjectMapper mapper = new ObjectMapper();
      responseData = mapper.readValue(responseStr, Map.class);
    } catch (IOException e) {
      throw new ProjectCommonException(
          ResponseCode.unableToConnectToES.getErrorCode(),
          ResponseCode.unableToConnectToES.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.unableToParseData.getErrorCode(),
          ResponseCode.unableToParseData.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, responseData);
    ProjectLogger.log(
        "ElasticSearchTcpImpl:searchMetricsData: "
            + "ElasticSearchUtil metrics search method end at == "
            + System.currentTimeMillis()
            + " ,Total time elapsed = "
            + ElasticSearchHelper.calculateEndTime(startTime),
        LoggerEnum.PERF_LOG);
    return response;
  }
}
