package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserLoginRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserLoginController extends BaseController {
  /**
   * Updates current login time for given user in Keycloak.
   *
   * @return Return a promise for update login time API result.
   */
  public Promise<Result> updateLoginTime() {

    return handleRequest(
        ActorOperations.USER_CURRENT_LOGIN.getValue(),
        request().body().asJson(),
        request -> {
          new UserLoginRequestValidator().validateUpdateLoginTimeRequest((Request) request);
          return null;
        });
  }
}
