package org.sunbird.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.MapUtils;
import org.apache.http.Consts;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/**
 * HTTP client utility for making REST API calls.
 * 
 * <p>This class provides a thread-safe singleton HTTP client with connection pooling
 * and keep-alive strategy. It supports GET, POST, PATCH, and DELETE operations
 * with custom headers and supports both JSON and form-encoded payloads.
 * 
 * <p>Features:
 * <ul>
 *   <li>Connection pooling with configurable max connections (200 total, 150 per route)</li>
 *   <li>Keep-alive strategy with 180-second timeout</li>
 *   <li>Automatic idle connection cleanup</li>
 *   <li>Comprehensive logging with request context</li>
 * </ul>
 * 
 * @author Sunbird
 * @version 1.0
 */
public class HttpClientUtil {
  
  private static final LoggerUtil logger = new LoggerUtil(HttpClientUtil.class);
  private static final int MAX_TOTAL_CONNECTIONS = 200;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 150;
  private static final int KEEP_ALIVE_TIMEOUT_SECONDS = 180;
  private static final int SUCCESS_STATUS_MIN = 200;
  private static final int SUCCESS_STATUS_MAX = 300;

  private static CloseableHttpClient httpclient = null;
  private static HttpClientUtil httpClientUtil;

  /**
   * Private constructor to initialize the HTTP client with connection pooling.
   * Configures keep-alive strategy and connection manager settings.
   */
  private HttpClientUtil() {
    ConnectionKeepAliveStrategy keepAliveStrategy =
        (response, context) -> {
          HeaderElementIterator it =
              new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
          while (it.hasNext()) {
            HeaderElement he = it.nextElement();
            String param = he.getName();
            String value = he.getValue();
            if (value != null && param.equalsIgnoreCase("timeout")) {
              return Long.parseLong(value) * 1000;
            }
          }
          return KEEP_ALIVE_TIMEOUT_SECONDS * 1000;
        };

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
    connectionManager.closeIdleConnections(KEEP_ALIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    
    httpclient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .useSystemProperties()
            .setKeepAliveStrategy(keepAliveStrategy)
            .build();
    
    logger.info(null, "HttpClientUtil initialized with max connections: " + MAX_TOTAL_CONNECTIONS);
  }

  /**
   * Gets the singleton instance of HttpClientUtil.
   * Thread-safe double-checked locking pattern.
   *
   * @return The singleton HttpClientUtil instance
   */
  public static HttpClientUtil getInstance() {
    if (httpClientUtil == null) {
      synchronized (HttpClientUtil.class) {
        if (httpClientUtil == null) {
          httpClientUtil = new HttpClientUtil();
        }
      }
    }
    return httpClientUtil;
  }

  /**
   * Performs an HTTP GET request.
   *
   * @param requestURL The target URL for the GET request
   * @param headers Optional HTTP headers to include in the request
   * @param context Request context for logging and tracking
   * @return Response body as a string, or empty string if request fails
   */
  public static String get(String requestURL, Map<String, String> headers, RequestContext context) {
    CloseableHttpResponse response = null;
    try {
      logger.debug(context, "HttpClientUtil:get: Making GET request to URL: " + requestURL);
      HttpGet httpGet = new HttpGet(requestURL);
      
      if (MapUtils.isNotEmpty(headers)) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          httpGet.addHeader(entry.getKey(), entry.getValue());
        }
      }
      
      response = httpclient.execute(httpGet);
      return getResponse(response, context, "GET");
    } catch (Exception ex) {
      logger.error(context, "HttpClientUtil:get: Exception occurred while calling GET method for URL: " + requestURL, ex);
      return "";
    } finally {
      closeResponse(response, context, "GET");
    }
  }

  /**
   * Performs an HTTP POST request with JSON payload.
   *
   * @param requestURL The target URL for the POST request
   * @param params The request body as a JSON string
   * @param headers Optional HTTP headers to include in the request
   * @param context Request context for logging and tracking
   * @return Response body as a string, or empty string if request fails
   */
  public static String post(
      String requestURL, String params, Map<String, String> headers, RequestContext context) {
    CloseableHttpResponse response = null;
    try {
      logger.debug(context, "HttpClientUtil:post: Making POST request to URL: " + requestURL);
      HttpPost httpPost = new HttpPost(requestURL);
      
      if (MapUtils.isNotEmpty(headers)) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          httpPost.addHeader(entry.getKey(), entry.getValue());
        }
      }
      
      StringEntity entity = new StringEntity(params, ContentType.APPLICATION_JSON);
      httpPost.setEntity(entity);

      response = httpclient.execute(httpPost);
      return getResponse(response, context, "POST");
    } catch (Exception ex) {
      logger.error(context, "HttpClientUtil:post: Exception occurred while calling POST method for URL: " + requestURL, ex);
      return "";
    } finally {
      closeResponse(response, context, "POST");
    }
  }

  /**
   * Performs an HTTP POST request with form-encoded payload.
   *
   * @param requestURL The target URL for the POST request
   * @param params Form parameters as key-value pairs
   * @param headers Optional HTTP headers to include in the request
   * @param context Request context for logging and tracking
   * @return Response body as a string, or empty string if request fails
   */
  public static String postFormData(
      String requestURL,
      Map<String, String> params,
      Map<String, String> headers,
      RequestContext context) {
    CloseableHttpResponse response = null;
    try {
      logger.debug(context, "HttpClientUtil:postFormData: Making POST form data request to URL: " + requestURL);
      HttpPost httpPost = new HttpPost(requestURL);
      
      if (MapUtils.isNotEmpty(headers)) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          httpPost.addHeader(entry.getKey(), entry.getValue());
        }
      }

      List<NameValuePair> form = new ArrayList<>();
      for (Map.Entry<String, String> entry : params.entrySet()) {
        form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
      httpPost.setEntity(entity);

      response = httpclient.execute(httpPost);
      return getResponse(response, context, "POST_FORM");
    } catch (Exception ex) {
      logger.error(context, "HttpClientUtil:postFormData: Exception occurred while calling POST form data method for URL: " + requestURL, ex);
      return "";
    } finally {
      closeResponse(response, context, "POST_FORM");
    }
  }

  /**
   * Performs an HTTP PATCH request with JSON payload.
   *
   * @param requestURL The target URL for the PATCH request
   * @param params The request body as a JSON string
   * @param headers Optional HTTP headers to include in the request
   * @param context Request context for logging and tracking
   * @return Response body as a string, or empty string if request fails
   */
  public static String patch(
      String requestURL, String params, Map<String, String> headers, RequestContext context) {
    CloseableHttpResponse response = null;
    try {
      logger.debug(context, "HttpClientUtil:patch: Making PATCH request to URL: " + requestURL);
      HttpPatch httpPatch = new HttpPatch(requestURL);
      
      if (MapUtils.isNotEmpty(headers)) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          httpPatch.addHeader(entry.getKey(), entry.getValue());
        }
      }
      
      StringEntity entity = new StringEntity(params, ContentType.APPLICATION_JSON);
      httpPatch.setEntity(entity);

      response = httpclient.execute(httpPatch);
      return getResponse(response, context, "PATCH");
    } catch (Exception ex) {
      logger.error(context, "HttpClientUtil:patch: Exception occurred while calling PATCH method for URL: " + requestURL, ex);
      return "";
    } finally {
      closeResponse(response, context, "PATCH");
    }
  }

  /**
   * Performs an HTTP DELETE request.
   *
   * @param requestURL The target URL for the DELETE request
   * @param headers Optional HTTP headers to include in the request
   * @param context Request context for logging and tracking
   * @return Response body as a string, or empty string if request fails
   */
  public static String delete(
      String requestURL, Map<String, String> headers, RequestContext context) {
    CloseableHttpResponse response = null;
    try {
      logger.debug(context, "HttpClientUtil:delete: Making DELETE request to URL: " + requestURL);
      HttpDelete httpDelete = new HttpDelete(requestURL);
      
      if (MapUtils.isNotEmpty(headers)) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          httpDelete.addHeader(entry.getKey(), entry.getValue());
        }
      }
      
      response = httpclient.execute(httpDelete);
      return getResponse(response, context, "DELETE");
    } catch (Exception ex) {
      logger.error(context, "HttpClientUtil:delete: Exception occurred while calling DELETE method for URL: " + requestURL, ex);
      return "";
    } finally {
      closeResponse(response, context, "DELETE");
    }
  }

  /**
   * Extracts and returns the response body from a successful HTTP response.
   *
   * @param response The HTTP response object
   * @param context Request context for logging
   * @param method The HTTP method name for logging purposes
   * @return Response body as a string
   * @throws IOException If reading the response fails
   */
  private static String getResponse(
      CloseableHttpResponse response, RequestContext context, String method) throws IOException {
    int status = response.getStatusLine().getStatusCode();
    
    if (status >= SUCCESS_STATUS_MIN && status < SUCCESS_STATUS_MAX) {
      HttpEntity httpEntity = response.getEntity();
      StatusLine sl = response.getStatusLine();
      
      logger.debug(
          context,
          "HttpClientUtil:getResponse: Response from "
              + method
              + " call - Status: "
              + sl.getStatusCode()
              + " - "
              + sl.getReasonPhrase());
      
      if (null != httpEntity) {
        byte[] bytes = EntityUtils.toByteArray(httpEntity);
        String resp = new String(bytes);
        logger.info(context, "HttpClientUtil:getResponse: Successfully received response from " + method + " call");
        return resp;
      } else {
        logger.warn(context, "HttpClientUtil:getResponse: Response entity is null for " + method + " call", null);
        return "";
      }
    } else {
      getErrorResponse(response, method, context);
      return "";
    }
  }

  /**
   * Logs error response details when an HTTP request fails.
   *
   * @param response The HTTP response object containing the error
   * @param method The HTTP method name for logging purposes
   * @param context Request context for logging
   */
  private static void getErrorResponse(
      CloseableHttpResponse response, String method, RequestContext context) {
    try {
      HttpEntity httpEntity = response.getEntity();
      byte[] bytes = EntityUtils.toByteArray(httpEntity);
      StatusLine sl = response.getStatusLine();
      String resp = new String(bytes);
      
      logger.error(
          context,
          "HttpClientUtil:getErrorResponse: Error response from "
              + method
              + " call - Response: "
              + resp
              + " - Status: "
              + sl.getStatusCode()
              + " - "
              + sl.getReasonPhrase(),
          null);
    } catch (Exception ex) {
      logger.error(context, "HttpClientUtil:getErrorResponse: Exception occurred while fetching error response for " + method + " method", ex);
    }
  }

  /**
   * Safely closes the HTTP response object.
   *
   * @param response The HTTP response to close
   * @param context Request context for logging
   * @param method The HTTP method name for logging purposes
   */
  private static void closeResponse(
      CloseableHttpResponse response, RequestContext context, String method) {
    if (null != response) {
      try {
        response.close();
        logger.debug(context, "HttpClientUtil:closeResponse: Successfully closed " + method + " response");
      } catch (Exception ex) {
        logger.error(
            context, "HttpClientUtil:closeResponse: Exception occurred while closing " + method + " response object", ex);
      }
    }
  }
}
