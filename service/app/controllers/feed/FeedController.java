package controllers.feed;

import controllers.BaseController;
import controllers.feed.validator.FeedRequestValidator;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class FeedController extends BaseController {

  public CompletionStage<Result> getUserFeed(String userId, Http.Request httpRequest) {
    String callerId = Common.getFromRequest(httpRequest, Attrs.USER_ID);

    return handleRequest(
        ActorOperations.GET_USER_FEED_BY_ID.getValue(),
        null,
        req -> {
          FeedRequestValidator.userIdValidation(callerId, userId);
          return null;
        },
        userId,
        JsonKey.USER_ID,
        false,
        httpRequest);
  }
}
