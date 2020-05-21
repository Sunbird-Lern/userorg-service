package org.sunbird.badge.service.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.badge.model.BadgeClassExtension;
import org.sunbird.badge.service.BadgeClassExtensionService;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.telemetry.util.TelemetryUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtil.class, TelemetryUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class BadgrServiceImplBadgeClassTest {
  private BadgingService badgrServiceImpl;

  private Request request;

  private BadgeClassExtensionService mockBadgeClassExtensionService;

  private static final String BADGE_CLASS_COMMON_RESPONSE_SUCCESS =
      "{\"created_at\":\"2018-03-05T09:35:33.722993Z\",\"id\":1,\"issuer\":\"http://localhost:8000/public/issuers/oracle-university\",\"json\":{\"name\":\"Java SE 8 Programmer\",\"image\":\"http://localhost:8000/public/badges/java-se-8-programmer/image\",\"criteria\":\"https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808\",\"@context\":\"https://w3id.org/openbadges/v1\",\"issuer\":\"http://localhost:8000/public/issuers/oracle-university\",\"type\":\"BadgeClass\",\"id\":\"http://localhost:8000/public/badges/java-se-8-programmer\",\"description\":\"A basic Java SE 8 certification.\"},\"name\":\"Java SE 8 Programmer\",\"image\":\"http://localhost:8000/media/uploads/badges/issuer_badgeclass_76c4cb77-40c7-4694-bee2-de15bd45f6cb.png\",\"slug\":\"java-se-8-programmer\",\"recipient_count\":1,\"created_by\":\"http://localhost:8000/user/1\"}";
  private static final String BADGE_CLASS_SEARCH_RESPONSE_SUCCESS =
      "[" + BADGE_CLASS_COMMON_RESPONSE_SUCCESS + "]";
  private static final String BADGE_CLASSS_DELETE_RESPONSE_SUCCESS =
      "Badge java-se-8-programmer has been deleted.";
  private static final String BADGE_CLASSS_DELETE_RESPONSE_FAILURE =
      "Badge class could not be deleted. It has already been issued at least once.";
  private static final String BADGE_CLASS_CREATE_RESPONSE_FAILURE_ISSUER_NOT_FOUND =
      "\"Issuer invalid not found or inadequate permissions.\"";
  private static final String BADGE_CLASS_GET_RESPONSE_FAILURE_BADGE_NOT_FOUND =
      "\"BadgeClass invalid could not be found, or inadequate permissions.\"";

  private static final String VALUE_BADGE_ID = "java-se-8-programmer";
  private static final String VALUE_BADGE_ID_URL =
      "http://localhost:8000/public/badges/java-se-8-programmer";
  private static final String VALUE_ISSUER_ID = "oracle-university";
  private static final String VALUE_ISSUER_ID_URL =
      "http://localhost:8000/public/issuers/oracle-university";
  private static final String VALUE_NAME = "Java SE 8 Programmer";
  private static final String VALUE_DESCRIPTION = "A basic Java SE 8 certification.";
  private static final String VALUE_BADGE_CRITERIA =
      "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808";
  private static final String VALUE_IMAGE =
      "http://localhost:8000/media/uploads/badges/issuer_badgeclass_76c4cb77-40c7-4694-bee2-de15bd45f6cb.png";
  private static final String VALUE_ROOT_ORG_ID = "AP";
  private static final String VALUE_TYPE = "user";
  private static final String VALUE_SUBTYPE = "award";
  private static final String VALUE_ROLES_JSON = "[ \"roleId1\" ]";
  private static final ArrayList<String> VALUE_ROLES_LIST =
      new ArrayList<>(Arrays.asList("roleId1"));
  private static final String VALUE_CREATED_DATE = "2018-03-05T09:35:33.722993Z";

  private static final String INVALID_VALUE = "invalid";

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(HttpUtil.class);

    PowerMockito.mockStatic(TelemetryUtil.class);
    PowerMockito.doNothing()
        .when(
            TelemetryUtil.class,
            "telemetryProcessingCall",
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyList());

    mockBadgeClassExtensionService = PowerMockito.mock(BadgeClassExtensionServiceImpl.class);

    badgrServiceImpl = new BadgrServiceImpl(mockBadgeClassExtensionService);
    request = new Request();
    Map<String, Object> roles = new HashMap<>();
    roles.put("roleId1", "roleId1");
    DataCacheHandler.setRoleMap(roles);
  }

  private void validateSuccessResponse(ResponseCode responseCode, Map<String, Object> responseMap) {
    assertEquals(ResponseCode.OK, responseCode);

    assertEquals(VALUE_BADGE_ID, responseMap.get(BadgingJsonKey.BADGE_ID));
    assertEquals(VALUE_BADGE_ID_URL, responseMap.get(BadgingJsonKey.BADGE_ID_URL));
    assertEquals(VALUE_ISSUER_ID, responseMap.get(BadgingJsonKey.ISSUER_ID));
    assertEquals(VALUE_ISSUER_ID_URL, responseMap.get(BadgingJsonKey.ISSUER_ID_URL));
    assertEquals(VALUE_NAME, responseMap.get(JsonKey.NAME));
    assertEquals(VALUE_DESCRIPTION, responseMap.get(JsonKey.DESCRIPTION));
    assertEquals(VALUE_BADGE_CRITERIA, responseMap.get(BadgingJsonKey.BADGE_CRITERIA));
    assertEquals(VALUE_IMAGE, responseMap.get(JsonKey.IMAGE));
    assertEquals(VALUE_ROOT_ORG_ID, responseMap.get(JsonKey.ROOT_ORG_ID));
    assertEquals(VALUE_TYPE, responseMap.get(JsonKey.TYPE));
    assertEquals(VALUE_SUBTYPE, responseMap.get(JsonKey.SUBTYPE));
    assertEquals(VALUE_ROLES_LIST.toString(), responseMap.get(JsonKey.ROLES).toString());
    assertEquals(VALUE_CREATED_DATE, responseMap.get(JsonKey.CREATED_DATE));
  }

  @Test
  public void testCreateBadgeClassSuccess() throws IOException {
    PowerMockito.when(
            HttpUtil.postFormData(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_CLASS_COMMON_RESPONSE_SUCCESS, 200));
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).save(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_CRITERIA, VALUE_BADGE_CRITERIA);
    request.put(JsonKey.NAME, VALUE_NAME);
    request.put(JsonKey.DESCRIPTION, VALUE_DESCRIPTION);
    request.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    request.put(JsonKey.TYPE, VALUE_TYPE);
    request.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    request.put(JsonKey.ROLES, VALUE_ROLES_JSON);
    request.put(JsonKey.IMAGE, VALUE_IMAGE.getBytes());

    Response response = badgrServiceImpl.createBadgeClass(request);
    validateSuccessResponse(response.getResponseCode(), response.getResult());
    Assert.assertTrue(null != response);
  }

  @Test
  public void testCreateBadgeClassFailureInvalidIssuer() throws IOException {
    PowerMockito.when(
            HttpUtil.postFormData(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(
            new HttpUtilResponse(
                BADGE_CLASS_CREATE_RESPONSE_FAILURE_ISSUER_NOT_FOUND,
                ResponseCode.RESOURCE_NOT_FOUND.getResponseCode()));
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).save(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, INVALID_VALUE);

    request.put(BadgingJsonKey.BADGE_CRITERIA, VALUE_BADGE_CRITERIA);
    request.put(JsonKey.NAME, VALUE_NAME);
    request.put(JsonKey.DESCRIPTION, VALUE_DESCRIPTION);
    request.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    request.put(JsonKey.TYPE, VALUE_TYPE);
    request.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    request.put(JsonKey.ROLES, VALUE_ROLES_JSON);
    request.put(JsonKey.IMAGE, VALUE_IMAGE.getBytes());

    boolean thrown = false;

    try {
      badgrServiceImpl.createBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testCreateBadgeClassFailureException() throws IOException {
    PowerMockito.when(
            HttpUtil.postFormData(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new IOException());
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).save(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, INVALID_VALUE);

    request.put(BadgingJsonKey.BADGE_CRITERIA, VALUE_BADGE_CRITERIA);
    request.put(JsonKey.NAME, VALUE_NAME);
    request.put(JsonKey.DESCRIPTION, VALUE_DESCRIPTION);
    request.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    request.put(JsonKey.TYPE, VALUE_TYPE);
    request.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    request.put(JsonKey.ROLES, VALUE_ROLES_JSON);
    request.put(JsonKey.IMAGE, VALUE_IMAGE.getBytes());

    boolean thrown = false;

    try {
      badgrServiceImpl.createBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testGetBadgeClassSuccess() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_CLASS_COMMON_RESPONSE_SUCCESS, 200));
    PowerMockito.when(mockBadgeClassExtensionService.get(VALUE_BADGE_ID))
        .thenReturn(
            new BadgeClassExtension(
                VALUE_BADGE_ID,
                VALUE_ISSUER_ID,
                VALUE_ROOT_ORG_ID,
                VALUE_TYPE,
                VALUE_SUBTYPE,
                VALUE_ROLES_LIST));

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, VALUE_BADGE_ID);

    Response response = badgrServiceImpl.getBadgeClassDetails(VALUE_BADGE_ID);
    validateSuccessResponse(response.getResponseCode(), response.getResult());
    Assert.assertTrue(null != response);
  }

  @Test
  public void testGetBadgeClassFailureInvalidBadgeId() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenReturn(
            new HttpUtilResponse(
                BADGE_CLASS_GET_RESPONSE_FAILURE_BADGE_NOT_FOUND,
                ResponseCode.RESOURCE_NOT_FOUND.getResponseCode()));
    PowerMockito.when(mockBadgeClassExtensionService.get(VALUE_BADGE_ID))
        .thenReturn(
            new BadgeClassExtension(
                VALUE_BADGE_ID,
                VALUE_ISSUER_ID,
                VALUE_ROOT_ORG_ID,
                VALUE_TYPE,
                VALUE_SUBTYPE,
                VALUE_ROLES_LIST));

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, INVALID_VALUE);

    boolean thrown = false;

    try {
      badgrServiceImpl.getBadgeClassDetails(INVALID_VALUE);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testGetBadgeClassFailureException() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenThrow(new IOException());
    PowerMockito.when(mockBadgeClassExtensionService.get(VALUE_BADGE_ID))
        .thenReturn(
            new BadgeClassExtension(
                VALUE_BADGE_ID,
                VALUE_ISSUER_ID,
                VALUE_ROOT_ORG_ID,
                VALUE_TYPE,
                VALUE_SUBTYPE,
                VALUE_ROLES_LIST));

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, VALUE_BADGE_ID);

    boolean thrown = false;

    try {
      badgrServiceImpl.getBadgeClassDetails(VALUE_BADGE_ID);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testSearchBadgeClassSuccessNonEmpty() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_CLASS_SEARCH_RESPONSE_SUCCESS, 200));
    PowerMockito.when(
            mockBadgeClassExtensionService.search(
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList()))
        .thenReturn(
            new ArrayList<>(
                Arrays.asList(
                    new BadgeClassExtension(
                        VALUE_BADGE_ID,
                        VALUE_ISSUER_ID,
                        VALUE_ROOT_ORG_ID,
                        VALUE_TYPE,
                        VALUE_SUBTYPE,
                        VALUE_ROLES_LIST))));

    Map<String, Object> filtersMap = new HashMap<>();

    filtersMap.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    filtersMap.put(JsonKey.TYPE, VALUE_TYPE);
    filtersMap.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    filtersMap.put(JsonKey.ROLES, VALUE_ROLES_LIST);
    filtersMap.put(BadgingJsonKey.ISSUER_LIST, new ArrayList<String>());

    request.put(JsonKey.FILTERS, filtersMap);

    Response response = badgrServiceImpl.searchBadgeClass(request);

    List<Map<String, Object>> badges =
        (List<Map<String, Object>>) response.getResult().get(BadgingJsonKey.BADGES);
    assertEquals(1, badges.size());

    validateSuccessResponse(response.getResponseCode(), badges.get(0));
  }

  @Test
  public void testSearchBadgeClassSuccessEmpty() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_CLASS_SEARCH_RESPONSE_SUCCESS, 200));
    PowerMockito.when(
            mockBadgeClassExtensionService.search(
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList()))
        .thenReturn(new ArrayList<>());

    Map<String, Object> filtersMap = new HashMap<>();

    filtersMap.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    filtersMap.put(JsonKey.TYPE, VALUE_TYPE);
    filtersMap.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    filtersMap.put(JsonKey.ROLES, VALUE_ROLES_LIST);
    filtersMap.put(BadgingJsonKey.ISSUER_LIST, new ArrayList<String>());

    request.put(JsonKey.FILTERS, filtersMap);

    Response response = badgrServiceImpl.searchBadgeClass(request);

    List<Map<String, Object>> badges =
        (List<Map<String, Object>>) response.getResult().get(BadgingJsonKey.BADGES);
    assertEquals(0, badges.size());
  }

  @Test
  public void testListBadgeClassFailureException() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenThrow(new IOException());
    PowerMockito.when(
            mockBadgeClassExtensionService.search(
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList()))
        .thenReturn(
            new ArrayList<>(
                Arrays.asList(
                    new BadgeClassExtension(
                        VALUE_BADGE_ID,
                        VALUE_ISSUER_ID,
                        VALUE_ROOT_ORG_ID,
                        VALUE_TYPE,
                        VALUE_SUBTYPE,
                        VALUE_ROLES_LIST))));

    Map<String, Object> filtersMap = new HashMap<>();

    filtersMap.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    filtersMap.put(JsonKey.TYPE, VALUE_TYPE);
    filtersMap.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    filtersMap.put(JsonKey.ROLES, VALUE_ROLES_LIST);
    filtersMap.put(BadgingJsonKey.ISSUER_LIST, new ArrayList<String>());

    request.put(JsonKey.FILTERS, filtersMap);

    boolean thrown = false;

    try {
      badgrServiceImpl.searchBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testRemoveBadgeClassSuccess() throws IOException {
    PowerMockito.when(HttpUtil.sendDeleteRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_CLASSS_DELETE_RESPONSE_SUCCESS, 200));
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).delete(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, VALUE_BADGE_ID);

    Response response = badgrServiceImpl.removeBadgeClass(request);

    assertEquals(ResponseCode.OK, response.getResponseCode());
    assertEquals(BADGE_CLASSS_DELETE_RESPONSE_SUCCESS, response.getResult().get(JsonKey.MESSAGE));
  }

  @Test
  public void testRemoveBadgeClassIssuedFailure() throws IOException {
    PowerMockito.when(HttpUtil.sendDeleteRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_CLASSS_DELETE_RESPONSE_FAILURE, 400));
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).delete(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, VALUE_BADGE_ID);

    boolean thrown = false;

    try {
      badgrServiceImpl.removeBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(
          MessageFormat.format(
              ResponseCode.customClientError.getErrorMessage(),
              BADGE_CLASSS_DELETE_RESPONSE_FAILURE),
          exception.getMessage());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testRemoveBadgeClassFailureInvalidBadgeId() throws IOException {
    PowerMockito.when(HttpUtil.sendDeleteRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse("", ResponseCode.RESOURCE_NOT_FOUND.getResponseCode()));
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).delete(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, INVALID_VALUE);

    boolean thrown = false;

    try {
      badgrServiceImpl.removeBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testRemoveBadgeClassFailureException() throws IOException {
    PowerMockito.when(HttpUtil.sendDeleteRequest(Mockito.any(), Mockito.any()))
        .thenThrow(new IOException());
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).delete(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, VALUE_ISSUER_ID);
    request.put(BadgingJsonKey.BADGE_ID, VALUE_BADGE_ID);

    boolean thrown = false;

    try {
      badgrServiceImpl.removeBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }

  @Test
  public void testCreateBadgeClassFailureExceptionInvalidRole() throws IOException {
    PowerMockito.when(
            HttpUtil.postFormData(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new IOException());
    PowerMockito.doNothing().when(mockBadgeClassExtensionService).save(Mockito.any());

    request.put(BadgingJsonKey.ISSUER_ID, INVALID_VALUE);

    request.put(BadgingJsonKey.BADGE_CRITERIA, VALUE_BADGE_CRITERIA);
    request.put(JsonKey.NAME, VALUE_NAME);
    request.put(JsonKey.DESCRIPTION, VALUE_DESCRIPTION);
    request.put(JsonKey.ROOT_ORG_ID, VALUE_ROOT_ORG_ID);
    request.put(JsonKey.TYPE, VALUE_TYPE);
    request.put(JsonKey.SUBTYPE, VALUE_SUBTYPE);
    request.put(JsonKey.ROLES, "[]");
    request.put(JsonKey.IMAGE, VALUE_IMAGE.getBytes());

    boolean thrown = false;

    try {
      badgrServiceImpl.createBadgeClass(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }
}
