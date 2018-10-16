package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserRoleRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserRoleController extends BaseController {

  public Promise<Result> getRoles() {
    return handleRequest(ActorOperations.GET_ROLES.getValue());
  }

  public Promise<Result> assignRoles() {
    return handleRequest(
        ActorOperations.ASSIGN_ROLES.getValue(),
        request().body().asJson(),
        request -> {
          new UserRoleRequestValidator().validateAssignRolesRequest((Request) request);
          return null;
        });
  }
  
}
