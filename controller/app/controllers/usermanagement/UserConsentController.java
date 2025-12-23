package controllers.usermanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import controllers.usermanagement.validator.UserConsentRequestValidator;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class UserConsentController extends BaseController {

  @Inject
  @Named("user_consent_actor")
  private ActorRef userConsentActor;

  public CompletionStage<Result> updateUserConsent(Http.Request httpRequest) {
    return handleRequest(
        userConsentActor,
        ActorOperations.UPDATE_USER_CONSENT.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserConsentRequestValidator().validateUpdateConsentRequest((Request) request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> getUserConsent(Http.Request httpRequest) {
    return handleRequest(
        userConsentActor,
        ActorOperations.GET_USER_CONSENT.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserConsentRequestValidator().validateReadConsentRequest((Request) request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
