/** */
package org.sunbird.common.models.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.KeycloakRequiredActionLinkUtil;

/**
 * This utility method will handle external http call
 *
 * @author Manzarul
 */
public class HttpUtil {

  private HttpUtil() {}

  /**
   * Makes an HTTP request using GET method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param headers the Map <String,String>
   * @return An String object
   * @throws IOException thrown if any I/O error occurred
   */
  public static String sendGetRequest(String requestURL, Map<String, String> headers)
      throws IOException {
    long startTime = System.currentTimeMillis();
    HttpURLConnection httpURLConnection = getRequest(requestURL, headers, startTime);
    String str = getResponse(httpURLConnection);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil sendGetRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return str;
  }

  /**
   * Makes an HTTP request using GET method to the specified URLand in response it will return Map
   * of status code with get response in String format.
   *
   * @param requestURL the URL of the remote server
   * @param headers the Map <String,String>
   * @return HttpUtilResponse
   * @throws IOException thrown if any I/O error occurred
   */
  public static HttpUtilResponse doGetRequest(String requestURL, Map<String, String> headers)
      throws IOException {
    long startTime = System.currentTimeMillis();
    HttpURLConnection httpURLConnection = getRequest(requestURL, headers, startTime);
    HttpUtilResponse response = null;
    String body = "";
    try {
      body = getResponse(httpURLConnection);
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while reading body" + ex);
    }
    response = new HttpUtilResponse(body, httpURLConnection.getResponseCode());
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil doGetRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return response;
  }

  /**
   * @param requestURL
   * @param headers
   * @param startTime
   * @return
   * @throws MalformedURLException
   * @throws IOException
   * @throws ProtocolException
   */
  private static HttpURLConnection getRequest(
      String requestURL, Map<String, String> headers, long startTime) throws IOException {
    ProjectLogger.log(
        "HttpUtil sendGetRequest method started at =="
            + startTime
            + " for requestURL "
            + requestURL,
        LoggerEnum.PERF_LOG);
    URL url = new URL(requestURL);
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setUseCaches(false);
    httpURLConnection.setDoInput(true);
    httpURLConnection.setDoOutput(false);
    httpURLConnection.setRequestMethod(ProjectUtil.Method.GET.name());
    if (headers != null && headers.size() > 0) {
      setHeaders(httpURLConnection, headers);
    }
    return httpURLConnection;
  }

  /**
   * Makes an HTTP request using POST method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param params A map containing POST data in form of key-value pairs
   * @return String
   * @throws IOException thrown if any I/O error occurred
   */
  public static String sendPostRequest(
      String requestURL, Map<String, String> params, Map<String, String> headers)
      throws IOException {
    long startTime = System.currentTimeMillis();
    HttpURLConnection httpURLConnection = postRequest(requestURL, params, headers, startTime);
    String str = getResponse(httpURLConnection);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil sendPostRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return str;
  }

  /**
   * Makes an HTTP request using POST method to the specified URL and in response it will return Map
   * of status code with post response in String format.
   *
   * @param requestURL the URL of the remote server
   * @param params A map containing POST data in form of key-value pairs
   * @return HttpUtilResponse
   * @throws IOException thrown if any I/O error occurred
   */
  public static HttpUtilResponse doPostRequest(
      String requestURL, Map<String, String> params, Map<String, String> headers)
      throws IOException {
    long startTime = System.currentTimeMillis();
    HttpURLConnection httpURLConnection = postRequest(requestURL, params, headers, startTime);
    HttpUtilResponse response = null;
    String body = "";
    try {
      body = getResponse(httpURLConnection);
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while reading body" + ex);
    }
    response = new HttpUtilResponse(body, httpURLConnection.getResponseCode());
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil doPostRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return response;
  }

