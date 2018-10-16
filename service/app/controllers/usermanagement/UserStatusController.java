package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserStatusRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserStatusController extends BaseController {

  public Promise<Result> blockUser() {
    return handleRequest(
        ActorOperations.BLOCK_USER.getValue(),
        request().body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateBlockUserRequest((Request) request);
          return null;
        });
  }

  public Promise<Result> unblockUser() {
    return handleRequest(
        ActorOperations.UNBLOCK_USER.getValue(),
        request().body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateUnblockUserRequest((Request) request);
          return null;
        });
  }
  
}
