package org.sunbird.util.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.logging.LoggerUtil;

public class UserTncUtil {
  private static final LoggerUtil logger = new LoggerUtil(UserTncUtil.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  // Convert Acceptance tnc object as a Json String in cassandra table
  public static Map<String, Object> convertTncStringToJsonMap(
      Map<String, String> allTncAcceptedMap) {
    Map<String, Object> allTncMap = new HashMap<>();
    for (Map.Entry<String, String> mapItr : allTncAcceptedMap.entrySet()) {
      try {
        allTncMap.put(mapItr.getKey(), mapper.readValue(mapItr.getValue(), Map.class));
      } catch (JsonProcessingException e) {
        logger.error("JsonParsing error while parsing tnc acceptance", e);
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
      }
    }
    return allTncMap;
  }
}
