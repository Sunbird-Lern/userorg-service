package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.FormUtil.FormApiUtilRequestPayload;
import org.sunbird.models.FormUtil.FormUtilRequest;
import org.sunbird.models.adminutil.Params;

public class FormApiUtilHandler {

  private static LoggerUtil logger = new LoggerUtil(FormApiUtilHandler.class);

  /**
   * Prepare payload for Form Api Config utils
   *
   * @param reqData FormUtilRequest
   * @return formApiUtilReq FormApiUtilRequestPayload
   */
  public static FormApiUtilRequestPayload prepareFormApiUtilPayload(FormUtilRequest reqData) {
    FormApiUtilRequestPayload formApiUtilReq = new FormApiUtilRequestPayload();
    formApiUtilReq.setId(JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD);
    formApiUtilReq.setVer(JsonKey.EKSTEP_SIGNING_SIGN_PAYLOAD_VER);
    formApiUtilReq.setTs(Calendar.getInstance().getTime().getTime());
    formApiUtilReq.setParams(new Params());
    formApiUtilReq.setRequest(reqData);
    return formApiUtilReq;
  }

  /**
   * Fetch Form Api config details of location, userType, userSubType for all states
   *
   * @param reqObject
   * @param context
   * @return
   */
  public static Map<String, Object> fetchFormApiConfigDetails(
      FormApiUtilRequestPayload reqObject, RequestContext context) {

    Map<String, Object> data = null;
    ObjectMapper mapper = new ObjectMapper();
    try {
      String body = mapper.writeValueAsString(reqObject);
      logger.info(
          context, "FormApiUtilHandler :: fetchFormApiConfigDetails: request payload" + body);
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put(
          "Authorization",
          "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ4OFNxQVNvSVFINFluWTY4ejhSOVluWE1qbzYwZWFFUCJ9.nUoXDSIwF9KGOa__Go9jmWk66yCFc1fPsxF-saNyv9M");
      ProjectUtil.setTraceIdInHeader(headers, context);
      String response =
          HttpClientUtil.post(
              ProjectUtil.getConfigValue(JsonKey.FORM_API_BASE_URL)
                  + ProjectUtil.getConfigValue(JsonKey.FORM_API_ENDPOINT),
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
          "FormApiUtilHandler:fetchFormApiConfigDetails Exception occurred : " + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.unableToParseData.getErrorCode(),
          ResponseCode.unableToParseData.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    return data;
  }
}
