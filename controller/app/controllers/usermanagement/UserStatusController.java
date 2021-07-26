package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserStatusRequestValidator;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class UserStatusController extends BaseController {

  public CompletionStage<Result> blockUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.BLOCK_USER.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateBlockUserRequest((Request) request);
          return null;
        },
        httpRequest);
  }

  public CompletionStage<Result> unblockUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.UNBLOCK_USER.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateUnblockUserRequest((Request) request);
          return null;
        },
        httpRequest);
  }
}
