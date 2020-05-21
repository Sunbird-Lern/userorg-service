package org.sunbird.content.util;

import static java.util.Objects.isNull;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.sunbird.common.exception.ProjectCommonException.throwServerErrorException;
import static org.sunbird.common.models.util.JsonKey.BEARER;
import static org.sunbird.common.models.util.JsonKey.EKSTEP_BASE_URL;
import static org.sunbird.common.models.util.JsonKey.SUNBIRD_AUTHORIZATION;
import static org.sunbird.common.models.util.LoggerEnum.ERROR;
import static org.sunbird.common.models.util.LoggerEnum.INFO;
import static org.sunbird.common.models.util.ProjectLogger.log;
import static org.sunbird.common.models.util.ProjectUtil.getConfigValue;
import static org.sunbird.common.responsecode.ResponseCode.SERVER_ERROR;
import static org.sunbird.common.responsecode.ResponseCode.errorProcessingRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

public class TextBookTocUtil {

  private static ObjectMapper mapper = new ObjectMapper();

  private static Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put(AUTHORIZATION, BEARER + getConfigValue(SUNBIRD_AUTHORIZATION));
    headers.put("Content-Type", "application/json");
    return headers;
  }

  public static Response getRelatedFrameworkById(String frameworkId) {
    log("TextBookTocUtil::getRelatedFrameworkById: frameworkId = " + frameworkId, INFO.name());
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("categories", "topic");
    return handleReadRequest(frameworkId, JsonKey.FRAMEWORK_READ_API_URL, requestParams);
  }

  private static String requestParams(Map<String, String> params) {
    if (null != params) {
      StringBuilder sb = new StringBuilder();
      sb.append("?");
      int i = 0;
      for (Entry param : params.entrySet()) {
        if (i++ > 1) {
          sb.append("&");
        }
        sb.append(param.getKey()).append("=").append(param.getValue());
      }
      return sb.toString();
    } else {
      return "";
    }
  }

  public static Response readContent(String contentId, String url) {
    log("TextBookTocUtil::readContent: contentId = " + contentId, INFO.name());
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("mode", "edit");
    return handleReadRequest(contentId, url, requestParams);
  }

  private static Response handleReadRequest(
      String id, String urlPath, Map<String, String> requestParams) {
    Map<String, String> headers = getHeaders();
    ObjectMapper mapper = new ObjectMapper();

    log("TextBookTocUtil:handleReadRequest: id = " + id, INFO.name());
    Response response = null;
    try {
      String requestUrl =
          getConfigValue(EKSTEP_BASE_URL)
              + getConfigValue(urlPath)
              + "/"
              + id
              + requestParams(requestParams);

      log(
          "TextBookTocUtil:handleReadRequest: Sending GET Request | TextBook Id: "
              + id
              + ", Request URL: "
              + requestUrl,
          INFO.name());

      HttpResponse<String> httpResponse = Unirest.get(requestUrl).headers(headers).asString();

      if (StringUtils.isBlank(httpResponse.getBody())) {
        log(
            "TextBookTocUtil:handleReadRequest: Received Empty Response | TextBook Id: "
                + id
                + ", Request URL: "
                + requestUrl,
            ERROR.name());
        throwServerErrorException(
            ResponseCode.SERVER_ERROR, errorProcessingRequest.getErrorMessage());
      }
      ProjectLogger.log(
          "Sized :TextBookTocUtil:handleReadRequest: "
              + " TextBook Id: "
              + id
              + " | Request URL: "
              + requestUrl
              + "  | size of response "
              + httpResponse.getBody().getBytes().length,
          INFO);

      response = mapper.readValue(httpResponse.getBody(), Response.class);
      if (!ResponseCode.OK.equals(response.getResponseCode())) {
        log(
            "TextBookTocUtil:handleReadRequest: Response code is not ok | TextBook Id: "
                + id
                + "| Request URL: "
                + requestUrl,
            ERROR.name());
        throw new ProjectCommonException(
            response.getResponseCode().name(),
            response.getParams().getErrmsg(),
            response.getResponseCode().getResponseCode());
      }
    } catch (IOException e) {
      log(
          "TextBookTocUtil:handleReadRequest: Exception occurred with error message = "
              + e.getMessage(),
          e);
      throwServerErrorException(ResponseCode.SERVER_ERROR);
    } catch (UnirestException e) {
      log(
          "TextBookTocUtil:handleReadRequest: Exception occurred with error message = "
              + e.getMessage(),
          e);
      throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return response;
  }

  public static <T> T getObjectFrom(String s, Class<T> clazz) {
    if (StringUtils.isBlank(s)) {
      log("Invalid String cannot be converted to Map.", ERROR.name());
      throw new ProjectCommonException(
          errorProcessingRequest.getErrorCode(),
          errorProcessingRequest.getErrorMessage(),
          SERVER_ERROR.getResponseCode());
    }

    try {
      return mapper.readValue(s, clazz);
    } catch (IOException e) {
      log("Error Mapping File input Mapping Properties.", ERROR.name());
      throw new ProjectCommonException(
          errorProcessingRequest.getErrorCode(),
          errorProcessingRequest.getErrorMessage(),
          SERVER_ERROR.getResponseCode());
    }
  }

  public static <T> String serialize(T o) {
    try {
      return mapper.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      log("Error Serializing Object To String", ERROR.name());
      throw new ProjectCommonException(
          errorProcessingRequest.getErrorCode(),
          errorProcessingRequest.getErrorMessage(),
          SERVER_ERROR.getResponseCode());
    }
  }

  public static Object stringify(Object o) {
    if (isNull(o)) return "";
    if (o instanceof List) {
      List l = (List) o;
      if (!l.isEmpty() && l.get(0) instanceof String) {
        return String.join(",", l);
      }
    }
    if (o instanceof String[]) {
      String[] l = (String[]) o;
      if (l.length > 0) {
        return String.join(",", l);
      }
    }
    return o;
  }
}
