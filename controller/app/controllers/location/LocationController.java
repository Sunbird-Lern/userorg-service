package controllers.location;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.actor.location.validator.BaseLocationRequestValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

/**
 * LocationController handles location APIs.
 *
 * @author arvind on 18/4/18.
 */
public class LocationController extends BaseController {

  @Inject
  @Named("location_actor")
  private ActorRef locationActor;

  BaseLocationRequestValidator validator = new BaseLocationRequestValidator();
  /**
   * Method to create new location.
   *
   * <p>Request body contains following parameters - name: A name given to location . code: Unique
   * code for the location. type: Each location has specific type for example location type can be
   * STATE, DISTRICT, BLOCK, CLUSTER. parentId: The location has hierarchy , so the parentId defines
   * the one level up parentId of location ,for root level location parentId not required and other
   * than root location parentId required.
   *
   * @return Return a promise for create location API result
   */
  public CompletionStage<Result> createLocation(Http.Request httpRequest) {
    Request request = new Request();
    try {
      JsonNode jsonNode = httpRequest.body().asJson();
      request =
          createAndInitRequest(ActorOperations.CREATE_LOCATION.getValue(), jsonNode, httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      validator.validateCreateLocationRequest(request);
      return actorResponseHandler(locationActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  /**
   * Method to update the location.
   *
   * <p>Request body contains following parameters - id: Id of the location to uniquely identify the
   * location , Every location assigned with unique identifier while location creation.The fields
   * that can not be updated are code, type. The allowed fields that can be updated are -
   * parentCode, parentId, name. The parentId or parentCode should be of the immediate one level up
   * location id. For root level location can not update parentId or parentCode.
   *
   * @return Return a promise for update location API result
   */
  public CompletionStage<Result> updateLocation(Http.Request httpRequest) {
    Request request = new Request();
    try {
      JsonNode jsonNode = httpRequest.body().asJson();
      request =
          createAndInitRequest(ActorOperations.UPDATE_LOCATION.getValue(), jsonNode, httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      validator.validateUpdateLocationRequest(request);
      return actorResponseHandler(locationActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  /**
   * Method to delete the location on basis of location id .
   *
   * <p>Path param contains locationId .
   *
   * @param locationId Id of location to
   * @return Return a promise for update location API result.
   */
  public CompletionStage<Result> deleteLocation(String locationId, Http.Request httpRequest) {
    Request request = new Request();
    try {
      request = createAndInitRequest(ActorOperations.DELETE_LOCATION.getValue(), httpRequest);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.LOCATION_ID, locationId);
      setContextAndPrintEntryLog(httpRequest, request);
      validator.validateDeleteLocationRequest(locationId);
      return actorResponseHandler(locationActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));

      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  /**
   * Method to search the location on basis of search query in request body.
   *
   * <p>Request body contains the various filters on which basis location search execute and also
   * parameters like offset and size. For example parameter filter contain the fields and their
   * values , parameter size represents the max search result size.
   *
   * @return Return a promise for update location API result.
   */
  public CompletionStage<Result> searchLocation(Http.Request httpRequest) {
    Request request = new Request();
    try {
      JsonNode jsonNode = httpRequest.body().asJson();
      request =
          createAndInitRequest(ActorOperations.SEARCH_LOCATION.getValue(), jsonNode, httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      validator.validateSearchLocationRequest(request);
      return actorResponseHandler(locationActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }
}
