package controllers.feed;

import controllers.BaseController;
import controllers.feed.validator.FeedRequestValidator;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class FeedController extends BaseController {

  public CompletionStage<Result> getUserFeed(String userId, Http.Request httpRequest) {
    String callerId1 = Common.getFromRequest(httpRequest, Attrs.USER_ID);
    String callerId2 = Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR);
    return handleRequest(
        ActorOperations.GET_USER_FEED_BY_ID.getValue(),
        null,
        req -> {
          FeedRequestValidator.userIdValidation(callerId1, callerId2, userId);
          return null;
        },
        userId,
        JsonKey.USER_ID,
        false,
        httpRequest);
  }

  public CompletionStage<Result> createUserFeed(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_USER_FEED.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          FeedRequestValidator.validateFeedRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> deleteUserFeed(Http.Request httpRequest) {
    String callerId1 = Common.getFromRequest(httpRequest, Attrs.USER_ID);
    String callerId2 = Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR);
    return handleRequest(
        ActorOperations.DELETE_USER_FEED.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          FeedRequestValidator.validateFeedDeleteRequest(request, callerId1, callerId2);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> updateUserFeed(Http.Request httpRequest) {
    String callerId1 = Common.getFromRequest(httpRequest, Attrs.USER_ID);
    String callerId2 = Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR);
    return handleRequest(
        ActorOperations.UPDATE_USER_FEED.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          FeedRequestValidator.validateFeedUpdateRequest(request, callerId1, callerId2);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
