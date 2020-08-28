package controllers.geolocation;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/** Created by arvind on 31/10/17. */
public class GeoLocationController extends BaseController {

  public CompletionStage<Result> createGeoLocation(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("create geo location" + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.CREATE_GEO_LOCATION.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> getGeoLocation(String id, Http.Request httpRequest) {
    try {
      ProjectLogger.log("get geo location by id ");
      String type = httpRequest.getQueryString(JsonKey.TYPE);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_GEO_LOCATION.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      innerMap.put(JsonKey.TYPE, type);
      innerMap.put(JsonKey.ID, id);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> updateGeoLocation(String locationId, Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("update geo location" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.UPDATE_GEO_LOCATION.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      innerMap.put(JsonKey.LOCATION_ID, locationId);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> deleteGeoLocation(String locationId, Http.Request httpRequest) {
    try {
      ProjectLogger.log("delete geo location");
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.DELETE_GEO_LOCATION.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      innerMap.put(JsonKey.LOCATION_ID, locationId);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> sendNotification(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("createGeoLocation:sendNotification called", LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSendNotification(reqObj);
      reqObj.setOperation(ActorOperations.SEND_NOTIFICATION.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> getUserCount(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("get User Count api call" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateGetUserCount(reqObj);
      reqObj.setOperation(ActorOperations.GET_USER_COUNT.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
