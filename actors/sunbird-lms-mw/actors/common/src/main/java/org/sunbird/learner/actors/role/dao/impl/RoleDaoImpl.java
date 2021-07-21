package org.sunbird.learner.actors.role.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.role.dao.RoleDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.role.Role;
import org.sunbird.response.Response;

public class RoleDaoImpl implements RoleDao {

  private static final String TABLE_NAME = "role";
  private ObjectMapper mapper = new ObjectMapper();
  private static RoleDao roleDao;

  public static RoleDao getInstance() {
    if (roleDao == null) {
      roleDao = new RoleDaoImpl();
    }
    return roleDao;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Role> getRoles() {
    Response roleResults =
        getCassandraOperation().getAllRecords(Util.KEY_SPACE_NAME, TABLE_NAME, null);
    TypeReference<List<Role>> roleMapType = new TypeReference<List<Role>>() {};
    List<Map<String, Object>> roleMapList =
        (List<Map<String, Object>>) roleResults.get(JsonKey.RESPONSE);
    List<Role> roleList = mapper.convertValue(roleMapList, roleMapType);
    return roleList;
  }

  public CassandraOperation getCassandraOperation() {
    return ServiceFactory.getInstance();
  }
}
