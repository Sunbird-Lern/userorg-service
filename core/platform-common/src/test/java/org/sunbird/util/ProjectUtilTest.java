package org.sunbird.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.sso.impl.BaseHttpTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Created by arvind on 6/10/17. */
public class ProjectUtilTest extends BaseHttpTest {

  private final PropertiesCache propertiesCache = ProjectUtil.propertiesCache;

  @Ignore
  public void testGetContextFailureWithNameAbsent() {
    Map<String, Object> templateMap = new HashMap<>();
    templateMap.put(JsonKey.ACTION_URL, "googli.com");

    VelocityContext context = ProjectUtil.getContext(templateMap);
    assertFalse(context.internalContainsKey(JsonKey.NAME));
  }

  @Test
  public void testGetContextFailureWithoutActionUrl() {
    Map<String, Object> templateMap = new HashMap<>();
    templateMap.put(JsonKey.ORG_NAME, "orgName");
    templateMap.put(JsonKey.COURSE_NAME, "courseName");
    templateMap.put(JsonKey.BATCH_START_DATE, "2020");
    templateMap.put(JsonKey.BATCH_END_DATE, "2019");
    templateMap.put(JsonKey.BATCH_NAME, "name");
    templateMap.put(JsonKey.NAME, "firstName");
    templateMap.put(JsonKey.SIGNATURE, "signature");
    templateMap.put(JsonKey.COURSE_BATCH_URL, "url");
    VelocityContext context = ProjectUtil.getContext(templateMap);
    assertFalse(context.internalContainsKey(JsonKey.ACTION_URL));
  }

  @Test
  public void testGetContextSuccessWithFromMail() {
    Map<String, Object> templateMap = new HashMap<>();
    templateMap.put(JsonKey.ACTION_URL, "googli.com");
    templateMap.put(JsonKey.NAME, "userName");

    boolean envVal = !StringUtils.isBlank(System.getenv(JsonKey.EMAIL_SERVER_FROM));
    boolean cacheVal = propertiesCache.getProperty(JsonKey.EMAIL_SERVER_FROM) != null;

    VelocityContext context = ProjectUtil.getContext(templateMap);
    if (envVal) {
      assertEquals(System.getenv(JsonKey.EMAIL_SERVER_FROM), context.internalGet(JsonKey.FROM_EMAIL));
    } else if (cacheVal) {
      assertEquals(propertiesCache.getProperty(JsonKey.EMAIL_SERVER_FROM), context.internalGet(JsonKey.FROM_EMAIL));
    }
  }

  @Test
  public void testGetContextSuccessWithOrgImageUrl() {
    Map<String, Object> templateMap = new HashMap<>();
    templateMap.put(JsonKey.ACTION_URL, "googli.com");
    templateMap.put(JsonKey.NAME, "userName");

    boolean envVal = !StringUtils.isBlank(System.getenv(JsonKey.SUNBIRD_ENV_LOGO_URL));
    boolean cacheVal = propertiesCache.getProperty(JsonKey.SUNBIRD_ENV_LOGO_URL) != null;

    VelocityContext context = ProjectUtil.getContext(templateMap);
    if (envVal) {
      assertEquals(System.getenv(JsonKey.SUNBIRD_ENV_LOGO_URL), context.internalGet(JsonKey.ORG_IMAGE_URL));
    } else if (cacheVal) {
      assertEquals(propertiesCache.getProperty(JsonKey.SUNBIRD_ENV_LOGO_URL), context.internalGet(JsonKey.ORG_IMAGE_URL));
    }
  }

  @Test
  public void testCreateCheckResponseSuccess() {
    Map<String, Object> responseMap = ProjectUtil.createCheckResponse("UserOrgService", false, null);
    assertEquals(true, responseMap.get(JsonKey.Healthy));
  }

