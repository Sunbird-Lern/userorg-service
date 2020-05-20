package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import modules.ApplicationStart;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.ClientErrorResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryWriter;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import util.AuthenticationHelper;

/**
 * This controller we can use for writing some common method.
 *
 * @author Manzarul
 */
public class BaseController extends Controller {

  public static final int AKKA_WAIT_TIME = 30;
  private static final String version = "v1";
  private static Object actorRef = null;
  protected Timeout timeout = new Timeout(AKKA_WAIT_TIME, TimeUnit.SECONDS);

  static {
    try {
      actorRef = SunbirdMWService.getRequestRouter();
    } catch (Exception ex) {
      ProjectLogger.log("Exception occured while getting actor ref in base controller " + ex);
    }
  }

  private org.sunbird.common.request.Request initRequest(
      org.sunbird.common.request.Request request, String operation, Request httpRequest) {
    request.setOperation(operation);
    request.setRequestId(httpRequest.flash().get(JsonKey.REQUEST_ID));
    request.getParams().setMsgid(httpRequest.flash().get(JsonKey.REQUEST_ID));
    request.setEnv(getEnvironment());
    request.getContext().put(JsonKey.REQUESTED_BY, httpRequest.flash().get(JsonKey.USER_ID));
    request = transformUserId(request);
    return request;
  }

  /**
   * Helper method for creating and initialising a request for given operation and request body.
   *
   * @param operation A defined actor operation
   * @param requestBodyJson Optional information received in request body (JSON)
   * @param httpRequest
   * @return Created and initialised Request (@see {@link org.sunbird.common.request.Request})
   *     instance.
   */
  protected org.sunbird.common.request.Request createAndInitRequest(
      String operation, JsonNode requestBodyJson, Request httpRequest) {
    org.sunbird.common.request.Request request =
        (org.sunbird.common.request.Request)
            mapper.RequestMapper.mapRequest(
                requestBodyJson, org.sunbird.common.request.Request.class);
    return initRequest(request, operation, httpRequest);
  }

