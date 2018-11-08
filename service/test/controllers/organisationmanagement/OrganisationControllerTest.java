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
            createOrganisationRequest(orgName, null, false, null, null, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateOrgFailureWithoutOrgName() {

    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrganisationRequest(null, null, false, null, null, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateOrgFailureWithRootOrgWithoutChannel() {

    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrganisationRequest(orgName, null, true, rootOrgId, null, null));
    assertEquals(getResponseCode(result), ResponseCode.dependentParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgSuccess() {

    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrganisationRequest(null, orgId, false, rootOrgId, null, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrganisationRequest(null, null, false, rootOrgId, null, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgStatusSuccess() {

    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrganisationRequest(null, orgId, false, null, status, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgStatusFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrganisationRequest(null, null, false, null, status, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetOrgDetailsSuccess() {

    Result result =
        performTest(
            "/v1/org/read",
            "POST",
            createOrganisationRequest(null, orgId, false, null, status, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetOrgDetailsFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/read",
            "POST",
            createOrganisationRequest(null, null, false, null, status, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testSearchOrgSuccess() {

    Result result =
        performTest(
            "/v1/org/search",
            "POST",
            createOrganisationRequest(null, null, false, null, status, new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchOrgFailureWithoutFilters() {

    Result result =
        performTest(
            "/v1/org/search",
            "POST",
            createOrganisationRequest(null, null, false, null, status, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createOrganisationRequest(
      String orgName,
      String orgId,
      boolean isRootOrg,
      String rootOrgId,
      String status,
      Map<String, Object> filterMap) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORG_NAME, orgName);
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    innerMap.put(JsonKey.IS_ROOT_ORG, isRootOrg);
    innerMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);

    if (status != null) innerMap.put(JsonKey.STATUS, new BigInteger(status));
    innerMap.put(JsonKey.FILTERS, filterMap);
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
