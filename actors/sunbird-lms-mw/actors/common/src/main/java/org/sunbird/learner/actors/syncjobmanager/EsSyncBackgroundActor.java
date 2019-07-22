package org.sunbird.learner.actors.syncjobmanager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
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
import org.sunbird.learner.actors.coursebatch.service.UserCoursesService;
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
    List<Map<String, Object>> reponseList = null;
    Map<String, Object> responseMap = new HashMap<>();
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
    String primaryKey = JsonKey.COURSE_ID;
    String identifier = JsonKey.BATCH_ID;
    if (objectType.equals(JsonKey.USER_COURSE)) {
      primaryKey = JsonKey.BATCH_ID;
      identifier = UserCoursesService.generateUserCourseESId(JsonKey.BATCH_ID, JsonKey.USER_ID);
    }
    String requestLogMsg = "";

    if (CollectionUtils.isNotEmpty(objectIds)) {
      requestLogMsg =
          MessageFormat.format(
              "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));

      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " started",
          LoggerEnum.INFO);
      if (objectIds.get(0) instanceof String) {
        Response response =
            cassandraOperation.getRecordsByProperty(
                dbInfo.getKeySpace(), dbInfo.getTableName(), primaryKey, objectIds);
        reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      }

      if (objectIds.get(0) instanceof Map) {
        Map<String, Object> filters = new HashMap<>();
        objectIds
            .stream()
            .forEach(
                objectId -> {
                  if (!filters.containsKey(
                      ((Map<String, Object>) objectId).get(JsonKey.BATCH_ID))) {
                    filters.put(
                        (String) ((Map<String, Object>) objectId).get(JsonKey.BATCH_ID),
                        new ArrayList<Map<String, Object>>());
                  }
                  ((List<String>)
                          filters.get(((Map<String, Object>) objectId).get(JsonKey.BATCH_ID)))
                      .add(JsonKey.COURSE_ID);
                });
        Response response =
            cassandraOperation.getRecords(
                dbInfo.getKeySpace(), dbInfo.getTableName(), filters, null);
        reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      }
      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " completed",
          LoggerEnum.INFO);
    }

    ProjectLogger.log(
        "EsSyncBackgroundActor:sync: Fetching all data for type = " + objectType + " completed",
        LoggerEnum.INFO);
    if (null != reponseList && !reponseList.isEmpty()) {
      for (Map<String, Object> map : reponseList) {
        map.put(JsonKey.IDENTIFIER, map.get(identifier));
        responseMap.put((String) map.get(identifier), map);
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
          "EsSyncBackgroundActor:sync: Number of entries to sync for type = "
              + objectType
              + " is "
              + reponseList.size(),
          LoggerEnum.INFO);

      if (null != reponseList) {
        for (Map<String, Object> map : reponseList) {
          map.put(JsonKey.IDENTIFIER, map.get(JsonKey.BATCH_ID));
          responseMap.put((String) map.get(JsonKey.BATCH_ID), map);
        }
      }
    }

    Iterator<Entry<String, Object>> itr = responseMap.entrySet().iterator();
    while (itr.hasNext()) {
      result.add((Map<String, Object>) (itr.next().getValue()));
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

  private String getType(String objectType) {
    String type = "";
    if (objectType.equals(JsonKey.BATCH)) {
      type = ProjectUtil.EsType.course.getTypeName();
    } else if (objectType.equals(JsonKey.USER_COURSE)) {
      type = ProjectUtil.EsType.usercourses.getTypeName();
    }
    return type;
  }

  private DbInfo getDbInfoObj(String objectType) {

    if (objectType.equals(JsonKey.BATCH)) {
      return Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
    } else if (objectType.equals(JsonKey.USER_COURSE)) {
      return Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
    }

    return null;
  }
}
