package controllers.usermanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

public class UserTypeController extends BaseController {

  @Inject
  @Named("user_type_actor")
  private ActorRef userTypeActor;

  public CompletionStage<Result> getUserTypes(Http.Request httpRequest) {
    return handleRequest(userTypeActor, ActorOperations.GET_USER_TYPES.getValue(), httpRequest);
  }
}
