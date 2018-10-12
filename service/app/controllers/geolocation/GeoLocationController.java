package controllers.geolocation;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.geolocation.validator.GeolocationRequestValidator;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/** Created by arvind on 31/10/17. */
public class GeoLocationController extends BaseController {

  public Promise<Result> createGeoLocation() {

    return handleRequest(
        ActorOperations.CREATE_GEO_LOCATION.getValue(),
        request().body().asJson(),
        request -> {
          new GeolocationRequestValidator().validateCreateGeolocationRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> getGeoLocation(String id) {

    try {
      ProjectLogger.log("get geo location by id ");
      String type = request().getQueryString(JsonKey.TYPE);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_GEO_LOCATION.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.TYPE, type);
      innerMap.put(JsonKey.ID, id);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> updateGeoLocation(String locationId) {

    return handleRequest(
        ActorOperations.UPDATE_GEO_LOCATION.getValue(),
        request().body().asJson(),
        request -> {
          new GeolocationRequestValidator().valdiateUpdateGeolocationRequest((Request) request);
          return null;
        },
        locationId,
        JsonKey.LOCATION_ID);
  }

  public Promise<Result> deleteGeoLocation(String locationId) {
    return handleRequest(
        ActorOperations.DELETE_GEO_LOCATION.getValue(), locationId, JsonKey.LOCATION_ID, false);
  }

  public Promise<Result> sendNotification() {
    return handleRequest(
        ActorOperations.SEND_NOTIFICATION.getValue(),
        request().body().asJson(),
        request -> {
          RequestValidator.validateSendNotification((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> getUserCount() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("get User Count api call" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateGetUserCount(reqObj);
      reqObj.setOperation(ActorOperations.GET_USER_COUNT.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
