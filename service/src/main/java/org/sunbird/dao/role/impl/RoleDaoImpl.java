package org.sunbird.dao.role.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.role.RoleDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.role.Role;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class RoleDaoImpl implements RoleDao {

  private static final String TABLE_NAME = "role";
  private final ObjectMapper mapper = new ObjectMapper();
  private static RoleDao roleDao;

  public static RoleDao getInstance() {
    if (roleDao == null) {
      roleDao = new RoleDaoImpl();
    }
    return roleDao;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Role> getRoles(RequestContext context) {
    Response roleResults =
        getCassandraOperation().getAllRecords(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), TABLE_NAME, context);
    TypeReference<List<Role>> roleMapType = new TypeReference<>() {};
    List<Map<String, Object>> roleMapList =
        (List<Map<String, Object>>) roleResults.get(JsonKey.RESPONSE);
    List<Role> roleList = mapper.convertValue(roleMapList, roleMapType);
    return roleList;
  }

  public CassandraOperation getCassandraOperation() {
    return ServiceFactory.getInstance();
  }
}
