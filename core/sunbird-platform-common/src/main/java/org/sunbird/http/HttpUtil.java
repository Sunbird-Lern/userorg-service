package org.sunbird.http;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.response.HttpUtilResponse;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.response.ResponseCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to handle external HTTP calls.
 * Provides methods for GET, POST, and PATCH requests using Unirest.
 */
public class HttpUtil {

  public static LoggerUtil logger = new LoggerUtil(HttpUtil.class);
  private HttpUtil() {}

  /**
   * Makes an HTTP request using GET method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param headers the Map &lt;String,String&gt; containing request headers
   * @return The response body as a String, or an empty string if the call fails with a non-200 status
   * @throws UnirestException thrown if any error occurred during the request
   */
  public static String sendGetRequest(String requestURL, Map<String, String> headers)
          throws UnirestException {
    long startTime = System.currentTimeMillis();
    HttpResponse<String> httpResponse = Unirest.get(requestURL).headers(headers).asString();
    if(200 == httpResponse.getStatus()) {
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      logger.info(null, "HttpUtil:sendGetRequest: Execution finished for URL: " + requestURL + ", duration: " + elapsedTime + " ms");
      return httpResponse.getBody();
    } else {
      logger.error(null, "HttpUtil:sendGetRequest: Failed for URL: " + requestURL + ", Status: " + httpResponse.getStatus() + ", Response: " + httpResponse.getBody(), null);
      return "";
    }
  }

  /**
   * Makes an HTTP request using POST method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param params A map containing POST data in form of key-value pairs
   * @param headers the Map &lt;String,String&gt; containing request headers
   * @return The response body as a String
   * @throws Exception thrown if any error occurred during the request
   */
  public static String sendPostRequest(
      String requestURL, Map<String, String> params, Map<String, String> headers)
      throws Exception {
    long startTime = System.currentTimeMillis();
    HttpResponse<String> httpResponse = Unirest.post(requestURL).headers(headers).body(params).asString();
    String str = httpResponse.getBody();
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    logger.info( null,
        "HttpUtil:sendPostRequest: Execution finished for URL: "
            + requestURL
            + ", Duration: "
            + elapsedTime
            + " ms");
    return str;
  }


  /**
   * Makes an HTTP request using POST method to the specified URL with a String body.
   *
   * @param requestURL the URL of the remote server
   * @param params String payload data
   * @param headers the Map &lt;String,String&gt; containing request headers
   * @return The response body as a String
   * @throws Exception thrown if any error occurred during the request
   */
  public static String sendPostRequest(
      String requestURL, String params, Map<String, String> headers) throws Exception {
    long startTime = System.currentTimeMillis();
    HttpResponse<String> httpResponse = Unirest.post(requestURL).headers(headers).body(params).asString();
    String str = httpResponse.getBody();
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    logger.info( null,
        "HttpUtil:sendPostRequest: Execution finished for URL: "
            + requestURL
            + ", Duration: "
            + elapsedTime
            + " ms");
    return str;
  }


  /**
   * Makes an HTTP request using POST method to the specified URL and returns a comprehensive response object.
   *
   * @param requestURL the URL of the remote server
   * @param params The request body as a String
   * @param headers the Map &lt;String,String&gt; containing request headers
   * @return HttpUtilResponse containing status code and body
   * @throws IOException thrown if any I/O error occurred
   */
  public static HttpUtilResponse doPostRequest(
      String requestURL, String params, Map<String, String> headers) throws IOException {
    long startTime = System.currentTimeMillis();
    HttpUtilResponse response = new HttpUtilResponse();
    try {
      HttpResponse<String> httpResponse = Unirest.post(requestURL).headers(headers).body(params).asString();
      response = new HttpUtilResponse(httpResponse.getBody(), httpResponse.getStatus());
    } catch (Exception ex) {
      logger.error(null, "HttpUtil:doPostRequest: Exception occurred while reading response body for URL: " + requestURL, ex);
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    logger.info(null, 
        "HttpUtil:doPostRequest: Execution finished for URL: "
            + requestURL
            + ", Duration: "
            + elapsedTime
            + " ms");
    return response;
  }

  /**
   * Makes an HTTP request using PATCH method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param params The request body as a String
   * @param headers the Map &lt;String,String&gt; containing request headers
   * @return ResponseCode string success or Failure string
   */
  public static String sendPatchRequest(
      String requestURL, String params, Map<String, String> headers) {
    long startTime = System.currentTimeMillis();
    logger.info(null, 
        "HttpUtil:sendPatchRequest: Started for URL: "
            + requestURL
            + " with params: "
            + params);

    try {
      HttpResponse<String> httpResponse = Unirest.patch(requestURL).headers(headers).body(params).asString();
      
      if (ResponseCode.OK.getResponseCode() == httpResponse.getStatus()) {
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        logger.info(null,
                "HttpUtil:sendPatchRequest: Success for URL: "
                + requestURL
                + ", Status: "
                + httpResponse.getStatus()
                + ", Duration: "
                + elapsedTime
                + " ms");
        return ResponseCode.success.getErrorCode();
      }
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      logger.info(null,
              "HttpUtil:sendPatchRequest: Failed for URL: "
              + requestURL
              + ", Status: "
              + httpResponse.getStatus()
              + ", Duration: "
              + elapsedTime
              + " ms");
      return "Failure";
    } catch (Exception e) {
      logger.error(null, "HttpUtil:sendPatchRequest: Exception for URL: " + requestURL, e);
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    logger.info( null, 
        "HttpUtil:sendPatchRequest: Ended with failure for URL: "
            + requestURL
            + ", Duration: "
            + elapsedTime
            + " ms");
    return "Failure";
  }


  /**
   * Helper method to construct headers.
   * Adds Content-Type: application/json by default.
   *
   * @param input Additional headers map
   * @return A new Map containing all headers
   * @throws Exception if an error occurs
   */
  public static Map<String, String> getHeader(Map<String, String> input) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    if (MapUtils.isNotEmpty(input)) {
      headers.putAll(input);
    }
    return headers;
  }
}