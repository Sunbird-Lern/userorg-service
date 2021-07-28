package controllers.usermanagement;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

public class UserTypeController extends BaseController {

  public CompletionStage<Result> getUserTypes(Http.Request httpRequest) {
    return handleRequest(ActorOperations.GET_USER_TYPES.getValue(), httpRequest);
  }
}
