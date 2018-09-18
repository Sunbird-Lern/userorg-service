/** */
package controllers.coursemanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.coursemanagement.validator.CourseBatchRequestValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the API related to course batches , add batch,update batch,join
 *
 * @author Manzarul
 */
public class CourseBatchController extends BaseController {

  /**
   * This method will add a new batch for a particular course.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createBatch() {

    return handleRequest(
        ActorOperations.CREATE_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateCreateBatchRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  /**
   * This method will update existing batch details.
   *
   * @return Promise<Result>
   */
  public Promise<Result> updateBatch() {
    return handleRequest(
        ActorOperations.UPDATE_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateUpdateCourseBatchRequest((Request) request);
          return null;
        });
  }

  /**
   * This method will do soft delete to the batch.
   *
   * @return Promise<Result>
   */
  public Promise<Result> deleteBatch() {
    return handleRequest(
        ActorOperations.REMOVE_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateAddBatchCourse((Request) request);
          return null;
        });
  }

  /**
   * This method will do the user batch enrollment
   *
   * @return Promise<Result>
   */
  public Promise<Result> addUserToBatch(String batchId) {
    return handleRequest(
        ActorOperations.ADD_USER_TO_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateAddBatchCourse((Request) request);
          return null;
        });
  }

  /**
   * This method will remove user batch enrollment.
   *
   * @return Promise<Result>
   */
  public Promise<Result> removeUsersFromBatch() {
    return handleRequest(
        ActorOperations.REMOVE_USER_FROM_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateAddBatchCourse((Request) request);
          return null;
        });
  }

  /**
   * This method will fetch batch details from ES.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getBatch(String batchId) {
    return handleRequest(
        ActorOperations.GET_BATCH.getValue(), request().body().asJson(), JsonKey.BATCH_ID, batchId);
  }

  /**
   * This method will do the user search for Elastic search. this will internally call composite
   * search api.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Promise<Result> search() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "CourseBatchController: search called with data = " + requestData,
          LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.COMPOSITE_SEARCH.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));

      List<String> esObjectType = new ArrayList<>();
      esObjectType.add(EsType.course.getTypeName());
      if (reqObj.getRequest().containsKey(JsonKey.FILTERS)
          && reqObj.getRequest().get(JsonKey.FILTERS) != null
          && reqObj.getRequest().get(JsonKey.FILTERS) instanceof Map) {
        ((Map) (reqObj.getRequest().get(JsonKey.FILTERS))).put(JsonKey.OBJECT_TYPE, esObjectType);
      } else {
        Map<String, Object> filtermap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(JsonKey.OBJECT_TYPE, esObjectType);
        filtermap.put(JsonKey.FILTERS, dataMap);
      }
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
