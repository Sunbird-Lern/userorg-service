package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

public class SearchUtil {
  private static final String BASE_URL =
      ProjectUtil.getConfigValue(JsonKey.SEARCH_SERVICE_API_BASE_URL);
  private static final String SEARCH_URL = "/v3/search";
  private static ObjectMapper mapper = new ObjectMapper();

  private static Map<String, String> getHeaders() {
    return new HashMap<String, String>() {
      {
        put(JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.EKSTEP_AUTHORIZATION));
        put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        put(HttpHeaders.ACCEPT_ENCODING.toLowerCase(), "UTF-8");
      }
    };
  }

  public static List<Map<String, Object>> search(String payload)
      throws UnirestException, IOException {
    if (StringUtils.isNotBlank(payload)) {
      HttpResponse<String> httpResponse =
          Unirest.post(BASE_URL + SEARCH_URL).headers(getHeaders()).body(payload).asString();
      if (httpResponse.getStatus() == 200) {
        Response response = mapper.readValue(httpResponse.getBody(), Response.class);
        return (List<Map<String, Object>>) response.get("content");
      } else {
        throw new ProjectCommonException(
            ResponseCode.SERVER_ERROR.name(),
            httpResponse.getBody(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
    }
    return null;
  }
}
