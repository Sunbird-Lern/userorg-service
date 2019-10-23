package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserLoginRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class UserLoginController extends BaseController {
  /**
   * Updates current login time for given user in Keycloak.
   *
   * @return Return a promise for update login time API result.
   */
  public CompletionStage<Result> updateLoginTime(Http.Request httpRequest) {

    return handleRequest(
        ActorOperations.USER_CURRENT_LOGIN.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new UserLoginRequestValidator().validateUpdateLoginTimeRequest((Request) request);
          return null;
        }, httpRequest);
  }
}
