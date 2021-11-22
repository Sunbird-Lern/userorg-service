package org.sunbird.actor.role;

import akka.actor.ActorRef;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.actor.user.UserBaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.role.RoleService;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;

public class UserRoleActor extends UserBaseActor {

  private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();

  @Inject
  @Named("user_role_background_actor")
  private ActorRef userRoleBackgroundActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();

    switch (operation) {
      case "getRoles":
        getRoles(request.getRequestContext());
        break;

      case "assignRoles":
      case "assignRolesV2":
        assignRoles(request);
        break;

      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void getRoles(RequestContext context) {
    Response response = DataCacheHandler.getRoleResponse();
    if (response == null) {
      response = new RoleService().getUserRoles(context);
      DataCacheHandler.setRoleResponse(response);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void assignRoles(Request actorMessage) {
    List<Map<String, Object>> userRolesList;

    Map<String, Object> requestMap = actorMessage.getRequest();
    requestMap.put(JsonKey.REQUESTED_BY, actorMessage.getContext().get(JsonKey.USER_ID));

    if (actorMessage.getOperation().equals(ActorOperations.ASSIGN_ROLES.getValue())) {
      requestMap.put(JsonKey.ROLE_OPERATION, "assignRole");
      List<String> roles = (List<String>) requestMap.get(JsonKey.ROLES);
      RoleService.validateRoles(roles);
      userRolesList = userRoleService.updateUserRole(requestMap, actorMessage.getRequestContext());
    } else {
      List<Map<String, Object>> roleList =
          (List<Map<String, Object>>) requestMap.get(JsonKey.ROLES);
      RoleService.validateRolesV2(roleList);
      userRolesList =
          userRoleService.updateUserRoleV2(requestMap, actorMessage.getRequestContext());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);

    sender().tell(response, self());
    syncUserRoles(
        JsonKey.USER,
        (String) requestMap.get(JsonKey.USER_ID),
        userRolesList,
        actorMessage.getRequestContext());
    generateTelemetryEvent(
        requestMap,
        (String) requestMap.get(JsonKey.USER_ID),
        "userLevel",
        actorMessage.getContext());
  }

  private void syncUserRoles(
      String type, String userId, List<Map<String, Object>> userRolesList, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    request.getRequest().put(JsonKey.TYPE, type);
    request.getRequest().put(JsonKey.USER_ID, userId);
    request.getRequest().put(JsonKey.ROLES, userRolesList);
    logger.debug(context, "UserRoleActor:syncUserRoles: Syncing to ES");
    try {
      userRoleBackgroundActor.tell(request, self());
    } catch (Exception ex) {
      logger.error(
          context,
          "UserRoleActor:syncUserRoles: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
  }
}
