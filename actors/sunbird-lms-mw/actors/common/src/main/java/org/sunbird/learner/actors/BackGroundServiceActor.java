package org.sunbird.learner.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {},
  asyncTasks = {"updateUserCountToLocationID"}
)
public class BackGroundServiceActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request
        .getOperation()
        .equalsIgnoreCase(BackgroundOperations.updateUserCountToLocationID.name())) {
      updateUserCount(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void updateUserCount(Request actorMessage) {
    ProjectLogger.log("In BackgroundService actor in updateUserCount method.");
    Util.DbInfo locDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
    List<Object> locationIds = (List<Object>) actorMessage.getRequest().get(JsonKey.LOCATION_IDS);
    String operation = (String) actorMessage.getRequest().get(JsonKey.OPERATION);
    ProjectLogger.log("operation for updating UserCount" + operation);
    Response response =
        cassandraOperation.getRecordsByProperty(
            locDbInfo.getKeySpace(), locDbInfo.getTableName(), JsonKey.ID, locationIds);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != list && !list.isEmpty()) {
      for (Map<String, Object> map : list) {
        String locationId = (String) map.get(JsonKey.ID);
        ProjectLogger.log("Processing start for LocationId " + locationId);
        Long userCountTTL = 0L;
        int userCount =
            (map.get(JsonKey.USER_COUNT) == null) ? 0 : (int) (map.get(JsonKey.USER_COUNT));
        ProjectLogger.log("userCount is " + userCount + "for location id " + locationId);
        if (userCount == 0
            && !StringUtils.isBlank(operation)
            && operation.equalsIgnoreCase("UpdateUserCountScheduler")) {
          ProjectLogger.log("Processing start for LocationId for Scheduler " + locationId);
          int count = getUserCount(locationId);
          Map<String, Object> reqMap = new HashMap<>();
          reqMap.put(JsonKey.ID, locationId);
          reqMap.put(JsonKey.USER_COUNT, count);
          reqMap.put(JsonKey.USER_COUNT_TTL, String.valueOf(System.currentTimeMillis()));
          cassandraOperation.updateRecord(
              locDbInfo.getKeySpace(), locDbInfo.getTableName(), reqMap);
        } else if (!StringUtils.isBlank(operation)
            && operation.equalsIgnoreCase("GeoLocationManagementActor")) {
          ProjectLogger.log(
              "Processing start for LocationId for GeoLocationManagementActor " + locationId);
          try {
            if (!StringUtils.isBlank((String) map.get(JsonKey.USER_COUNT_TTL))) {
              userCountTTL = Long.valueOf((String) map.get(JsonKey.USER_COUNT_TTL));
            }
          } catch (Exception ex) {
            ProjectLogger.log(
                "Exception occurred while converting string to long "
                    + (String) map.get(JsonKey.USER_COUNT_TTL));
            userCountTTL = 0L;
          }
          ProjectLogger.log("userCountTTL == " + userCountTTL);
          Long currentTime = System.currentTimeMillis();
          Long diff = currentTime - userCountTTL;
          int hours = (int) (diff / (1000 * 60 * 60));
          if (hours >= 24) {
            ProjectLogger.log("Updating user count for LocnId " + locationId);
            int usrCount = getUserCount(locationId);
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put(JsonKey.ID, locationId);
            reqMap.put(JsonKey.USER_COUNT, usrCount);
            reqMap.put(JsonKey.USER_COUNT_TTL, String.valueOf(System.currentTimeMillis()));
            cassandraOperation.updateRecord(
                locDbInfo.getKeySpace(), locDbInfo.getTableName(), reqMap);
          }
        }
      }
      ProjectLogger.log("Processing end user count update ");
    }
  }

  private static int getUserCount(String locationId) {
    ProjectLogger.log("fetching user count start ");
    SearchDTO searchDto = new SearchDTO();
    List<String> list = new ArrayList<>();
    list.add(JsonKey.ID);
    searchDto.setFields(list);
    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.LOCATION_ID, locationId);
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Future<Map<String, Object>> esResponseF =
        esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName());
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
    List<Map<String, Object>> orgList = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);

    List<String> orgIdList = new ArrayList<>();
    for (Map<String, Object> map : orgList) {
      orgIdList.add((String) map.get(JsonKey.ID));
    }
    ProjectLogger.log(
        "Total No of Organisation for Location Id " + locationId + " , " + orgIdList.size());
    if (!orgIdList.isEmpty()) {
      searchDto = new SearchDTO();
      List<String> list2 = new ArrayList<>();
      list2.add(JsonKey.ID);
      searchDto.setFields(list2);
      searchDto.setLimit(0);
      Map<String, Object> filter2 = new HashMap<>();
      filter2.put(JsonKey.ORGANISATIONS + "." + JsonKey.ORGANISATION_ID, orgIdList);
      ProjectLogger.log("filter2.get(JsonKey.ORGANISATIONS.JsonKey.ORGANISATION_ID) " + orgIdList);
      searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter2);
      Future<Map<String, Object>> esResponse2F =
          esService.search(searchDto, ProjectUtil.EsType.user.getTypeName());
      Map<String, Object> esResponse2 =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponse2F);
      long userCount = (long) esResponse2.get(JsonKey.COUNT);
      ProjectLogger.log("Total No of User for Location Id " + locationId + " , " + userCount);
      return (int) userCount;
    } else {
      return 0;
    }
  }
}
