package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserProfileRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class UserProfileController extends BaseController {

  public CompletionStage<Result> getProfileSupportedSocialMediaTypes(Http.Request httpRequest) {
    return handleRequest(ActorOperations.GET_MEDIA_TYPES.getValue(), null, null, null, null, false, httpRequest);
  }

  public CompletionStage<Result> setProfileVisibility(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.PROFILE_VISIBILITY.getValue(),
        httpRequest.body().asJson(),
        (req) -> {
          Request request = (Request) req;
          new UserProfileRequestValidator().validateProfileVisibility(request);
          return null;
        },
        null,
        null,
        true,
            httpRequest);
  }

}
