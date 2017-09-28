/**
 * 
 */
package controllers.badges;

import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;

import com.fasterxml.jackson.databind.JsonNode;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all api related to badges.
 * @author Manzarul
 *
 */
public class BadgesController  extends BaseController{

  /**
   * This method will provide all badges master data.
   * @return Promise<Result>
   */
  public Promise<Result> getBadges() {
    try {
      ProjectLogger.log("Call to get badges master data. = " , LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ALL_BADGE.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add badges to user profile.
   * @return Promise<Result>
   */
  public Promise<Result> addUserBadges() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("call to add user badges api." + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateAddUserBadge(reqObj);
      reqObj.setOperation(ActorOperations.ADD_USER_BADGE.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
