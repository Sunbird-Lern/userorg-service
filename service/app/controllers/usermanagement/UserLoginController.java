package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserLoginRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserLoginController extends BaseController {

  /**
   * This method will update user current login time to keyCloack.
   *
   * @return promise<Result>
   */
  public Promise<Result> updateLoginTime() {

    return handleRequest(
        ActorOperations.USER_CURRENT_LOGIN.getValue(),
        request().body().asJson(),
        request -> {
          new UserLoginRequestValidator().validateUpdateLoginTime((Request) request);
          return null;
        });
  }
}
