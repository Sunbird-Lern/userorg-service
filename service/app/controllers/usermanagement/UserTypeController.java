package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class UserTypeController extends BaseController {

  public CompletionStage<Result> getUserTypes(Http.Request httpRequest) {
    return handleRequest(ActorOperations.GET_USER_TYPES.getValue(), httpRequest);
  }
}
