package controllers.location;

import controllers.BaseController;
import controllers.location.validator.LocationRequestValidator;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/** Created by arvind on 18/4/18. */
public class LocationController extends BaseController {

  LocationRequestValidator validator = new LocationRequestValidator();
  /**
   * Method to create the location of the given type .
   *
   * @return Result
   */
  public Promise<Result> createLocation() {

    try {
      Request request = createAndInitRequest(LocationActorOperation.CREATE_LOCATION.getValue());
      validator.validateCreateLocationRequest(request);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to update the location .
   *
   * @return Result
   */
  public Promise<Result> updateLocation() {

    try {
      Request request = createAndInitRequest(LocationActorOperation.UPDATE_LOCATION.getValue());
      validator.validateUpdateLocationRequest(request);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to delete the location on basis of location id .
   *
   * @return Result
   */
  public Promise<Result> deleteLocation(String locationId) {
    try {
      Request request = createAndInitRequest(LocationActorOperation.DELETE_LOCATION.getValue());
      validator.validateDeleteLocationRequest(locationId);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      requestMap.put(JsonKey.ID, locationId);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to search the location on basis of search query in request body
   *
   * @return Result
   */
  public Promise<Result> searchLocation() {
    try {
      Request request = createAndInitRequest(LocationActorOperation.DELETE_LOCATION.getValue());
      validator.validateSearchLocationRequest(request);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
