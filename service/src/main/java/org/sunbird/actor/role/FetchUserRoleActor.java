package org.sunbird.actor.role;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;

public class FetchUserRoleActor extends BaseActor {

  private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private final UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "getUserRolesById":
        getUserRolesById(request);
        break;

      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void getUserRolesById(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    String userId = (String) requestMap.get(JsonKey.USER_ID);
    List<Map<String, Object>> userRoles =
        userRoleService.getUserRoles(userId, request.getRequestContext());
    Response response = new Response();
    if (CollectionUtils.isNotEmpty(userRoles)) {
      userRoles
          .stream()
          .forEach(
              role -> {
                role.remove(JsonKey.CREATED_BY);
                role.remove(JsonKey.CREATED_DATE);
                role.remove(JsonKey.UPDATED_BY);
                role.remove(JsonKey.UPDATED_DATE);
              });
      Map<String, Object> user =
          userService.getUserDetailsById(userId, request.getRequestContext());
      if (MapUtils.isNotEmpty(user)) {
        String name = (String) user.get(JsonKey.FIRST_NAME);
        if (StringUtils.isNotEmpty((String) user.get(JsonKey.LAST_NAME))) {
          name = name + " " + user.get(JsonKey.LAST_NAME);
        }
        response.put(JsonKey.NAME, name);
      }
    }
    response.put(JsonKey.ROLES, userRoles);
    sender().tell(response, self());
  }
}
