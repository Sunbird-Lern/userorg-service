package controllers;

import static util.Common.createResponseParamObj;
import static util.PrintEntryExitLog.printEntryLog;
import static util.PrintEntryExitLog.printExitLogOnFailure;
import static util.PrintEntryExitLog.printExitLogOnSuccessResponse;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.PatternsCS;
import org.apache.pekko.util.Timeout;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import modules.ApplicationStart;
import modules.OnRequestHandler;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.HeaderParam;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ClientErrorResponse;
import org.sunbird.response.Response;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryWriter;
import org.sunbird.util.ProjectUtil;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import util.Attrs;
import util.Common;

/**
 * This controller we can use for writing some common method.
 *
 * @author Manzarul
 */
public class BaseController extends Controller {

  private static final LoggerUtil logger = new LoggerUtil(BaseController.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();
  public static final int PEKKO_WAIT_TIME = 30;
  private static final String version = "v1";
  protected Timeout timeout = new Timeout(PEKKO_WAIT_TIME, TimeUnit.SECONDS);
  private static final String debugEnabled = "false";

  private org.sunbird.request.Request initRequest(
      org.sunbird.request.Request request, String operation, Request httpRequest) {
    request.setOperation(operation);

    String requestId = Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID);
    request.setRequestId(requestId);
    request.getParams().setMsgid(requestId);
    request.setEnv(getEnvironment());
    request
        .getContext()
        .put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
    request
        .getContext()
        .put(JsonKey.MANAGED_FOR, Common.getFromRequest(httpRequest, Attrs.MANAGED_FOR));
    Optional<String> manageToken = httpRequest.header(HeaderParam.X_Authenticated_For.getName());
    String managedToken = manageToken.isPresent() ? manageToken.get() : "";
    request.getContext().put(JsonKey.MANAGED_TOKEN, managedToken);

    request = transformUserId(request);
    return request;
  }

  /**
   * Helper method for creating and initialising a request for given operation and request body.
   *
   * @param operation A defined actor operation
   * @param requestBodyJson Optional information received in request body (JSON)
   * @param httpRequest
   * @return Created and initialised Request (@see {@link org.sunbird.request.Request}) instance.
   */
  protected org.sunbird.request.Request createAndInitRequest(
      String operation, JsonNode requestBodyJson, Request httpRequest) {
    org.sunbird.request.Request request =
        (org.sunbird.request.Request)
            mapper.RequestMapper.mapRequest(requestBodyJson, org.sunbird.request.Request.class);
    return initRequest(request, operation, httpRequest);
  }

