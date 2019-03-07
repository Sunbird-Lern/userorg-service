package controllers.textbook;

import static org.sunbird.common.exception.ProjectCommonException.throwClientErrorException;

import akka.util.Timeout;
import controllers.BaseController;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.TextbookActorOperation;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Handles Textbook TOC APIs.
 *
 * @author gauraw
 */
public class TextbookController extends BaseController {

  private static final int UPLOAD_TOC_TIMEOUT = 30;

  public Promise<Result> uploadTOC(String textbookId) {
    try {
      Request request =
          createAndInitUploadRequest(
              TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue(), JsonKey.TEXTBOOK);
      request.put(JsonKey.TEXTBOOK_ID, textbookId);
      request.setTimeout(UPLOAD_TOC_TIMEOUT);
      Timeout uploadTimeout = new Timeout(UPLOAD_TOC_TIMEOUT, TimeUnit.SECONDS);
      return actorResponseHandler(getActorRef(), request, uploadTimeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getTocUrl(String textbookId) {
    try {
      return handleRequest(
          TextbookActorOperation.TEXTBOOK_TOC_URL.getValue(), textbookId, JsonKey.TEXTBOOK_ID);
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  @Override
  public Request createAndInitUploadRequest(String operation, String objectType)
      throws IOException {
    ProjectLogger.log("API call for operation : " + operation);
    Request reqObj = new Request();
    Map<String, Object> map = new HashMap<>();
    InputStream inputStream = null;

    String fileUrl = request().getQueryString(JsonKey.FILE_URL);
    if (StringUtils.isNotBlank(fileUrl)) {
      ProjectLogger.log("Got fileUrl from path parameter: " + fileUrl, LoggerEnum.INFO.name());
      URL url = new URL(fileUrl.trim());
      inputStream = url.openStream();
    } else {
      Http.MultipartFormData body = request().body().asMultipartFormData();
      if (body != null) {
        Map<String, String[]> data = body.asFormUrlEncoded();
        if (MapUtils.isNotEmpty(data) && data.containsKey(JsonKey.FILE_URL)) {
          fileUrl = data.getOrDefault(JsonKey.FILE_URL, new String[] {""})[0];
          if (StringUtils.isBlank(fileUrl) || !StringUtils.endsWith(fileUrl, ".csv")) {
            throwClientErrorException(
                ResponseCode.csvError, ResponseCode.csvError.getErrorMessage());
          }
          URL url = new URL(fileUrl.trim());
          inputStream = url.openStream();
        } else {
          List<Http.MultipartFormData.FilePart> filePart = body.getFiles();
          if (CollectionUtils.isEmpty(filePart)) {
            throwClientErrorException(
                ResponseCode.fileNotFound, ResponseCode.fileNotFound.getErrorMessage());
          }
          inputStream = new FileInputStream(filePart.get(0).getFile());
        }
      } else {
        ProjectLogger.log("Textbook toc upload request body is empty", LoggerEnum.INFO.name());
        throwClientErrorException(
            ResponseCode.invalidData, ResponseCode.invalidData.getErrorMessage());
      }
    }

    byte[] byteArray = IOUtils.toByteArray(inputStream);
    try {
      if (null != inputStream) {
        inputStream.close();
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "TextbookController:createAndInitUploadRequest : Exception occurred while closing stream");
    }
    reqObj.setOperation(operation);
    reqObj.setRequestId(ExecutionContext.getRequestId());
    reqObj.setEnv(getEnvironment());
    map.put(JsonKey.OBJECT_TYPE, objectType);
    map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
    map.put(JsonKey.DATA, byteArray);
    reqObj.setRequest(map);
    return reqObj;
  }
}
