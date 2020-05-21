package org.sunbird.learner.actors.syncjobmanager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;

/** Background sync of data between Cassandra and Elastic Search. */
@ActorConfig(
  tasks = {},
  asyncTasks = {"backgroundSync"}
)
public class EsSyncBackgroundActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();

    if (ActorOperations.BACKGROUND_SYNC.getValue().equalsIgnoreCase(operation)) {
      sync(request);
    } else {
      onReceiveUnsupportedOperation("EsSyncBackgroundActor");
    }
  }

  private void sync(Request message) {
    ProjectLogger.log("EsSyncBackgroundActor: sync called", LoggerEnum.INFO);

    long startTime = System.currentTimeMillis();
    Map<String, Object> req = message.getRequest();
    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> reponseList = null;
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, Object> dataMap = (Map<String, Object>) req.get(JsonKey.DATA);

    String objectType = (String) dataMap.get(JsonKey.OBJECT_TYPE);

    List<Object> objectIds = new ArrayList<>();
    if (null != dataMap.get(JsonKey.OBJECT_IDS)) {
      objectIds = (List<Object>) dataMap.get(JsonKey.OBJECT_IDS);
    }
    Util.DbInfo dbInfo = getDbInfoObj(objectType);
    if (null == dbInfo) {
      throw new ProjectCommonException(
          ResponseCode.invalidObjectType.getErrorCode(),
          ResponseCode.invalidObjectType.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    String requestLogMsg = "";

    if (JsonKey.USER.equals(objectType)) {
      handleUserSyncRequest(objectIds);
      return;
    }
    if (CollectionUtils.isNotEmpty(objectIds)) {
      requestLogMsg =
          MessageFormat.format(
              "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));

      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " started",
          LoggerEnum.INFO);
      Response response =
          cassandraOperation.getRecordsByProperty(
              dbInfo.getKeySpace(), dbInfo.getTableName(), JsonKey.ID, objectIds);
      reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " completed",
          LoggerEnum.INFO);
    }
    if (null != reponseList && !reponseList.isEmpty()) {
      for (Map<String, Object> map : reponseList) {
        responseMap.put((String) map.get(JsonKey.ID), map);
      }
    } else {
      if (objectIds.size() > 0) {
        ProjectLogger.log(
            "EsSyncBackgroundActor:sync: Skip sync for "
                + requestLogMsg
                + " as all IDs are invalid",
            LoggerEnum.ERROR);
        return;
      }

      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Sync all data for type = "
              + objectType
              + " as no IDs provided",
          LoggerEnum.INFO);

      Response response =
          cassandraOperation.getAllRecords(dbInfo.getKeySpace(), dbInfo.getTableName());
      reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching all data for type = " + objectType + " completed",
          LoggerEnum.INFO);

      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Number of entries to sync for type = "
              + objectType
              + " is "
              + reponseList.size(),
          LoggerEnum.INFO);

      if (null != reponseList) {
        for (Map<String, Object> map : reponseList) {
          responseMap.put((String) map.get(JsonKey.ID), map);
        }
      }
    }

    Iterator<Entry<String, Object>> itr = responseMap.entrySet().iterator();
    while (itr.hasNext()) {
      if (objectType.equals(JsonKey.ORGANISATION)) {
        result.add(getOrgDetails(itr.next()));
      }
    }

    esService.bulkInsert(getType(objectType), result);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    ProjectLogger.log(
        "EsSyncBackgroundActor:sync: Total time taken to sync for type = "
            + objectType
            + " is "
            + elapsedTime
            + " ms",
        LoggerEnum.INFO);
  }

  private void handleUserSyncRequest(List<Object> objectIds) {
    if (CollectionUtils.isEmpty(objectIds)) {
      Response response =
          cassandraOperation.getRecordsByProperties(
              JsonKey.SUNBIRD, JsonKey.USER, null, Arrays.asList(JsonKey.ID));
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      objectIds = responseList.stream().map(i -> i.get(JsonKey.ID)).collect(Collectors.toList());
    }
    invokeUserSync(objectIds);
  }

  private void invokeUserSync(List<Object> objectIds) {
    if (CollectionUtils.isNotEmpty(objectIds)) {
      for (Object userId : objectIds) {
        Request userRequest = new Request();
        userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
        userRequest.getRequest().put(JsonKey.ID, userId);
        ProjectLogger.log(
            "EsSyncBackgroundActor:invokeUserSync: Trigger sync of user details to ES");
        tellToAnother(userRequest);
      }
    }
  }

  private String getType(String objectType) {
    String type = "";
    if (objectType.equals(JsonKey.USER)) {
      type = ProjectUtil.EsType.user.getTypeName();
    } else if (objectType.equals(JsonKey.ORGANISATION)) {
      type = ProjectUtil.EsType.organisation.getTypeName();
    }
    return type;
  }

  private Map<String, Object> getOrgDetails(Entry<String, Object> entry) {
    ProjectLogger.log("EsSyncBackgroundActor: getOrgDetails called", LoggerEnum.INFO);
    Map<String, Object> orgMap = (Map<String, Object>) entry.getValue();
    orgMap.remove(JsonKey.ORG_TYPE);
    if (orgMap.containsKey(JsonKey.ADDRESS_ID)
        && !StringUtils.isBlank((String) orgMap.get(JsonKey.ADDRESS_ID))) {
      orgMap.put(
          JsonKey.ADDRESS,
          getDetailsById(
              Util.dbInfoMap.get(JsonKey.ADDRESS_DB), (String) orgMap.get(JsonKey.ADDRESS_ID)));
    }
    ProjectLogger.log("EsSyncBackgroundActor: getOrgDetails returned", LoggerEnum.INFO);
    return orgMap;
  }

  private Map<String, Object> getDetailsById(DbInfo dbInfo, String userId) {
    try {
      Response response =
          cassandraOperation.getRecordById(dbInfo.getKeySpace(), dbInfo.getTableName(), userId);
      return ((((List<Map<String, Object>>) response.get(JsonKey.RESPONSE)).isEmpty())
          ? new HashMap<>()
          : ((List<Map<String, Object>>) response.get(JsonKey.RESPONSE)).get(0));
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
    }
    return null;
  }

  private DbInfo getDbInfoObj(String objectType) {
    if (objectType.equals(JsonKey.USER)) {
      return Util.dbInfoMap.get(JsonKey.USER_DB);
    } else if (objectType.equals(JsonKey.ORGANISATION)) {
      return Util.dbInfoMap.get(JsonKey.ORG_DB);
    } else if (objectType.equals(JsonKey.BATCH)) {
      return Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
    } else if (objectType.equals(JsonKey.USER_COURSE)) {
      return Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
    }

    return null;
  }
}
