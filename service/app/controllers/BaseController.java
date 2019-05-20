package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.exception.ProjectCommonException;
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
import org.sunbird.telemetry.util.TelemetryLmaxWriter;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import util.AuthenticationHelper;
import util.Global;

/**
 * This controller we can use for writing some common method.
 *
 * @author Manzarul
 */
public class BaseController extends Controller {

  public static final int AKKA_WAIT_TIME = 30;
  private static Object actorRef = null;
  private TelemetryLmaxWriter lmaxWriter = TelemetryLmaxWriter.getInstance();
  protected Timeout timeout = new Timeout(AKKA_WAIT_TIME, TimeUnit.SECONDS);

  static {
    try {
      actorRef = SunbirdMWService.getRequestRouter();
    } catch (Exception ex) {
      ProjectLogger.log("Exception occured while getting actor ref in base controller " + ex);
    }
  }

  private org.sunbird.common.request.Request initRequest(
      org.sunbird.common.request.Request request, String operation) {
    request.setOperation(operation);
    request.setRequestId(ExecutionContext.getRequestId());
    request.setEnv(getEnvironment());
    request.getContext().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
    request = transformUserId(request);
    return request;
  }

  /**
   * Helper method for creating and initialising a request for given operation and request body.
   *
   * @param operation A defined actor operation
   * @param requestBodyJson Optional information received in request body (JSON)
   * @return Created and initialised Request (@see {@link org.sunbird.common.request.Request})
   *     instance.
   */
  protected org.sunbird.common.request.Request createAndInitRequest(
      String operation, JsonNode requestBodyJson) {
    org.sunbird.common.request.Request request =
        (org.sunbird.common.request.Request)
            mapper.RequestMapper.mapRequest(
                requestBodyJson, org.sunbird.common.request.Request.class);
    return initRequest(request, operation);
  }

  /**
   * Helper method for creating and initialising a request for given operation.
   *
   * @param operation A defined actor operation
   * @return Created and initialised Request (@see {@link org.sunbird.common.request.Request})
   *     instance.
   */
  protected org.sunbird.common.request.Request createAndInitRequest(String operation) {
    org.sunbird.common.request.Request request = new org.sunbird.common.request.Request();
    return initRequest(request, operation);
  }

  protected Promise<Result> handleRequest(String operation) {
    return handleRequest(operation, null, null, null, null, false);
  }

  protected Promise<Result> handleRequest(String operation, JsonNode requestBodyJson) {
    return handleRequest(operation, requestBodyJson, null, null, null, true);
  }

  protected Promise<Result> handleRequest(
      String operation, java.util.function.Function requestValidatorFn) {
    return handleRequest(operation, null, requestValidatorFn, null, null, false);
  }

  protected Promise<Result> handleRequest(
      String operation, JsonNode requestBodyJson, java.util.function.Function requestValidatorFn) {
    return handleRequest(operation, requestBodyJson, requestValidatorFn, null, null, true);
  }

  protected Promise<Result> handleRequest(String operation, String pathId, String pathVariable) {
    return handleRequest(operation, null, null, pathId, pathVariable, false);
  }

  protected Promise<Result> handleRequest(
      String operation, String pathId, String pathVariable, boolean isJsonBodyRequired) {
    return handleRequest(operation, null, null, pathId, pathVariable, isJsonBodyRequired);
  }

