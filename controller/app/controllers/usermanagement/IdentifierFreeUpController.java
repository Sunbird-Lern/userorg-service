package controllers.usermanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.UserFreeUpRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

/**
 * this action method is responsible to free Up the user account attributes.
 *
 * @author anmolgupta
 */
public class IdentifierFreeUpController extends BaseController {

  @Inject
  @Named("identifier_free_up_actor")
  private ActorRef identifierFreeUpActor;

  /**
   * this action method will be used to free Up user Identifier from user DB
   *
   * @return
   */
  public CompletionStage<Result> freeUpIdentifier(Http.Request httpRequest) {
    return handleRequest(
        identifierFreeUpActor,
        ActorOperations.FREEUP_USER_IDENTITY.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          UserFreeUpRequestValidator.getInstance(request).validate();
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
