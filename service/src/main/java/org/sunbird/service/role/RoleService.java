package org.sunbird.service.role;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.dao.role.RoleDao;
import org.sunbird.dao.role.impl.RoleDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.role.Role;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.urlaction.UrlActionService;
import org.sunbird.util.DataCacheHandler;

public class RoleService {

  private final RoleDao roleDao = RoleDaoImpl.getInstance();
  private final RoleGroupService roleGroupService = new RoleGroupService();

  public Response getUserRoles(RequestContext context) {
    Response response = new Response();
    List<Map<String, Object>> roleMapList = new ArrayList<>();
    List<Role> roleList = roleDao.getRoles(context);

    if (CollectionUtils.isNotEmpty(roleList)) {

      for (Role role : roleList) {
        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put(JsonKey.ID, role.getId());
        roleMap.put(JsonKey.NAME, role.getName());
        List<String> roleGroupIdList = role.getRoleGroupId();

        List<Map<String, Object>> actionGroupMapList = new ArrayList<>();
        roleMap.put(JsonKey.ACTION_GROUPS, actionGroupMapList);

        Map<String, Object> actionGroupMap;
        for (String roleGroupId : roleGroupIdList) {
          Map<String, Object> roleGroupMap = roleGroupService.getRoleGroupMap(roleGroupId, context);

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

    if (MapUtils.isNotEmpty(roleMap) && roleList != null) {
      roleList.forEach(
          roleObj -> {
            if (null == roleMap.get(roleObj.trim())) {
              throw new ProjectCommonException(
                  ResponseCode.invalidParameter,
                  MessageFormat.format(
                      ResponseCode.invalidParameter.getErrorMessage(), JsonKey.ROLE),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
          });
    }
  }

  public static void validateRolesV2(List<Map<String, Object>> roleList) {
    Map<String, Object> roleMap = DataCacheHandler.getRoleMap();

    if (MapUtils.isNotEmpty(roleMap) && roleList != null) {
      roleList.forEach(
          roleObj -> {
            String roleStr = (String) roleObj.get(JsonKey.ROLE);
            String operation = (String) roleObj.get(JsonKey.OPERATION);
            // For remove operation don't validate the role
            if (null == roleMap.get(roleStr.trim())
                && !JsonKey.REMOVE.equalsIgnoreCase(operation)) {
              throw new ProjectCommonException(
                  ResponseCode.invalidParameter,
                  MessageFormat.format(
                      ResponseCode.invalidParameter.getErrorMessage(), JsonKey.ROLE),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
          });
    }
  }
}
