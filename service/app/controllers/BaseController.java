package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import controllers.actorutility.ActorSystemFactory;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
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

  public static final int Akka_wait_time = 10;
  private static Object actorRef = null;

  static {
    actorRef = ActorSystemFactory.getActorSystem().initializeActorSystem();
  }

  /**
   * This method will provide remote Actor selection
   * 
   * @return ActorSelection
   */
  public Object getRemoteActor() {

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
  public static Response createFailureResponse(Request request, ResponseCode code,
      ResponseCode headerCode) {

    Response response = new Response();
    response.setVer(getApiVersion(request.path()));
    response.setId(getApiResponseId(request));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(headerCode);
    response.setParams(createResponseParamObj(code));
    return response;
  }

  /**
   * This method will create response parameter
   * 
   * @param code ResponseCode
   * @return ResponseParams
   */
  public static ResponseParams createResponseParamObj(ResponseCode code) {
    ResponseParams params = new ResponseParams();
    if (code.getResponseCode() != 200) {
      params.setErr(code.getErrorCode());
      params.setErrmsg(code.getErrorMessage());
    }
    params.setMsgid(ExecutionContext.getRequestId());
    params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
    return params;
  }

  /**
   * This method will create data for success response.
   * 
   * @param request play.mvc.Http.Request
   * @param response Response
   * @return Response
   */
  public static Response createSuccessResponse(play.mvc.Http.Request request, Response response) {

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
  public static Response createResponseOnException(play.mvc.Http.Request request,
      ProjectCommonException exception) {
    ProjectLogger.log(exception != null ? exception.getMessage() : "Message is not coming",
        exception);
    Response response = new Response();
    if (request != null) {
      response.setVer(getApiVersion(request.path()));
    } else {
      response.setVer("");
    }
    response.setId(getApiResponseId(request));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(ResponseCode.getHeaderResponseCode(exception.getResponseCode()));
    ResponseCode code = ResponseCode.getResponse(exception.getCode());
    if (code == null) {
      code = ResponseCode.SERVER_ERROR;
    }
    response.setParams(createResponseParamObj(code));
    if (response.getParams() != null && response.getParams().getStatus() != null) {
      response.getParams().setStatus(
          exception.getCode() != null ? exception.getCode() : response.getParams().getStatus());
    }
    if (response.getParams() != null
        && !ProjectUtil.isStringNullOREmpty(response.getParams().getErrmsg())
        && response.getParams().getErrmsg().contains("{0}")) {
      response.getParams().setErrmsg(exception.getMessage());
    }
    return response;
  }


  /**
   * 
   * @param path String
   * @param method String
   * @param exception ProjectCommonException
   * @return Response
   */
  public static Response createResponseOnException(String path, String method,
      ProjectCommonException exception) {

    Response response = new Response();
    response.setVer(getApiVersion(path));
    response.setId(getApiResponseId(path, method));
    response.setTs(ProjectUtil.getFormattedDate());
    response.setResponseCode(ResponseCode.getHeaderResponseCode(exception.getResponseCode()));
    ResponseCode code = ResponseCode.getResponse(exception.getCode());
    response.setParams(createResponseParamObj(code));
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
  public Result createCommonResponse(Object response, String key, play.mvc.Http.Request request) {

    if (response instanceof Response) {
      Response courseResponse = (Response) response;
      if (!ProjectUtil.isStringNullOREmpty(key)) {
        Object value = courseResponse.getResult().get(JsonKey.RESPONSE);
        courseResponse.getResult().remove(JsonKey.RESPONSE);
        courseResponse.getResult().put(key, value);
      }
      return Results.ok(
          Json.toJson(BaseController.createSuccessResponse(request, (Response) courseResponse)));
    } else {
      ProjectCommonException exception = (ProjectCommonException) response;
      return Results.status(exception.getResponseCode(),
          Json.toJson(BaseController.createResponseOnException(request, exception)));
    }
  }


  /**
   * Common exception response handler method.
   * 
   * @param e Exception
   * @param request play.mvc.Http.Request
   * @return Result
   */
  public Result createCommonExceptionResponse(Exception e, play.mvc.Http.Request request) {

    ProjectLogger.log(e.getMessage(), e);
    if (request == null) {
      request = request();
    }
    ProjectCommonException exception = null;
    if (e instanceof ProjectCommonException) {
      exception = (ProjectCommonException) e;
    } else {
      exception = new ProjectCommonException(ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return Results.status(exception.getResponseCode(),
        Json.toJson(BaseController.createResponseOnException(request(), exception)));
  }



  /**
   * This method will make a call to Akka actor and return promise.
   * 
   * @param selection ActorSelection
   * @param request Request
   * @param timeout Timeout
   * @param responseKey String
   * @param httpReq play.mvc.Http.Request
   * @return Promise<Result>
   */
  public Promise<Result> actorResponseHandler(Object actorRef,
      org.sunbird.common.request.Request request, Timeout timeout, String responseKey,
      play.mvc.Http.Request httpReq) {
    if (actorRef instanceof ActorRef) {
      return Promise.wrap(Patterns.ask((ActorRef) actorRef, request, timeout))
          .map(new Function<Object, Result>() {
            public Result apply(Object result) {
              if (result instanceof Response) {
                Response response = (Response) result;
                return createCommonResponse(response, responseKey, httpReq);
              } else if (result instanceof ProjectCommonException) {
                return createCommonExceptionResponse((ProjectCommonException) result, request());
              } else {
                ProjectLogger.log("Unsupported Actor Response format", LoggerEnum.INFO.name());
                return createCommonExceptionResponse(new Exception(), httpReq);
              }
            }
          });
    } else {
      return Promise.wrap(Patterns.ask((ActorSelection) actorRef, request, timeout))
          .map(new Function<Object, Result>() {
            public Result apply(Object result) {
              if (result instanceof Response) {
                Response response = (Response) result;
                return createCommonResponse(response, responseKey, httpReq);
              } else if (result instanceof ProjectCommonException) {
                return createCommonExceptionResponse((ProjectCommonException) result, request());
              } else {
                ProjectLogger.log("Unsupported Actor Response format", LoggerEnum.INFO.name());
                return createCommonExceptionResponse(new Exception(), httpReq);
              }
            }
          });
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
    return ProjectUtil.Environment.prod.getValue();
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
  private static String getApiResponseId(play.mvc.Http.Request request) {

    String val = "";
    if (request != null) {
      String path = request.path();
      if (request.method().equalsIgnoreCase(ProjectUtil.Method.GET.name())) {
        val = Global.apiMap.get(path);
        if (ProjectUtil.isStringNullOREmpty(val)) {
          String[] splitedpath = path.split("[/]");
          path = removeLastValue(splitedpath);
          val = Global.apiMap.get(path);
        }
      } else {
        val = Global.apiMap.get(path);
      }
      if (ProjectUtil.isStringNullOREmpty(val)) {
        val = Global.apiMap.get(path);
        if (ProjectUtil.isStringNullOREmpty(val)) {
          String[] splitedpath = path.split("[/]");
          path = removeLastValue(splitedpath);
          val = Global.apiMap.get(path);
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
      val = Global.apiMap.get(path);
      if (ProjectUtil.isStringNullOREmpty(val)) {
        String[] splitedpath = path.split("[/]");
        path = removeLastValue(splitedpath);
        val = Global.apiMap.get(path);
      }
    } else {
      val = Global.apiMap.get(path);
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

}
