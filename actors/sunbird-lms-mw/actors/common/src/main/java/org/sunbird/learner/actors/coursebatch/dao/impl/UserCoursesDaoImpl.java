package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.UserCoursesDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.courses.UserCourses;

public class UserCoursesDaoImpl implements UserCoursesDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  static UserCoursesDao userCoursesDao;
  private static final String KEYSPACE_NAME =
      Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB).getKeySpace();
  private static final String TABLE_NAME =
      Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB).getTableName();

  public static UserCoursesDao getInstance() {
    if (userCoursesDao == null) {
      userCoursesDao = new UserCoursesDaoImpl();
    }
    return userCoursesDao;
  }

  @Override
  public UserCourses read(String id) {
    Response response = cassandraOperation.getRecordById(KEYSPACE_NAME, TABLE_NAME, id);
    List<Map<String, Object>> userCoursesList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(userCoursesList)) {
      return null;
    }
    try {
      return mapper.convertValue((Map<String, Object>) userCoursesList.get(0), UserCourses.class);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public Response update(Map<String, Object> updateAttributes) {
    return cassandraOperation.updateRecord(KEYSPACE_NAME, TABLE_NAME, updateAttributes);
  }

  @Override
  public List<String> getAllActiveUserOfBatch(String batchId) {
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put(JsonKey.BATCH_ID, batchId);
    Response response =
        cassandraOperation.getRecordsByProperties(
            KEYSPACE_NAME,
            TABLE_NAME,
            queryMap,
            Arrays.asList(JsonKey.USER_ID, JsonKey.ACTIVE, JsonKey.ID));
    List<Map<String, Object>> userCoursesList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(userCoursesList)) {
      return null;
    }
    List<String> users = new ArrayList<>();

    userCoursesList.forEach(
        userCourses -> {
          if ((boolean) userCourses.get(JsonKey.ACTIVE))
            users.add((String) userCourses.get(JsonKey.USER_ID));
        });
    return users;
  }

  @Override
  public Response batchInsert(List<Map<String, Object>> userCoursesDetails) {
    return cassandraOperation.batchInsert(KEYSPACE_NAME, TABLE_NAME, userCoursesDetails);
  }

  @Override
  public Response insert(Map<String, Object> userCoursesDetails) {
    return cassandraOperation.insertRecord(KEYSPACE_NAME, TABLE_NAME, userCoursesDetails);
  }
}