  /**
   * Helper method for creating and initialising a request for given operation.
   *
   * @param operation A defined actor operation
   * @param httpRequest
   * @return Created and initialised Request (@see {@link org.sunbird.request.Request}) instance.
   */
  protected org.sunbird.request.Request createAndInitRequest(
      String operation, Request httpRequest) {
    org.sunbird.request.Request request = new org.sunbird.request.Request();
    return initRequest(request, operation, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef, String operation, Http.Request httpRequest) {
    return handleRequest(actorRef, operation, null, null, null, null, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef, String operation, JsonNode requestBodyJson, Request httpRequest) {
    return handleRequest(actorRef, operation, requestBodyJson, null, null, null, true, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      java.util.function.Function requestValidatorFn,
      Request httpRequest) {
    return handleRequest(
        actorRef, operation, null, requestValidatorFn, null, null, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      Request httpRequest) {
    return handleRequest(
        actorRef, operation, requestBodyJson, requestValidatorFn, null, null, true, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      String pathId,
      String pathVariable,
      Request httpRequest) {
    return handleRequest(actorRef, operation, null, null, pathId, pathVariable, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      String pathId,
      String pathVariable,
      boolean isJsonBodyRequired,
      Request httpRequest) {
    return handleRequest(
        actorRef, operation, null, null, pathId, pathVariable, isJsonBodyRequired, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      Map<String, String> headers,
      Request httpRequest) {
    return handleRequest(
        actorRef,
        operation,
        requestBodyJson,
        requestValidatorFn,
        null,
        null,
        headers,
        true,
        httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
          ActorRef actorRef,
          String operation,
          Function requestValidatorFn,
          Map<String, String> headers,
          Request httpRequest) {
    return handleRequest(
            actorRef,
            operation,
            null,
            requestValidatorFn,
            null,
            null,
            headers,
            false,
            httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Request httpRequest) {
    return handleRequest(
        actorRef, operation, null, requestValidatorFn, pathId, pathVariable, false, httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Request httpRequest) {
    return handleRequest(
        actorRef,
        operation,
        requestBodyJson,
        requestValidatorFn,
        pathId,
        pathVariable,
        true,
        httpRequest);
  }

  protected CompletionStage<Result> handleRequest(
      ActorRef actorRef,
      String operation,
      JsonNode requestBodyJson,
      java.util.function.Function requestValidatorFn,
      String pathId,
      String pathVariable,
      boolean isJsonBodyRequired,
      Request httpRequest) {
    return handleRequest(
        actorRef,
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
      ActorRef actorRef,
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Map<String, String> headers,
      boolean isJsonBodyRequired,
      Request httpRequest) {
    org.sunbird.request.Request request = null;
    try {
      if (!isJsonBodyRequired) {
        request = createAndInitRequest(operation, httpRequest);
      } else {
        request = createAndInitRequest(operation, requestBodyJson, httpRequest);
      }
      if (pathId != null) {
        request.getRequest().put(pathVariable, pathId);
        request.getContext().put(pathVariable, pathId);
      }
      if (headers != null) request.getContext().put(JsonKey.HEADER, headers);
      setContextAndPrintEntryLog(httpRequest, request);
      if (requestValidatorFn != null) requestValidatorFn.apply(request);
      return actorResponseHandler(actorRef, request, timeout, null, httpRequest);
    } catch (Exception e) {
      logger.error(
          request.getRequestContext(),
          "BaseController:handleRequest for operation: "
              + operation
              + " Exception occurred with error message = "
              + e.getMessage(),
          e);
      if (e instanceof ProjectCommonException) {
        ProjectCommonException exception =
            new ProjectCommonException(
                (ProjectCommonException) e,
                ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
        e = exception;
        printExitLogOnFailure(request, (ProjectCommonException) e);
      } else {
        printExitLogOnFailure(request, null);
      }
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  protected CompletionStage<Result> handleSearchRequest(
      ActorRef actorRef,
      String operation,
      JsonNode requestBodyJson,
      Function requestValidatorFn,
      String pathId,
      String pathVariable,
      Map<String, String> headers,
      String esObjectType,
      Request httpRequest) {
    org.sunbird.request.Request request = null;
    try {
      if (null != requestBodyJson) {
        request = createAndInitRequest(operation, requestBodyJson, httpRequest);
      } else {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData, null);
      }
      if (pathId != null) {
        request.getRequest().put(pathVariable, pathId);
        request.getContext().put(pathVariable, pathId);
      }
      if (headers != null) request.getContext().put(JsonKey.HEADER, headers);
      request
          .getRequest()
          .put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      setContextAndPrintEntryLog(httpRequest, request);
      if (requestValidatorFn != null) requestValidatorFn.apply(request);
      if (StringUtils.isNotBlank(esObjectType)) {
        List<String> esObjectTypeList = new ArrayList<>();
        esObjectTypeList.add(esObjectType);
        ((Map) (request.getRequest().get(JsonKey.FILTERS)))
            .put(JsonKey.OBJECT_TYPE, esObjectTypeList);
      }
      return actorResponseHandler(actorRef, request, timeout, null, httpRequest);
    } catch (Exception e) {
      logger.error(
          request.getRequestContext(),
          "BaseController:handleSearchRequest: Exception occurred with error message = "
              + e.getMessage(),
          e);
      if (e instanceof ProjectCommonException) {
        ProjectCommonException exception =
            new ProjectCommonException(
                (ProjectCommonException) e,
                ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
        e = exception;
        printExitLogOnFailure(request, (ProjectCommonException) e);
      } else {
        printExitLogOnFailure(request, null);
      }
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  protected void setContextAndPrintEntryLog(
      Request httpRequest, org.sunbird.request.Request request) {
    setContextData(httpRequest, request);
    printEntryLog(request);
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
      Http.Request request, ResponseCode code, ResponseCode headerCode) {
    Response response = new Response();
    response.setId(getApiResponseId(request));
    response.setVer(request.path().split("[/]")[1]);
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(headerCode);
    response.setParams(
        createResponseParamObj(code, null, Common.getFromRequest(request, Attrs.X_REQUEST_ID)));
    return response;
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
    ResponseCode code = ResponseCode.success;
    code.setResponseCode(ResponseCode.OK.getResponseCode());
    response.setParams(
        createResponseParamObj(code, null, Common.getFromRequest(request, Attrs.X_REQUEST_ID)));
    String value = null;
    try {
      if (response.getResult() != null) {
        String json = new ObjectMapper().writeValueAsString(response.getResult());
        value = getResponseSize(json);
      }
    } catch (Exception e) {
      value = "0.0";
    }
    logTelemetry(response, request);
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
    logger.error(
        null,
        exception != null ? exception.getMessage() : "Message is not coming",
        exception,
        generateTelemetryInfoForError(request));
    Response response = new Response();
    response.setVer("");
    if (request != null) {
      response.setVer(getApiVersion(request.path()));
    }
    response.setId(getApiResponseId(request));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(ResponseCode.getResponseCodeByCode(exception.getErrorResponseCode()));
    ResponseCode code = exception.getResponseCode();
    if (code == null) {
      code = ResponseCode.SERVER_ERROR;
    }
    response.setParams(
        createResponseParamObj(
            code, exception.getMessage(), Common.getFromRequest(request, Attrs.X_REQUEST_ID)));
    if (response.getParams() != null) {
      response.getParams().setErr(exception.getErrorCode());
      if (!StringUtils.isBlank(response.getParams().getErrmsg())
          && response.getParams().getErrmsg().contains("{0}")) {
        response.getParams().setErrmsg(exception.getMessage());
      }
    }
    handleBackwardCompatibility(request, exception, response);
    return response;
  }

  private static void handleBackwardCompatibility(
      Request request, ProjectCommonException exception, Response response) {
    // This code is for backwards compatibility
    if (request.path() != null && request.path().startsWith("/v1/otp/generate")) {
      if ("errorRateLimitExceeded".equalsIgnoreCase(exception.getResponseCode().name())) {
        response.getParams().setErr("ERROR_RATE_LIMIT_EXCEEDED");
        response.getParams().setStatus("ERROR_RATE_LIMIT_EXCEEDED");
      }
    }

    if (request.path() != null
        && request.path().startsWith("/v1/otp/verify")
        && ("otpVerificationFailed".equalsIgnoreCase(exception.getResponseCode().name()))) {
      response.getParams().setErr("OTP_VERIFICATION_FAILED");
      response.getParams().setStatus("OTP_VERIFICATION_FAILED");
    }

    if (request.path() != null
        && (request.path().startsWith("/v1/manageduser/create")
            || request.path().startsWith("/v4/user/create"))
        && ("managedUserLimitExceeded".equalsIgnoreCase(exception.getResponseCode().name()))) {
      response.getParams().setErr("MANAGED_USER_LIMIT_EXCEEDED");
      response.getParams().setStatus("MANAGED_USER_LIMIT_EXCEEDED");
    }

    if (request.path() != null
        && (request.path().startsWith("/v1/user/consent/read"))
        && ("resourceNotFound".equalsIgnoreCase(exception.getResponseCode().name()))) {
      response.getParams().setErr("USER_CONSENT_NOT_FOUND");
      response.getParams().setStatus("USER_CONSENT_NOT_FOUND");
    }
    if (request.path() != null
            && (request.path().startsWith("/v1/user/get/"))
            && ("resourceNotFound".equalsIgnoreCase(exception.getResponseCode().name()))) {
      response.getParams().setErr("USER_NOT_FOUND");
      response.getParams().setStatus("USER_NOT_FOUND");
    }
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
    response.setResponseCode(exception.getResponseCode());
    ResponseCode code = exception.getResponseCode();
    response.setParams(createResponseParamObj(code, exception.getMessage(), null));
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
    Response courseResponse = (Response) response;
    if (!StringUtils.isBlank(key)) {
      Object value = courseResponse.getResult().get(JsonKey.RESPONSE);
      courseResponse.getResult().remove(JsonKey.RESPONSE);
      courseResponse.getResult().put(key, value);
    }
    return BaseController.createSuccessResponse(request, courseResponse);
  }

  private static void removeFields(Map<String, Object> params, String... properties) {
    for (String property : properties) {
      params.remove(property);
    }
  }

  /**
   * @param file
   * @return
   */
  public Result createFileDownloadResponse(File file) {
    return Results.ok(file)
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/x-download")
        .withHeader("Content-disposition", "attachment; filename=" + file.getName());
  }

  private String generateStackTrace(StackTraceElement[] elements) {
    StringBuilder builder = new StringBuilder("");
    for (StackTraceElement element : elements) {

      builder.append(element.toString());
      builder.append("\n");
    }
    return ProjectUtil.getFirstNCharacterString(builder.toString(), 100);
  }

  private static Map<String, Object> generateTelemetryRequestForController(
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
    ProjectCommonException exception = null;
    if (e instanceof ProjectCommonException) {
      exception = (ProjectCommonException) e;
    } else {
      exception =
          new ProjectCommonException(
              ResponseCode.serverError,
              ResponseCode.serverError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    generateExceptionTelemetry(request, exception);
    // cleaning request info ...
    return Results.status(
        exception.getErrorResponseCode(), Json.toJson(createResponseOnException(req, exception)));
  }

  private void generateExceptionTelemetry(Request request, ProjectCommonException exception) {
    try {
      String reqContext = Common.getFromRequest(request, Attrs.CONTEXT);
      Map<String, Object> requestInfo =
          objectMapper.readValue(reqContext, new TypeReference<>() {});
      org.sunbird.request.Request reqForTelemetry = new org.sunbird.request.Request();
      Map<String, Object> params = (Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO);
      params.put(JsonKey.LOG_TYPE, JsonKey.API_ACCESS);
      params.put(JsonKey.MESSAGE, "");
      params.put(JsonKey.METHOD, request.method());
      params.put("err", exception.getErrorResponseCode() + "");
      params.put("errtype", exception.getErrorCode());
      if (null != params.get(JsonKey.START_TIME)) {
        long startTime = (Long) params.get(JsonKey.START_TIME);
        params.put(JsonKey.DURATION, calculateApiTimeTaken(startTime));
      }
      removeFields(params, JsonKey.START_TIME);
      params.put(JsonKey.STATUS, String.valueOf(exception.getErrorResponseCode()));
      params.put(JsonKey.LOG_LEVEL, "error");
      params.put(JsonKey.STACKTRACE, generateStackTrace(exception.getStackTrace()));
      reqForTelemetry.setRequest(
          generateTelemetryRequestForController(
              TelemetryEvents.ERROR.getName(),
              params,
              (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT)));
      TelemetryWriter.write(reqForTelemetry);
    } catch (Exception ex) {
      logger.error("Exception occurred while generating Telemetry.", ex);
    }
  }

  private static void logTelemetry(Response response, Request request) {
    if (null != request.path()
        && !(request.path().contains("/health") || request.path().contains("/service/health"))) {
      try {
        String reqContext = Common.getFromRequest(request, Attrs.CONTEXT);
        Map<String, Object> requestInfo =
            objectMapper.readValue(reqContext, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> params = (Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO);
        if (MapUtils.isEmpty(params)) {
          params = new WeakHashMap<>();
        }
        long startTime = System.currentTimeMillis();
        if (null != params.get(JsonKey.START_TIME)) {
          startTime = (Long) params.get(JsonKey.START_TIME);
        }
        removeFields(params, JsonKey.START_TIME);
        params.put(JsonKey.DURATION, calculateApiTimeTaken(startTime));
        params.put(JsonKey.URL, request.uri());
        params.put(JsonKey.METHOD, request.method());
        params.put(JsonKey.LOG_TYPE, JsonKey.API_ACCESS);
        params.put(JsonKey.MESSAGE, "");
        params.put(JsonKey.METHOD, request.method());
        params.put(JsonKey.STATUS, response.getResponseCode()); // result.status()
        params.put(JsonKey.LOG_LEVEL, JsonKey.INFO);

        org.sunbird.request.Request req = new org.sunbird.request.Request();
        req.setRequest(
            generateTelemetryRequestForController(
                TelemetryEvents.LOG.getName(),
                params,
                (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT)));
        TelemetryWriter.write(req);
      } catch (Exception ex) {
        logger.error("BaseController:apply Exception in writing telemetry", ex);
      }
    }
  }

  private static long calculateApiTimeTaken(Long startTime) {
    Long timeConsumed = null;
    if (null != startTime) {
      timeConsumed = System.currentTimeMillis() - startTime;
    }
    return timeConsumed;
  }

  /**
   * This method will make a call to Pekko actor and return CompletionStage.
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
      org.sunbird.request.Request request,
      Timeout timeout,
      String responseKey,
      Request httpReq) {
    Function<Object, Result> function =
        result -> {
          if (ActorOperations.HEALTH_CHECK.getValue().equals(request.getOperation())) {
            setGlobalHealthFlag(result);
          }
          if (result instanceof Response) {
            Response response = (Response) result;
            if (ResponseCode.OK.getResponseCode()
                == (response.getResponseCode().getResponseCode())) {
              Result reslt = createCommonResponse(response, responseKey, httpReq);
              printExitLogOnSuccessResponse(request, response);
              return reslt;
            } else if (ResponseCode.CLIENT_ERROR.getResponseCode()
                == (response.getResponseCode().getResponseCode())) {
              ProjectCommonException exception =
                  new ProjectCommonException(
                      ((ClientErrorResponse) response).getException(),
                      ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
              ((ClientErrorResponse) response).setException(exception);
              Result reslt = createClientErrorResponse(httpReq, (ClientErrorResponse) response);
              printExitLogOnFailure(request, ((ClientErrorResponse) response).getException());
              return reslt;
            } else if (result instanceof ProjectCommonException) {
              Result reslt =
                  createCommonExceptionResponse((ProjectCommonException) result, httpReq);
              printExitLogOnFailure(request, (ProjectCommonException) result);
              return reslt;
            } else if (result instanceof File) {
              logTelemetry(response, httpReq);
              return createFileDownloadResponse((File) result);
            } else {
              if (StringUtils.isNotEmpty((String) response.getResult().get(JsonKey.MESSAGE))
                  && response.getResponseCode().getResponseCode() == 0) {
                Result reslt = createCommonResponse(response, responseKey, httpReq);
                printExitLogOnSuccessResponse(request, response);
                return reslt;
              } else {
                return createCommonExceptionResponse((Exception) result, httpReq);
              }
            }
          } else if (result instanceof ProjectCommonException) {
            Result reslt = createCommonExceptionResponse((ProjectCommonException) result, httpReq);
            printExitLogOnFailure(request, (ProjectCommonException) result);
            return reslt;
          } else if (result instanceof File) {
            return createFileDownloadResponse((File) result);
          } else {
            Result reslt = createCommonExceptionResponse(new Exception(), httpReq);
            printExitLogOnFailure(request, null);
            return reslt;
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
    Response responseObj = createResponseOnException(httpReq, errorResponse.getException());
    responseObj.getResult().putAll(errorResponse.getResult());
    return Results.status(
        errorResponse.getException().getErrorResponseCode(), Json.toJson(responseObj));
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
    final String ver4 = "/" + JsonKey.VERSION_4;
    final String ver5 = "/" + JsonKey.VERSION_5;
    final String privateVersion = "/" + JsonKey.PRIVATE;
    path = path.trim();
    String respId = "";
    if (path.startsWith(ver)
        || path.startsWith(ver2)
        || path.startsWith(ver3)
        || path.startsWith(ver4)
        || path.startsWith(ver5)) {
      String requestUrl = (path.split("\\?"))[0];
      if (requestUrl.contains(ver)) {
        requestUrl = requestUrl.replaceFirst(ver, "api");
      } else if (requestUrl.contains(ver2)) {
        requestUrl = requestUrl.replaceFirst(ver2, "api");
      } else if (requestUrl.contains(ver3)) {
        requestUrl = requestUrl.replaceFirst(ver3, "api");
      } else if (requestUrl.contains(ver4)) {
        requestUrl = requestUrl.replaceFirst(ver4, "api");
      } else if (requestUrl.contains(ver5)) {
        requestUrl = requestUrl.replaceFirst(ver5, "api");
      }
      String[] list = requestUrl.split("/");
      respId = String.join(".", list);
    } else {
      if ("/health".equalsIgnoreCase(path)) {
        respId = "api.all.health";
      } else if (path.startsWith(privateVersion)) {
        String[] list = path.split("/");
        respId = String.join(".", list);
      }
    }
    return respId;
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

  private static Map<String, Object> generateTelemetryInfoForError(Request request) {
    try {
      Map<String, Object> map = new HashMap<>();
      String reqContext = Common.getFromRequest(request, Attrs.CONTEXT);
      Map<String, Object> requestInfo =
          objectMapper.readValue(reqContext, new TypeReference<Map<String, Object>>() {});
      if (requestInfo != null) {
        Map<String, Object> contextInfo = (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT);
        map.put(JsonKey.CONTEXT, contextInfo);
      }
      Map<String, Object> params = new HashMap<>();
      params.put(JsonKey.ERR_TYPE, JsonKey.API_ACCESS);
      map.put(JsonKey.PARAMS, params);
      return map;
    } catch (Exception ex) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return Collections.emptyMap();
  }

  public void setContextData(Http.Request httpReq, org.sunbird.request.Request reqObj) {
    try {
      String context = Common.getFromRequest(httpReq, Attrs.CONTEXT);
      Map<String, Object> requestInfo = objectMapper.readValue(context, new TypeReference<>() {});
      reqObj.setRequestId(Common.getFromRequest(httpReq, Attrs.X_REQUEST_ID));
      reqObj.getContext().putAll((Map<String, Object>) requestInfo.get(JsonKey.CONTEXT));
      reqObj.getContext().putAll((Map<String, Object>) requestInfo.get(JsonKey.ADDITIONAL_INFO));
      reqObj.setRequestContext(getRequestContext(requestInfo, reqObj.getOperation()));
    } catch (Exception ex) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }

  private RequestContext getRequestContext(Map<String, Object> requestInfo, String actorOperation) {
    Map<String, Object> context = (Map<String, Object>) requestInfo.get(JsonKey.CONTEXT);
    RequestContext requestContext =
        new RequestContext(
            (String) context.get(JsonKey.ACTOR_ID),
            (String) context.get(JsonKey.DEVICE_ID),
            (String) context.get(JsonKey.X_Session_ID),
            (String) context.get(JsonKey.APP_ID),
            (String) context.get(JsonKey.X_APP_VERSION),
            (String) context.get(JsonKey.X_REQUEST_ID),
            (String) context.get(JsonKey.X_Source),
            (String)
                ((context.get(JsonKey.X_TRACE_ENABLED) != null)
                    ? context.get(JsonKey.X_TRACE_ENABLED)
                    : debugEnabled),
            actorOperation);
    requestContext.setTelemetryContext(requestInfo);
    return requestContext;
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
  }

  public static String getResponseSize(String response) throws UnsupportedEncodingException {
    if (StringUtils.isNotBlank(response)) {
      return response.getBytes("UTF-8").length + "";
    }
    return "0.0";
  }

  public org.sunbird.request.Request transformUserId(org.sunbird.request.Request request) {
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
