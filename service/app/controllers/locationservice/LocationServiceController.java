package controllers.locationservice;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.locationservice.validator.LocationServiceRequestValidator;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationServiceOperation;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/** Created by arvind on 18/4/18. */
public class LocationServiceController extends BaseController {

  /**
   * Method to create the location of the given type .
   *
   * @return Result
   */
  public Promise<Result> createLocation() {

    try {
      Request reqObj = getRequestOject();
      LocationServiceRequestValidator.validateCreateLocationRequest(reqObj);
      prepareRequestObject(reqObj, LocationServiceOperation.CREATE_LOCATION.getValue());
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
      LocationServiceRequestValidator.validateUpdateLocationRequest(reqObj);
      prepareRequestObject(reqObj, LocationServiceOperation.UPDATE_LOCATION.getValue());
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
      LocationServiceRequestValidator.validateDeleteLocationRequest(locationId);
      prepareRequestObject(reqObj, LocationServiceOperation.DELETE_LOCATION.getValue());
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
      LocationServiceRequestValidator.validateSearchLocationRequest(reqObj);
      prepareRequestObject(reqObj, LocationServiceOperation.DELETE_LOCATION.getValue());
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
