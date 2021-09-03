package org.sunbird.actor.role;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;

@ActorConfig(
  tasks = {"getUserRolesById"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class FetchUserRoleActor extends BaseActor {

  private UserRoleService userRoleService = UserRoleServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();

    switch (operation) {
      case "getUserRolesById":
        getUserRolesById(request);
        break;

      default:
        onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void getUserRolesById(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    List<Map<String, Object>> userRoles =
        userRoleService.getUserRoles(
            (String) requestMap.get(JsonKey.USER_ID), request.getRequestContext());
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
    }
    response.put(JsonKey.ROLES, userRoles);
    sender().tell(response, self());
  }
}
