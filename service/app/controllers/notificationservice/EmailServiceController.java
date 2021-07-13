package controllers.notificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.validator.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

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
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSendMail(reqObj);
      reqObj.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.EMAIL_REQUEST, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
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

  public CompletionStage<Result> sendNotification(Http.Request httpRequest) {

    try {
      JsonNode requestData = httpRequest.body().asJson();
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSendMail(reqObj);
      reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.EMAIL_REQUEST, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setRequest(innerMap);

      JsonNode reqObjJson = omapper.convertValue(reqObj, JsonNode.class);
      return handleRequest(
          ActorOperations.V2_NOTIFICATION.getValue(),
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
