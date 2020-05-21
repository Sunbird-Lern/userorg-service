package org.sunbird.learner.actors.role.group.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.learner.actors.role.group.dao.RoleGroupDao;
import org.sunbird.learner.actors.role.group.dao.impl.RoleGroupDaoImpl;
import org.sunbird.models.role.group.RoleGroup;

public class RoleGroupService {

  private static RoleGroupDao roleGroupDao = RoleGroupDaoImpl.getInstance();

  public static Map<String, Object> getRoleGroupMap(String roleName) {
    Map<String, Object> response = new HashMap<>();
    List<RoleGroup> roleGroupList = roleGroupDao.getRoleGroups();

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
