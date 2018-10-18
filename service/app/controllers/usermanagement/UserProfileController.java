package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserProfileRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserProfileController extends BaseController {

  /**
   * Get all the social media types supported
   *
   * @return
   */
  public Promise<Result> getMediaTypes() {

    return handleRequest(ActorOperations.GET_MEDIA_TYPES.getValue(), null, null, null, null, false);
  }

  /**
   * This method will add or update user profile visibility control. User can make all field as
   * private except name. any private filed of user is not search-able.
   *
   * @return Promise<Result>
   */
  public Promise<Result> profileVisibility() {

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
