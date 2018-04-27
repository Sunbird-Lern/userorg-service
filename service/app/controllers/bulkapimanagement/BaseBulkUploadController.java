package controllers.bulkapimanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;

/**
 * Class to provide common functionality to ulk upload controllers.
 *
 * @author arvind
 */
public class BaseBulkUploadController extends BaseController {

  private final String FILE_SIZE_UNIT = "MB";

  private final Integer MB_to_byte = 1000000;
  /**
   * Helper method for creating and initialising a request for given operation for content type
   * Multiform data.
   *
   * @param operation A defined actor operation
   * @param objectType A defined type of object to set in he request body
   * @return Created and initialised Request (@see {@link org.sunbird.common.request.Request})
   *     instance.
   */
  protected org.sunbird.common.request.Request createAndInitBulkRequest(
      String operation, String objectType, Boolean validateFileZize) throws IOException {
    ProjectLogger.log("API call for operation : " + operation);
    org.sunbird.common.request.Request reqObj = new org.sunbird.common.request.Request();
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
      if (filePart != null && !filePart.isEmpty()) {
        InputStream is = new FileInputStream(filePart.get(0).getFile());
        byteArray = IOUtils.toByteArray(is);
      }
    } else if (null != formUrlEncodeddata) {
      // read data as string from request
      for (Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
        map.put(entry.getKey(), entry.getValue()[0]);
      }
      InputStream is =
          new ByteArrayInputStream(
              ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
      byteArray = IOUtils.toByteArray(is);
    } else if (null != requestData) {
      reqObj =
          (org.sunbird.common.request.Request)
              mapper.RequestMapper.mapRequest(
                  request().body().asJson(), org.sunbird.common.request.Request.class);
      InputStream is =
          new ByteArrayInputStream(
              ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
      byteArray = IOUtils.toByteArray(is);
      reqObj.getRequest().remove(JsonKey.DATA);
      map.putAll(reqObj.getRequest());
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidData.getErrorCode(),
          ResponseCode.invalidData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (validateFileZize) {
      checkFileSize(byteArray, objectType);
    }
    reqObj.setOperation(operation);
    reqObj.setRequestId(ExecutionContext.getRequestId());
    reqObj.setEnv(getEnvironment());
    map.put(JsonKey.OBJECT_TYPE, objectType);
    map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
    map.put(JsonKey.FILE, byteArray);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.DATA, map);
    reqObj.setRequest(innerMap);
    return reqObj;
  }

  private void checkFileSize(byte[] byteArray, String objectType) {

    if (null == byteArray) {
      throw new ProjectCommonException(
          ResponseCode.missingFileAttachment.getErrorCode(),
          ResponseCode.missingFileAttachment.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (objectType == JsonKey.LOCATION) {
      // allowed max size in MB
      String allowedMaxSize = ProjectUtil.getConfigValue(JsonKey.UPLOAD_FILE_MAX_SIZE);
      if (StringUtils.isEmpty(allowedMaxSize)) {
        throw new ProjectCommonException(
            ResponseCode.fileAttachmentSizeNotConfigured.getErrorCode(),
            ResponseCode.fileAttachmentSizeNotConfigured.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      Double filesize = Double.parseDouble(allowedMaxSize.trim());
      filesize = filesize * MB_to_byte;
      // converting MB to bytes
      Long allowedSize = filesize.longValue();
      if (byteArray.length > allowedSize) {
        throw new ProjectCommonException(
            ResponseCode.sizeLimitExceed.getErrorCode(),
            ResponseCode.sizeLimitExceed.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode(),
            allowedMaxSize + FILE_SIZE_UNIT);
      }
    }
  }
}
