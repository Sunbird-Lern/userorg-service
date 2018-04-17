package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.badging.validator.BadgeIssuerRequestValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/** Created by arvind on 2/3/18. This controller is for handling the badge issuer apis. */
public class BadgeIssuerController extends BaseController {

  /**
   * This method will add badges to user profile.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createBadgeIssuer() {
    try {
      Request reqObj = new Request();
      Map<String, Object> map = new HashMap<>();
      MultipartFormData body = request().body().asMultipartFormData();
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("call to create badge issuer api." + requestData, LoggerEnum.DEBUG.name());
      if (body != null) {
        map = readFormData(body, map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(request().body().asJson(), Request.class);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e =
            new ProjectCommonException(
                ResponseCode.invalidData.getErrorCode(),
                ResponseCode.invalidData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }
      reqObj.getRequest().putAll(map);
      BadgeIssuerRequestValidator.validateCreateBadgeIssuer(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgeOperations.createBadgeIssuer.name(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add badges to user profile.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getBadgeIssuer(String issuerId) {
    try {
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgeOperations.getBadgeIssuer.name(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.SLUG, issuerId);
      BadgeIssuerRequestValidator.validateGetBadgeIssuerDetail(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add badges to user profile.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getAllIssuer() {
    try {
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgeOperations.getAllIssuer.name(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add badges to user profile.
   *
   * @return Promise<Result>
   */
  public Promise<Result> deleteBadgeIssuer(String issuerId) {
    try {
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              BadgeOperations.deleteIssuer.name(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.SLUG, issuerId);
      BadgeIssuerRequestValidator.validateGetBadgeIssuerDetail(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will read from input data and put all the requested key and value inside a map.
   *
   * @param body MultipartFormData
   * @param map Map<String,Object>
   * @return Map<String,Object>
   * @throws IOException
   */
  private Map<String, Object> readFormData(MultipartFormData body, Map<String, Object> map)
      throws IOException {
    Map<String, String[]> data = body.asFormUrlEncoded();
    for (Entry<String, String[]> entry : data.entrySet()) {
      map.put(entry.getKey(), entry.getValue()[0]);
    }
    List<FilePart> filePart = body.getFiles();
    if (filePart != null && !filePart.isEmpty()) {
      File f = filePart.get(0).getFile();
      InputStream is = new FileInputStream(f);
      byte[] byteArray = IOUtils.toByteArray(is);
      map.put(JsonKey.FILE_NAME, filePart.get(0).getFilename());
      map.put(JsonKey.IMAGE, byteArray);
    }
    return map;
  }
}
