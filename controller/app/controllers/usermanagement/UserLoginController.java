package controllers.usermanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

public class UserLoginController extends BaseController {

  @Inject
  @Named("user_login_actor")
  private ActorRef userLoginActor;

  /**
   * Updates current login time for given user in Keycloak.
   *
   * @return Return a promise for update login time API result.
   */
  public CompletionStage<Result> updateLoginTime(Http.Request httpRequest) {

    return handleRequest(
        userLoginActor,
        ActorOperations.USER_CURRENT_LOGIN.getValue(),
        httpRequest.body().asJson(),
        request -> null,
        httpRequest);
  }
}
