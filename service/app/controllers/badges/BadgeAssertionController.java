/**
 * 
 */
package controllers.badges;

import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.badge.BadgeAssertionValidator;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all api related to badge assertions.
 * issue badge, revoke badge,get badge instance etc.
 * @author Manzarul
 *
 */
public class BadgeAssertionController extends BaseController {

	/**
	 * This method will be called to issue a badge either on content or user.
	 * Request Param {"issuerSlug":"unique String","badgeSlug" : "unique string",
	 * "recipientEmail":"email","evidence":"url , basically it is badge class public
	 * url" ,"notify":boolean}
	 * 
	 * @return Promise<Result>
	 */
	public Promise<Result> issueBadge() {
		try {
			JsonNode requestData = request().body().asJson();
			ProjectLogger.log(" Issue badge method called = " + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			BadgeAssertionValidator.validateBadgeAssertion(reqObj);
			reqObj.setOperation(BadgingActorOperations.CREATE_BADGE_ASSERTION.getValue());
			reqObj.setRequestId(ExecutionContext.getRequestId());
			reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
			reqObj.setEnv(getEnvironment());
			return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
		} catch (Exception e) {
			return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
		}
	}

	/**
	 * This controller will provide assertion details.based on issuerSlug, badgeSlug and
	 * assertionSlug.
	 * @param badgeSlug
	 *            String
	 * @param assertionSlug
	 * @return Promise<Result>
	 */
	public Promise<Result> getAssertionDetails() {
		try {
			JsonNode requestData = request().body().asJson();
			ProjectLogger.log(" get badge assertion details api called = " + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			BadgeAssertionValidator.validategetBadgeAssertion(reqObj);
			reqObj.setOperation(BadgingActorOperations.GET_BADGE_ASSERTION.getValue());
			reqObj.setRequestId(ExecutionContext.getRequestId());
			reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
			reqObj.setEnv(getEnvironment());
			return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
		} catch (Exception e) {
			return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
		}
	}

	/**
	 * This controller will provide list of assertions based on issuerSlug,badgeSlug and 
	 * assertionSlug
	 * @return Promise<Result>
	 */
	public Promise<Result> getAssertionList() {
		try {
			JsonNode requestData = request().body().asJson();
			ProjectLogger.log(" Issue badge method called = " + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			reqObj.setOperation(BadgingActorOperations.GET_BADGE_ASSERTION_LIST.getValue());
			reqObj.setRequestId(ExecutionContext.getRequestId());
			reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
			reqObj.setEnv(getEnvironment());
			return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
		} catch (Exception e) {
			return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
		}
	}

	/**
	 * This controller will revoke user assertion based on issuerSlug,
	 * badgeSlug and assertionSlug
	 * @param badgeSlug
	 *            String
	 * @param assertionSlug
	 * @return Promise<Result>
	 */
	public Promise<Result> revokeAssertion() {
		try {
			//BadgeAssertionValidator.validategetBadgeAssertion(issuerSlug, badgeSlug, assertionSlug);
			JsonNode requestData = request().body().asJson();
			ProjectLogger.log(" Issue badge method called = " + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			/*reqObj.getRequest().put(BadgingJsonKey.ISSUER_SLUG, issuerSlug);
			reqObj.getRequest().put(BadgingJsonKey.BADGE_CLASS_SLUG, badgeSlug);
			reqObj.getRequest().put(BadgingJsonKey.ASSERTION_SLUG, assertionSlug);
			reqObj.setOperation(BadgingActorOperations.REVOKE_BADGE.getValue());*/
			reqObj.setRequestId(ExecutionContext.getRequestId());
			reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
			reqObj.setEnv(getEnvironment());
			return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
		} catch (Exception e) {
			return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
		}
	}

}
