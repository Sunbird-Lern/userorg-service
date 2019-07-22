package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.CourseBatchDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.course.batch.CourseBatch;

public class CourseBatchDaoImpl implements CourseBatchDao {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo courseBatchDb = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);

  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public Response create(CourseBatch courseBatch) {
    Map<String, Object> map = mapper.convertValue(courseBatch, Map.class);
    return cassandraOperation.insertRecord(
        courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), map);
  }

  @Override
  public Response update(String courseId, String batchId, Map<String, Object> map) {
    Map<String, Object> primaryKey = new HashMap<>();
    primaryKey.put(JsonKey.COURSE_ID, courseId);
    primaryKey.put(JsonKey.BATCH_ID, batchId);
    Map<String, Object> attributeMap = new HashMap<>();
    attributeMap.putAll(map);
    attributeMap.remove(JsonKey.COURSE_ID);
    attributeMap.remove(JsonKey.BATCH_ID);
    return cassandraOperation.updateRecord(
        courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), attributeMap, primaryKey);
  }

  @Override
  public CourseBatch readById(String courseId, String batchId) {
    Map<String, Object> primaryKey = new HashMap<>();
    primaryKey.put(JsonKey.COURSE_ID, courseId);
    primaryKey.put(JsonKey.BATCH_ID, batchId);
    Response courseBatchResult =
        cassandraOperation.getRecordById(
            courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), primaryKey);
    List<Map<String, Object>> courseList =
        (List<Map<String, Object>>) courseBatchResult.get(JsonKey.RESPONSE);
    if (courseList.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidCourseBatchId.getErrorCode(),
          ResponseCode.invalidCourseBatchId.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else {
      courseList.get(0).remove(JsonKey.PARTICIPANT);
      return mapper.convertValue(courseList.get(0), CourseBatch.class);
    }
  }

  @Override
  public Response delete(String id) {
    return cassandraOperation.deleteRecord(
        courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), id);
  }
}
