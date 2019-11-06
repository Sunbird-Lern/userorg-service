package controllers.tenantpreference;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;


/** Created by arvind on 27/10/17. */
public class TenantPreferenceController extends BaseController {

  public CompletionStage<Result> createTenantPreference(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("Create tenant preferences: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.CREATE_TENANT_PREFERENCE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, httpRequest.flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> updateTenantPreference(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("Update tenant preferences: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, httpRequest.flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> getTenantPreference(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("Get tenant preferences: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.GET_TENANT_PREFERENCE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, httpRequest.flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
