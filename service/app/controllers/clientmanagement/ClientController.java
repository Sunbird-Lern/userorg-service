package controllers.clientmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/**
 * Controller class to handle request for clients. Created mainly for handling operations related to
 * the master key for various clients
 */
public class ClientController extends BaseController {

  /**
   * Method to register the client and generate master key for the client
   *
   * @return Response object on success else Error object
   */
  public CompletionStage<Result> registerClient(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("Register client: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateRegisterClient(reqObj);
      reqObj.setOperation(ActorOperations.REGISTER_CLIENT.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Method to update the client key for the given clientId in the header
   *
   * @return Response object on success else Error object
   */
  public CompletionStage<Result> updateClientKey(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("Update client key: " + requestData, LoggerEnum.INFO.name());
      Optional<String> masterKey =
          httpRequest.getHeaders().get(HeaderParam.X_Authenticated_Client_Token.getName());
      Optional<String> clientId =
          httpRequest.getHeaders().get(HeaderParam.X_Authenticated_Client_Id.getName());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateUpdateClientKey(clientId.get(), masterKey.get());
      reqObj.setOperation(ActorOperations.UPDATE_CLIENT_KEY.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = (HashMap<String, Object>) reqObj.getRequest();
      innerMap.put(JsonKey.CLIENT_ID, clientId);
      innerMap.put(JsonKey.MASTER_KEY, masterKey);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Method to get client data such as Master Key based on given clientId
   *
   * @param clientId
   * @return Response object on success else Error object
   */
  public CompletionStage<Result> getClientKey(String clientId, Http.Request httpRequest) {
    try {
      ProjectLogger.log("Get client key: " + clientId, LoggerEnum.INFO.name());
      String type = httpRequest.getQueryString(JsonKey.TYPE);
      RequestValidator.validateGetClientKey(clientId, type);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_CLIENT_KEY.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.CLIENT_ID, clientId);
      innerMap.put(JsonKey.TYPE, type);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
