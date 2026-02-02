package org.sunbird.actor.role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;

public class UserRoleBackgroundActor extends BaseActor {

  private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_ROLES_ES.getValue())) {
      updateUserRoleToEs(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void updateUserRoleToEs(Request actorMessage) {
    List<Map<String, Object>> roles =
        (List<Map<String, Object>>) actorMessage.getRequest().get(JsonKey.ROLES);
    String type = (String) actorMessage.get(JsonKey.TYPE);
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.USER_ID, actorMessage.get(JsonKey.USER_ID));
    if (type.equals(JsonKey.USER)) {
      result.put(JsonKey.ROLES, roles);
    }
    userRoleService.updateUserRoleToES(
        (String) result.get(JsonKey.USER_ID), result, actorMessage.getRequestContext());
  }
}
