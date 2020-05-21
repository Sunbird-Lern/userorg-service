package org.sunbird.user.dao.impl;

import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.JobProfileDao;

public class JobProfileDaoImpl implements JobProfileDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo jobProDbInfo = Util.dbInfoMap.get(JsonKey.JOB_PROFILE_DB);

  private JobProfileDaoImpl() {}

  private static class LazyInitializer {
    private static JobProfileDao INSTANCE = new JobProfileDaoImpl();
  }

  public static JobProfileDao getInstance() {
    return LazyInitializer.INSTANCE;
  }

  @Override
  public void createJobProfile(Map<String, Object> jobProfile) {
    cassandraOperation.insertRecord(
        jobProDbInfo.getKeySpace(), jobProDbInfo.getTableName(), jobProfile);
  }

  @Override
  public void updateJobProfile(Map<String, Object> jobProfile) {
    cassandraOperation.updateRecord(
        jobProDbInfo.getKeySpace(), jobProDbInfo.getTableName(), jobProfile);
  }

  @Override
  public void upsertJobProfile(Map<String, Object> jobProfile) {
    cassandraOperation.upsertRecord(
        jobProDbInfo.getKeySpace(), jobProDbInfo.getTableName(), jobProfile);
  }

  @Override
  public void deleteJobProfile(String jobProfileId) {
    cassandraOperation.deleteRecord(
        jobProDbInfo.getKeySpace(), jobProDbInfo.getTableName(), jobProfileId);
  }

  @Override
  public Response getPropertiesValueById(String propertyName, String identifier) {
    return cassandraOperation.getPropertiesValueById(
        jobProDbInfo.getKeySpace(), jobProDbInfo.getTableName(), identifier, JsonKey.ADDRESS_ID);
  }
}
