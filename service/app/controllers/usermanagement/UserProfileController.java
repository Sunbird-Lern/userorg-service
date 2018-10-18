package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserProfileRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserProfileController extends BaseController {

  public Promise<Result> getMediaTypes() {
    return handleRequest(ActorOperations.GET_MEDIA_TYPES.getValue(), null, null, null, null, false);
  }

  public Promise<Result> setProfileVisibility() {
    return handleRequest(
        ActorOperations.PROFILE_VISIBILITY.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          new UserProfileRequestValidator().validateProfileVisibility(request);
          return null;
        },
        null,
        null,
        true);
  }

}