  protected Promise<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      Map<String, String> headers) {
    return handleRequest(operation, requestBodyJson, requestValidatorFn, null, null, headers, true);
  }

  protected Promise<Result> handleRequest(
      String operation,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable) {
    return handleRequest(operation, null, requestValidatorFn, pathId, pathVariable, false);
  }

  protected Promise<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable) {
    return handleRequest(
        operation, requestBodyJson, requestValidatorFn, pathId, pathVariable, true);
  }

  protected Promise<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable,
      boolean isJsonBodyRequired) {
    return handleRequest(
        operation,
        requestBodyJson,
        requestValidatorFn,
        pathId,
        pathVariable,
        null,
        isJsonBodyRequired);
  }

  protected Promise<Result> handleRequest(
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Map<String, String> headers,
      boolean isJsonBodyRequired) {
    try {
      org.sunbird.common.request.Request request = null;
      if (!isJsonBodyRequired) {
        request = createAndInitRequest(operation);
      } else {
        request = createAndInitRequest(operation, requestBodyJson);
      }
      if (pathId != null) {
        request.getRequest().put(pathVariable, pathId);
        request.getContext().put(pathVariable, pathId);
      }
      if (requestValidatorFn != null) requestValidatorFn.apply(request);
      if (headers != null) request.getContext().put(JsonKey.HEADER, headers);

      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseController:handleRequest: Exception occurred with error message = " + e.getMessage(),
          e);
      return Promise.pure(createCommonExceptionResponse(e, request()));
    }
  }

  protected Promise<Result> handleSearchRequest(
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Map<String, String> headers,
      String esObjectType) {
    try {
      org.sunbird.common.request.Request request = null;
      if (null != requestBodyJson) {
        request = createAndInitRequest(operation, requestBodyJson);
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
      request.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseController:handleRequest: Exception occurred with error message = " + e.getMessage(),
          e);
      return Promise.pure(createCommonExceptionResponse(e, request()));
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
  public static Response createSuccessResponse(Request request, Response response) {
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

    try {
      if (response.getResult() != null) {
        String json = new ObjectMapper().writeValueAsString(response.getResult());
        String value = getResponseSize(json);
        ctx().response().setHeader(HeaderParam.X_Response_Length.getName(), value);
      }
    } catch (Exception e) {
      ctx().response().setHeader(HeaderParam.X_Response_Length.getName(), "0.0");
    }

    return response;
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
        genarateTelemetryInfoForError());
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
    Global.requestInfo.remove(ctx().flash().get(JsonKey.REQUEST_ID));
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
    Map<String, Object> requestInfo = Global.requestInfo.get(ctx().flash().get(JsonKey.REQUEST_ID));
    org.sunbird.common.request.Request req = new org.sunbird.common.request.Request();

    Map<String, Object> params = (Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO);

    params.put(JsonKey.LOG_TYPE, JsonKey.API_ACCESS);
    params.put(JsonKey.MESSAGE, "");
    params.put(JsonKey.METHOD, request.method());
    // calculate  the total time consume
    long startTime = (Long) params.get(JsonKey.START_TIME);
    params.put(JsonKey.DURATION, calculateApiTimeTaken(startTime));
    removeFields(params, JsonKey.START_TIME);
    params.put(
        JsonKey.STATUS, String.valueOf(((Response) response).getResponseCode().getResponseCode()));
    params.put(JsonKey.LOG_LEVEL, JsonKey.INFO);
    req.setRequest(
        generateTelemetryRequestForController(
            TelemetryEvents.LOG.getName(),
            params,
            (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT)));
    // if any request is coming form /v1/telemetry/save then don't generate the telemetry log
    // for it.
    lmaxWriter.submitMessage(req);

    Response courseResponse = (Response) response;
    if (!StringUtils.isBlank(key)) {
      Object value = courseResponse.getResult().get(JsonKey.RESPONSE);
      courseResponse.getResult().remove(JsonKey.RESPONSE);
      courseResponse.getResult().put(key, value);
    }

    // remove request info from map
    Global.requestInfo.remove(ctx().flash().get(JsonKey.REQUEST_ID));
    return Results.ok(Json.toJson(BaseController.createSuccessResponse(request, courseResponse)));
    // }
    /*
     * else {
     *
     * ProjectCommonException exception = (ProjectCommonException) response; return
     * Results.status(exception.getResponseCode(),
     * Json.toJson(BaseController.createResponseOnException(request, exception))); }
     */
  }

  /**
   * @param file
   * @return
   */
  public Result createFileDownloadResponse(File file) {
    response().setContentType("application/x-download");
    response().setHeader("Content-disposition", "attachment; filename=" + file.getName());
    return Results.ok(file);
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
    ProjectLogger.log(e.getMessage(), e, genarateTelemetryInfoForError());
    if (req == null) {
      req = request();
    }
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

    Map<String, Object> requestInfo = Global.requestInfo.get(ctx().flash().get(JsonKey.REQUEST_ID));
    org.sunbird.common.request.Request reqForTelemetry = new org.sunbird.common.request.Request();
    Map<String, Object> params = (Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO);
    params.put(JsonKey.LOG_TYPE, JsonKey.API_ACCESS);
    params.put(JsonKey.MESSAGE, "");
    params.put(JsonKey.METHOD, request.method());
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
    lmaxWriter.submitMessage(reqForTelemetry);

    // cleaning request info ...
    return Results.status(
        exception.getResponseCode(),
        Json.toJson(BaseController.createResponseOnException(req, exception)));
  }

  private long calculateApiTimeTaken(Long startTime) {

    Long timeConsumed = null;
    if (null != startTime) {
      timeConsumed = System.currentTimeMillis() - startTime;
    }
    return timeConsumed;
  }

  /**
   * This method will make a call to Akka actor and return promise.
   *
   * @param actorRef ActorSelection
   * @param request Request
   * @param timeout Timeout
   * @param responseKey String
   * @param httpReq play.mvc.Http.Request
   * @return Promise<Result>
   */
  public Promise<Result> actorResponseHandler(
      Object actorRef,
      org.sunbird.common.request.Request request,
      Timeout timeout,
      String responseKey,
      Request httpReq) {

    String operation = request.getOperation();

    // set header to request object , setting actor type and channel headers value
    // ...
    setChannelAndActorInfo(ctx(), request);

    Function<Object, Result> function =
        new Function<Object, Result>() {
          @Override
          public Result apply(Object result) {
            if (ActorOperations.HEALTH_CHECK.getValue().equals(request.getOperation())) {
              setGlobalHealthFlag(result);
            }

            if (result instanceof Response) {
              Response response = (Response) result;
              return createCommonResponse(response, responseKey, httpReq);
            } else if (result instanceof ProjectCommonException) {
              return createCommonExceptionResponse((ProjectCommonException) result, request());
            } else if (result instanceof File) {
              return createFileDownloadResponse((File) result);
            } else {
              return createCommonExceptionResponse(new Exception(), httpReq);
            }
          }
        };

    if (actorRef instanceof ActorRef) {
      return Promise.wrap(Patterns.ask((ActorRef) actorRef, request, timeout)).map(function);
    } else {
      return Promise.wrap(Patterns.ask((ActorSelection) actorRef, request, timeout)).map(function);
    }
  }

  /**
   * This method will provide environment id.
   *
   * @return int
   */
  public int getEnvironment() {

    if (Global.env != null) {
      return Global.env.getValue();
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
        val = Global.getResponseId(path);
        if (StringUtils.isBlank(val)) {
          String[] splitedpath = path.split("[/]");
          path = removeLastValue(splitedpath);
          val = Global.getResponseId(path);
        }
      } else {
        val = Global.getResponseId(path);
      }
      if (StringUtils.isBlank(val)) {
        val = Global.getResponseId(path);
        if (StringUtils.isBlank(val)) {
          String[] splitedpath = path.split("[/]");
          path = removeLastValue(splitedpath);
          val = Global.getResponseId(path);
        }
      }
    }
    return val;
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
      val = Global.getResponseId(path);
      if (StringUtils.isBlank(val)) {
        String[] splitedpath = path.split("[/]");
        String tempPath = removeLastValue(splitedpath);
        val = Global.getResponseId(tempPath);
      }
    } else {
      val = Global.getResponseId(path);
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

  private static Map<String, Object> genarateTelemetryInfoForError() {

    Map<String, Object> map = new HashMap<>();
    Map<String, Object> requestInfo = Global.requestInfo.get(ctx().flash().get(JsonKey.REQUEST_ID));
    Map<String, Object> contextInfo = (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT);
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.ERR_TYPE, JsonKey.API_ACCESS);

    map.put(JsonKey.CONTEXT, contextInfo);
    map.put(JsonKey.PARAMS, params);
    return map;
  }

  public void setChannelAndActorInfo(Context ctx, org.sunbird.common.request.Request reqObj) {

    reqObj.getContext().put(JsonKey.CHANNEL, ctx().flash().get(JsonKey.CHANNEL));
    reqObj.getContext().put(JsonKey.ACTOR_ID, ctx().flash().get(JsonKey.ACTOR_ID));
    reqObj.getContext().put(JsonKey.ACTOR_TYPE, ctx().flash().get(JsonKey.ACTOR_TYPE));
    reqObj.getContext().put(JsonKey.APP_ID, ctx().flash().get(JsonKey.APP_ID));
    reqObj.getContext().put(JsonKey.DEVICE_ID, ctx.flash().get(JsonKey.DEVICE_ID));
    reqObj
        .getContext()
        .put(
            JsonKey.signupType,
            ctx.flash().get(JsonKey.signupType)); // adding signup type in request context
    ctx().current().flash().remove(JsonKey.APP_ID);
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
    Map<String, String[]> headers = request.headers();
    Iterator<Map.Entry<String, String[]>> itr = headers.entrySet().iterator();
    while (itr.hasNext()) {
      Map.Entry<String, String[]> entry = itr.next();
      map.put(entry.getKey(), entry.getValue()[0]);
    }
    return map;
  }

  /**
   * @param operation
   * @param objectType
   * @return
   * @throws IOException
   */
  protected org.sunbird.common.request.Request createAndInitUploadRequest(
      String operation, String objectType) throws IOException {
    ProjectLogger.log(
        "BaseController: createAndInitUploadRequest called with operation = " + operation);
    org.sunbird.common.request.Request reqObj = new org.sunbird.common.request.Request();
    Map<String, Object> map = new HashMap<>();
    byte[] byteArray = null;
    Http.MultipartFormData body = request().body().asMultipartFormData();
    Map<String, String[]> formUrlEncodeddata = request().body().asFormUrlEncoded();
    JsonNode requestData = request().body().asJson();
    if (body != null) {
      Map<String, String[]> data = body.asFormUrlEncoded();
      for (Map.Entry<String, String[]> entry : data.entrySet()) {
        map.put(entry.getKey(), entry.getValue()[0]);
      }
      List<Http.MultipartFormData.FilePart> filePart = body.getFiles();
      if (filePart != null && !filePart.isEmpty()) {
        InputStream is = new FileInputStream(filePart.get(0).getFile());
        byteArray = IOUtils.toByteArray(is);
      }
    } else if (null != formUrlEncodeddata) {
      for (Map.Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
        map.put(entry.getKey(), entry.getValue()[0]);
      }
      InputStream is =
          new ByteArrayInputStream(
              ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
      byteArray = IOUtils.toByteArray(is);
    } else if (null != requestData) {
      reqObj =
          (org.sunbird.common.request.Request)
              mapper.RequestMapper.mapRequest(
                  request().body().asJson(), org.sunbird.common.request.Request.class);
      InputStream is =
          new ByteArrayInputStream(
              ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
      byteArray = IOUtils.toByteArray(is);
      reqObj.getRequest().remove(JsonKey.DATA);
      map.putAll(reqObj.getRequest());
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidData.getErrorCode(),
          ResponseCode.invalidData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    reqObj.setOperation(operation);
    reqObj.setRequestId(ExecutionContext.getRequestId());
    reqObj.setEnv(getEnvironment());
    map.put(JsonKey.OBJECT_TYPE, objectType);
    map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
    map.put(JsonKey.FILE, byteArray);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.DATA, map);
    reqObj.setRequest(innerMap);
    return reqObj;
  }

  @SuppressWarnings("unchecked")
  private void setGlobalHealthFlag(Object result) {
    if (result instanceof Response) {
      Response response = (Response) result;
      if (Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_HEALTH_CHECK_ENABLE))
          && ((HashMap<String, Object>) response.getResult().get(JsonKey.RESPONSE))
              .containsKey(JsonKey.Healthy)) {
        Global.isServiceHealthy =
            (boolean)
                ((HashMap<String, Object>) response.getResult().get(JsonKey.RESPONSE))
                    .get(JsonKey.Healthy);
      }
    } else {
      Global.isServiceHealthy = false;
    }
    ProjectLogger.log(
        "BaseController:setGlobalHealthFlag: isServiceHealthy = " + Global.isServiceHealthy,
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
