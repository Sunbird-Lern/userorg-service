package org.sunbird.common.models;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sunbird.common.models.util.BaseHttpTest;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

public class AppTest extends BaseHttpTest {
  private static final String data =
      "{\"request\": { \"search\": {\"contentType\": [\"Story\"] }}}";
  private static Map<String, String> headers = new HashMap<String, String>();

  @BeforeClass
  public static void init() {
    headers.put("content-type", "application/json");
    headers.put("accept", "application/json");
    headers.put("user-id", "mahesh");
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    }
    headers.put("authorization", "Bearer " + header);
  }

  @Test
  public void testSendPostRequestSuccess() throws Exception {
    String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
    if (StringUtils.isBlank(ekStepBaseUrl)) {
      ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
    }
    String response = HttpUtil.sendPostRequest(ekStepBaseUrl + "/content/v3/list", data, headers);
    Assert.assertNotNull(response);
  }

  @Test()
  public void testSendPostRequestFailureWithWrongUrl() {
    String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
    if (StringUtils.isBlank(ekStepBaseUrl)) {
      ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
    }
    String response = null;
    try {
      Map<String, String> data = new HashMap<>();
      data.put("search", "\"contentType\": [\"Story\"]");
      response = HttpUtil.sendPostRequest(ekStepBaseUrl + "/content/wrong/v3/list", data, headers);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertNull(response);
  }

  @Test()
  public void testSendPatchRequestSuccess() {
    String response = null;
    try {
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      response =
          HttpUtil.sendPatchRequest(
              ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_TAG_API_URL)
                  + "/"
                  + "testt123",
              "{}",
              headers);
    } catch (Exception e) {
    }
    Assert.assertNotNull(response);
  }
}
