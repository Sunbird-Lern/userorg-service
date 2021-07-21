package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserConsentRequestValidator;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class UserConsentController extends BaseController {

  public CompletionStage<Result> updateUserConsent(Http.Request httpRequest) {
    return handleRequest(
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
