package controllers.badging.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Test class for BadgeClassValidator.
 *
 * @author B Vinaya Kumar
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class BadgeClassValidatorTest {
  private static final String READ_ROOT_ORG_TRUE_RESPONSE =
      "{\"id\":\"api.org.read\",\"ver\":\"v1\",\"ts\":\"2018-03-22 13:38:07:948+0000\",\"params\":{\"resmsgid\":null,\"msgid\":\"78070b77-a82d-4670-882e-1ed83af890bd\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":{\"dateTime\":null,\"preferredLanguage\":\"English\",\"approvedBy\":null,\"channel\":\"ROOT_ORG\",\"description\":\"Sunbird\",\"updatedDate\":\"2017-08-24 06:02:10:846+0000\",\"addressId\":null,\"orgType\":null,\"provider\":null,\"orgCode\":\"sunbird\",\"theme\":null,\"id\":\"ORG_001\",\"communityId\":null,\"isApproved\":null,\"slug\":\"sunbird\",\"identifier\":\"ORG_001\",\"thumbnail\":null,\"orgName\":\"Sunbird\",\"updatedBy\":\"user1\",\"externalId\":null,\"isRootOrg\":true,\"rootOrgId\":null,\"approvedDate\":null,\"imgUrl\":null,\"homeUrl\":null,\"isDefault\":null,\"contactDetail\":\"[{\\\"phone\\\":\\\"213124234234\\\",\\\"email\\\":\\\"test@test.com\\\"},{\\\"phone\\\":\\\"+91213124234234\\\",\\\"email\\\":\\\"test1@test.com\\\"}]\",\"createdDate\":null,\"createdBy\":null,\"parentOrgId\":null,\"hashTagId\":\"b00bc992ef25f1a9a8d63291e20efc8d\",\"noOfMembers\":1,\"status\":null}}}";
  private static final String READ_ROOT_ORG_FALSE_RESPONSE =
      "{\"id\":\"api.org.read\",\"ver\":\"v1\",\"ts\":\"2018-03-22 13:38:07:948+0000\",\"params\":{\"resmsgid\":null,\"msgid\":\"78070b77-a82d-4670-882e-1ed83af890bd\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":{\"dateTime\":null,\"preferredLanguage\":\"English\",\"approvedBy\":null,\"channel\":\"ROOT_ORG\",\"description\":\"Sunbird\",\"updatedDate\":\"2017-08-24 06:02:10:846+0000\",\"addressId\":null,\"orgType\":null,\"provider\":null,\"orgCode\":\"sunbird\",\"theme\":null,\"id\":\"ORG_001\",\"communityId\":null,\"isApproved\":null,\"slug\":\"sunbird\",\"identifier\":\"ORG_001\",\"thumbnail\":null,\"orgName\":\"Sunbird\",\"updatedBy\":\"user1\",\"externalId\":null,\"isRootOrg\":false,\"rootOrgId\":null,\"approvedDate\":null,\"imgUrl\":null,\"homeUrl\":null,\"isDefault\":null,\"contactDetail\":\"[{\\\"phone\\\":\\\"213124234234\\\",\\\"email\\\":\\\"test@test.com\\\"},{\\\"phone\\\":\\\"+91213124234234\\\",\\\"email\\\":\\\"test1@test.com\\\"}]\",\"createdDate\":null,\"createdBy\":null,\"parentOrgId\":null,\"hashTagId\":\"b00bc992ef25f1a9a8d63291e20efc8d\",\"noOfMembers\":1,\"status\":null}}}";
  private static final String READ_ROOT_ORG_EMTPY_RESPONSE =
      "{\"id\":\"api.org.read\",\"ver\":\"v1\",\"ts\":\"2018-03-22 13:38:07:948+0000\",\"params\":{\"resmsgid\":null,\"msgid\":\"78070b77-a82d-4670-882e-1ed83af890bd\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{}}";

  @Before
  public void setUp() throws IOException {
    PowerMockito.mockStatic(HttpUtil.class);
    PowerMockito.when(
            HttpUtil.doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(
            new HttpUtilResponse(READ_ROOT_ORG_TRUE_RESPONSE, ResponseCode.OK.getResponseCode()));
  }

  @Test
  public void testValidateCreateBadgeClassIssuerIdRequired() {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.issuerIdRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassCriteriaRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeCriteriaRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassNameRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeNameRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassDescriptionRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeDescriptionRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassRootOrgIdRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.rootOrgIdRequired.getErrorCode());
    }
  }

  private void performValidateCreateBadgeClassInvalidRootOrgId(Request request) {
    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.rootOrgIdRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassInvalidRootOrgId() throws IOException {
    PowerMockito.when(
            HttpUtil.doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(new HttpUtilResponse("", ResponseCode.RESOURCE_NOT_FOUND.getResponseCode()));

    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");

    request.setRequest(requestMap);

    PowerMockito.when(
            HttpUtil.doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(new HttpUtilResponse("", ResponseCode.RESOURCE_NOT_FOUND.getResponseCode()));
    performValidateCreateBadgeClassInvalidRootOrgId(request);

    PowerMockito.when(
            HttpUtil.doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(
            new HttpUtilResponse(READ_ROOT_ORG_FALSE_RESPONSE, ResponseCode.OK.getResponseCode()));
    performValidateCreateBadgeClassInvalidRootOrgId(request);

    PowerMockito.when(
            HttpUtil.doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(
            new HttpUtilResponse(READ_ROOT_ORG_EMTPY_RESPONSE, ResponseCode.OK.getResponseCode()));
    performValidateCreateBadgeClassInvalidRootOrgId(request);
  }

  @Test
  public void testValidateCreateBadgeClassTypeRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeTypeRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassInvalidType() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "invalid");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.invalidBadgeType.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassInvalidSubtype() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "invalid");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.invalidBadgeSubtype.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassRolesRequiredNull() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "award");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeRolesRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassRolesRequiredEmpty() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "award");
    requestMap.put(JsonKey.ROLES, "[]");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeRolesRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassSingleRoleImageRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "award");
    requestMap.put(JsonKey.ROLES, "OFFICIAL_TEXTBOOK_BADGE_ISSUER");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeImageRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassMultipleRolesImageRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "award");
    requestMap.put(
        JsonKey.ROLES, "[ \"OFFICIAL_TEXTBOOK_BADGE_ISSUER\", \"TEACHER_BADGE_ISSUER\" ]");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeImageRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassSuccess() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "award");
    requestMap.put(JsonKey.ROLES, "[ \"OFFICIAL_TEXTBOOK_BADGE_ISSUER\" ]");
    requestMap.put(JsonKey.IMAGE, "something");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testValidateGetBadgeBadgeIdRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateGetBadgeClass(request);
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeIdRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateGetBadgeClassSuccess() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(BadgingJsonKey.BADGE_ID, "java-se-8-programmer");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateGetBadgeClass(request);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testValidateSearchBadgeInvalidRequest() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateSearchBadgeClass(request);
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  @Test
  public void testValidateDeleteBadgeBadgeIdRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateDeleteBadgeClass(request);
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeIdRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateDeleteBadgeClassSuccess() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(BadgingJsonKey.BADGE_ID, "java-se-8-programmer");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateDeleteBadgeClass(request);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testValidateCreateBadgeClassSubTypeRequired() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");

    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeSubTypeRequired.getErrorCode());
    }
  }

  @Test
  public void testValidateCreateBadgeClassSubTypeWithEmpty() {
    Request request = new Request();

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
    requestMap.put(
        BadgingJsonKey.BADGE_CRITERIA,
        "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
    requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
    requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
    requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
    requestMap.put(JsonKey.TYPE, "user");
    requestMap.put(JsonKey.SUBTYPE, "   ");
    request.setRequest(requestMap);

    try {
      new BadgeClassValidator().validateCreateBadgeClass(request, new HashMap<>());
    } catch (ProjectCommonException e) {
      assertEquals(e.getCode(), ResponseCode.badgeSubTypeRequired.getErrorCode());
    }
  }
}
