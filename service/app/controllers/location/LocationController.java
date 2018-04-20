package controllers.location;

import com.fasterxml.jackson.databind.JsonNode;
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
   * Method to create new location.
   *
   * <p>Request body contains following parameters - name: A name given to location . code: Unique
   * code for the location. type: Each location has specific type for example location type can be
   * STATE, DISTRICT, BLOCK, CLUSTER. parentid: The location has hierarchy , so the parentid defines
   * the immediate id of parent location ,for root level location parentid not required and other
   * than root location parentid required .
   *
   * @return Return a promise for create location API result
   */
  public Promise<Result> createLocation() {

    try {
      JsonNode jsonNode = request().body().asJson();
      Request request =
          createAndInitRequest(LocationActorOperation.CREATE_LOCATION.getValue(), jsonNode);
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
   * <p>Request body contains following parameters - id: Id of the location to uniquely identify the
   * location , Every location assigned with unique identifier while location creation.
   *
   * @return Return a promise for update location API result
   */
  public Promise<Result> updateLocation() {

    try {
      JsonNode jsonNode = request().body().asJson();
      Request request =
          createAndInitRequest(LocationActorOperation.UPDATE_LOCATION.getValue(), jsonNode);
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
   * <p>Path param contains locationId .
   *
   * @return Return a promise for update location API result.
   */
  public Promise<Result> deleteLocation(String locationId) {
    try {
      Request request = createAndInitRequest(LocationActorOperation.DELETE_LOCATION.getValue());
      validator.validateDeleteLocationRequest(locationId);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      requestMap.put(JsonKey.LOCATION_ID, locationId);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to search the location on basis of search query in request body
   *
   * <p>Request body contains the varoius parameters on which basis location search execute and
   * result size varies . For example parameter filter contain the fields and thier values ,
   * parameter size represents the max search result size.
   *
   * @return Return a promise for update location API result.
   */
  public Promise<Result> searchLocation() {
    try {
      JsonNode jsonNode = request().body().asJson();
      Request request =
          createAndInitRequest(LocationActorOperation.SEARCH_LOCATION.getValue(), jsonNode);
      validator.validateSearchLocationRequest(request);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
