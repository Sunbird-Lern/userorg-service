package controllers.tac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

public class TnCControllerTest extends BaseControllerTest {

  public static final String url = "/v1/user/tnc/accept";
  public static final String post = "POST";
  public static final String version = "someVersion";

  @Test
  public void testTnCAcceptFailure() {
    Result result = performTest(url, post, getTnCData(false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testTnCAcceptSuccess() {
    Result result = performTest(url, post, getTnCData(true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  private Map getTnCData(boolean passVersion) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    if (passVersion) {
      innerMap.put(JsonKey.VERSION, version);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