  private static HttpURLConnection postRequest(
      String requestURL, Map<String, String> params, Map<String, String> headers, long startTime)
      throws IOException {
    HttpURLConnection httpURLConnection = null;
    OutputStreamWriter writer = null;
    ProjectLogger.log(
        "HttpUtil sendPostRequest method started at =="
            + startTime
            + " for requestURL "
            + requestURL,
        LoggerEnum.PERF_LOG);
    try {
      URL url = new URL(requestURL);
      httpURLConnection = (HttpURLConnection) url.openConnection();
      httpURLConnection.setUseCaches(false);
      httpURLConnection.setDoInput(true);
      httpURLConnection.setRequestMethod(ProjectUtil.Method.POST.name());
      StringBuilder requestParams = new StringBuilder();
      if (params != null && params.size() > 0) {
        httpURLConnection.setDoOutput(true);
        // creates the params string, encode them using URLEncoder
        for (Map.Entry<String, String> entry : params.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          requestParams.append(URLEncoder.encode(key, "UTF-8"));
          requestParams.append("=").append(URLEncoder.encode(value, "UTF-8"));
          requestParams.append("&");
        }
      }
      if (headers != null && headers.size() > 0) {
        setHeaders(httpURLConnection, headers);
      }
      if (requestParams.length() > 0) {
        writer =
            new OutputStreamWriter(httpURLConnection.getOutputStream(), StandardCharsets.UTF_8);
        writer.write(requestParams.toString());
        writer.flush();
      }
    } catch (IOException ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      throw ex;
    } finally {
      if (null != writer) {
        try {
          writer.close();
        } catch (IOException e) {
          ProjectLogger.log(e.getMessage(), e);
        }
      }
    }
    return httpURLConnection;
  }

  /**
   * Makes an HTTP request using POST method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param params A map containing POST data in form of key-value pairs
   * @return An HttpURLConnection object
   * @throws IOException thrown if any I/O error occurred
   */
  public static String sendPostRequest(
      String requestURL, String params, Map<String, String> headers) throws IOException {
    long startTime = System.currentTimeMillis();
    HttpURLConnection httpURLConnection = postRequest(requestURL, params, headers, startTime);
    String str = getResponse(httpURLConnection);
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil sendPostRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return str;
  }

  private static HttpURLConnection postRequest(
      String requestURL, String params, Map<String, String> headers, long startTime)
      throws IOException {
    ProjectLogger.log(
        "HttpUtil sendPostRequest method started at =="
            + startTime
            + " for requestURL "
            + requestURL,
        LoggerEnum.PERF_LOG);
    HttpURLConnection httpURLConnection = null;
    OutputStreamWriter writer = null;
    try {
      URL url = new URL(requestURL);
      httpURLConnection = (HttpURLConnection) url.openConnection();
      httpURLConnection.setUseCaches(false);
      httpURLConnection.setDoInput(true);
      httpURLConnection.setRequestMethod(ProjectUtil.Method.POST.name());
      httpURLConnection.setDoOutput(true);
      if (headers != null && headers.size() > 0) {
        setHeaders(httpURLConnection, headers);
      }
      writer = new OutputStreamWriter(httpURLConnection.getOutputStream(), StandardCharsets.UTF_8);
      writer.write(params);
      writer.flush();
    } catch (IOException e) {
      ProjectLogger.log("HttpUtil:postRequest call failure with error = " + e.getMessage(), e);
      throw e;
    } finally {
      if (null != writer) {
        try {
          writer.close();
        } catch (IOException e) {
          ProjectLogger.log(e.getMessage(), e);
        }
      }
    }
    return httpURLConnection;
  }

  /**
   * Makes an HTTP request using POST method to the specified URL and in response it will return Map
   * of status code with post response in String format.
   *
   * @param requestURL the URL of the remote server
   * @param params A map containing POST data in form of key-value pairs
   * @return HttpUtilResponse
   * @throws IOException thrown if any I/O error occurred
   */
  public static HttpUtilResponse doPostRequest(
      String requestURL, String params, Map<String, String> headers) throws IOException {
    long startTime = System.currentTimeMillis();
    HttpURLConnection httpURLConnection = postRequest(requestURL, params, headers, startTime);
    HttpUtilResponse response = null;
    String body = "";
    try {
      body = getResponse(httpURLConnection);
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while reading body" + ex);
    }
    response = new HttpUtilResponse(body, httpURLConnection.getResponseCode());
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil doPostRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return response;
  }