  /**
   * Helper method for creating and initialising a request for given operation.
   *
   * @param operation A defined actor operation
   * @param httpRequest
   * @return Created and initialised Request (@see {@link org.sunbird.common.request.Request})
   *     instance.
   */
  protected org.sunbird.common.request.Request createAndInitRequest(
      String operation, Request httpRequest) {
    org.sunbird.common.request.Request request = new org.sunbird.common.request.Request();
    return initRequest(request, operation, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(String operation, Http.Request httpRequest) {
    return handleRequest(operation, null, null, null, null, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation, JsonNode requestBodyJson, Request httpRequest) {
    return handleRequest(operation, requestBodyJson, null, null, null, true, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation, java.util.function.Function requestValidatorFn, Request httpRequest) {
    return handleRequest(operation, null, requestValidatorFn, null, null, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      Request httpRequest) {
    return handleRequest(
        operation, requestBodyJson, requestValidatorFn, null, null, true, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation, String pathId, String pathVariable, Request httpRequest) {
    return handleRequest(operation, null, null, pathId, pathVariable, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      String pathId,
      String pathVariable,
      boolean isJsonBodyRequired,
      Request httpRequest) {
    return handleRequest(
        operation, null, null, pathId, pathVariable, isJsonBodyRequired, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      Map<String, String> headers,
      Request httpRequest) {
    return handleRequest(
        operation, requestBodyJson, requestValidatorFn, null, null, headers, true, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Request httpRequest) {
    return handleRequest(
        operation, null, requestValidatorFn, pathId, pathVariable, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Request httpRequest) {
    return handleRequest(
        operation, requestBodyJson, requestValidatorFn, pathId, pathVariable, true, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable,
      boolean isJsonBodyRequired,
      Request httpRequest) {
    return handleRequest(
        operation,
        requestBodyJson,
        requestValidatorFn,
        pathId,
        pathVariable,
        null,
        isJsonBodyRequired,
        httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Map<String, String> headers,
      boolean isJsonBodyRequired,
      Request httpRequest) {
    try {
      org.sunbird.common.request.Request request = null;
      if (!isJsonBodyRequired) {
        request = createAndInitRequest(operation, httpRequest);
      } else {
        request = createAndInitRequest(operation, requestBodyJson, httpRequest);
      }
      if (pathId != null) {
        request.getRequest().put(pathVariable, pathId);
        request.getContext().put(pathVariable, pathId);
      }
      if (requestValidatorFn != null) requestValidatorFn.apply(request);
      if (headers != null) request.getContext().put(JsonKey.HEADER, headers);

      ProjectLogger.log(
          "BaseController:handleRequest for operation: "
              + operation
              + " requestId: "
              + request.getRequestId(),
          LoggerEnum.INFO.name());
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseController:handleRequest for operation: "
              + operation
              + " Exception occurred with error message = "
              + e.getMessage(),
          e);
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  protected CompletionStage<Result> handleSearchRequest(
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Map<String, String> headers,
      String esObjectType,
      Request httpRequest) {
    try {
      org.sunbird.common.request.Request request = null;
      if (null != requestBodyJson) {
        request = createAndInitRequest(operation, requestBodyJson, httpRequest);
      } else {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData, null);
      }
      if (pathId != null) {
        request.getRequest().put(pathVariable, pathId);
        request.getContext().put(pathVariable, pathId);
      }
      if (requestValidatorFn != null) requestValidatorFn.apply(request);
      if (headers != null) request.getContext().put(JsonKey.HEADER, headers);
      if (StringUtils.isNotBlank(esObjectType)) {
        List<String> esObjectTypeList = new ArrayList<>();
        esObjectTypeList.add(esObjectType);
        ((Map) (request.getRequest().get(JsonKey.FILTERS)))
            .put(JsonKey.OBJECT_TYPE, esObjectTypeList);
      }
      request.getRequest().put(JsonKey.REQUESTED_BY, httpRequest.flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseController:handleRequest: Exception occurred with error message = " + e.getMessage(),
          e);
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will provide remote Actor selection
   *
   * @return Object
   */
  public Object getActorRef() {

    return actorRef;
  }

  /**
   * This method will create failure response
   *
   * @param request Request
   * @param code ResponseCode
   * @param headerCode ResponseCode
   * @return Response
   */
  public static Response createFailureResponse(
      Request request, ResponseCode code, ResponseCode headerCode) {

    Response response = new Response();
    response.setVer(getApiVersion(request.path()));
    response.setId(getApiResponseId(request));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(headerCode);
    response.setParams(createResponseParamObj(code));
    return response;
  }

  public static ResponseParams createResponseParamObj(ResponseCode code, String customMessage) {
    ResponseParams params = new ResponseParams();
    if (code.getResponseCode() != 200) {
      params.setErr(code.getErrorCode());
      params.setErrmsg(
          StringUtils.isNotBlank(customMessage) ? customMessage : code.getErrorMessage());
    }
    params.setMsgid(ExecutionContext.getRequestId());
    params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
    return params;
  }

  /**
   * This method will create response parameter
   *
   * @param code ResponseCode
   * @return ResponseParams
   */
  public static ResponseParams createResponseParamObj(ResponseCode code) {
    return createResponseParamObj(code, null);
  }

  /**
   * This method will create data for success response.
   *
   * @param request play.mvc.Http.Request
   * @param response Response
   * @return Response
   */
  public static Result createSuccessResponse(Request request, Response response) {
    if (request != null) {
      response.setVer(getApiVersion(request.path()));
    } else {
      response.setVer("");
    }

    response.setId(getApiResponseId(request));
    response.setTs(ProjectUtil.getFormattedDate());
    ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
    code.setResponseCode(ResponseCode.OK.getResponseCode());
    response.setParams(createResponseParamObj(code));
    String value = null;
    try {
      if (response.getResult() != null) {
        String json = new ObjectMapper().writeValueAsString(response.getResult());
        value = getResponseSize(json);
      }
    } catch (Exception e) {
      value = "0.0";
    }

    return Results.ok(Json.toJson(response))
        .withHeader(HeaderParam.X_Response_Length.getName(), value);
  }

  /**
   * This method will provide api version.
   *
   * @param request String
   * @return String
   */
  public static String getApiVersion(String request) {

    return request.split("[/]")[1];
  }

  /**
   * This method will handle response in case of exception
   *
   * @param request play.mvc.Http.Request
   * @param exception ProjectCommonException
   * @return Response
   */
  public static Response createResponseOnException(
      Request request, ProjectCommonException exception) {
    ProjectLogger.log(
        exception != null ? exception.getMessage() : "Message is not coming",
        exception,
        genarateTelemetryInfoForError(request));
    Response response = new Response();
    response.setVer("");
    if (request != null) {
      response.setVer(getApiVersion(request.path()));
    }
    response.setId(getApiResponseId(request));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(ResponseCode.getHeaderResponseCode(exception.getResponseCode()));
    ResponseCode code = ResponseCode.getResponse(exception.getCode());
    if (code == null) {
      code = ResponseCode.SERVER_ERROR;
    }
    response.setParams(createResponseParamObj(code, exception.getMessage()));
    if (response.getParams() != null) {
      response.getParams().setStatus(response.getParams().getStatus());
      if (exception.getCode() != null) {
        response.getParams().setStatus(exception.getCode());
      }
      if (!StringUtils.isBlank(response.getParams().getErrmsg())
          && response.getParams().getErrmsg().contains("{0}")) {
        response.getParams().setErrmsg(exception.getMessage());
      }
    }
    OnRequestHandler.requestInfo.remove(request.flash().get(JsonKey.REQUEST_ID));
    return response;
  }

  /**
   * @param path String
   * @param method String
   * @param exception ProjectCommonException
   * @return Response
   */
  public static Response createResponseOnException(
      String path, String method, ProjectCommonException exception) {
    Response response = new Response();
    response.setVer(getApiVersion(path));
    response.setId(getApiResponseId(path, method));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(ResponseCode.getHeaderResponseCode(exception.getResponseCode()));
    ResponseCode code = ResponseCode.getResponse(exception.getCode());
    response.setParams(createResponseParamObj(code, exception.getMessage()));
    return response;
  }

  /**
   * This method will create common response for all controller method
   *
   * @param response Object
   * @param key String
   * @param request play.mvc.Http.Request
   * @return Result
   */
  public Result createCommonResponse(Object response, String key, Request request) {
    String requestId = request.flash().getOptional(JsonKey.REQUEST_ID).orElse(null);
    if (requestId != null && OnRequestHandler.requestInfo.containsKey(requestId)) {
      Map<String, Object> requestInfo = OnRequestHandler.requestInfo.get(requestId);
      org.sunbird.common.request.Request req = new org.sunbird.common.request.Request();
      try {
        Map<String, Object> params = (Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO);

        params.put(JsonKey.LOG_TYPE, JsonKey.API_ACCESS);
        params.put(JsonKey.MESSAGE, "");
        params.put(JsonKey.METHOD, request.method());
        // calculate  the total time consume
        long startTime = (Long) params.get(JsonKey.START_TIME);
        params.put(JsonKey.DURATION, calculateApiTimeTaken(startTime));
        removeFields(params, JsonKey.START_TIME);
        params.put(
            JsonKey.STATUS,
            String.valueOf(((Response) response).getResponseCode().getResponseCode()));
        params.put(JsonKey.LOG_LEVEL, JsonKey.INFO);
        req.setRequest(
            generateTelemetryRequestForController(
                TelemetryEvents.LOG.getName(),
                params,
                (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT)));
        // if any request is coming form /v1/telemetry/save then don't generate the telemetry log
        // for it.
        TelemetryWriter.write(req);
      } catch (Exception ex) {
        ProjectLogger.log(
            "BaseController:createCommonResponse Exception in writing telemetry for request "
                + requestId,
            ex);
      } finally {
        // remove request info from map
        OnRequestHandler.requestInfo.remove(requestId);
        ProjectLogger.log(
            "BaseController:createCommonResponse removed details for messageId=" + requestId,
            LoggerEnum.INFO);
      }
    } else {
      ProjectLogger.log(
          "BaseController:createCommonResponse request details not found requestId=" + requestId,
          LoggerEnum.ERROR);
    }
    Response courseResponse = (Response) response;
    if (!StringUtils.isBlank(key)) {
      Object value = courseResponse.getResult().get(JsonKey.RESPONSE);
      courseResponse.getResult().remove(JsonKey.RESPONSE);
      courseResponse.getResult().put(key, value);
    }
    return BaseController.createSuccessResponse(request, courseResponse);
  }

  /**
   * @param file
   * @return
   */
  public Result createFileDownloadResponse(File file) {
    return Results.ok(file)
        .withHeader("Content-Type", "application/x-download")
        .withHeader("Content-disposition", "attachment; filename=" + file.getName());
  }

  private void removeFields(Map<String, Object> params, String... properties) {
    for (String property : properties) {
      params.remove(property);
    }
  }

  private String generateStackTrace(StackTraceElement[] elements) {
    StringBuilder builder = new StringBuilder("");
    for (StackTraceElement element : elements) {

      builder.append(element.toString());
      builder.append("\n");
    }
    return ProjectUtil.getFirstNCharacterString(builder.toString(), 100);
  }

  private Map<String, Object> generateTelemetryRequestForController(
      String eventType, Map<String, Object> params, Map<String, Object> context) {

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, eventType);
    map.put(JsonKey.CONTEXT, context);
    map.put(JsonKey.PARAMS, params);
    return map;
  }

  /**
   * Common exception response handler method.
   *
   * @param e Exception
   * @param request play.mvc.Http.Request
   * @return Result
   */
  public Result createCommonExceptionResponse(Exception e, Request request) {
    Request req = request;
    ProjectLogger.log(e.getMessage(), e, genarateTelemetryInfoForError(request));
    ProjectCommonException exception = null;
    if (e instanceof ProjectCommonException) {
      exception = (ProjectCommonException) e;
    } else {
      exception =
          new ProjectCommonException(
              ResponseCode.internalError.getErrorCode(),
              ResponseCode.internalError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    generateExceptionTelemetry(request, exception);
    // cleaning request info ...
    return Results.status(
        exception.getResponseCode(),
        Json.toJson(BaseController.createResponseOnException(req, exception)));
  }

  private void generateExceptionTelemetry(Request request, ProjectCommonException exception) {
    try {
      Map<String, Object> requestInfo =
          OnRequestHandler.requestInfo.get(request.flash().get(JsonKey.REQUEST_ID));
      org.sunbird.common.request.Request reqForTelemetry = new org.sunbird.common.request.Request();
      Map<String, Object> params = (Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO);
      params.put(JsonKey.LOG_TYPE, JsonKey.API_ACCESS);
      params.put(JsonKey.MESSAGE, "");
      params.put(JsonKey.METHOD, request.method());
      params.put("err", exception.getResponseCode() + "");
      params.put("errtype", exception.getCode());
      // calculate  the total time consume
      long startTime = (Long) params.get(JsonKey.START_TIME);
      params.put(JsonKey.DURATION, calculateApiTimeTaken(startTime));
      removeFields(params, JsonKey.START_TIME);
      params.put(JsonKey.STATUS, String.valueOf(exception.getResponseCode()));
      params.put(JsonKey.LOG_LEVEL, "error");
      params.put(JsonKey.STACKTRACE, generateStackTrace(exception.getStackTrace()));
      reqForTelemetry.setRequest(
          generateTelemetryRequestForController(
              TelemetryEvents.ERROR.getName(),
              params,
              (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT)));
      TelemetryWriter.write(reqForTelemetry);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private long calculateApiTimeTaken(Long startTime) {

    Long timeConsumed = null;
    if (null != startTime) {
      timeConsumed = System.currentTimeMillis() - startTime;
    }
    return timeConsumed;
  }

  /**
   * This method will make a call to Akka actor and return CompletionStage.
   *
   * @param actorRef ActorSelection
   * @param request Request
   * @param timeout Timeout
   * @param responseKey String
   * @param httpReq play.mvc.Http.Request
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> actorResponseHandler(
      Object actorRef,
      org.sunbird.common.request.Request request,
      Timeout timeout,
      String responseKey,
      Request httpReq) {
    setChannelAndActorInfo(httpReq, request);
    Function<Object, Result> function =
        result -> {
          if (ActorOperations.HEALTH_CHECK.getValue().equals(request.getOperation())) {
            setGlobalHealthFlag(result);
          }

          if (result instanceof Response) {
            Response response = (Response) result;
            if (ResponseCode.OK.getResponseCode()
                == (response.getResponseCode().getResponseCode())) {
              return createCommonResponse(response, responseKey, httpReq);
            } else if (ResponseCode.CLIENT_ERROR.getResponseCode()
                == (response.getResponseCode().getResponseCode())) {
              return createClientErrorResponse(httpReq, (ClientErrorResponse) response);
            } else if (result instanceof ProjectCommonException) {
              return createCommonExceptionResponse((ProjectCommonException) result, httpReq);
            } else if (result instanceof File) {
              return createFileDownloadResponse((File) result);
            }  else {
              if(StringUtils.isNotEmpty((String)response.getResult().get(JsonKey.MESSAGE)) &&
                response.getResponseCode().getResponseCode() == 0) {
                return createCommonResponse(response, responseKey, httpReq);
              } else {
                return createCommonExceptionResponse((Exception) result, httpReq);
              }
            }
          } else if (result instanceof ProjectCommonException) {
            return createCommonExceptionResponse((ProjectCommonException) result, httpReq);
          } else if (result instanceof File) {
            return createFileDownloadResponse((File) result);
          } else {
            return createCommonExceptionResponse(new Exception(), httpReq);
          }
        };

    if (actorRef instanceof ActorRef) {
      return PatternsCS.ask((ActorRef) actorRef, request, timeout).thenApplyAsync(function);
    } else {
      return PatternsCS.ask((ActorSelection) actorRef, request, timeout).thenApplyAsync(function);
    }
  }

  private Result createClientErrorResponse(Request httpReq, ClientErrorResponse response) {
    ClientErrorResponse errorResponse = response;
    generateExceptionTelemetry(httpReq, errorResponse.getException());
    Response responseObj =
        BaseController.createResponseOnException(httpReq, errorResponse.getException());
    responseObj.getResult().putAll(errorResponse.getResult());
    return Results.status(errorResponse.getException().getResponseCode(), Json.toJson(responseObj));
  }

  /**
   * This method will provide environment id.
   *
   * @return int
   */
  public int getEnvironment() {
    if (ApplicationStart.env != null) {
      return ApplicationStart.env.getValue();
    }
    return ProjectUtil.Environment.dev.getValue();
  }

  /**
   * Method to get UserId by AuthToken
   *
   * @param token
   * @return String
   */
  public String getUserIdByAuthToken(String token) {

    return AuthenticationHelper.verifyUserAccesToken(token);
  }

  /**
   * Method to get API response Id
   *
   * @param request play.mvc.Http.Request
   * @return String
   */
  private static String getApiResponseId(Request request) {

    String val = "";
    if (request != null) {
      String path = request.path();
      if (request.method().equalsIgnoreCase(ProjectUtil.Method.GET.name())) {
        val = getResponseId(path);
        if (StringUtils.isBlank(val)) {
          String[] splitedpath = path.split("[/]");
          path = removeLastValue(splitedpath);
          val = getResponseId(path);
        }
      } else {
        val = getResponseId(path);
      }
      if (StringUtils.isBlank(val)) {
        val = getResponseId(path);
        if (StringUtils.isBlank(val)) {
          String[] splitedpath = path.split("[/]");
          path = removeLastValue(splitedpath);
          val = getResponseId(path);
        }
      }
    }
    return val;
  }

  /**
   * Method to get the response id on basis of request path.
   *
   * @param requestPath
   * @return
   */
  public static String getResponseId(String requestPath) {

    String path = requestPath;
    final String ver = "/" + version;
    final String ver2 = "/" + JsonKey.VERSION_2;
    final String ver3 = "/" + JsonKey.VERSION_3;
    path = path.trim();
    StringBuilder builder = new StringBuilder("");
    if (path.startsWith(ver) || path.startsWith(ver2) || path.startsWith(ver3)) {
      String requestUrl = (path.split("\\?"))[0];
      if (requestUrl.contains(ver)) {
        requestUrl = requestUrl.replaceFirst(ver, "api");
      } else if (requestUrl.contains(ver2)) {
        requestUrl = requestUrl.replaceFirst(ver2, "api");
      } else if (requestUrl.contains(ver3)) {
        requestUrl = requestUrl.replaceFirst(ver3, "api");
      }
      String[] list = requestUrl.split("/");
      for (String str : list) {
        if (str.matches("[A-Za-z]+")) {
          builder.append(str).append(".");
        }
      }
      builder.deleteCharAt(builder.length() - 1);
    } else {
      if ("/health".equalsIgnoreCase(path)) {
        builder.append("api.all.health");
      }
    }
    return builder.toString();
  }

  /**
   * Method to get API response Id
   *
   * @param path String
   * @param method String
   * @return String
   */
  private static String getApiResponseId(String path, String method) {
    String val = "";
    if (ProjectUtil.Method.GET.name().equalsIgnoreCase(method)) {
      val = getResponseId(path);
      if (StringUtils.isBlank(val)) {
        String[] splitedpath = path.split("[/]");
        String tempPath = removeLastValue(splitedpath);
        val = getResponseId(tempPath);
      }
    } else {
      val = getResponseId(path);
    }
    return val;
  }

  /**
   * Method to remove last value
   *
   * @param splited String []
   * @return String
   */
  private static String removeLastValue(String splited[]) {

    StringBuilder builder = new StringBuilder();
    if (splited != null && splited.length > 0) {
      for (int i = 1; i < splited.length - 1; i++) {
        builder.append("/" + splited[i]);
      }
    }
    return builder.toString();
  }

  public static void setActorRef(Object obj) {
    actorRef = obj;
  }

  private static Map<String, Object> genarateTelemetryInfoForError(Request request) {

    Map<String, Object> map = new HashMap<>();
    Map<String, Object> requestInfo =
        OnRequestHandler.requestInfo.get(request.flash().get(JsonKey.REQUEST_ID));
    if (requestInfo != null) {
      Map<String, Object> contextInfo = (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT);
      map.put(JsonKey.CONTEXT, contextInfo);
    }
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.ERR_TYPE, JsonKey.API_ACCESS);
    map.put(JsonKey.PARAMS, params);
    return map;
  }

  public void setChannelAndActorInfo(
      Http.Request httpReq, org.sunbird.common.request.Request reqObj) {

    reqObj.getContext().put(JsonKey.CHANNEL, httpReq.flash().get(JsonKey.CHANNEL));
    reqObj.getContext().put(JsonKey.ACTOR_ID, httpReq.flash().get(JsonKey.ACTOR_ID));
    reqObj.getContext().put(JsonKey.ACTOR_TYPE, httpReq.flash().get(JsonKey.ACTOR_TYPE));
    reqObj.getContext().put(JsonKey.APP_ID, httpReq.flash().get(JsonKey.APP_ID));
    reqObj.getContext().put(JsonKey.DEVICE_ID, httpReq.flash().get(JsonKey.DEVICE_ID));
    reqObj
        .getContext()
        .put(
            JsonKey.SIGNUP_TYPE,
            httpReq.flash().get(JsonKey.SIGNUP_TYPE)); // adding signup type in request context
    reqObj
        .getContext()
        .put(
            JsonKey.REQUEST_SOURCE,
            httpReq.flash().get(JsonKey.REQUEST_SOURCE)); // ADDING Source under params in context
    httpReq.flash().remove(JsonKey.APP_ID);
  }

  /**
   * This method will set extra param to request body which is required for actor layer.
   *
   * @param request Request
   * @param requestId String
   * @param actorOpName String
   * @param requestedUserId String
   * @param env int
   * @return Request
   */
  public org.sunbird.common.request.Request setExtraParam(
      org.sunbird.common.request.Request request,
      String requestId,
      String actorOpName,
      String requestedUserId,
      int env) {
    request.setRequestId(requestId);
    request.setOperation(actorOpName);
    request.getRequest().put(JsonKey.CREATED_BY, requestedUserId);
    request.setEnv(env);
    return request;
  }

  public Map<String, String> getAllRequestHeaders(Request request) {
    Map<String, String> map = new HashMap<>();
    Map<String, List<String>> headers = request.getHeaders().toMap();
    Iterator<Map.Entry<String, List<String>>> itr = headers.entrySet().iterator();
    while (itr.hasNext()) {
      Map.Entry<String, List<String>> entry = itr.next();
      map.put(entry.getKey(), entry.getValue().get(0));
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private void setGlobalHealthFlag(Object result) {
    if (result instanceof Response) {
      Response response = (Response) result;
      if (Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_HEALTH_CHECK_ENABLE))
          && ((HashMap<String, Object>) response.getResult().get(JsonKey.RESPONSE))
              .containsKey(JsonKey.Healthy)) {
        OnRequestHandler.isServiceHealthy =
            (boolean)
                ((HashMap<String, Object>) response.getResult().get(JsonKey.RESPONSE))
                    .get(JsonKey.Healthy);
      }
    } else {
      OnRequestHandler.isServiceHealthy = false;
    }
    ProjectLogger.log(
        "BaseController:setGlobalHealthFlag: isServiceHealthy = "
            + OnRequestHandler.isServiceHealthy,
        LoggerEnum.INFO.name());
  }

  protected String getQueryString(Map<String, String[]> queryStringMap) {
    return queryStringMap
        .entrySet()
        .stream()
        .map(p -> p.getKey() + "=" + String.join(",", p.getValue()))
        .reduce((p1, p2) -> p1 + "&" + p2)
        .map(s -> "?" + s)
        .orElse("");
  }

  public static String getResponseSize(String response) throws UnsupportedEncodingException {
    if (StringUtils.isNotBlank(response)) {
      return response.getBytes("UTF-8").length + "";
    }
    return "0.0";
  }

  public org.sunbird.common.request.Request transformUserId(
      org.sunbird.common.request.Request request) {
    if (request != null && request.getRequest() != null) {
      String id = (String) request.getRequest().get(JsonKey.ID);
      request.getRequest().put(JsonKey.ID, ProjectUtil.getLmsUserId(id));
      id = (String) request.getRequest().get(JsonKey.USER_ID);
      request.getRequest().put(JsonKey.USER_ID, ProjectUtil.getLmsUserId(id));
      return request;
    }
    return request;
  }
}
