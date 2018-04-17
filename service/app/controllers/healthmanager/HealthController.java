/** */
package controllers.healthmanager;

import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/** @author Manzarul */
public class HealthController extends BaseController {
  private static List<String> list = new ArrayList<>();

  static {
    list.add("learner");
    list.add("actor");
    list.add("cassandra");
    list.add("es");
    list.add("ekstep");
  }

  /**
   * This method will do the complete health check
   *
   * @return Promise<Result>
   */
  public Promise<Result> getHealth() {
    try {
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.HEALTH_CHECK.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will do the health check for play service.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getLearnerServiceHealth(String val) {
    ProjectLogger.log("Call to get play service health api = " + val, LoggerEnum.INFO.name());
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    if (list.contains(val) && !"learner".equalsIgnoreCase(val)) {
      if (ActorOperations.EKSTEP.name().equalsIgnoreCase(val)) {
        return getEkStepHealtCheck(request());
      } else {
        try {
          Request reqObj = new Request();
          reqObj.setOperation(val);
          reqObj.setRequestId(ExecutionContext.getRequestId());
          reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
          reqObj.setEnv(getEnvironment());
          return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
        } catch (Exception e) {
          return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
      }
    } else {
      responseList.add(ProjectUtil.createCheckResponse(JsonKey.LEARNER_SERVICE, false, null));
      finalResponseMap.put(JsonKey.CHECKS, responseList);
      finalResponseMap.put(JsonKey.NAME, "Learner service health");
      finalResponseMap.put(JsonKey.Healthy, true);
      Response response = new Response();
      response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
      response.setId("learner.service.health.api");
      response.setVer(getApiVersion(request().path()));
      response.setTs(ExecutionContext.getRequestId());
      return Promise.<Result>pure(ok(play.libs.Json.toJson(response)));
    }
  }

  public Promise<Result> getEkStepHealtCheck(play.mvc.Http.Request request) {
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    // check EKStep Util.
    try {
      String body = "{\"request\":{\"filters\":{\"identifier\":\"test\"}}}";
      Map<String, String> headers = new HashMap<>();
      headers.put(JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.AUTHORIZATION));
      if (StringUtils.isBlank((String) headers.get(JsonKey.AUTHORIZATION))) {
        headers.put(
            JsonKey.AUTHORIZATION,
            PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));
      }
      headers.put("Content-Type", "application/json");
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      String response =
          HttpUtil.sendPostRequest(
              ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_SEARCH_URL),
              body,
              headers);
      if (response.contains("OK")) {
        responseList.add(ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, false, null));
        finalResponseMap.put(JsonKey.Healthy, true);
      } else {
        responseList.add(ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, true, null));
        finalResponseMap.put(JsonKey.Healthy, false);
      }
    } catch (Exception e) {
      responseList.add(ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, true, null));
      finalResponseMap.put(JsonKey.Healthy, false);
    }
    finalResponseMap.put(JsonKey.CHECKS, responseList);
    finalResponseMap.put(JsonKey.NAME, "EkStep service health");
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
    response.setId("Ekstep.service.health.api");
    response.setVer(getApiVersion(request().path()));
    response.setTs(ExecutionContext.getRequestId());
    return Promise.<Result>pure(ok(play.libs.Json.toJson(response)));
  }
}
