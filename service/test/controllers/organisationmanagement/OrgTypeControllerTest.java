package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

public class OrgTypeControllerTest extends BaseControllerTest {

  private static String orgTypeName = "org-type-name";
  private static String id = "id";

  @Test
  public void testCreateOrgTypeSuccess() {
    Result result =
        performTest(
            "/v1/org/type/create", "POST", createOrgTypeRequest(true, orgTypeName, false, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateOrgTypeFailureWithEmptyName() {

    Result result =
        performTest("/v1/org/type/create", "POST", createOrgTypeRequest(true, null, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateOrgTypeFailureWithoutName() {

    Result result =
        performTest("/v1/org/type/create", "POST", createOrgTypeRequest(false, null, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testListOrgTypeListSuccess() {
    Result result = performTest("/v1/org/type/list", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgTypeSuccess() {
    Result result =
        performTest(
            "/v1/org/type/update", "PATCH", createOrgTypeRequest(true, orgTypeName, true, id));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgTypeFailureWithoutName() {
    Result result =
        performTest("/v1/org/type/update", "PATCH", createOrgTypeRequest(false, null, true, id));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgTypeFailureWithEmptyName() {
    Result result =
        performTest("/v1/org/type/update", "PATCH", createOrgTypeRequest(true, null, true, id));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgTypeFailureWithoutId() {
    Result result =
        performTest(
            "/v1/org/type/update", "PATCH", createOrgTypeRequest(true, orgTypeName, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createOrgTypeRequest(boolean isName, String name, boolean isId, String id) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isName) innerMap.put(JsonKey.NAME, name);
    if (isId) innerMap.put(JsonKey.ID, id);
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }

}
