package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import play.Application;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

/** Created by arvind on 19/4/18. */
public class TestUtil {

  public static Result performTest(String url, String method, Map map, Application application) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (StringUtils.isNotBlank(data)) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    req.header("Content-Type", "application/json");
    Result result = Helpers.route(application, req);
    return result;
  }

  public static String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return jsonResp;
  }

  public static String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);

      if (response != null) {
        ResponseParams params = response.getParams();
        if (result.status() != 200) {
          return response.getResponseCode().name();
        } else {
          return params.getStatus();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public static int getResponseStatus(Result result) {
    return result.status();
  }
}
