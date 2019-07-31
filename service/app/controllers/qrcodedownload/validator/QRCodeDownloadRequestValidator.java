package controllers.qrcodedownload.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.List;
import java.util.Map;

public class QRCodeDownloadRequestValidator {
    public static void validateRequest(Request request) {
        Map<String, Object> filterMap = (Map) request.getRequest().get(JsonKey.FILTER);
        if (null == filterMap) {
            throw new ProjectCommonException(
                    ResponseCode.invalidRequestData.getErrorCode(),
                    ResponseCode.invalidRequestData.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        Object userList = filterMap.get(JsonKey.USER_IDs);
        if (null == userList|| !(userList instanceof List) || ((List) userList).isEmpty()) {
            throw new ProjectCommonException(
                    ResponseCode.invalidRequestData.getErrorCode(),
                    ResponseCode.invalidRequestData.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }}
