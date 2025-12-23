package controllers.organisationmanagement;

import org.apache.pekko.stream.javadsl.FileIO;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;
import util.RequestInterceptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
// @Ignore
public class OrganisationControllerTest extends BaseApplicationTest {

  private static final String PATCH = "PATCH";
  private static String orgName = "someOrgName";
  private static String orgId = "someOrgOId";
  private static String rootOrgId = "someRootOrgId";
  private static String status = "1";

  @Before
  public void before() {
    setup(
        Arrays.asList(ACTORS.ORG_MANAGEMENT_ACTOR, ACTORS.SEARCH_HANDLER_ACTOR), DummyActor.class);
  }

  @Test
  public void testCreateOrgSuccess() {
    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrUpdateOrganisationRequest(orgName, null, false, null, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testCreateSubOrgWithLicenseSuccess() {
    Map<String, Object> reqMap =
        createOrUpdateOrganisationRequest(orgName, null, false, null, null);
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.LICENSE, "Test MIT license");
    Result result = performTest("/v1/org/create", "POST", reqMap);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testCreateRootOrgWithLicenseSuccess() {
    Map<String, Object> reqMap = createOrUpdateOrganisationRequest(orgName, null, true, null, null);
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.CHANNEL, "test-123");
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.LICENSE, "Test MIT license");
    Result result = performTest("/v1/org/create", "POST", reqMap);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testCreateRootOrgWithLicenseEmptyFailure() {
    Map<String, Object> reqMap = createOrUpdateOrganisationRequest(orgName, null, true, null, null);
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.CHANNEL, "test-123");
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.LICENSE, "");
    Result result = performTest("/v1/org/create", "POST", reqMap);
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testCreateOrgFailureWithoutOrgName() {
    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrUpdateOrganisationRequest(null, null, false, null, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testCreateOrgFailureWithRootOrgWithoutChannel() {
    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrUpdateOrganisationRequest(orgName, null, true, rootOrgId, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testUpdateOrgSuccess() {
    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, orgId, false, rootOrgId, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testUpdateOrgFailureWithoutOrgId() {
    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, null, false, rootOrgId, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testUpdateOrgStatusSuccess() {
    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, orgId, false, null, status));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testUpdateOrgStatusFailureWithoutOrgId() {
    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, null, false, null, status));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testGetOrgDetailsSuccess() {
    Result result = performTest("/v1/org/read", "POST", getOrganisationRequest(orgId, status));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testGetOrgDetailsFailureWithoutOrgId() {
    Result result = performTest("/v1/org/read", "POST", getOrganisationRequest(null, status));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testSearchOrgSuccess() {
    Result result =
        performTest("/v1/org/search", "POST", searchOrganisationRequest(status, new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testSearchOrgV2Success() {
    Result result =
        performTest("/v2/org/search", "POST", searchOrganisationRequest(status, new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertEquals(200, getResponseStatus(result));
  }

  @Test
  public void testSearchOrgFailureWithoutFilters() {
    Result result = performTest("/v1/org/search", "POST", searchOrganisationRequest(status, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertEquals(400, getResponseStatus(result));
  }

  @Test
  public void testAddEncyptionKeyPublicPem() throws IOException {
    String controllerPath = (Paths.get("").toAbsolutePath().toString().endsWith("controller"))?Paths.get("").toAbsolutePath().toString():Paths.get("").toAbsolutePath().toString()+File.separator +"controller";
    File file =
        new File(
                controllerPath + File.separator + "test/resources/samplepublic.pem");
    Http.MultipartFormData.Part<Source<ByteString, ?>> part =
        new Http.MultipartFormData.FilePart<>(
            "fileName",
            "samplepublic.pem",
            "application/text",
            FileIO.fromPath(file.toPath()),
            Files.size(file.toPath()));

    Http.RequestBuilder request =
        Helpers.fakeRequest()
            .uri("/v1/org/update/encryptionkey")
            .method("PATCH")
            .bodyRaw(
                Collections.singletonList(part),
                play.libs.Files.singletonTemporaryFileCreator(),
                application.asScala().materializer());

    Result result = Helpers.route(application, request);
    assertEquals(200, result.status());
  }

  @Test
  public void testAddEncyptionKeyPDF() throws IOException {
    String controllerPath = (Paths.get("").toAbsolutePath().toString().endsWith("controller"))?Paths.get("").toAbsolutePath().toString():Paths.get("").toAbsolutePath().toString()+File.separator +"controller";
    File file =
        new File(controllerPath + File.separator + "test/resources/sample.pdf");
    Http.MultipartFormData.Part<Source<ByteString, ?>> part =
        new Http.MultipartFormData.FilePart<>(
            "fileName",
            "sample.pdf",
            "application/pdf",
            FileIO.fromPath(file.toPath()),
            Files.size(file.toPath()));

    Http.RequestBuilder request =
        Helpers.fakeRequest()
            .uri("/v1/org/update/encryptionkey")
            .method("PATCH")
            .bodyRaw(
                Collections.singletonList(part),
                play.libs.Files.singletonTemporaryFileCreator(),
                application.asScala().materializer());

    Result result = Helpers.route(application, request);
    assertEquals(400, result.status());
  }

  @Test
  public void testAddEncryptionKeyException() {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject(), Mockito.anyMap()))
        .thenReturn(userAuthentication);

    JsonNode json = null;
    Http.RequestBuilder req =
        new Http.RequestBuilder()
            .bodyJson(json)
            .uri("/v1/org/update/encryptionkey")
            .method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  private Map createOrUpdateOrganisationRequest(
      String orgName, String orgId, boolean isRootOrg, String rootOrgId, String status) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORG_NAME, orgName);
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    innerMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    innerMap.put(JsonKey.IS_TENANT, isRootOrg);
    innerMap.put(JsonKey.ORG_TYPE, "board");

    if (status != null) innerMap.put(JsonKey.STATUS, Integer.valueOf(status));

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }

  private Map getOrganisationRequest(String orgId, String status) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (status != null) innerMap.put(JsonKey.STATUS, Integer.valueOf(status));

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }

  private Map searchOrganisationRequest(String status, HashMap<Object, Object> filterMap) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.FILTERS, filterMap);
    if (status != null) innerMap.put(JsonKey.STATUS, Integer.valueOf(status));

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }
}
