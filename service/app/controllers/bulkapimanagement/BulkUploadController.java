package controllers.bulkapimanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.ByteArrayInputStream;
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
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/**
 * This controller will handle all the request related to bulk api's for user management.
 * 
 * @author Amit Kumar
 */
public class BulkUploadController extends BaseController {

  /**
   * This method will allow to upload bulk user.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> uploadUser() {

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
        InputStream is = new FileInputStream(filePart.get(0).getFile());
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != formUrlEncodeddata) {
        // read data as string from request
        for (Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        InputStream is = new ByteArrayInputStream(
            ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(request().body().asJson(), Request.class);
        InputStream is = new ByteArrayInputStream(
            ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e = new ProjectCommonException(
            ResponseCode.invalidData.getErrorCode(), ResponseCode.invalidData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }
      RequestValidator.validateUploadUser(reqObj);
      reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
      map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);


      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide the status of bulk operation by their processId.
   * 
   * @param processId Stirng
   * @return Promise<Result>
   */
  public Promise<Result> getUploadStatus(String processId) {
    try {
      ProjectLogger.log("get bulk operation status =" + processId, LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.setOperation(ActorOperations.GET_BULK_OP_STATUS.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.PROCESS_ID, processId);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /*
   * This method will allow to upload bulk organisation.
   *
   * @return Promise<Result>
   */
  public Promise<Result> uploadOrg() {

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
        InputStream is = new FileInputStream(filePart.get(0).getFile());
        byteArray = IOUtils.toByteArray(is);
      } else if (null != formUrlEncodeddata) {
        // read data as string from request
        for (Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        InputStream is = new ByteArrayInputStream(
            ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(request().body().asJson(), Request.class);
        InputStream is = new ByteArrayInputStream(
            ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e = new ProjectCommonException(
            ResponseCode.invalidData.getErrorCode(), ResponseCode.invalidData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }
      reqObj.getRequest().putAll(map);
      reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
      map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to upload bulk user.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> bulkBatchEnrollment() {

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
        InputStream is = new FileInputStream(filePart.get(0).getFile());
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != formUrlEncodeddata) {
        // read data as string from request
        for (Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        InputStream is = new ByteArrayInputStream(
            ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(request().body().asJson(), Request.class);
        InputStream is = new ByteArrayInputStream(
            ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e = new ProjectCommonException(
            ResponseCode.invalidData.getErrorCode(), ResponseCode.invalidData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
        return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
      }

      reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.OBJECT_TYPE, JsonKey.BATCH);
      map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);

      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> userDataEncryption() {
    try {
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.ENCRYPT_USER_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> userDataDecryption() {
    try {
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.DECRYPT_USER_DATA.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
