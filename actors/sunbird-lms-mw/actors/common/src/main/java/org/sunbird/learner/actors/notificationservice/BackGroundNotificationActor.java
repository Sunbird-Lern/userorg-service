package org.sunbird.learner.actors.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ActorConfig(
  tasks = {},
  asyncTasks = {"processNotification"}
)
public class BackGroundNotificationActor extends BaseActor {
  @Override
  public void onReceive(Request request) throws Throwable {
    callNotificationService(request);
  }

  private void callNotificationService(Request reqObj) {
    Map<String, Object> request = reqObj.getRequest();
    ProjectLogger.log("BackGroundNotificationActor:callNotificationService :: Method called.", LoggerEnum.INFO.name());
    try {
      ObjectMapper mapper = new ObjectMapper();
      String notification_service_base_url = System.getenv("notification_service_base_url");
      String NOTIFICATION_SERVICE_URL = notification_service_base_url+"/v1/notification/send";
      ProjectLogger.log("BackGroundNotificationActor:callNotificationService :: calling notification service URL :"+NOTIFICATION_SERVICE_URL,LoggerEnum.INFO.name());

      String json = mapper.writeValueAsString(request);
      json = new String(json.getBytes(), StandardCharsets.UTF_8);

      Map<String,String> headers = new HashMap<>();
      headers.put("Accept", "application/json");
      headers.put("Content-type", "application/json");
      headers.put("requestId", reqObj.getRequestId());

      String response = HttpClientUtil.post(NOTIFICATION_SERVICE_URL,json,headers);
      ProjectLogger.log("BackGroundNotificationActor:callNotificationService :: Response =" + response, LoggerEnum.INFO.name());
    } catch (Exception ex) {
      ProjectLogger.log("BackGroundNotificationActor:callNotificationService :: Error occurred",ex);
    }
  }
}