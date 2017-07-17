package controllers;

import java.text.MessageFormat;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
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
  public static final int Akka_wait_time = 6;
  private static ActorSelection selection = null;
  static {
    ActorSystem system =
        ActorSystem.create("ActorApplication", ConfigFactory.load().getConfig("ActorConfig"));
    String path = "akka.tcp://RemoteMiddlewareSystem@127.0.1.1:8088/user/RequestRouterActor";
    try {
      path = play.Play.application().configuration().getString("remote.actor.path");
      if (!ProjectUtil.isStringNullOREmpty(System.getenv(JsonKey.SUNBIRD_ACTOR_IP))
          && !ProjectUtil.isStringNullOREmpty(System.getenv(JsonKey.SUNBIRD_ACTOR_PORT))) {
        ProjectLogger.log("value is taking from system evn");
        path = MessageFormat.format(
            play.Play.application().configuration().getString("remote.actor.env.path"),
            System.getenv(JsonKey.SUNBIRD_ACTOR_IP), System.getenv(JsonKey.SUNBIRD_ACTOR_PORT));
      }
       ProjectLogger.log("Actor path is ==" + path, LoggerEnum.INFO.name());
    } catch (Exception e) {
       ProjectLogger.log(e.getMessage(), e);
    }

    selection = system.actorSelection(path);
  }

  /**
   * This method will provide remote Actor selection
   * 
   * @return
   */
  public ActorSelection getRemoteActor() {
    return selection;
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
   * This method will create response param
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
   * @param request String
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
   * @param request String
   * @param exception ProjectCommonException
   * @return Response
   */
  public static Response createResponseOnException(play.mvc.Http.Request request,
      ProjectCommonException exception) {
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
   * @param path
   * @param method
   * @param t
   * @return
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
      return Results.status(exception.getResponseCode(), Json.toJson(BaseController.createResponseOnException(request, exception)));
    }
  }


  /**
   * Common exception response handler method.
   * 
   * @param e Exception
   * @return Result
   */
  public Result createCommonExceptionResponse(Exception e, play.mvc.Http.Request requerst) {
    ProjectLogger.log(e.getMessage(), e);
    if (requerst == null) {
      requerst = request();
    }
    ProjectCommonException exception = null;
    if (e instanceof ProjectCommonException) {
      exception = (ProjectCommonException) e;
    } else {
      exception = new ProjectCommonException(ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return Results.status(exception.getResponseCode(),Json.toJson(BaseController.createResponseOnException(request(), exception)));
  }



  /**
   * This method will make a call to Akka actor and return promise.
   * 
   * @param selection ActorSelection
   * @param request Request
   * @param timeout Timeout
   * @param responseKey String
   * @return Promise<Result>
   */
  public Promise<Result> actorResponseHandler(ActorSelection selection,
      org.sunbird.common.request.Request request, Timeout timeout, String responseKey,
      play.mvc.Http.Request httpReq) {
    Promise<Result> res =
        Promise.wrap(Patterns.ask(selection, request, timeout)).map(new Function<Object, Result>() {
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
    return res;
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

  public String getUserIdByAuthToken(String token) {
    return AuthenticationHelper.verifyUserAccesToken(token);

  }


  /**
   * 
   * @param requerst play.mvc.Http.Request
   * @return
   */
  private static String getApiResponseId(play.mvc.Http.Request requerst) {
    String val = "";
    if (requerst != null) {
      String path = requerst.path();
      if (requerst.method().equalsIgnoreCase(ProjectUtil.Method.GET.name())) {
        val = Global.apiMap.get(path);
        if (ProjectUtil.isStringNullOREmpty(val)) {
          String[] splitedpath = path.split("[/]");
          path = removelastvalue(splitedpath);
          val = Global.apiMap.get(path);
        }
      } else {
        val = Global.apiMap.get(path);
      }
    }
    return val;
  }


  /**
   * 
   * @param path String
   * @param method String
   * @return Stirng
   */
  private static String getApiResponseId(String path, String method) {
    String val = "";
    if (ProjectUtil.Method.GET.name().equalsIgnoreCase(method)) {
      val = Global.apiMap.get(path);
      if (ProjectUtil.isStringNullOREmpty(val)) {
        String[] splitedpath = path.split("[/]");
        path = removelastvalue(splitedpath);
        val = Global.apiMap.get(path);
      }
    } else {
      val = Global.apiMap.get(path);
    }
    return val;
  }

  /**
   * 
   * @param splited String []
   * @return String
   */
  private static String removelastvalue(String splited[]) {
    StringBuilder builder = new StringBuilder();
    if (splited != null && splited.length > 0) {
      for (int i = 1; i < splited.length - 1; i++) {
        builder.append("/" + splited[i]);
      }
    }
    return builder.toString();
  }

}