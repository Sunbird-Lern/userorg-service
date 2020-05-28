package org.sunbird.common.models.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by arvind on 6/10/17. */
public class ProjectUtilTest extends BaseHttpTest {

  private PropertiesCache propertiesCache = ProjectUtil.propertiesCache;

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

  @Ignore
  public void testGetContextFailureWithNameAbsent() {

    Map<String, Object> templateMap = new HashMap<>();
    templateMap.put(JsonKey.ACTION_URL, "googli.com");

    VelocityContext context = ProjectUtil.getContext(templateMap);
    assertEquals(false, context.internalContainsKey(JsonKey.NAME));
  }

  @Test
  public void testGetContextFailureWithoutActionUrl() {

    Map<String, Object> templateMap = new HashMap<>();
    templateMap.put(JsonKey.NAME, "userName");

    VelocityContext context = ProjectUtil.getContext(templateMap);
    assertEquals(false, context.internalContainsKey(JsonKey.ACTION_URL));
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
      assertEquals(
          System.getenv(JsonKey.EMAIL_SERVER_FROM), context.internalGet(JsonKey.FROM_EMAIL));
    } else if (cacheVal) {
      assertEquals(
          propertiesCache.getProperty(JsonKey.EMAIL_SERVER_FROM),
          context.internalGet(JsonKey.FROM_EMAIL));
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
      assertEquals(
          System.getenv(JsonKey.SUNBIRD_ENV_LOGO_URL), context.internalGet(JsonKey.ORG_IMAGE_URL));
    } else if (cacheVal) {
      assertEquals(
          propertiesCache.getProperty(JsonKey.SUNBIRD_ENV_LOGO_URL),
          context.internalGet(JsonKey.ORG_IMAGE_URL));
    }
  }

  @Test
  public void testCreateAuthTokenSuccess() {
    String authToken = ProjectUtil.createAuthToken("test", "tset1234");
    assertNotNull(authToken);
  }

  @Test
  public void testValidatePhoneNumberFailureWithInvalidPhoneNumber() {
    assertFalse(ProjectUtil.validatePhoneNumber("312"));
  }

  @Test
  public void testValidatePhoneNumberSuccess() {
    assertTrue(ProjectUtil.validatePhoneNumber("9844016699"));
  }

  @Test
  public void testGenerateRandomPasswordSuccess() {
    assertNotNull(ProjectUtil.generateRandomPassword());
  }

  @Test
  public void testCreateCheckResponseSuccess() {
    Map<String, Object> responseMap =
        ProjectUtil.createCheckResponse("LearnerService", false, null);
    assertEquals(true, responseMap.get(JsonKey.Healthy));
  }

  @Test
  public void testCreateCheckResponseFailureWithException() {
    Map<String, Object> responseMap =
        ProjectUtil.createCheckResponse(
            "LearnerService",
            true,
            new ProjectCommonException(
                ResponseCode.invalidObjectType.getErrorCode(),
                ResponseCode.invalidObjectType.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode()));
    assertEquals(false, responseMap.get(JsonKey.Healthy));
    assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), responseMap.get(JsonKey.ERROR));
    assertEquals(
        ResponseCode.invalidObjectType.getErrorMessage(), responseMap.get(JsonKey.ERRORMSG));
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
  public void testGetEkstepHeaderSuccess() {
    Map<String, String> map = ProjectUtil.getEkstepHeader();
    assertEquals(map.get("Content-Type"), "application/json");
    assertNotNull(map.get(JsonKey.AUTHORIZATION));
  }

  @Test
  public void testRegisterTagSuccess() {
    String response = null;
    try {
      response = ProjectUtil.registertag("testTag", "{}", ProjectUtil.getEkstepHeader());
    } catch (IOException e) {

    }
    assertNotNull(response);
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
    assertEquals("content", ProjectUtil.EsType.content.getTypeName());
    assertEquals("cbatch", ProjectUtil.EsType.course.getTypeName());
    assertEquals("course-batch", ProjectUtil.EsType.courseBatch.getTypeName());
    assertEquals("user", ProjectUtil.EsType.user.getTypeName());
    assertEquals("org", ProjectUtil.EsType.organisation.getTypeName());
    assertEquals("user-courses", ProjectUtil.EsType.usercourses.getTypeName());
    assertEquals("usernotes", ProjectUtil.EsType.usernotes.getTypeName());
    assertEquals("userprofilevisibility", ProjectUtil.EsType.userprofilevisibility.getTypeName());
  }

  @Test
  public void testEsIndexSuccess() {
    assertEquals("searchindex", ProjectUtil.EsIndex.sunbird.getIndexName());
  }

  @Test
  public void testUserRoleSuccess() {
    assertEquals("PUBLIC", ProjectUtil.UserRole.PUBLIC.getValue());
    assertEquals("CONTENT_CREATOR", ProjectUtil.UserRole.CONTENT_CREATOR.getValue());
    assertEquals("CONTENT_REVIEWER", ProjectUtil.UserRole.CONTENT_REVIEWER.getValue());
    assertEquals("ORG_ADMIN", ProjectUtil.UserRole.ORG_ADMIN.getValue());
    assertEquals("ORG_MEMBER", ProjectUtil.UserRole.ORG_MEMBER.getValue());
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
    assertEquals(new Integer(0), ProjectUtil.OrgStatus.INACTIVE.getValue());
    assertEquals(new Integer(1), ProjectUtil.OrgStatus.ACTIVE.getValue());
    assertEquals(new Integer(2), ProjectUtil.OrgStatus.BLOCKED.getValue());
    assertEquals(new Integer(3), ProjectUtil.OrgStatus.RETIRED.getValue());
  }

  @Test
  public void testCourseMgmtStatusSuccess() {
    assertEquals("draft", ProjectUtil.CourseMgmtStatus.DRAFT.getValue());
    assertEquals("live", ProjectUtil.CourseMgmtStatus.LIVE.getValue());
    assertEquals("retired", ProjectUtil.CourseMgmtStatus.RETIRED.getValue());
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
  public void testObjectTypesSuccess() {
    assertEquals("batch", ProjectUtil.ObjectTypes.batch.getValue());
    assertEquals("user", ProjectUtil.ObjectTypes.user.getValue());
    assertEquals("organisation", ProjectUtil.ObjectTypes.organisation.getValue());
  }

  @Test
  public void testSourceSuccess() {
    assertEquals("web", ProjectUtil.Source.WEB.getValue());
    assertEquals("android", ProjectUtil.Source.ANDROID.getValue());
    assertEquals("ios", ProjectUtil.Source.IOS.getValue());
  }

  @Test
  public void testSectionDataTypeSuccess() {
    assertEquals("course", ProjectUtil.SectionDataType.course.getTypeName());
    assertEquals("content", ProjectUtil.SectionDataType.content.getTypeName());
  }

  @Test
  public void testStatusSuccess() {
    assertEquals(1, ProjectUtil.Status.ACTIVE.getValue());
    assertEquals(0, ProjectUtil.Status.INACTIVE.getValue());
    assertEquals(false, ProjectUtil.ActiveStatus.INACTIVE.getValue());
    assertEquals(true, ProjectUtil.ActiveStatus.ACTIVE.getValue());
    assertEquals("orgimg", ProjectUtil.AzureContainer.orgImage.getName());
    assertEquals("userprofileimg", ProjectUtil.AzureContainer.userProfileImg.getName());
  }

  @Test
  public void testCreateAndThrowServerErrorSuccess() {
    try {
      ProjectUtil.createAndThrowServerError();
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.SERVER_ERROR.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCreateAndThrowInvalidUserDataExceptionSuccess() {
    try {
      ProjectUtil.createAndThrowInvalidUserDataException();
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidUsrData.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testGetDateRangeSuccess() {
    int noOfDays = 7;
    Map<String, String> map = ProjectUtil.getDateRange(noOfDays);
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, -noOfDays);
    assertEquals(map.get("startDate"), new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
    cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, -1);
    assertEquals(map.get("endDate"), new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
  }

  @Test
  public void testGetDateRangeFailure() {
    int noOfDays = 14;
    Map<String, String> map = ProjectUtil.getDateRange(noOfDays);
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, -noOfDays);
    assertEquals(map.get("startDate"), new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
    cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, noOfDays);
    assertNotEquals(map.get("endDate"), new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
  }

  @Test
  public void testGetDateRangeFailureWithZeroDays() {
    int noOfDays = 0;
    Map<String, String> map = ProjectUtil.getDateRange(noOfDays);
    assertNull(map.get("startDate"));
    assertNull(map.get("endDate"));
  }

  @Test
  public void testGetDateRangeFailureWithNegativeValue() {
    int noOfDays = -100;
    Map<String, String> map = ProjectUtil.getDateRange(noOfDays);
    assertNull(map.get("startDate"));
    assertNull(map.get("endDate"));
  }

  @Test
  public void testIsEmailValidFailureWithInvalidFormat() {
    boolean bool = ProjectUtil.isEmailvalid("amit.kumartarento.com");
    Assert.assertFalse(bool);
  }

  @Test
  public void testIsEmailValidSuccess() {
    boolean bool = ProjectUtil.isEmailvalid("amit.kumar@tarento.com");
    assertTrue(bool);
  }

  @Test
  public void testSendGetRequestSuccessWithEkStepBaseUrl() throws Exception {
    String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
    if (StringUtils.isBlank(ekStepBaseUrl)) {
      ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
    }
    String response = HttpUtil.sendGetRequest(ekStepBaseUrl + "/search/health", headers);
    assertNotNull(response);
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
}
