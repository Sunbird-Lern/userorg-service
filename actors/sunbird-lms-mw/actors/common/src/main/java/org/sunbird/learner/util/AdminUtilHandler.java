package org.sunbird.learner.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.json.JSONObject;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AdminUtilHandler {
    public static JSONObject prepareAdminUtilPayload(JSONObject jData){
        JSONObject jReqObject = new JSONObject();
        jReqObject.put(JsonKey.ID, JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD);
        jReqObject.put(JsonKey.VER, JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD_VER);
        jReqObject.put(JsonKey.TS, Calendar.getInstance().getTime().getTime());
        JSONObject jsonObjectParams = new JSONObject();
        jsonObjectParams.put(JsonKey.DEVICE_ID,"");
        jsonObjectParams.put(JsonKey.KEY,"");
        jsonObjectParams.put(JsonKey.MSGID,"");
        jReqObject.put(JsonKey.PARAMS, jsonObjectParams);
        jReqObject.put(JsonKey.REQUEST, jData);
        return jReqObject;
    }

    public static Map<String, Object> fetchEncryptedToken(JSONObject jsonRequestObj){
        Map<String, Object> data = null;
        try {
            String body = jsonRequestObj.toString();

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            String response = HttpUtil.sendPostRequest(
                            ProjectUtil.getConfigValue(JsonKey.ADMINUTIL_BASE_URL) +
                                    ProjectUtil.getConfigValue(JsonKey.ADMINUTIL_SIGN_ENDPOINT),
                            body,
                            headers);
            ObjectMapper mapper = new ObjectMapper();
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
