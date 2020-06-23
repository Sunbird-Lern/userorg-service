package org.sunbird.common.models.util;

import akka.dispatch.Futures;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/** @author Mahesh Kumar Gangula */
public class RestUtil {

  static {
    String apiKey = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(apiKey)) {
      apiKey = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    }
    Unirest.setDefaultHeader("Content-Type", "application/json");
    Unirest.setDefaultHeader("Authorization", "Bearer " + apiKey);
    Unirest.setDefaultHeader("Connection", "Keep-Alive");
  }

  public static Future<HttpResponse<JsonNode>> executeAsync(BaseRequest request) {
    ProjectLogger.log("RestUtil:execute: request url = " + request.getHttpRequest().getUrl());
    Promise<HttpResponse<JsonNode>> promise = Futures.promise();

    request.asJsonAsync(
        new Callback<JsonNode>() {

          @Override
          public void failed(UnirestException e) {
            promise.failure(e);
          }

          @Override
          public void completed(HttpResponse<JsonNode> response) {
            promise.success(response);
          }

          @Override
          public void cancelled() {
            promise.failure(new Exception("cancelled"));
          }
        });

    return promise.future();
  }

  public static HttpResponse<JsonNode> execute(BaseRequest request) throws Exception {
    return request.asJson();
  }

  public static String getFromResponse(HttpResponse<JsonNode> resp, String key) throws Exception {
    String[] nestedKeys = key.split("\\.");
    JSONObject obj = resp.getBody().getObject();

    for (int i = 0; i < nestedKeys.length - 1; i++) {
      String nestedKey = nestedKeys[i];
      if (obj.has(nestedKey)) obj = obj.getJSONObject(nestedKey);
    }

    String val = obj.getString(nestedKeys[nestedKeys.length - 1]);
    return val;
  }

  public static boolean isSuccessful(HttpResponse<JsonNode> resp) {
    int status = resp.getStatus();
    return (status == 200);
  }
}
