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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.IOUtils;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Files;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/** Created by arvind on 2/3/18. This controller is for handling the badge issuer apis. */
public class BadgeIssuerController extends BaseController {

  /**
   * This method will add badges to user profile.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> createBadgeIssuer(Http.Request httpRequest) {
    try {
      Request reqObj = new Request();
      Map<String, Object> map = new HashMap<>();
      MultipartFormData body = httpRequest.body().asMultipartFormData();
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("call to create badge issuer api." + requestData, LoggerEnum.DEBUG.name());
      if (body != null) {
        map = readFormData(body, map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(httpRequest.body().asJson(), Request.class);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e =
            new ProjectCommonException(
                ResponseCode.invalidData.getErrorCode(),
                ResponseCode.invalidData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
      }
      reqObj.getRequest().putAll(map);
      new BadgeIssuerRequestValidator().validateCreateBadgeIssuer(reqObj);
      reqObj =
          setExtraParam(
              reqObj,
              httpRequest.flash().get(JsonKey.REQUEST_ID),
              BadgeOperations.createBadgeIssuer.name(),
              httpRequest.flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will add badges to user profile.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getBadgeIssuer(String issuerId, Http.Request httpRequest) {
    try {
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              httpRequest.flash().get(JsonKey.REQUEST_ID),
              BadgeOperations.getBadgeIssuer.name(),
              httpRequest.flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.SLUG, issuerId);
      new BadgeIssuerRequestValidator().validateGetBadgeIssuerDetail(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will add badges to user profile.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getAllIssuer(Http.Request httpRequest) {
    try {
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              httpRequest.flash().get(JsonKey.REQUEST_ID),
              BadgeOperations.getAllIssuer.name(),
              httpRequest.flash().get(JsonKey.USER_ID),
              getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will add badges to user profile.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> deleteBadgeIssuer(String issuerId, Http.Request httpRequest) {
    try {
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              httpRequest.flash().get(JsonKey.REQUEST_ID),
              BadgeOperations.deleteIssuer.name(),
              httpRequest.flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.SLUG, issuerId);
      new BadgeIssuerRequestValidator().validateGetBadgeIssuerDetail(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
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
    List<FilePart<Files.TemporaryFile>> filePart = body.getFiles();
    if (filePart != null && !filePart.isEmpty()) {
      File f = filePart.get(0).getRef().path().toFile();
      InputStream is = new FileInputStream(f);
      byte[] byteArray = IOUtils.toByteArray(is);
      map.put(JsonKey.FILE_NAME, filePart.get(0).getFilename());
      map.put(JsonKey.IMAGE, byteArray);
    }
    return map;
  }
}
