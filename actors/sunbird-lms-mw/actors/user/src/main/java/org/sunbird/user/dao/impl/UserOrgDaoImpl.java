package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;

public final class UserOrgDaoImpl implements UserOrgDao {

  private static final String TABLE_NAME = "user_org";
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
    return cassandraOperation.updateRecord(
        Util.KEY_SPACE_NAME, TABLE_NAME, mapper.convertValue(userOrg, Map.class));
  }

  @Override
  public Response createUserOrg(UserOrg userOrg) {
    return cassandraOperation.insertRecord(
        Util.KEY_SPACE_NAME, TABLE_NAME, mapper.convertValue(userOrg, Map.class));
  }
}
