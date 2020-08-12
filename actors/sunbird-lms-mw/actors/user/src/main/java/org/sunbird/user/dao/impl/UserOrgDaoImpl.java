package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;

public final class UserOrgDaoImpl implements UserOrgDao {

  private static final String TABLE_NAME = JsonKey.USER_ORG;
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private ObjectMapper mapper = new ObjectMapper();

  private UserOrgDaoImpl() {}

  private static class LazyInitializer {
    private static UserOrgDao INSTANCE = new UserOrgDaoImpl();
  }

  public static UserOrgDao getInstance() {
    return LazyInitializer.INSTANCE;
  }

  @Override
  public Response updateUserOrg(UserOrg userOrg) {
    Map<String, Object> compositeKey = new LinkedHashMap<>(2);
    Map<String, Object> request = mapper.convertValue(userOrg, Map.class);
    compositeKey.put(JsonKey.USER_ID, request.remove(JsonKey.USER_ID));
    compositeKey.put(JsonKey.ORGANISATION_ID, request.remove(JsonKey.ORGANISATION_ID));
    return cassandraOperation.updateRecord(Util.KEY_SPACE_NAME, TABLE_NAME, request, compositeKey);
  }

  @Override
  public Response createUserOrg(UserOrg userOrg) {
    return cassandraOperation.insertRecord(
        Util.KEY_SPACE_NAME, TABLE_NAME, mapper.convertValue(userOrg, Map.class));
  }
}
