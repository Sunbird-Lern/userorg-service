package controllers.usermanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import controllers.usermanagement.validator.UserStatusRequestValidator;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class UserStatusController extends BaseController {

  @Inject
  @Named("user_status_actor")
  private ActorRef userStatusActor;

  public CompletionStage<Result> blockUser(Http.Request httpRequest) {
    return handleRequest(
        userStatusActor,
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
        userStatusActor,
        ActorOperations.UNBLOCK_USER.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new UserStatusRequestValidator().validateUnblockUserRequest((Request) request);
          return null;
        },
        httpRequest);
  }

  public CompletionStage<Result> deleteUser(Http.Request httpRequest) {
    return handleRequest(
        userStatusActor,
        ActorOperations.DELETE_USER.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new UserStatusRequestValidator()
              .validateUserId(Common.getFromRequest(httpRequest, Attrs.USER_ID));
          return null;
        },
        httpRequest);
  }
}
