package org.sunbird.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.ProjectUtil;
import org.sunbird.model.adminutil.AdminUtilRequest;
import org.sunbird.model.adminutil.AdminUtilRequestData;
import org.sunbird.model.adminutil.AdminUtilRequestPayload;
import org.sunbird.model.adminutil.Params;
import org.sunbird.request.RequestContext;

public class AdminUtilHandler {
  private static final LoggerUtil logger = new LoggerUtil(AdminUtilHandler.class);

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
              headers,
              context);
      data = mapper.readValue(response, Map.class);
      if (MapUtils.isNotEmpty(data)) {
        data = (Map<String, Object>) data.get(JsonKey.RESULT);
      }
    } catch (IOException e) {
      logger.error(
          context,
          "AdminUtilHandler:fetchEncryptedToken Exception occurred : " + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
        ResponseCode.SERVER_ERROR);
    } catch (Exception e) {
      logger.error(
          context,
          "AdminUtilHandler:fetchEncryptedToken Exception occurred : " + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
        ResponseCode.SERVER_ERROR);
    }

    return data;
  }
}
