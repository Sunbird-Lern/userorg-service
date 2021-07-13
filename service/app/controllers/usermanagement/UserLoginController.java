package controllers.usermanagement;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

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
        request -> null,
        httpRequest);
  }
}
