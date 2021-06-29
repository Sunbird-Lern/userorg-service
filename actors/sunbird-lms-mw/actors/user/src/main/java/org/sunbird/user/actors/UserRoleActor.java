package org.sunbird.user.actors;

import java.util.*;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.role.service.RoleService;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.user.service.UserRoleService;
import org.sunbird.user.service.impl.UserRoleServiceImpl;

@ActorConfig(
  tasks = {"getRoles", "assignRoles", "assignRolesV2"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class UserRoleActor extends UserBaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private OrgService orgService = OrgServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();

    switch (operation) {
      case "getRoles":
        getRoles();
        break;

      case "assignRoles":
        assignRoles(request);
        break;

      case "assignRolesV2":
        assignRolesV2(request);
        break;

      default:
        onReceiveUnsupportedOperation("UserRoleActor");
    }
  }

  private void getRoles() {
    logger.info("UserRoleActor: getRoles called");
    Response response = DataCacheHandler.getRoleResponse();
    if (response == null) {
      response = RoleService.getUserRoles();
      DataCacheHandler.setRoleResponse(response);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void assignRoles(Request actorMessage) {
    logger.info(actorMessage.getRequestContext(), "UserRoleActor: assignRoles called");
    Response response = new Response();
    Map<String, Object> requestMap = actorMessage.getRequest();
    requestMap.put(JsonKey.REQUESTED_BY, actorMessage.getContext().get(JsonKey.USER_ID));
    requestMap.put(JsonKey.ROLE_OPERATION, "assignRole");
    List<String> roles = (List<String>) requestMap.get(JsonKey.ROLES);
    RoleService.validateRoles(roles);

    UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
    List<Map<String, Object>> userRolesList =
        userRoleService.updateUserRole(requestMap, actorMessage.getRequestContext());
    if (!userRolesList.isEmpty()) {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      syncUserRoles(
          JsonKey.USER,
          (String) requestMap.get(JsonKey.USER_ID),
          userRolesList,
          actorMessage.getRequestContext());
    } else {
      logger.info(actorMessage.getRequestContext(), "UserRoleActor: No ES call to save user roles");
      throw new ProjectCommonException(
          ResponseCode.roleSaveError.getErrorCode(),
          ResponseCode.roleSaveError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    generateTelemetryEvent(
        requestMap,
        (String) requestMap.get(JsonKey.USER_ID),
        "userLevel",
        actorMessage.getContext());
  }

  private void assignRolesV2(Request actorMessage) {
    logger.info(actorMessage.getRequestContext(), "UserRoleActor: assignRolesV2 called");
    Response response = new Response();
    Map<String, Object> requestMap = actorMessage.getRequest();
    requestMap.put(JsonKey.REQUESTED_BY, actorMessage.getContext().get(JsonKey.USER_ID));
    RoleService.validateRolesV2(requestMap);

    UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
    List<Map<String, Object>> userRolesList =
        userRoleService.updateUserRoleV2(requestMap, actorMessage.getRequestContext());
    if (!userRolesList.isEmpty()) {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      syncUserRoles(
          JsonKey.USER,
          (String) requestMap.get(JsonKey.USER_ID),
          userRolesList,
          actorMessage.getRequestContext());
    } else {
      logger.info(actorMessage.getRequestContext(), "UserRoleActor: No ES call to save user roles");
      throw new ProjectCommonException(
          ResponseCode.roleSaveError.getErrorCode(),
          ResponseCode.roleSaveError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
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
    logger.info(context, "UserRoleActor:syncUserRoles: Syncing to ES");
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      logger.error(
          context,
          "UserRoleActor:syncUserRoles: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
  }
}
