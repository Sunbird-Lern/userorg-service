package controllers.storage;

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
import org.apache.commons.io.IOUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/** Created by arvind on 28/8/17. */
public class FileStorageController extends BaseController {

  /**
   * This method to upload the files on cloud storage .
   *
   * @return Promise<Result>
   */
  public Promise<Result> uploadFileService() {

    try {

      Request reqObj = new Request();
      Map<String, Object> map = new HashMap<>();
      byte[] byteArray = null;
      MultipartFormData body = request().body().asMultipartFormData();
      Map<String, String[]> formUrlEncodeddata = request().body().asFormUrlEncoded();
      JsonNode requestData = request().body().asJson();
      if (body != null) {
        Map<String, String[]> data = body.asFormUrlEncoded();
        for (Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        List<FilePart> filePart = body.getFiles();
        File f = filePart.get(0).getFile();

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
            (Request) mapper.RequestMapper.mapRequest(request().body().asJson(), Request.class);
        InputStream is =
            new ByteArrayInputStream(
                ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e =
            new ProjectCommonException(
                ResponseCode.invalidData.getErrorCode(),
                ResponseCode.invalidData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }
      reqObj.setOperation(ActorOperations.FILE_STORAGE_SERVICE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);

      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
