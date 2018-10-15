package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserStatusRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserStatusController extends BaseController {

  /**
   * This method will facilitate the block user based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> blockUser() {

    return handleRequest(
        ActorOperations.BLOCK_USER.getValue(),
        request().body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateBlockUser((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> unBlockUser() {
    return handleRequest(
        ActorOperations.UNBLOCK_USER.getValue(),
        request().body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateunBlockUser((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }
}
