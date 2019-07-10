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

public class CourseBatchController extends BaseController {

  public Promise<Result> createBatch() {
    return handleRequest(
        ActorOperations.CREATE_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateCreateCourseBatchRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> getBatch(String batchId) {
    return handleRequest(ActorOperations.GET_BATCH.getValue(), batchId, JsonKey.BATCH_ID, false);
  }

  public Promise<Result> updateBatch() {
    return handleRequest(
        ActorOperations.UPDATE_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateUpdateCourseBatchRequest((Request) request);
          return null;
        });
  }

  public Promise<Result> addUserToCourseBatch(String batchId) {
    return handleRequest(
        ActorOperations.ADD_USER_TO_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateAddUserToCourseBatchRequest((Request) request);
          return null;
        },
        batchId,
        JsonKey.BATCH_ID);
  }

  public Promise<Result> deleteBatch() {
    return handleRequest(
        ActorOperations.REMOVE_BATCH.getValue(),
        request().body().asJson(),
        (request) -> {
          new CourseBatchRequestValidator().validateDeleteCourseBatchRequest((Request) request);
          return null;
        });
  }

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
      String requestedField = request().getQueryString(JsonKey.FIELDS);
      reqObj.getContext().put(JsonKey.PARTICIPANTS, requestedField);
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
