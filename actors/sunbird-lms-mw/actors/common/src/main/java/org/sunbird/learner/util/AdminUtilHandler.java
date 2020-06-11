package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.*;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.adminutil.AdminUtilParams;
import org.sunbird.models.adminutil.AdminUtilRequestPayload;
import org.sunbird.models.adminutil.AdminUtilRequestData;
import org.sunbird.models.adminutil.AdminUtilRequest;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUtilHandler {

    /**
     * Prepare payload for admin utils
     * @param reqData List<AdminUtilRequestData>
     * @return adminUtilsReq AdminUtilRequestPayload
     */
    public static AdminUtilRequestPayload prepareAdminUtilPayload(List<AdminUtilRequestData> reqData){
        AdminUtilRequestPayload adminUtilsReq = new AdminUtilRequestPayload();
        adminUtilsReq.setId(JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD);
        adminUtilsReq.setVer(JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD_VER);
        adminUtilsReq.setTs(Calendar.getInstance().getTime().getTime());
        adminUtilsReq.setParams(new AdminUtilParams());
        adminUtilsReq.setRequest(new AdminUtilRequest(reqData));
        return adminUtilsReq;
    }

    /**
     * Fetch encrypted token list from admin utils
     * @param reqObject AdminUtilRequestPayload
     * @return encryptedTokenList
     */
    public static Map<String, Object> fetchEncryptedToken(AdminUtilRequestPayload reqObject){
        Map<String, Object> data = null;
        ObjectMapper mapper = new ObjectMapper();
        try {

            String body = mapper.writeValueAsString(reqObject);
            ProjectLogger.log(
                    "AdminUtilHandler :: fetchEncryptedToken: request payload" + body,
                    LoggerEnum.DEBUG);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            String response = HttpUtil.sendPostRequest(
                            ProjectUtil.getConfigValue(JsonKey.ADMINUTIL_BASE_URL) +
                                    ProjectUtil.getConfigValue(JsonKey.ADMINUTIL_SIGN_ENDPOINT),
                            body,
                            headers);
            ProjectLogger.log(
                    "AdminUtilHandler :: fetchEncryptedToken: response payload" + response,
                    LoggerEnum.DEBUG);
            data = mapper.readValue(response, Map.class);
            if (MapUtils.isNotEmpty(data)) {
                data = (Map<String, Object>) data.get(JsonKey.RESULT);
            }
        } catch (IOException e) {
            throw new ProjectCommonException(
                    ResponseCode.unableToConnectToAdminUtil.getErrorCode(),
                    ResponseCode.unableToConnectToAdminUtil.getErrorMessage(),
                    ResponseCode.SERVER_ERROR.getResponseCode());
        } catch (Exception e) {
            throw new ProjectCommonException(
                    ResponseCode.unableToParseData.getErrorCode(),
                    ResponseCode.unableToParseData.getErrorMessage(),
                    ResponseCode.SERVER_ERROR.getResponseCode());
        }

        return data;
    }
}
