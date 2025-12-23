package controllers.notificationservice;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class EmailServiceController extends BaseController {

  @Inject
  @Named("email_service_actor")
  private ActorRef emailServiceActor;

  @Inject
  @Named("send_notification_actor")
  private ActorRef sendNotificationActor;

  private ObjectMapper omapper = new ObjectMapper();

  /**
   * This method will add a new course entry into cassandra DB.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> sendMail(Http.Request httpRequest) {
    Request reqObj = new Request();
    try {
      JsonNode requestData = httpRequest.body().asJson();
      reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
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
          emailServiceActor,
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
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  public CompletionStage<Result> sendNotification(Http.Request httpRequest) {
    Request reqObj = new Request();
    try {
      JsonNode requestData = httpRequest.body().asJson();
      reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
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
          sendNotificationActor,
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
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }
}
