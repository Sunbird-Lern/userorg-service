package controllers.usermanagement;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.JsonKey;
import play.mvc.Http;
import play.mvc.Result;

public class UserFeedController extends BaseController {

  public CompletionStage<Result> getUserFeedById(String userId, Http.Request httpRequest) {
    return handleRequest(
        "getUserFeedById", null, null, userId, JsonKey.USER_ID, false, httpRequest);
  }
}
