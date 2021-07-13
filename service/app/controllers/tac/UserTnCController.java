package controllers.tac;

import controllers.BaseController;
import controllers.tac.validator.UserTnCRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

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
