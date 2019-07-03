package org.sunbird.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;

public class ContentService {

  private static final String contentServiceBaseUrl = System.getenv(JsonKey.SUNBIRD_API_BASE_URL);
  private static ObjectMapper mapper = new ObjectMapper();
  private static Map<String, String> headers = new HashMap<String, String>();
  private static final String BADGE_ASSERTION = "badgeAssertion";

  static {
    String authorization = System.getenv(JsonKey.SUNBIRD_AUTHORIZATION);
    if (StringUtils.isBlank(authorization)) {
      authorization = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_AUTHORIZATION);
    } else {
      authorization = JsonKey.BEARER + authorization;
    }
    headers.put(JsonKey.AUTHORIZATION, authorization);
    headers.put("Content-Type", "application/json");
  }

  public static Response assignBadge(Request request) throws Exception {
    return processBadge(request, "ASSIGNBADGE");
  }

  public static Response revokeBadge(Request request) throws Exception {
    return processBadge(request, "REVOKEBADGE");
  }

  @SuppressWarnings("unchecked")
  private static Response processBadge(Request request, String operation) throws Exception {
    String id = (String) request.getRequest().get("id");
    Map<String, Object> badge = (Map<String, Object>) request.getRequest().get(BADGE_ASSERTION);
    Map<String, String> props = getProperties(operation);
    if (StringUtils.isBlank(id)) {
      throw new ProjectCommonException(
          props.get("errCode"),
          "Please provide content id.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (null == badge || badge.isEmpty()) {
      throw new ProjectCommonException(
          props.get("errCode"),
          "Please provide badge details.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    String badgeStr = mapper.writeValueAsString(badge);
    String reqBody =
        "{\"request\": {\"content\": {\"" + BADGE_ASSERTION + "\": " + badgeStr + "}}}";

    String url = props.get("basePath") + id;
    ProjectLogger.log(
        "Making call to update badge for content: " + url,
        request.getRequest(),
        LoggerEnum.INFO.name());
    String result = HttpUtil.sendPostRequest(url, reqBody, headers);
    ProjectLogger.log(
        "Status for badge processing of content: " + result,
        request.getRequest(),
        LoggerEnum.INFO.name());
    // TODO: Get the response and return msg based on it's value.
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private static Map<String, String> getProperties(String operation) {
    Map<String, String> props = new HashMap<String, String>();
    switch (operation.toUpperCase()) {
      case "ASSIGNBADGE":
        props.put(
            "basePath",
            contentServiceBaseUrl
                + PropertiesCache.getInstance()
                    .getProperty(JsonKey.SUNBIRD_CONTENT_BADGE_ASSIGN_URL));
        props.put("errCode", "INVALID_ASSIGN_BADGE_REQUEST");
        break;
      case "REVOKEBADGE":
        props.put(
            "basePath",
            contentServiceBaseUrl
                + PropertiesCache.getInstance()
                    .getProperty(JsonKey.SUNBIRD_CONTENT_BADGE_REVOKE_URL));
        props.put("errCode", "INVALID_REVOKE_BADGE_REQUEST");
        break;
      default:
        break;
    }
    return props;
  }

  public static boolean updateEkstepContent(
      String courseId, String attributeName, List<Map<String, Object>> activeBadges) {
    String response = "";
    try {
      ProjectLogger.log(
          "ContentService:updateEkstepContent: updating badgeAssociations details to Ekstep ",
          LoggerEnum.INFO.name());
      String contentUpdateBaseUrl = ProjectUtil.getConfigValue(JsonKey.EKSTEP_BASE_URL);
      String requestBody = getRequestBody(attributeName, activeBadges);
      System.out.println(requestBody);
      response =
          HttpUtil.sendPatchRequest(
              contentUpdateBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_UPDATE_URL)
                  + courseId,
              requestBody,
              CourseBatchSchedulerUtil.headerMap);
      ProjectLogger.log(
          "ContentService:updateEkstepContent: updating badgeAssociations response=="
              + response
              + " "
              + courseId,
          LoggerEnum.INFO.name());
    } catch (IOException e) {
      ProjectLogger.log(
          "ContentService:updateEkstepContent: Error while updating badgeAssociations "
              + e.getMessage(),
          e);
    }
    return JsonKey.SUCCESS.equalsIgnoreCase(response);
  }

  private static String getRequestBody(
      String attributeName, List<Map<String, Object>> activeBadges) {
    Map<String, Object> reqMap = new HashMap<>();
    Map<String, Object> contentReqMap = new HashMap<>();
    Map<String, Object> badgeAssociationMap = new HashMap<>();
    badgeAssociationMap.put(attributeName, activeBadges);
    contentReqMap.put(JsonKey.CONTENT, badgeAssociationMap);
    reqMap.put(JsonKey.REQUEST, contentReqMap);
    try {
      return mapper.writeValueAsString(reqMap);
    } catch (JsonProcessingException e) {
      ProjectLogger.log(
          "ContentService:getRequestBody: error occured while converting to string",
          LoggerEnum.INFO);
    }
    return null;
  }
}
