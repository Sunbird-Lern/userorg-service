package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.BadgeIssuerRequestValidator;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/**
 * Created by arvind on 2/3/18.
 * This controller is for handling the badge issuer apis.
 */
public class BadgeIssuerController extends BaseController {

  /**
   * This method will add badges to user profile.
   * @return Promise<Result>
   */
  public Promise<Result> createBadgeIssuer() {
    try {
      Request reqObj = new Request();
      Map<String, Object> map = new HashMap<>();
      byte[] byteArray = null;
      MultipartFormData body = request().body().asMultipartFormData();
      JsonNode requestData = request().body().asJson();
      Tika tika = new Tika();
      ProjectLogger.log("call to create badge issuer api." + requestData, LoggerEnum.INFO.name());
      if (body != null) {
        Map<String, String[]> data = body.asFormUrlEncoded();
        for (Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        List<FilePart> filePart = body.getFiles();
        if(filePart != null && !filePart.isEmpty()){
          File f = filePart.get(0).getFile();
          String mimeType = tika.detect(f);
          InputStream is = new FileInputStream(f);
          byteArray = IOUtils.toByteArray(is);
          map.put(JsonKey.FILE_NAME , filePart.get(0).getFilename());
        }
      }else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(request().body().asJson(), Request.class);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e = new ProjectCommonException(
            ResponseCode.invalidData.getErrorCode(), ResponseCode.invalidData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }
      map.put(JsonKey.IMAGE, byteArray);
      reqObj.getRequest().putAll(map);
      BadgeIssuerRequestValidator.validateCreateBadgeIssuer(reqObj);
      reqObj.setOperation(BadgingActorOperations.CREATE_BADGE_ISSUER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add badges to user profile.
   * @return Promise<Result>
   */
  public Promise<Result> getBadgeIssuer(String slug) {
    try {
      Request reqObj = new Request();
      reqObj.setOperation(BadgingActorOperations.CREATE_BADGE_ISSUER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.getRequest().put(JsonKey.SLUG , slug);
      reqObj.setEnv(getEnvironment());
      BadgeIssuerRequestValidator.validateCreateBadgeIssuer(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