  @Test
  public void testCreateCheckResponseFailureWithException() {
    Map<String, Object> responseMap =
        ProjectUtil.createCheckResponse(
            "UserOrgService",
            true,
            new ProjectCommonException(
                ResponseCode.invalidObjectType,
                ResponseCode.invalidObjectType.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode()));
    assertEquals(false, responseMap.get(JsonKey.Healthy));
    assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), responseMap.get(JsonKey.ERROR));
    assertEquals(ResponseCode.invalidObjectType.getErrorMessage(), responseMap.get(JsonKey.ERRORMSG));
  }

  @Ignore
  public void testSetRequestSuccessWithLowerCaseValues() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SOURCE, "Test");
    requestObj.put(JsonKey.LOGIN_ID, "SunbirdUser");
    requestObj.put(JsonKey.EXTERNAL_ID, "testExternal");
    requestObj.put(JsonKey.USER_NAME, "username");
    requestObj.put(JsonKey.USERNAME, "userName");
    requestObj.put(JsonKey.PROVIDER, "Provider");
    requestObj.put(JsonKey.ID, "TEST123");
    request.setRequest(requestObj);
    assertEquals("test", requestObj.get(JsonKey.SOURCE));
    assertEquals("sunbirduser", requestObj.get(JsonKey.LOGIN_ID));
    assertEquals("testexternal", requestObj.get(JsonKey.EXTERNAL_ID));
    assertEquals("username", requestObj.get(JsonKey.USER_NAME));
    assertEquals("username", requestObj.get(JsonKey.USERNAME));
    assertEquals("provider", requestObj.get(JsonKey.PROVIDER));
    assertEquals("TEST123", requestObj.get(JsonKey.ID));
  }

  @Test
  public void testFormatMessageSuccess() {
    String msg = ProjectUtil.formatMessage("Hello {0}", "user");
    assertEquals("Hello user", msg);
  }

  @Test
  public void testFormatMessageFailureWithInvalidVariable() {
    String msg = ProjectUtil.formatMessage("Hello ", "user");
    assertNotEquals("Hello user", msg);
  }

  @Test
  public void testIsEmailValidFailureWithWrongEmail() {
    boolean msg = ProjectUtil.isEmailvalid("Hello ");
    assertFalse(msg);
  }

  @Test
  public void testSetTraceIdInHeader() {
    ProjectUtil.setTraceIdInHeader(new HashMap<>(), new RequestContext());
    assertTrue(true);
  }

  @Test
  public void testValidatePhone() {
    boolean bool = ProjectUtil.validatePhone("9742500121", "91");
    assertTrue(bool);
  }

  @Test
  public void testIsDateValidFormatSuccess() {
    boolean bool = ProjectUtil.isDateValidFormat("yyyy-MM-dd", "2017-12-18");
    assertTrue(bool);
  }

  @Test
  public void testIsDateValidFormatFailureWithEmptyDate() {
    boolean bool = ProjectUtil.isDateValidFormat("yyyy-MM-dd", "");
    assertFalse(bool);
  }

  @Test
  public void testUserRoleSuccess() {
    assertEquals("PUBLIC", ProjectUtil.UserRole.PUBLIC.getValue());
  }

  @Test
  public void testIsDateValidFormatFailureWithInvalidDate() {
    boolean bool = ProjectUtil.isDateValidFormat("yyyy-MM-dd", "2017-12-18");
    assertTrue(bool);
  }

  @Test
  public void testIsDateValidFormatFailureWithEmptyDateTime() {
    boolean bool = ProjectUtil.isDateValidFormat("yyyy-MM-dd HH:mm:ss:SSSZ", "");
    assertFalse(bool);
  }

  @Test
  public void testReportTrackingStatusSuccess() {
    assertEquals(0, ProjectUtil.ReportTrackingStatus.NEW.getValue());
    assertEquals(1, ProjectUtil.ReportTrackingStatus.GENERATING_DATA.getValue());
    assertEquals(2, ProjectUtil.ReportTrackingStatus.UPLOADING_FILE.getValue());
    assertEquals(3, ProjectUtil.ReportTrackingStatus.UPLOADING_FILE_SUCCESS.getValue());
    assertEquals(4, ProjectUtil.ReportTrackingStatus.SENDING_MAIL.getValue());
    assertEquals(5, ProjectUtil.ReportTrackingStatus.SENDING_MAIL_SUCCESS.getValue());
    assertEquals(9, ProjectUtil.ReportTrackingStatus.FAILED.getValue());
  }

  @Test
  public void testEsTypeSuccess() {
    assertEquals(ProjectUtil.getConfigValue("user_index_alias"), ProjectUtil.EsType.user.getTypeName());
    assertEquals(ProjectUtil.getConfigValue("org_index_alias"), ProjectUtil.EsType.organisation.getTypeName());
    assertEquals("usernotes", ProjectUtil.EsType.usernotes.getTypeName());
  }

  @Test
  public void testBulkProcessStatusSuccess() {
    assertEquals(0, ProjectUtil.BulkProcessStatus.NEW.getValue());
    assertEquals(1, ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    assertEquals(2, ProjectUtil.BulkProcessStatus.INTERRUPT.getValue());
    assertEquals(3, ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    assertEquals(9, ProjectUtil.BulkProcessStatus.FAILED.getValue());
  }

  @Test
  public void testOrgStatusSuccess() {
    assertEquals(Integer.valueOf(0), ProjectUtil.OrgStatus.INACTIVE.getValue());
    assertEquals(Integer.valueOf(1), ProjectUtil.OrgStatus.ACTIVE.getValue());
    assertEquals(Integer.valueOf(2), ProjectUtil.OrgStatus.BLOCKED.getValue());
    assertEquals(Integer.valueOf(3), ProjectUtil.OrgStatus.RETIRED.getValue());
  }

  @Test
  public void testProgressStatusSuccess() {
    assertEquals(0, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
    assertEquals(1, ProjectUtil.ProgressStatus.STARTED.getValue());
    assertEquals(2, ProjectUtil.ProgressStatus.COMPLETED.getValue());
  }

  @Test
  public void testEnvironmentSuccess() {
    assertEquals(1, ProjectUtil.Environment.dev.getValue());
    assertEquals(2, ProjectUtil.Environment.qa.getValue());
    assertEquals(3, ProjectUtil.Environment.prod.getValue());
  }

  @Test
  public void testStatusSuccess() {
    assertEquals(1, ProjectUtil.Status.ACTIVE.getValue());
    assertEquals(0, ProjectUtil.Status.INACTIVE.getValue());
    assertFalse(ProjectUtil.ActiveStatus.INACTIVE.getValue());
    assertTrue(ProjectUtil.ActiveStatus.ACTIVE.getValue());

    assertEquals("username", ProjectUtil.UserLookupType.USERNAME.getType());
    assertEquals("email", ProjectUtil.UserLookupType.EMAIL.getType());
    assertEquals("phone", ProjectUtil.UserLookupType.PHONE.getType());
  }

  @Test
  public void testGetFormattedDate() {
    Assert.assertNotNull(ProjectUtil.getFormattedDate());
  }

  @Test
  public void testGetUniqueIdFromTimestamp() {
    Assert.assertNotNull(ProjectUtil.getUniqueIdFromTimestamp(1)); // generateUniqueId
  }

  @Test
  public void testGenerateUniqueId() {
    Assert.assertNotNull(ProjectUtil.generateUniqueId());
  }

  @Test
  public void testGetDateFormatter() {
    Assert.assertNotNull(ProjectUtil.getDateFormatter());
  }

  @Test
  public void testIsEmailvalid() {
    Assert.assertTrue(ProjectUtil.isEmailvalid("xyz@xyz.com"));
    Assert.assertFalse(ProjectUtil.isEmailvalid("xy@z@xyz.com"));
    Assert.assertFalse(ProjectUtil.isEmailvalid(""));
  }

  @Test
  public void testIsEmailValidFailureWithInvalidFormat() {
    boolean bool = ProjectUtil.isEmailvalid("xyz.com");
    Assert.assertFalse(bool);
  }

  @Test
  public void testIsEmailValidSuccess() {
    boolean bool = ProjectUtil.isEmailvalid("xyz@xyz.com");
    assertTrue(bool);
  }

  @Test
  public void testGetLmsUserIdSuccessWithoutFedUserId() {
    String userid = ProjectUtil.getLmsUserId("1234567890");
    assertEquals("1234567890", userid);
  }

  @Test
  public void testGetLmsUserIdSuccessWithFedUserId() {
    String userid =
        ProjectUtil.getLmsUserId(
            "f:"
                + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID)
                + ":"
                + "1234567890");
    assertEquals("1234567890", userid);
  }

  @Test
  public void testMigrateActionAcceptValueFailure() {
    Assert.assertNotEquals("ok", ProjectUtil.MigrateAction.ACCEPT.getValue());
  }

  @Test
  public void testMigrateActionRejectValueFailure() {
    Assert.assertNotEquals("no", ProjectUtil.MigrateAction.REJECT.getValue());
  }

  @Test
  public void testMigrateActionAcceptValueSuccess() {
    Assert.assertEquals("accept", ProjectUtil.MigrateAction.ACCEPT.getValue());
  }

  @Test
  public void testMigrateActionRejectValueSuccess() {
    Assert.assertEquals("reject", ProjectUtil.MigrateAction.REJECT.getValue());
  }

  @Test
  public void testValidateCountryCode() {
    boolean isValid = ProjectUtil.validateCountryCode("+91");
    assertTrue(isValid);
    isValid = ProjectUtil.validateCountryCode("9a");
    assertFalse(isValid);
  }

  @Test
  public void testValidateUUID() {
    boolean isValid = ProjectUtil.validateUUID("1df03f56-ceba-4f2d-892c-2b1609e7b05f");
    assertTrue(isValid);
  }
}
