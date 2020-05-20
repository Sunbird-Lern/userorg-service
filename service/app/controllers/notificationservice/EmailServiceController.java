package controllers.notificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;

public class EmailServiceController extends BaseController {
  
  private ObjectMapper omapper = new ObjectMapper();

  /**
   * This method will add a new course entry into cassandra DB.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> sendMail(Http.Request httpRequest) {

    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("send Mail : =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSendMail(reqObj);
      reqObj.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.EMAIL_REQUEST, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, httpRequest.flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
  
      JsonNode reqObjJson = omapper.convertValue(reqObj, JsonNode.class);
      return handleRequest(
        ActorOperations.EMAIL_SERVICE.getValue(),
        reqObjJson,
        req -> {
          // We have validated earlier.
          return null;
        },
        null,
        null,
        true,
        httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}