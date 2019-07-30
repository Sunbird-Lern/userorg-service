package org.sunbird.learner.actors.syncjobmanager;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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

/** Sync data between Cassandra and Elastic Search. */
@ActorConfig(
  tasks = {"sync"},
  asyncTasks = {}
)
public class EsSyncActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private static final int BATCH_SIZE = 100;

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();

    if (operation.equalsIgnoreCase(ActorOperations.SYNC.getValue())) {
      triggerSync(request);
    } else {
      onReceiveUnsupportedOperation("EsSyncActor");
    }
  }

  private void triggerSync(Request req) {
    Map<String, Object> dataMap = (Map<String, Object>) req.get(JsonKey.DATA);
    String objectType = (String) dataMap.get(JsonKey.OBJECT_TYPE);
    ProjectLogger.log(
        "EsSyncBackgroundActor: sync called for objectType=" + objectType, LoggerEnum.INFO);
    Util.DbInfo dbInfo = getDbInfoObj(objectType);
    if (null == dbInfo) {
      throw new ProjectCommonException(
          ResponseCode.invalidObjectType.getErrorCode(),
          ResponseCode.invalidObjectType.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());

    List<Object> objectIds = new ArrayList<>();
    if (null != dataMap.get(JsonKey.OBJECT_IDS)) {
      objectIds = (List<Object>) dataMap.get(JsonKey.OBJECT_IDS);
    }
    if (CollectionUtils.isEmpty(objectIds)) {
      ProjectLogger.log(
          "EsSyncBackgroundActor:sync: Sync all data for type = " + objectType, LoggerEnum.INFO);

      cassandraOperation.applyOperationOnRecordsAsync(
          dbInfo.getKeySpace(), dbInfo.getTableName(), null, null, getSyncCallback(objectType));
      return;
    }

    final String partitionKey =
        objectType.equals(JsonKey.USER_COURSE)
            ? JsonKey.BATCH_ID
            : (objectType.equals(JsonKey.BATCH) ? JsonKey.COURSE_ID : null);

    String requestLogMsg =
        MessageFormat.format(
            "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));

    ProjectLogger.log(
        "EsSyncBackgroundActor:sync: Syncing data for " + requestLogMsg + " started",
        LoggerEnum.INFO);

    List<String> partitionKeys = new ArrayList<>();
    List<Map<String, Object>> idFilters = new ArrayList<>();
    for (Object objectId : objectIds) {
      if (objectId instanceof String) {
        partitionKeys.add((String) objectId);
      } else if (objectId instanceof Map) {
        idFilters.add((Map<String, Object>) objectId);
      }
    }

    if (CollectionUtils.isNotEmpty(partitionKeys)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(partitionKey, partitionKeys);
      cassandraOperation.applyOperationOnRecordsAsync(
          dbInfo.getKeySpace(), dbInfo.getTableName(), filters, null, getSyncCallback(objectType));
    }

    if (CollectionUtils.isNotEmpty(idFilters)) {
      idFilters.forEach(
          idMap ->
              cassandraOperation.applyOperationOnRecordsAsync(
                  dbInfo.getKeySpace(),
                  dbInfo.getTableName(),
                  idMap,
                  null,
                  getSyncCallback(objectType)));
    }
    ProjectLogger.log(
        "EsSyncBackgroundActor:sync: Syncing data for " + requestLogMsg + " completed",
        LoggerEnum.INFO);
  }

  private String getType(String objectType) {
    String type = "";
    if (objectType.equals(JsonKey.BATCH)) {
      type = ProjectUtil.EsType.courseBatch.getTypeName();
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
        List<Map<String, Object>> docList = new ArrayList<>();
        Map<String, String> columnMap = CassandraUtil.fetchColumnsMapping(result);
        long count = 0;
        try {

          Iterator<Row> resultIterator = result.iterator();
          while (resultIterator.hasNext()) {
            Row row = resultIterator.next();
            Map<String, Object> doc = syncDataForEachRow(row, columnMap, objectType);
            docList.add(doc);
            count++;
            if (docList.size() >= BATCH_SIZE) {
              esService.bulkInsert(getType(objectType), docList);
              docList.clear();
            }
          }
          if (!docList.isEmpty()) {
            esService.bulkInsert(getType(objectType), docList);
          }
          ProjectLogger.log(
              "getSyncCallback sync successful objectType=" + objectType + " count=" + count,
              LoggerEnum.INFO.name());
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while getSyncCallback on count" + count, e);
        }
      }

      @Override
      public void onFailure(Throwable t) {
        ProjectLogger.log("Exception occurred while getSyncCallback ", t);
      }
    };
  }

  private Map<String, Object> syncDataForEachRow(
      Row row, Map<String, String> columnMap, String objectType) {
    Map<String, Object> rowMap = new HashMap<>();
    columnMap
        .entrySet()
        .forEach(
            entry -> {
              Object value = row.getObject(entry.getValue());
              if (entry.getKey().equals("contentStatus") && value != null) {
                try {
                  rowMap.put(entry.getKey(), new ObjectMapper().writeValueAsString(value));
                } catch (JsonProcessingException e) {
                  ProjectLogger.log("JsonProcessingException occurred while getSyncCallback ", e);
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
    return rowMap;
  }
}
