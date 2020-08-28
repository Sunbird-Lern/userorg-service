package controllers.datapersistence;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.libs.typedmap.TypedKey;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class DbOperationController extends BaseController {

  /**
   * This method will allow create Data in DB.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> create(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: create called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.CREATE_DATA.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will allow update Data in DB.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> update(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: update called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.UPDATE_DATA.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will allow to delete Data in DB.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> delete(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: delete called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.DELETE_DATA.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will allow to read Data from DB/ES.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> read(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: read called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.READ_DATA.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will allow to read all Data from DB/ES.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> readAll(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: readAll called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.READ_ALL_DATA.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will allow to search Data from ES.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> search(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: search called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.SEARCH_DATA.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Method to get data from ElasticSearch based on query
   *
   * @return ES Response
   */
  public CompletionStage<Result> getMetrics(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("DbOperationController: getMetrics called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.GET_METRICS.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
