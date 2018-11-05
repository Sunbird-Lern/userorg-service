package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

public class OrganisationControllerTest extends BaseControllerTest {

  private static String orgName = "org-name";
  private static String orgId = "org-Id";
  private static String rootOrgId = "root-org-id";
  private static String status = "1";

  @Test
  public void testCreateOrgSuccess() {

    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrganisationRequest(true, false, false, false, false, false, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateOrgFailureWithoutOrgName() {

    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrganisationRequest(false, false, false, false, false, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateOrgFailureWithRootOrgWithoutChannel() {

    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrganisationRequest(true, true, false, false, false, false, null));
    assertEquals(getResponseCode(result), ResponseCode.dependentParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgSuccess() {

    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrganisationRequest(false, false, true, true, false, false, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrganisationRequest(false, false, false, true, false, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgStatusSuccess() {

    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrganisationRequest(false, false, true, false, true, false, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgStatusFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrganisationRequest(false, false, false, false, true, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetOrgDetailsSuccess() {

    Result result =
        performTest(
            "/v1/org/read",
            "POST",
            createOrganisationRequest(false, false, true, false, true, false, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetOrgDetailsFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/read",
            "POST",
            createOrganisationRequest(false, false, false, false, true, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testSearchOrgsSuccess() {

    Result result =
        performTest(
            "/v1/org/search",
            "POST",
            createOrganisationRequest(false, false, false, false, true, true, "filter"));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchOrgsFailureWithoutFilters() {

    Result result =
        performTest(
            "/v1/org/search",
            "POST",
            createOrganisationRequest(false, false, false, false, true, true, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createOrganisationRequest(
      boolean isOrgName,
      boolean isRootOrg,
      boolean isOrgId,
      boolean isRootOrgId,
      boolean isStatus,
      boolean isFilter,
      String isFilterNUll) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isOrgName) innerMap.put(JsonKey.ORG_NAME, orgName);
    if (isRootOrg) innerMap.put(JsonKey.IS_ROOT_ORG, true);
    if (isOrgId) innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (isRootOrgId) innerMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    if (isStatus) innerMap.put(JsonKey.STATUS, new BigInteger(status));
    if (isFilter) {
      Map<String, Object> filterMap = new HashMap<>();
      if (isFilterNUll != null) {
        innerMap.put(JsonKey.FILTERS, filterMap);
      } else innerMap.put(JsonKey.FILTERS, null);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
