package controllers.audit;

import static org.junit.Assert.assertEquals;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import play.mvc.Result;

@Ignore
public class AuditLogControllerTest extends BaseControllerTest {

  @Test
  public void testSearchAuditHistorySuccess() {
    Result result = performTest("/v1/audit/history", "POST", createRequest());
    assertEquals(200, result.status());
  }

  private Map<String, Object> createRequest() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
