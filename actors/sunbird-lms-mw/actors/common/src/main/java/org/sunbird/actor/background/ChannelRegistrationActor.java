package org.sunbird.actor.background;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      registerChannel();
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void registerChannel() {
    List<String> ekstepChannelList = getEkstepChannelList();
    List<Map<String, Object>> sunbirdChannelList = null;
    if (null != ekstepChannelList) {
      ProjectLogger.log("channel list size from ekstep : " + ekstepChannelList.size());
      sunbirdChannelList = getSunbirdChannelList();
      ProjectLogger.log("channel list size from sunbird : " + sunbirdChannelList.size());
      if (!ekstepChannelList.isEmpty()) {
        processChannelReg(ekstepChannelList, sunbirdChannelList);
      }
    }
  }

  private void processChannelReg(
      List<String> ekstepChannelList, List<Map<String, Object>> sunbirdChannelList) {
    Boolean bool = true;
    for (Map<String, Object> map : sunbirdChannelList) {
      ProjectLogger.log("processing start for hashTagId " + map.get(JsonKey.HASHTAGID));
      if (!StringUtils.isBlank((String) map.get(JsonKey.HASHTAGID))
          && (!ekstepChannelList.contains(map.get(JsonKey.HASHTAGID)))
          && (!Util.registerChannel(map))) {
        bool = false;
      }
    }
    if (bool) {
      updateSystemSettingTable(bool);
    }
  }

  private void updateSystemSettingTable(Boolean bool) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, JsonKey.CHANNEL_REG_STATUS_ID);
    map.put(JsonKey.FIELD, JsonKey.CHANNEL_REG_STATUS);
    map.put(JsonKey.VALUE, String.valueOf(bool));
    Response response = cassandraOperation.upsertRecord("sunbird", JsonKey.SYSTEM_SETTINGS_DB, map);
    ProjectLogger.log(
        "Upsert operation result for channel reg status =  "
            + response.getResult().get(JsonKey.RESPONSE));
  }

  private List<Map<String, Object>> getSunbirdChannelList() {
    ProjectLogger.log("start call for getting List of channel from sunbird ES");
    SearchDTO searchDto = new SearchDTO();
    List<String> list = new ArrayList<>();
    list.add(JsonKey.HASHTAGID);
    list.add(JsonKey.DESCRIPTION);
    list.add(JsonKey.CHANNEL);
    searchDto.setFields(list);
    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.IS_ROOT_ORG, true);
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Future<Map<String, Object>> esResponseF =
        esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName());
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
    List<Map<String, Object>> orgList = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
    ProjectLogger.log("End call for getting List of channel from sunbird ES");
    return orgList;
  }

  private List<String> getEkstepChannelList() {
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
      ProjectLogger.log("start call for getting List of channel from Ekstep");
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
          HttpUtil.sendPostRequest(
              (ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_GET_CHANNEL_LIST)),
              reqString,
              headerMap);
      jObject = new JSONObject(response);
      data = jObject.getJSONObject(JsonKey.RESULT);
      ProjectLogger.log(
          "Total number of content fetched from Ekstep while getting List of channel : "
              + data.get("count"));
      JSONArray contentArray = data.getJSONArray(JsonKey.CHANNELS);
      result = mapper.readValue(contentArray.toString(), Object[].class);
      for (Object object : result) {
        Map<String, Object> tempMap = (Map<String, Object>) object;
        channelList.add((String) tempMap.get(JsonKey.CODE));
      }
      ProjectLogger.log("end call for getting List of channel from Ekstep");
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
      channelList = null;
    }
    return channelList;
  }
}
