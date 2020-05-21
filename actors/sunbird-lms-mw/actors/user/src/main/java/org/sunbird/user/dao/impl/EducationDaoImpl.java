package org.sunbird.user.dao.impl;

import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.EducationDao;

public class EducationDaoImpl implements EducationDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo eduDbInfo = Util.dbInfoMap.get(JsonKey.EDUCATION_DB);

  private EducationDaoImpl() {}

  private static class LazyInitializer {
    private static EducationDao INSTANCE = new EducationDaoImpl();
  }

  public static EducationDao getInstance() {
    return LazyInitializer.INSTANCE;
  }

  @Override
  public void createEducation(Map<String, Object> education) {
    cassandraOperation.insertRecord(eduDbInfo.getKeySpace(), eduDbInfo.getTableName(), education);
  }

  @Override
  public void updateEducation(Map<String, Object> education) {
    cassandraOperation.updateRecord(eduDbInfo.getKeySpace(), eduDbInfo.getTableName(), education);
  }

  @Override
  public void deleteEducation(String educationId) {
    cassandraOperation.deleteRecord(eduDbInfo.getKeySpace(), eduDbInfo.getTableName(), educationId);
  }

  @Override
  public void upsertEducation(Map<String, Object> education) {
    cassandraOperation.upsertRecord(eduDbInfo.getKeySpace(), eduDbInfo.getTableName(), education);
  }

  @Override
  public Response getPropertiesValueById(String propertyName, String identifier) {
    return cassandraOperation.getPropertiesValueById(
        eduDbInfo.getKeySpace(), eduDbInfo.getTableName(), identifier, JsonKey.ADDRESS_ID);
  }
}
