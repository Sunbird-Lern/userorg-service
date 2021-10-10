package org.sunbird.service.role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.dao.role.RoleGroupDao;
import org.sunbird.dao.role.impl.RoleGroupDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.role.RoleGroup;
import org.sunbird.request.RequestContext;

public class RoleGroupService {

  private final RoleGroupDao roleGroupDao = RoleGroupDaoImpl.getInstance();

  public Map<String, Object> getRoleGroupMap(String roleName, RequestContext context) {
    Map<String, Object> response = new HashMap<>();
    List<RoleGroup> roleGroupList = roleGroupDao.getRoleGroups(context);

    if (CollectionUtils.isNotEmpty(roleGroupList)) {
      for (RoleGroup roleGroup : roleGroupList) {
        if (roleGroup.getId().equals(roleName)) {
          response.put(JsonKey.ID, roleGroup.getId());
          response.put(JsonKey.NAME, roleGroup.getName());
          response.put(
              JsonKey.URL_ACTION_ID,
              roleGroup.getUrlActionIds() != null
                  ? roleGroup.getUrlActionIds()
                  : new ArrayList<>());
          return response;
        }
      }
    }

    return response;
  }
}
