package org.sunbird.badge.actors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {},
  asyncTasks = {"assignBadgeToUser", "revokeBadgeFromUser"}
)
public class UserBadgeAssertion extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private DbInfo dbInfo = Util.dbInfoMap.get(BadgingJsonKey.USER_BADGE_ASSERTION_DB);
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    ExecutionContext.setRequestId(request.getRequestId());
    String operation = request.getOperation();
    if (BadgeOperations.assignBadgeToUser.name().equalsIgnoreCase(operation)) {
      addBadgeData(request);
    } else if (BadgeOperations.revokeBadgeFromUser.name().equalsIgnoreCase(operation)) {
      revokeBadgeData(request);
    }
  }

  private void revokeBadgeData(Request request) {
    // request came to revoke the badge from user
    Map<String, Object> badge = getBadgeAssertion(request);
    cassandraOperation.deleteRecord(
        dbInfo.getKeySpace(),
        dbInfo.getTableName(),
        (String) badge.get(BadgingJsonKey.ASSERTION_ID));
    updateUserBadgeDataToES(badge);
    tellToSender(request, badge);
  }

  private void addBadgeData(Request request) {
    // request came to assign the badge from user
    Map<String, Object> badge = getBadgeAssertion(request);
    cassandraOperation.insertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), badge);
    updateUserBadgeDataToES(badge);
    tellToSender(request, badge);
  }

  @SuppressWarnings("unchecked")
  private void updateUserBadgeDataToES(Map<String, Object> map) {
    Future<Map<String, Object>> resultF =
        esUtil.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(), (String) map.get(JsonKey.USER_ID));
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (MapUtils.isEmpty(result)) {
      ProjectLogger.log(
          "UserBadgeAssertion:updateUserBadgeDataToES user with userId "
              + (String) map.get(JsonKey.USER_ID)
              + " not found",
          LoggerEnum.INFO.name());
      return;
    }
    if (CollectionUtils.isNotEmpty(
        (List<Map<String, Object>>) result.get(BadgingJsonKey.BADGE_ASSERTIONS))) {
      List<Map<String, Object>> badgeAssertionsList =
          (List<Map<String, Object>>) result.get(BadgingJsonKey.BADGE_ASSERTIONS);

      boolean bool = true;
      Iterator<Map<String, Object>> itr = badgeAssertionsList.iterator();
      while (itr.hasNext()) {
        Map<String, Object> tempMap = itr.next();
        if (((String) tempMap.get(JsonKey.ID)).equalsIgnoreCase((String) map.get(JsonKey.ID))) {
          itr.remove();
          bool = false;
        }
      }

      if (bool) {
        badgeAssertionsList.add(map);
      }
    } else {
      List<Map<String, Object>> mapList = new ArrayList<>();
      mapList.add(map);
      result.put(BadgingJsonKey.BADGE_ASSERTIONS, mapList);
    }
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) result.get(JsonKey.IDENTIFIER),
        result);
  }

  private boolean updateDataToElastic(
      String indexName, String typeName, String identifier, Map<String, Object> data) {

    Future<Boolean> responseF = esUtil.update(typeName, identifier, data);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (!response) {
      ProjectLogger.log(
          "unbale to save the data inside ES for user badge " + identifier, LoggerEnum.INFO.name());
    }
    return response;
  }

  private Map<String, Object> getBadgeAssertion(Request request) {
    String userId = (String) request.getRequest().get(JsonKey.ID);
    @SuppressWarnings("unchecked")
    Map<String, Object> badge =
        (Map<String, Object>) request.getRequest().get(BadgingJsonKey.BADGE_ASSERTION);
    badge.put(JsonKey.USER_ID, userId);
    badge.put(JsonKey.ID, badge.get(BadgingJsonKey.ASSERTION_ID));
    // removing status from map
    badge.remove(JsonKey.STATUS);
    return badge;
  }

  private void tellToSender(Request request, Map<String, Object> badge) {
    Response reponse = new Response();
    reponse.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(reponse, self());
    sendTelemetry(request.getRequest(), badge);
  }

  private void sendTelemetry(Map<String, Object> map, Map<String, Object> badge) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject;
    String userId = (String) badge.get(JsonKey.USER_ID);
    targetObject = TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.generateCorrelatedObject(
        (String) badge.get(BadgingJsonKey.ASSERTION_ID),
        BadgingJsonKey.BADGE_ASSERTION,
        null,
        correlatedObject);
    TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);
    TelemetryUtil.telemetryProcessingCall(map, targetObject, correlatedObject);
  }
}
