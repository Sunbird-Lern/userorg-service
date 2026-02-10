package org.sunbird.dao.userconsent.impl;

import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.userconsent.UserConsentDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class UserConsentDaoImpl implements UserConsentDao {
  private final String TABLE_NAME = "user_consent";
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private static UserConsentDao consentDao = null;

  public static UserConsentDao getInstance() {
    if (consentDao == null) {
      consentDao = new UserConsentDaoImpl();
    }
    return consentDao;
  }

  @Override
  public Response updateConsent(Map<String, Object> consent, RequestContext context) {
    return cassandraOperation.upsertRecord(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), TABLE_NAME, consent, context);
  }

  @Override
  public List<Map<String, Object>> getConsent(
      Map<String, Object> consentReq, RequestContext context) {
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), TABLE_NAME, consentReq, context);
    return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
  }
}
