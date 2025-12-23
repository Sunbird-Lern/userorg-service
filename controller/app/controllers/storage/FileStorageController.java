package controllers.storage;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.libs.Files;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/** Created by arvind on 28/8/17. */
public class FileStorageController extends BaseController {

  @Inject
  @Named("file_upload_service_actor")
  private ActorRef fileUploadServiceActor;

  /**
   * This method to upload the files on cloud storage .
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> uploadFileService(Http.Request httpRequest) {
    Request reqObj = new Request();
    try {
      Map<String, Object> map = new HashMap<>();
      byte[] byteArray = null;
      MultipartFormData body = httpRequest.body().asMultipartFormData();
      Map<String, String[]> formUrlEncodeddata = httpRequest.body().asFormUrlEncoded();
      JsonNode requestData = httpRequest.body().asJson();
      reqObj.setOperation(ActorOperations.FILE_STORAGE_SERVICE.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      if (body != null) {
        Map<String, String[]> data = body.asFormUrlEncoded();
        for (Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        List<FilePart<Files.TemporaryFile>> filePart = body.getFiles();
        File f = filePart.get(0).getRef().path().toFile();

        InputStream is = new FileInputStream(f);
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.put(JsonKey.FILE_NAME, filePart.get(0).getFilename());
      } else if (null != formUrlEncodeddata) {
        // read data as string from request
        for (Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        InputStream is =
            new ByteArrayInputStream(
                ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(httpRequest.body().asJson(), Request.class);
        InputStream is =
            new ByteArrayInputStream(
                ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e =
            new ProjectCommonException(
                ResponseCode.invalidRequestData,
                ResponseCode.invalidRequestData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        ProjectCommonException exception =
            new ProjectCommonException(
                e, ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));

        return CompletableFuture.completedFuture(
            createCommonExceptionResponse(exception, httpRequest));
      }
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);
      setContextAndPrintEntryLog(httpRequest, reqObj);
      return actorResponseHandler(fileUploadServiceActor, reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }
}
