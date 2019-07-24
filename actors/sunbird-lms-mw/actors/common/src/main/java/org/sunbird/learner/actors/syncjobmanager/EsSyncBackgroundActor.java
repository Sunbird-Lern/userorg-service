package org.sunbird.learner.actors.syncjobmanager;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
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

    Map<String, Object> req = message.getRequest();
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
    final String primaryKey;
    final String identifier;
    if (objectType.equals(JsonKey.USER_COURSE)) {
      primaryKey = JsonKey.BATCH_ID;
      identifier = JsonKey.USER_ID;
    } else {
      primaryKey = JsonKey.COURSE_ID;
      identifier = JsonKey.BATCH_ID;
    }
    String requestLogMsg = "";

    if (CollectionUtils.isNotEmpty(objectIds)) {
      requestLogMsg =
          MessageFormat.format(
              "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));

      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " started",
          LoggerEnum.INFO);

      Map<String, Object> filters = new HashMap<>();
      if (objectIds.get(0) instanceof String) {
        filters.put(primaryKey, objectIds);
        cassandraOperation.applyOperationOnRecordsAsync(
            dbInfo.getKeySpace(),
            dbInfo.getTableName(),
            filters,
            null,
            getSyncCallback(objectType));
      }

      if (objectIds.get(0) instanceof Map) {
        objectIds
            .stream()
            .forEach(
                objectId -> {
                  Map<String, Object> filterMap = new HashMap<>();
                  filterMap.put(primaryKey, ((Map<String, Object>) objectId).get(primaryKey));
                  filterMap.put(identifier, ((Map<String, Object>) objectId).get(identifier));
                  cassandraOperation.applyOperationOnRecordsAsync(
                      dbInfo.getKeySpace(),
                      dbInfo.getTableName(),
                      filterMap,
                      null,
                      getSyncCallback(objectType));
                });
      }
      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " completed",
          LoggerEnum.INFO);
      return;
    }

    ProjectLogger.log(
        "EsSyncBackgroundActor:sync: Sync all data for type = "
            + objectType
            + " as no IDs provided",
        LoggerEnum.INFO);

    cassandraOperation.applyOperationOnRecordsAsync(
        dbInfo.getKeySpace(), dbInfo.getTableName(), null, null, getSyncCallback(objectType));
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

  private FutureCallback<ResultSet> getSyncCallback(String objectType) {
    return new FutureCallback<ResultSet>() {
      @Override
      public void onSuccess(ResultSet result) {
        Map<String, String> columnMap = CassandraUtil.fetchColumnsMapping(result);
        result
            .iterator()
            .forEachRemaining(
                row -> {
                  try {
                    syncDataForEachRow(row, columnMap, objectType);
                  } catch (Exception e) {
                    ProjectLogger.log("Exception occurred while getSyncCallback ", e);
                  }
                });
        ProjectLogger.log("getSyncCallback sync successful " + objectType, LoggerEnum.INFO.name());
      }

      @Override
      public void onFailure(Throwable t) {
        ProjectLogger.log("Exception occurred while getSyncCallback ", t);
      }
    };
  }

  private void syncDataForEachRow(Row row, Map<String, String> columnMap, String objectType) {
    Map<String, Object> rowMap = new HashMap<>();
    columnMap
        .entrySet()
        .stream()
        .forEach(
            entry -> {
              Object value = row.getObject(entry.getValue());
              if (entry.getKey().equals("contentStatus") && value != null) {
                try {
                  rowMap.put(entry.getKey(), new ObjectMapper().writeValueAsString(value));
                } catch (Exception e) {
                  ProjectLogger.log("Exception occurred while getSyncCallback ", e);
                }
              } else {
                rowMap.put(entry.getKey(), value);
              }
            });
    String id = (String) rowMap.get(JsonKey.ID);
    if (objectType.equals(JsonKey.USER_COURSE)) {
      id =
          UserCoursesService.generateUserCourseESId(
              (String) rowMap.get(JsonKey.BATCH_ID), (String) rowMap.get(JsonKey.USER_ID));
    } else if (objectType.equals(JsonKey.BATCH)) {
      id = (String) rowMap.get(JsonKey.BATCH_ID);
    }
    rowMap.put(JsonKey.ID, id);
    esService.save(getType(objectType), id, rowMap);
  }
}
