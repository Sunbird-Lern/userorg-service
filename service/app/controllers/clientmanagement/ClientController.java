package controllers.clientmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

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
  public Promise<Result> registerClient() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Register client: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateRegisterClient(reqObj);
      reqObj.setOperation(ActorOperations.REGISTER_CLIENT.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to update the client key for the given clientId in the header
   *
   * @return Response object on success else Error object
   */
  public Promise<Result> updateClientKey() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Update client key: " + requestData, LoggerEnum.INFO.name());
      String masterKey = request().getHeader(HeaderParam.X_Authenticated_Client_Token.getName());
      String clientId = request().getHeader(HeaderParam.X_Authenticated_Client_Id.getName());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateUpdateClientKey(clientId, masterKey);
      reqObj.setOperation(ActorOperations.UPDATE_CLIENT_KEY.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = (HashMap<String, Object>) reqObj.getRequest();
      innerMap.put(JsonKey.CLIENT_ID, clientId);
      innerMap.put(JsonKey.MASTER_KEY, masterKey);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to get client data such as Master Key based on given clientId
   *
   * @param clientId
   * @return Response object on success else Error object
   */
  public Promise<Result> getClientKey(String clientId) {
    try {
      ProjectLogger.log("Get client key: " + clientId, LoggerEnum.INFO.name());
      String type = request().getQueryString(JsonKey.TYPE);
      RequestValidator.validateGetClientKey(clientId, type);
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_CLIENT_KEY.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.CLIENT_ID, clientId);
      innerMap.put(JsonKey.TYPE, type);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
