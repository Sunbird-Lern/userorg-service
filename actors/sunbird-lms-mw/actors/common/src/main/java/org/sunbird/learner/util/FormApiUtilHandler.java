package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
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

  public static Map<String, Object> getFormApiConfig(String stateCode, RequestContext reqContext) {
    FormUtilRequest reqObj = new FormUtilRequest();
    reqObj.setSubType(stateCode);
    reqObj.setType(JsonKey.PROFILE_CONFIG);
    reqObj.setAction(JsonKey.GET);
    reqObj.setComponent("*");
    FormApiUtilRequestPayload formApiUtilRequestPayload = prepareFormApiUtilPayload(reqObj);
    Map<String, Object> formData = fetchFormApiConfigDetails(formApiUtilRequestPayload, reqContext);
    return formData;
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

    Map<String, Object> data = new HashMap<>();
    ObjectMapper mapper = new ObjectMapper();
    try {
      String body = mapper.writeValueAsString(reqObject);
      logger.info(
          context, "FormApiUtilHandler :: fetchFormApiConfigDetails: request payload" + body);
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
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
          "FormApiUtilHandler:fetchEncryptedToken Exception occurred : " + e.getMessage(),
          e);

    } catch (Exception e) {
      logger.error(
          context,
          "FormApiUtilHandler:fetchFormApiConfigDetails Exception occurred : " + e.getMessage(),
          e);
    }

    return data;
  }
}
