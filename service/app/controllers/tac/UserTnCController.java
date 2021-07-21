package controllers.tac;

import controllers.BaseController;
import controllers.tac.validator.UserTnCRequestValidator;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class UserTnCController extends BaseController {

  public CompletionStage<Result> acceptTnC(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.USER_TNC_ACCEPT.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new UserTnCRequestValidator().validateTnCRequest((Request) request);
          return null;
        },
        httpRequest);
  }
}
