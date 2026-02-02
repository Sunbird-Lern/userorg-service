package org.sunbird.actor.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.request.Request;
import org.sunbird.common.ProjectUtil;

public class BackGroundNotificationActor extends BaseActor {

  private final ObjectMapper mapper = new ObjectMapper();
  private final String notification_service_base_url =
      System.getenv("notification_service_base_url");
  private final String NOTIFICATION_SERVICE_URL =
      notification_service_base_url + "/v1/notification/send";

  @Override
  public void onReceive(Request request) throws Throwable {
    callNotificationService(request);
  }

  private void callNotificationService(Request reqObj) {
    Map<String, Object> request = reqObj.getRequest();
    logger.debug(
        reqObj.getRequestContext(),
        "BackGroundNotificationActor:callNotificationService :: Method called.");
    try {
      logger.debug(
          reqObj.getRequestContext(),
          "BackGroundNotificationActor:callNotificationService :: calling notification service URL :"
              + NOTIFICATION_SERVICE_URL);

      String json = mapper.writeValueAsString(request);
      json = new String(json.getBytes(), StandardCharsets.UTF_8);

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "application/json");
      headers.put("Content-type", "application/json");
      headers.put("requestId", reqObj.getRequestId());
      ProjectUtil.setTraceIdInHeader(headers, reqObj.getRequestContext());
      String response =
          HttpClientUtil.post(NOTIFICATION_SERVICE_URL, json, headers, reqObj.getRequestContext());
      logger.debug(
          reqObj.getRequestContext(),
          "BackGroundNotificationActor:callNotificationService :: Response =" + response);
    } catch (Exception ex) {
      logger.error(
          reqObj.getRequestContext(),
          "BackGroundNotificationActor:callNotificationService :: Error occurred",
          ex);
    }
  }
}
