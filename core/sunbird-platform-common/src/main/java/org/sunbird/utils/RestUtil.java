package org.sunbird.utils;

import org.apache.pekko.dispatch.Futures;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.PropertiesCache;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Utility class for performing REST API operations using Unirest.
 * Supports synchronous and asynchronous JSON requests.
 */
public class RestUtil {

  private static final LoggerUtil logger = new LoggerUtil(RestUtil.class);

  static {
    String apiKey = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(apiKey)) {
      apiKey = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    }
    Unirest.setDefaultHeader("Content-Type", "application/json");
    Unirest.setDefaultHeader("Authorization", "Bearer " + apiKey);
    Unirest.setDefaultHeader("Connection", "Keep-Alive");
  }

  private RestUtil() {}

  /**
   * Executes an asynchronous JSON request.
   *
   * @param request The Unirest BaseRequest to execute.
   * @return A Future containing the HttpResponse with JsonNode.
   */
  public static Future<HttpResponse<JsonNode>> executeAsync(BaseRequest request) {
    logger.debug("RestUtil:executeAsync: request url = " + request.getHttpRequest().getUrl());
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

  /**
   * Executes a synchronous JSON request.
   *
   * @param request The Unirest BaseRequest to execute.
   * @return The HttpResponse with JsonNode.
   * @throws Exception If the request fails.
   */
  public static HttpResponse<JsonNode> execute(BaseRequest request) throws Exception {
    return request.asJson();
  }

  /**
   * Extracts a value from a nested JSON response using a dot-separated key.
   *
   * @param resp The HttpResponse containing the JSON body.
   * @param key The dot-separated key to locate the value.
   * @return The string value at the specified key.
   * @throws Exception If extracting the value fails.
   */
  public static String getFromResponse(HttpResponse<JsonNode> resp, String key) throws Exception {
    String[] nestedKeys = key.split("\\.");
    JSONObject obj = resp.getBody().getObject();

    for (int i = 0; i < nestedKeys.length - 1; i++) {
      String nestedKey = nestedKeys[i];
      if (obj.has(nestedKey)) {
        obj = obj.getJSONObject(nestedKey);
      }
    }

    return obj.getString(nestedKeys[nestedKeys.length - 1]);
  }

  /**
   * Checks if the response status indicates success (HTTP 200).
   *
   * @param resp The HttpResponse to check.
   * @return True if status is 200, false otherwise.
   */
  public static boolean isSuccessful(HttpResponse<JsonNode> resp) {
    return resp.getStatus() == 200;
  }
}
