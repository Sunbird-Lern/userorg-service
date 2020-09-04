/** */
package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.badging.validator.BadgeAssertionValidator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/**
 * This controller will handle all api related to badge assertions. issue badge, revoke badge,get
 * badge instance etc.
 *
 * @author Manzarul
 */
public class BadgeAssertionController extends BaseController {

  /**
   * This method will be called to issue a badge either on content or user. Request Param
   * {"issuerSlug":"unique String","badgeSlug" : "unique string",
   * "recipientEmail":"email","evidence":"url , basically it is badge class public url"
   * ,"notify":boolean}
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> issueBadge(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log(" Issue badge method called = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      BadgeAssertionValidator.validateBadgeAssertion(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              Common.getFromRequest(httpRequest, Attrs.REQUEST_ID),
              BadgingActorOperations.CREATE_BADGE_ASSERTION.getValue(),
              Common.getFromRequest(httpRequest, Attrs.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This controller will provide assertion details.based on assertionSlug.
   *
   * @param assertionId
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getAssertionDetails(String assertionId, Http.Request httpRequest) {
    try {
      ProjectLogger.log(
          " get badge assertion details api called = " + assertionId, LoggerEnum.DEBUG.name());
      Request reqObj = new Request();
      reqObj.getRequest().put(BadgingJsonKey.ASSERTION_ID, assertionId);
      BadgeAssertionValidator.validategetBadgeAssertion(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              Common.getFromRequest(httpRequest, Attrs.REQUEST_ID),
              BadgingActorOperations.GET_BADGE_ASSERTION.getValue(),
              Common.getFromRequest(httpRequest, Attrs.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This controller will provide list of assertions based on issuerSlug,badgeSlug and assertionSlug
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getAssertionList(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log(" get assertion list api called = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      BadgeAssertionValidator.validateGetAssertionList(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              Common.getFromRequest(httpRequest, Attrs.REQUEST_ID),
              BadgingActorOperations.GET_BADGE_ASSERTION_LIST.getValue(),
              Common.getFromRequest(httpRequest, Attrs.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This controller will revoke user assertion based on issuerSlug, badgeSlug and assertionSlug
   *
   * @return CompletionStage<Result>
   */
  @BodyParser.Of(BodyParser.Json.class)
  public CompletionStage<Result> revokeAssertion(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log(" Revoke badge method called = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      BadgeAssertionValidator.validateRevokeAssertion(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              Common.getFromRequest(httpRequest, Attrs.REQUEST_ID),
              BadgingActorOperations.REVOKE_BADGE.getValue(),
              Common.getFromRequest(httpRequest, Attrs.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
