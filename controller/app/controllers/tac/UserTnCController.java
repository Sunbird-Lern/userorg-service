package controllers.tac;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import controllers.tac.validator.UserTnCRequestValidator;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class UserTnCController extends BaseController {

  @Inject
  @Named("user_tnc_actor")
  private ActorRef userTnCActor;

  public CompletionStage<Result> acceptTnC(Http.Request httpRequest) {
    return handleRequest(
        userTnCActor,
        ActorOperations.USER_TNC_ACCEPT.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new UserTnCRequestValidator().validateTnCRequest((Request) request);
          return null;
        },
        httpRequest);
  }
}
