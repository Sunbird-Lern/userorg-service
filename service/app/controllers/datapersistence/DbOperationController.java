package controllers.datapersistence;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class DbOperationController extends BaseController {

  /**
   * This method will allow create Data in DB.
   *
   * @return Promise<Result>
   */
  public Promise<Result> create() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: create called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.CREATE_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow update Data in DB.
   *
   * @return Promise<Result>
   */
  public Promise<Result> update() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: update called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.UPDATE_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to delete Data in DB.
   *
   * @return Promise<Result>
   */
  public Promise<Result> delete() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: delete called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.DELETE_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to read Data from DB/ES.
   *
   * @return Promise<Result>
   */
  public Promise<Result> read() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: read called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.READ_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to read all Data from DB/ES.
   *
   * @return Promise<Result>
   */
  public Promise<Result> readAll() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: readAll called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.READ_ALL_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to search Data from ES.
   *
   * @return Promise<Result>
   */
  public Promise<Result> search() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: search called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.SEARCH_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to get data from ElasticSearch based on query
   *
   * @return ES Response
   */
  public Promise<Result> getMetrics() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("DbOperationController: getMetrics called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.GET_METRICS.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
