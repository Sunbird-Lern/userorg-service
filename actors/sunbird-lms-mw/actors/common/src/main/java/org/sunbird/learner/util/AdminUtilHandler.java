package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.models.adminutil.AdminUtilRequest;
import org.sunbird.models.adminutil.AdminUtilRequestData;
import org.sunbird.models.adminutil.AdminUtilRequestPayload;
import org.sunbird.models.adminutil.Params;

public class AdminUtilHandler {
  private static LoggerUtil logger = new LoggerUtil(AdminUtilHandler.class);

  /**
   * Prepare payload for admin utils
   *
   * @param reqData List<AdminUtilRequestData>
   * @return adminUtilsReq AdminUtilRequestPayload
   */
  public static AdminUtilRequestPayload prepareAdminUtilPayload(
      List<AdminUtilRequestData> reqData) {
    AdminUtilRequestPayload adminUtilsReq = new AdminUtilRequestPayload();
    adminUtilsReq.setId(JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD);
    adminUtilsReq.setVer(JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD_VER);
    adminUtilsReq.setTs(Calendar.getInstance().getTime().getTime());
    adminUtilsReq.setParams(new Params());
    adminUtilsReq.setRequest(new AdminUtilRequest(reqData));
    return adminUtilsReq;
  }

  /**
   * Fetch encrypted token list from admin utils
   *
   * @param reqObject AdminUtilRequestPayload
   * @return encryptedTokenList
   */
  public static Map<String, Object> fetchEncryptedToken(
      AdminUtilRequestPayload reqObject, RequestContext context) {
    Map<String, Object> data = null;
    ObjectMapper mapper = new ObjectMapper();
    try {

      String body = mapper.writeValueAsString(reqObject);
      logger.info(context, "AdminUtilHandler :: fetchEncryptedToken: request payload" + body);
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      ProjectUtil.setTraceIdInHeader(headers, context);
      String response =
          HttpClientUtil.post(
              ProjectUtil.getConfigValue(JsonKey.ADMINUTIL_BASE_URL)
                  + ProjectUtil.getConfigValue(JsonKey.ADMINUTIL_SIGN_ENDPOINT),
              body,
              headers);
      data = mapper.readValue(response, Map.class);
      if (MapUtils.isNotEmpty(data)) {
        data = (Map<String, Object>) data.get(JsonKey.RESULT);
      }
    } catch (IOException e) {
      logger.error(
          context,
          "AdminUtilHandler:fetchEncryptedToken Exception occurred : " + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.unableToConnectToAdminUtil.getErrorCode(),
          ResponseCode.unableToConnectToAdminUtil.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } catch (Exception e) {
      logger.error(
          context,
          "AdminUtilHandler:fetchEncryptedToken Exception occurred : " + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.unableToParseData.getErrorCode(),
          ResponseCode.unableToParseData.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    return data;
  }
}
