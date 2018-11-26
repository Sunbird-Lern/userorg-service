package controllers.textbook;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import org.apache.commons.io.IOUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This base controller class has all common methods for Textbook Toc API
 * @author gauraw
 */
public class BaseTextbookController extends BaseController {

    /**
     * @param operation
     * @param objectType
     * @return
     * @throws IOException
     */
    protected org.sunbird.common.request.Request createAndInitUploadRequest(
            String operation, String objectType) throws IOException {
        ProjectLogger.log("API call for operation : " + operation);
        org.sunbird.common.request.Request reqObj = new org.sunbird.common.request.Request();
        Map<String, Object> map = new HashMap<>();
        byte[] byteArray = null;
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Map<String, String[]> formUrlEncodeddata = request().body().asFormUrlEncoded();
        JsonNode requestData = request().body().asJson();
        if (body != null) {
            Map<String, String[]> data = body.asFormUrlEncoded();
            for (Map.Entry<String, String[]> entry : data.entrySet()) {
                map.put(entry.getKey(), entry.getValue()[0]);
            }
            List<Http.MultipartFormData.FilePart> filePart = body.getFiles();
            if (filePart != null && !filePart.isEmpty()) {
                InputStream is = new FileInputStream(filePart.get(0).getFile());
                byteArray = IOUtils.toByteArray(is);
            }
        } else if (null != formUrlEncodeddata) {
            for (Map.Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
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
}
