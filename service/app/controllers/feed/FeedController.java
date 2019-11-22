package controllers.feed;

import controllers.BaseController;
import java.util.*;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import play.mvc.Http;
import play.mvc.Result;

public class FeedController extends BaseController {

  public CompletionStage<Result> feed(String userId, Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.GET_USER_FEED_BY_ID.getValue(),
        null,
        null,
        userId,
        JsonKey.USER_ID,
        false,
        httpRequest);
  }
}
