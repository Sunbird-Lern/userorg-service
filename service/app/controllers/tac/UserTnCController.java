package controllers.tac;

import controllers.BaseController;
import controllers.tac.validator.UserTnCRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserTnCController extends BaseController {

  public Promise<Result> acceptTnC() {
    return handleRequest(
        ActorOperations.USER_TNC_ACCEPT.getValue(),
        request().body().asJson(),
        (request) -> {
          new UserTnCRequestValidator().validateTnCRequest((Request) request);
          return null;
        });
  }

}
