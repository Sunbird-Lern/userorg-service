/** */
package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.badging.validator.BadgeAssertionValidator;
import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;

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
   * @return Promise<Result>
   */
  public Promise<Result> issueBadge() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" Issue badge method called = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      BadgeAssertionValidator.validateBadgeAssertion(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgingActorOperations.CREATE_BADGE_ASSERTION.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This controller will provide assertion details.based on assertionSlug.
   *
   * @param badgeSlug String
   * @param assertionSlug
   * @return Promise<Result>
   */
  public Promise<Result> getAssertionDetails(String assertionId) {
    try {
      ProjectLogger.log(
          " get badge assertion details api called = " + assertionId, LoggerEnum.DEBUG.name());
      Request reqObj = new Request();
      reqObj.getRequest().put(BadgingJsonKey.ASSERTION_ID, assertionId);
      BadgeAssertionValidator.validategetBadgeAssertion(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgingActorOperations.GET_BADGE_ASSERTION.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This controller will provide list of assertions based on issuerSlug,badgeSlug and assertionSlug
   *
   * @return Promise<Result>
   */
  public Promise<Result> getAssertionList() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get assertion list api called = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      BadgeAssertionValidator.validateGetAssertionList(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgingActorOperations.GET_BADGE_ASSERTION_LIST.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This controller will revoke user assertion based on issuerSlug, badgeSlug and assertionSlug
   *
   * @param badgeSlug String
   * @param assertionSlug
   * @return Promise<Result>
   */
  @BodyParser.Of(BodyParser.Json.class)
  public Promise<Result> revokeAssertion() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" Revoke badge method called = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      BadgeAssertionValidator.validateRevokeAssertion(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgingActorOperations.REVOKE_BADGE.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
