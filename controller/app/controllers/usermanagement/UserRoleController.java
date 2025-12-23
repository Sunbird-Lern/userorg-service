package controllers.usermanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import controllers.usermanagement.validator.UserRoleRequestValidator;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class UserRoleController extends BaseController {

  @Inject
  @Named("user_role_actor")
  private ActorRef userRoleActor;

  @Inject
  @Named("fetch_user_role_actor")
  private ActorRef fetchUserRoleActor;

  public CompletionStage<Result> getRoles(Http.Request httpRequest) {
    return handleRequest(userRoleActor, ActorOperations.GET_ROLES.getValue(), httpRequest);
  }

  public CompletionStage<Result> assignRoles(Http.Request httpRequest) {
    return handleAssignRoleRequest(ActorOperations.ASSIGN_ROLES.getValue(), httpRequest);
  }

  public CompletionStage<Result> assignRolesV2(Http.Request httpRequest) {
    return handleAssignRoleRequest(ActorOperations.ASSIGN_ROLES_V2.getValue(), httpRequest);
  }

  public CompletionStage<Result> getUserRolesById(String userId, Http.Request httpRequest) {
    String usrId = ProjectUtil.getLmsUserId(userId);
    return handleRequest(
        fetchUserRoleActor,
        ActorOperations.GET_USER_ROLES_BY_ID.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.USER_ID, usrId);
          request.getRequest().put(JsonKey.USER_ID, usrId);
          request.setTimeout(3);
          return null;
        },
        usrId,
        JsonKey.USER_ID,
        false,
        httpRequest);
  }

  private CompletionStage<Result> handleAssignRoleRequest(
      String operation, Http.Request httpRequest) {
    final boolean isPrivate = httpRequest.path().contains(JsonKey.PRIVATE) ? true : false;
    return handleRequest(
        userRoleActor,
        operation,
        httpRequest.body().asJson(),
        (request) -> {
          Request req = (Request) request;
          req.getContext().put(JsonKey.USER_ID, Common.getFromRequest(httpRequest, Attrs.USER_ID));
          req.getContext().put(JsonKey.PRIVATE, isPrivate);
          if (operation.equals(ActorOperations.ASSIGN_ROLES.getValue())) {
            new UserRoleRequestValidator().validateAssignRolesRequest(req);
          } else {
            new UserRoleRequestValidator().validateAssignRolesRequestV2(req);
          }
          return null;
        },
        httpRequest);
  }
}
