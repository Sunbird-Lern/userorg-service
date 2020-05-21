package org.sunbird.common.models.util;

import java.util.HashMap;
import java.util.Map;

public class LearnerServiceUrls {
  public static final String BASE_URL = "sunbird_learner_service_url";

  public static final String PREFIX_ORG_SERVICE = "/api/org";

  public enum Path {
    API_GW_PATH_READ_ORG("/v1/read"),
    LOCAL_PATH_READ_ORG("/v1/org/read");

    private final String text;

    Path(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  public static String getRequestUrl(String baseUrl, String prefix, Path path) {
    String pathEnumName = path.name();

    if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
      prefix = "";
      pathEnumName = pathEnumName.replace("API_GW", "LOCAL");
    }
    return String.format("%s%s%s", baseUrl, prefix, Path.valueOf(pathEnumName));
  }

  public static Map<String, String> getRequestHeaders(Map<String, String[]> inputMap) {
    Map<String, String> outputMap = new HashMap<>();

    for (Map.Entry<String, String[]> entry : inputMap.entrySet()) {
      if (entry.getKey().toLowerCase().startsWith("x-")
          || entry.getKey().equalsIgnoreCase("Authorization")) {
        if (entry.getValue() != null) {
          outputMap.put(entry.getKey(), entry.getValue()[0]);
        }
      }
    }

    outputMap.put("Content-Type", "application/json");
    return outputMap;
  }
}
