package org.sunbird.actor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.Util;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;
import scala.concurrent.Future;

/** @author Amit Kumar */
@ActorConfig(
  tasks = {},
  asyncTasks = {"registerChannel"}
)
public class ChannelRegistrationActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase(BackgroundOperations.registerChannel.name())) {
      registerChannel(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void registerChannel(Request request) {
    List<String> ekstepChannelList = getEkstepChannelList(request.getRequestContext());
    List<Map<String, Object>> sunbirdChannelList = null;
    if (null != ekstepChannelList) {
      logger.info(
          request.getRequestContext(),
          "channel list size from ekstep : " + ekstepChannelList.size());
      sunbirdChannelList = getSunbirdChannelList(request.getRequestContext());
      logger.info(
          request.getRequestContext(),
          "channel list size from sunbird : " + sunbirdChannelList.size());
      if (!ekstepChannelList.isEmpty()) {
        processChannelReg(ekstepChannelList, sunbirdChannelList, request.getRequestContext());
      }
    }
  }

  private void processChannelReg(
      List<String> ekstepChannelList,
      List<Map<String, Object>> sunbirdChannelList,
      RequestContext context) {
    Boolean bool = true;
    for (Map<String, Object> map : sunbirdChannelList) {
      logger.info(context, "processing start for hashTagId " + map.get(JsonKey.ID));
      if (!StringUtils.isBlank((String) map.get(JsonKey.ID))
          && (!ekstepChannelList.contains(map.get(JsonKey.ID)))
          && (!Util.registerChannel(map, context))) {
        bool = false;
      }
    }
    if (bool) {
      updateSystemSettingTable(bool, context);
    }
  }

  private void updateSystemSettingTable(Boolean bool, RequestContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, JsonKey.CHANNEL_REG_STATUS_ID);
    map.put(JsonKey.FIELD, JsonKey.CHANNEL_REG_STATUS);
    map.put(JsonKey.VALUE, String.valueOf(bool));
    Response response =
        cassandraOperation.upsertRecord("sunbird", JsonKey.SYSTEM_SETTINGS_DB, map, context);
    logger.info(
        context,
        "Upsert operation result for channel reg status =  "
            + response.getResult().get(JsonKey.RESPONSE));
  }

  private List<Map<String, Object>> getSunbirdChannelList(RequestContext context) {
    logger.info(context, "start call for getting List of channel from sunbird ES");
    SearchDTO searchDto = new SearchDTO();
    List<String> list = new ArrayList<>();
    list.add(JsonKey.ID);
    list.add(JsonKey.DESCRIPTION);
    list.add(JsonKey.CHANNEL);
    searchDto.setFields(list);
    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.IS_TENANT, true);
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Future<Map<String, Object>> esResponseF =
        esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
    List<Map<String, Object>> orgList = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
    logger.info(context, "End call for getting List of channel from sunbird ES");
    return orgList;
  }

  private List<String> getEkstepChannelList(RequestContext context) {
    List<String> channelList = new ArrayList<>();
    Map<String, String> headerMap = new HashMap<>();
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
    headerMap.put("user-id", "");
    String reqString = "";
    String response = "";
    JSONObject data;
    JSONObject jObject;
    Object[] result = null;
    try {
      logger.info(context, "start call for getting List of channel from Ekstep");
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      Map<String, Object> map = new HashMap<>();
      Map<String, Object> reqMap = new HashMap<>();
      map.put(JsonKey.REQUEST, reqMap);

      ObjectMapper mapper = new ObjectMapper();
      reqString = mapper.writeValueAsString(map);
      response =
          HttpClientUtil.post(
              (ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_GET_CHANNEL_LIST)),
              reqString,
              headerMap);
      jObject = new JSONObject(response);
      data = jObject.getJSONObject(JsonKey.RESULT);
      logger.info(
          context,
          "Total number of content fetched from Ekstep while getting List of channel : "
              + data.get("count"));
      JSONArray contentArray = data.getJSONArray(JsonKey.CHANNELS);
      result = mapper.readValue(contentArray.toString(), Object[].class);
      for (Object object : result) {
        Map<String, Object> tempMap = (Map<String, Object>) object;
        channelList.add((String) tempMap.get(JsonKey.CODE));
      }
      logger.info(context, "end call for getting List of channel from Ekstep");
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
      channelList = null;
    }
    return channelList;
  }
}
