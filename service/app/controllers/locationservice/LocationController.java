package controllers.locationservice;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.locationservice.validator.LocationRequestValidator;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/** Created by arvind on 18/4/18. */
public class LocationController extends BaseController {

  /**
   * Method to create the location of the given type .
   *
   * @return Result
   */
  public Promise<Result> createLocation() {

    try {
      Request reqObj = getRequestOject();
      LocationRequestValidator.validateCreateLocationRequest(reqObj);
      prepareRequestObject(reqObj, LocationActorOperation.CREATE_LOCATION.getValue());
      Map<String, Object> requestMap = reqObj.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /** @return */
  public Promise<Result> updateLocation() {

    try {
      Request reqObj = getRequestOject();
      LocationRequestValidator.validateUpdateLocationRequest(reqObj);
      prepareRequestObject(reqObj, LocationActorOperation.UPDATE_LOCATION.getValue());
      Map<String, Object> requestMap = reqObj.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /** @return */
  public Promise<Result> deleteLocation(String locationId) {
    try {
      Request reqObj = new Request();
      LocationRequestValidator.validateDeleteLocationRequest(locationId);
      prepareRequestObject(reqObj, LocationActorOperation.DELETE_LOCATION.getValue());
      Map<String, Object> requestMap = reqObj.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      requestMap.put(JsonKey.ID, locationId);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /** @return */
  public Promise<Result> searchLocation() {
    try {
      Request reqObj = getRequestOject();
      LocationRequestValidator.validateSearchLocationRequest(reqObj);
      prepareRequestObject(reqObj, LocationActorOperation.DELETE_LOCATION.getValue());
      Map<String, Object> requestMap = reqObj.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getLocationType() {
    try {
      Request reqObj = new Request();
      prepareRequestObject(reqObj, LocationActorOperation.READ_LOCATION_TYPE.getValue());
      Map<String, Object> requestMap = reqObj.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  private void prepareRequestObject(Request reqObj, String operationName) {
    reqObj.setOperation(operationName);
    reqObj.setRequestId(ExecutionContext.getRequestId());
    reqObj.setEnv(getEnvironment());
  }

  private Request getRequestOject() {
    JsonNode requestData = request().body().asJson();
    // ProjectLogger.log("Create location request: " + requestData, LoggerEnum.INFO.name());
    return (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
  }
}
