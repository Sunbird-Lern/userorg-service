package org.sunbird.learner.actors.role.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.role.dao.RoleDao;
import org.sunbird.learner.actors.role.dao.impl.RoleDaoImpl;
import org.sunbird.learner.actors.role.group.service.RoleGroupService;
import org.sunbird.learner.actors.url.action.service.UrlActionService;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.models.role.Role;

public class RoleService {

  private static RoleDao roleDao = RoleDaoImpl.getInstance();

  @SuppressWarnings("unchecked")
  public static Response getUserRoles() {
    Response response = new Response();
    List<Map<String, Object>> roleMapList = new ArrayList<>();
    List<Role> roleList = roleDao.getRoles();

    if (CollectionUtils.isNotEmpty(roleList)) {

      for (Role role : roleList) {
        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put(JsonKey.ID, role.getId());
        roleMap.put(JsonKey.NAME, role.getName());
        List<String> roleGroupIdList = role.getRoleGroupId();

        List<Map<String, Object>> actionGroupMapList = new ArrayList<>();
        roleMap.put(JsonKey.ACTION_GROUPS, actionGroupMapList);

        Map<String, Object> actionGroupMap = null;
        for (String roleGroupId : roleGroupIdList) {
          Map<String, Object> roleGroupMap = RoleGroupService.getRoleGroupMap(roleGroupId);

          actionGroupMap = new HashMap<>();
          actionGroupMap.put(JsonKey.ID, roleGroupMap.get(JsonKey.ID));
          actionGroupMap.put(JsonKey.NAME, roleGroupMap.get(JsonKey.NAME));

          List<Map<String, Object>> urlActionMapList = new ArrayList<>();
          List<String> urlActionIds = (List) roleGroupMap.get(JsonKey.URL_ACTION_ID);

          for (String urlActionId : urlActionIds) {
            urlActionMapList.add(UrlActionService.getUrlActionMap(urlActionId));
          }

          if (actionGroupMap.containsKey(JsonKey.ACTIONS)) {
            List<Map<String, Object>> actionsMap =
                (List<Map<String, Object>>) actionGroupMap.get(JsonKey.ACTIONS);
            actionsMap.addAll(urlActionMapList);
          } else {
            actionGroupMap.put(JsonKey.ACTIONS, urlActionMapList);
          }

          actionGroupMapList.add(actionGroupMap);
        }

        roleMapList.add(roleMap);
      }
    }

    response.getResult().put(JsonKey.ROLES, roleMapList);
    return response;
  }

  public static void validateRoles(List<String> roleList) {
    Map<String, Object> roleMap = DataCacheHandler.getRoleMap();

    if (MapUtils.isNotEmpty(roleMap)) {
      for (String role : roleList) {
        if (null == roleMap.get(role.trim())) {
          throw new ProjectCommonException(
              ResponseCode.invalidRole.getErrorCode(),
              ResponseCode.invalidRole.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }
  }
}
