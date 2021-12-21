package controllers.feed;

import akka.actor.ActorRef;
import controllers.BaseController;
import controllers.feed.validator.FeedRequestValidator;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class FeedController extends BaseController {

  @Inject
  @Named("user_feed_actor")
  private ActorRef userFeedActor;

  public CompletionStage<Result> getUserFeed(String userId, Http.Request httpRequest) {
    // Read userId from auth token, ignore the request
    String managedForTokenUserId = Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR);
    String usrId = Common.getFromRequest(httpRequest, Attrs.USER_ID);
    if (StringUtils.isNotBlank(managedForTokenUserId)) {
      usrId = managedForTokenUserId;
    }
    return handleRequest(
        userFeedActor,
        ActorOperations.GET_USER_FEED_BY_ID.getValue(),
        null,
        req -> null,
        usrId,
        JsonKey.USER_ID,
        false,
        httpRequest);
  }

  public CompletionStage<Result> createUserFeed(Http.Request httpRequest) {
    return handleRequest(
        userFeedActor,
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
    return handleRequest(
        userFeedActor,
        ActorOperations.DELETE_USER_FEED.getValue(),
        httpRequest.body().asJson(),
        req -> {
          // Read userId from auth token, ignore the request
          String managedForTokenUserId = Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR);
          String userId = Common.getFromRequest(httpRequest, Attrs.USER_ID);
          if (StringUtils.isNotBlank(managedForTokenUserId)) {
            userId = managedForTokenUserId;
          }
          Request request = (Request) req;
          request.getRequest().put(JsonKey.USER_ID, userId);
          FeedRequestValidator.validateFeedDeleteRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> updateUserFeed(Http.Request httpRequest) {
    return handleRequest(
        userFeedActor,
        ActorOperations.UPDATE_USER_FEED.getValue(),
        httpRequest.body().asJson(),
        req -> {
          // Read userId from auth token, ignore the request
          String managedForTokenUserId = Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR);
          String userId = Common.getFromRequest(httpRequest, Attrs.USER_ID);
          if (StringUtils.isNotBlank(managedForTokenUserId)) {
            userId = managedForTokenUserId;
          }
          Request request = (Request) req;
          request.getRequest().put(JsonKey.USER_ID, userId);
          FeedRequestValidator.validateFeedUpdateRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