  private static String getResponse(HttpURLConnection httpURLConnection) throws IOException {
    InputStream inStream = null;
    BufferedReader reader = null;
    StringBuilder builder = new StringBuilder();
    try {
      inStream = httpURLConnection.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
      String line = null;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
    } catch (IOException e) {
      ProjectLogger.log("Error in getResponse HttpUtil:", e);
      throw e;
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          ProjectLogger.log("Error while closing the reader:", e);
        }
      }
      if (inStream != null) {
        try {
          inStream.close();
        } catch (IOException e) {
          ProjectLogger.log("Error while closing the stream:", e);
        }
      }
      if (httpURLConnection != null) {
        httpURLConnection.disconnect();
      }
    }
    return builder.toString();
  }

  /**
   * Makes an HTTP request using PATCH method to the specified URL.
   *
   * @param requestURL the URL of the remote server
   * @param params A map containing POST data in form of key-value pairs
   * @return An HttpURLConnection object
   * @throws IOException thrown if any I/O error occurred
   */
  public static String sendPatchRequest(
      String requestURL, String params, Map<String, String> headers) throws IOException {
    long startTime = System.currentTimeMillis();
    Map<String, Object> logInfo = genarateLogInfo(JsonKey.API_CALL, "API CALL : " + requestURL);
    ProjectLogger.log(
        "HttpUtil sendPatchRequest method started at =="
            + startTime
            + " for requestURL and params "
            + requestURL
            + " param=="
            + params,
        LoggerEnum.PERF_LOG);

    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPatch patch = new HttpPatch(requestURL);
      setHeaders(patch, headers);
      StringEntity entity = new StringEntity(params);
      patch.setEntity(entity);
      CloseableHttpResponse response = httpClient.execute(patch);
      if (response.getStatusLine().getStatusCode() == ResponseCode.OK.getResponseCode()) {
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        ProjectLogger.log(
            "HttpUtil sendPatchRequest method end at =="
                + stopTime
                + " for requestURL "
                + requestURL
                + " ,Total time elapsed = "
                + elapsedTime,
            LoggerEnum.PERF_LOG);
        return ResponseCode.success.getErrorCode();
      }
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      ProjectLogger.log(
          "Patch request failure status code =="
              + response.getStatusLine().getStatusCode()
              + stopTime
              + " for requestURL "
              + requestURL
              + " ,Total time elapsed = "
              + elapsedTime,
          LoggerEnum.PERF_LOG);
      return "Failure";
    } catch (Exception e) {
      ProjectLogger.log("HttpUtil call fails == " + e.getMessage(), e);
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    ProjectLogger.log(
        "HttpUtil sendPatchRequest method end at =="
            + stopTime
            + " for requestURL "
            + requestURL
            + " ,Total time elapsed = "
            + elapsedTime,
        LoggerEnum.PERF_LOG);
    return "Failure";
  }

  /**
   * Set the header for request.
   *
   * @param httpPatch HttpURLConnection
   * @param headers Map<String,String>
   */
  private static void setHeaders(HttpPatch httpPatch, Map<String, String> headers) {
    Iterator<Entry<String, String>> itr = headers.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, String> entry = itr.next();
      httpPatch.setHeader(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Set the header for request.
   *
   * @param httpURLConnection HttpURLConnection
   * @param headers Map<String,String>
   */
  private static void setHeaders(HttpURLConnection httpURLConnection, Map<String, String> headers) {
    Iterator<Entry<String, String>> itr = headers.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, String> entry = itr.next();
      httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
    }
  }

  private static Map<String, Object> genarateLogInfo(String logType, String message) {

    Map<String, Object> info = new HashMap<>();
    info.put(JsonKey.LOG_TYPE, logType);
    long startTime = System.currentTimeMillis();
    info.put(JsonKey.START_TIME, startTime);
    info.put(JsonKey.MESSAGE, message);
    info.put(JsonKey.LOG_LEVEL, JsonKey.INFO);
    return info;
  }

  /**
   * @description this method will send the patch request and in response it will return Map of
   *     status code with patch method response in String format
   * @param requestURL
   * @param params
   * @param headers (Map<String,String>)
   * @return HttpUtilResponse
   * @throws IOException
   */
  public static HttpUtilResponse doPatchRequest(
      String requestURL, String params, Map<String, String> headers) throws IOException {
    long startTime = System.currentTimeMillis();
    Map<String, Object> logInfo = genarateLogInfo(JsonKey.API_CALL, "API CALL : " + requestURL);
    ProjectLogger.log(
        "HttpUtil sendPatchRequest method started at =="
            + startTime
            + " for requestURL "
            + requestURL,
        LoggerEnum.PERF_LOG);

    HttpPatch patch = new HttpPatch(requestURL);
    setHeaders(patch, headers);
    StringEntity entity = new StringEntity(params);
    patch.setEntity(entity);
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      ProjectLogger.log("response code for Patch Resques");
      HttpResponse httpResponse = httpClient.execute(patch);
      HttpUtilResponse response = null;
      String body = "";
      try {
        body = generateResponse(httpResponse);
      } catch (Exception ex) {
        ProjectLogger.log("Exception occurred while reading body" + ex);
      }
      response = new HttpUtilResponse(body, httpResponse.getStatusLine().getStatusCode());
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      ProjectLogger.log(
          "HttpUtil doPatchRequest method end at =="
              + stopTime
              + " for requestURL "
              + requestURL
              + " ,Total time elapsed = "
              + elapsedTime,
          LoggerEnum.PERF_LOG);
      //            telemetryProcessingCall(logInfo);
      return response;
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
      throw e;
    }
  }

  /**
   * @description this method will post the form data and in response it will return Map of status
   *     code with post response in String format
   * @param reqData (Map<String,String>)
   * @param fileData (Map<fileName,byte[]>)
   * @param headers (Map<String,String>)
   * @param url
   * @return HttpUtilResponse
   * @throws IOException
   */
  public static HttpUtilResponse postFormData(
      Map<String, String> reqData,
      Map<String, byte[]> fileData,
      Map<String, String> headers,
      String url)
      throws IOException {
    long startTime = System.currentTimeMillis();
    Map<String, Object> logInfo = genarateLogInfo(JsonKey.API_CALL, "API CALL : " + url);
    ProjectLogger.log(
        "HttpUtil postFormData method started at ==" + startTime + " for requestURL " + url,
        LoggerEnum.PERF_LOG);
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(url);
      Set<Entry<String, String>> headerEntry = headers.entrySet();
      for (Entry<String, String> headerObj : headerEntry) {
        httpPost.addHeader(headerObj.getKey(), headerObj.getValue());
      }

      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      Set<Entry<String, String>> entry = reqData.entrySet();
      for (Entry<String, String> entryObj : entry) {
        builder.addTextBody(
            entryObj.getKey(),
            entryObj.getValue(),
            ContentType.create("text/plain", MIME.UTF8_CHARSET));
      }
      Set<Entry<String, byte[]>> fileEntry = fileData.entrySet();
      for (Entry<String, byte[]> entryObj : fileEntry) {
        if (!StringUtils.isBlank(entryObj.getKey()) && null != entryObj.getValue()) {
          builder.addBinaryBody(
              entryObj.getKey(),
              entryObj.getValue(),
              ContentType.APPLICATION_OCTET_STREAM,
              entryObj.getKey());
        }
      }
      HttpEntity multipart = builder.build();
      httpPost.setEntity(multipart);
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      ProjectLogger.log(
          "HttpUtil postFormData method end at =="
              + stopTime
              + " for requestURL "
              + url
              + " ,Total time elapsed = "
              + elapsedTime,
          LoggerEnum.PERF_LOG);
      HttpResponse httpResponse = client.execute(httpPost);
      HttpUtilResponse response = null;
      String body = "";
      try {
        body = generateResponse(httpResponse);
      } catch (Exception ex) {
        ProjectLogger.log("Exception occurred while reading body" + ex);
      }
      response = new HttpUtilResponse(body, httpResponse.getStatusLine().getStatusCode());
      // telemetryProcessingCall(logInfo);
      return response;
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while calling postFormData method.", ex);
      throw ex;
    }
  }

  private static String generateResponse(HttpResponse httpResponse) throws IOException {
    StringBuilder builder1 = new StringBuilder();
    BufferedReader br =
        new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
    String output;
    while ((output = br.readLine()) != null) {
      builder1.append(output);
    }
    return builder1.toString();
  }

  /**
   * @description this method will process send delete request and in response it will return Map of
   *     status code with post response in String format
   * @param headers
   * @param url
   * @return HttpUtilResponse
   * @throws IOException
   */
  public static HttpUtilResponse sendDeleteRequest(Map<String, String> headers, String url)
      throws IOException {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "HttpUtil sendDeleteRequest method started at ==" + startTime + " for requestURL " + url,
        LoggerEnum.PERF_LOG);
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpDelete httpDelete = new HttpDelete(url);
      ProjectLogger.log("Executing sendDeleteRequest " + httpDelete.getRequestLine());
      Map<String, Object> logInfo = genarateLogInfo(JsonKey.API_CALL, "API CALL : " + url);
      Set<Entry<String, String>> headerEntry = headers.entrySet();
      for (Entry<String, String> headerObj : headerEntry) {
        httpDelete.addHeader(headerObj.getKey(), headerObj.getValue());
      }
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      ProjectLogger.log(
          "HttpUtil sendDeleteRequest method end at =="
              + stopTime
              + " for requestURL "
              + url
              + " ,Total time elapsed = "
              + elapsedTime,
          LoggerEnum.PERF_LOG);
      HttpResponse httpResponse = httpclient.execute(httpDelete);
      HttpUtilResponse response = null;
      String body = "";
      try {
        body = generateResponse(httpResponse);
      } catch (Exception ex) {
        ProjectLogger.log("Exception occurred while reading body" + ex);
      }
      response = new HttpUtilResponse(body, httpResponse.getStatusLine().getStatusCode());
      // telemetryProcessingCall(logInfo);
      return response;
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while calling sendDeleteRequest method.", ex);
      throw ex;
    }
  }

  /**
   * @description this method will process send delete request and in response it will return Map of
   *     status code with post response in String format
   * @param headers
   * @param url
   * @param reqBody Map<String,Object>
   * @return HttpUtilResponse
   * @throws IOException
   */
  public static HttpUtilResponse sendDeleteRequest(
      Map<String, Object> reqBody, Map<String, String> headers, String url) throws IOException {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "HttpUtil sendDeleteRequest method started at ==" + startTime + " for requestURL " + url,
        LoggerEnum.PERF_LOG);
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      ObjectMapper mapper = new ObjectMapper();
      String reqString = mapper.writeValueAsString(reqBody);
      HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
      StringEntity input = new StringEntity(reqString, ContentType.APPLICATION_JSON);
      httpDelete.setEntity(input);
      ProjectLogger.log("Executing sendDeleteRequest " + httpDelete.getRequestLine());
      Map<String, Object> logInfo = genarateLogInfo(JsonKey.API_CALL, "API CALL : " + url);
      Set<Entry<String, String>> headerEntry = headers.entrySet();
      for (Entry<String, String> headerObj : headerEntry) {
        httpDelete.addHeader(headerObj.getKey(), headerObj.getValue());
      }
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      ProjectLogger.log(
          "HttpUtil sendDeleteRequest method end at =="
              + stopTime
              + " for requestURL "
              + url
              + " ,Total time elapsed = "
              + elapsedTime,
          LoggerEnum.PERF_LOG);
      HttpResponse httpResponse = httpclient.execute(httpDelete);
      HttpUtilResponse response = null;
      String body = "";
      try {
        body = generateResponse(httpResponse);
      } catch (Exception ex) {
        ProjectLogger.log("Exception occurred while reading body" + ex);
      }
      response = new HttpUtilResponse(body, httpResponse.getStatusLine().getStatusCode());
      // telemetryProcessingCall(logInfo);
      return response;
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while calling sendDeleteRequest method.", ex);
      throw ex;
    }
  }

  /**
   * this method call the http post method which accept post body as byte[]
   *
   * @param byteArr
   * @param headers
   * @param url
   * @return
   * @throws IOException
   */
  public static HttpUtilResponse postInputStream(
      byte[] byteArr, Map<String, String> headers, String url) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(url);
      HttpEntity entity = new ByteArrayEntity(byteArr);
      httpPost.setEntity(entity);
      Set<Entry<String, String>> headerEntry = headers.entrySet();
      for (Entry<String, String> headerObj : headerEntry) {
        httpPost.addHeader(headerObj.getKey(), headerObj.getValue());
      }
      HttpResponse httpResponse = client.execute(httpPost);
      HttpUtilResponse response = null;
      String body = "";
      try {
        body = generateResponse(httpResponse);
      } catch (Exception ex) {
        ProjectLogger.log("Exception occurred while reading body" + ex);
      }
      response = new HttpUtilResponse(body, httpResponse.getStatusLine().getStatusCode());
      return response;
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while calling posting inputStream data method.", ex);
      throw ex;
    }
  }

  /**
   * @description this method will process send delete request and in response it will return Map of
   *     status code with post response in String format
   * @param headers
   * @param url
   * @param reqBody as JSON String
   * @return HttpUtilResponse
   * @throws IOException
   */
  public static HttpUtilResponse sendDeleteRequest(
      String reqBody, Map<String, String> headers, String url) throws IOException {
    long startTime = System.currentTimeMillis();
    ProjectLogger.log(
        "HttpUtil sendDeleteRequest method started at ==" + startTime + " for requestURL " + url,
        LoggerEnum.PERF_LOG);
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
      StringEntity input = new StringEntity(reqBody, ContentType.APPLICATION_JSON);
      httpDelete.setEntity(input);
      ProjectLogger.log("Executing sendDeleteRequest " + httpDelete.getRequestLine());
      Map<String, Object> logInfo = genarateLogInfo(JsonKey.API_CALL, "API CALL : " + url);
      Set<Entry<String, String>> headerEntry = headers.entrySet();
      for (Entry<String, String> headerObj : headerEntry) {
        httpDelete.addHeader(headerObj.getKey(), headerObj.getValue());
      }
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      ProjectLogger.log(
          "HttpUtil sendDeleteRequest method end at =="
              + stopTime
              + " for requestURL "
              + url
              + " ,Total time elapsed = "
              + elapsedTime,
          LoggerEnum.PERF_LOG);
      HttpResponse httpResponse = httpclient.execute(httpDelete);
      HttpUtilResponse response = null;
      String body = "";
      try {
        body = generateResponse(httpResponse);
      } catch (Exception ex) {
        ProjectLogger.log("Exception occurred while reading body" + ex);
      }
      response = new HttpUtilResponse(body, httpResponse.getStatusLine().getStatusCode());
      // telemetryProcessingCall(logInfo);
      return response;
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while calling sendDeleteRequest method.", ex);
      throw ex;
    }
  }

  public static Map<String, String> getHeader(Map<String, String> input) throws Exception {
    return new HashMap<String, String>() {
      {
        put("Content-Type", "application/json");
        put(
            JsonKey.X_AUTHENTICATED_USER_TOKEN,
            KeycloakRequiredActionLinkUtil.getAdminAccessToken());
        if (MapUtils.isNotEmpty(input)) putAll(input);
      }
    };
  }
}

@NotThreadSafe
class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
  public static final String METHOD_NAME = "DELETE";

  @Override
  public String getMethod() {
    return METHOD_NAME;
  }

  public HttpDeleteWithBody(final String uri) {
    super();
    setURI(URI.create(uri));
  }

  public HttpDeleteWithBody(final URI uri) {
    super();
    setURI(uri);
  }

  public HttpDeleteWithBody() {
    super();
  }
}
