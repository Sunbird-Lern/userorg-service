package org.sunbird.service.notification;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.RequestContext;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.common.ProjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserService.class,
  UserServiceImpl.class,
  EmailTemplateDao.class,
  EmailTemplateDaoImpl.class,
  OrgService.class,
  OrgServiceImpl.class,
  EmailTemplateDao.class,
  EmailTemplateDaoImpl.class,
  ISmsProvider.class,
  SMSFactory.class,
  ProjectUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class NotificationServiceTest {

  @Test(expected = ProjectCommonException.class)
  public void processSMSWithInvalidPhoneNumber() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    List<Map<String, Object>> users = new ArrayList<>();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE,"9999999999");
    user.put(JsonKey.FIRST_NAME,"FirstName");
    user.put(JsonKey.ID,"21315-456-7894");
    users.add(user);
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserIds(Mockito.anyList(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(users);

    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> phones = new ArrayList<>();
    phones.add("1111111111");
    String smsText = "SMS Text";

    NotificationService service = new NotificationService();
    service.processSMS(userIds, phones, smsText, new RequestContext());
  }

  @Test(expected = ProjectCommonException.class)
  public void processSMSWithInvalidUserId() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    List<Map<String, Object>> users = new ArrayList<>();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE,"9999999999");
    user.put(JsonKey.FIRST_NAME,"FirstName");
    user.put(JsonKey.ID,"21315-456-7894");
    users.add(user);
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserIds(Mockito.anyList(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(users);

    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    userIds.add("invalidUserIds");
    List<String> phones = new ArrayList<>();
    phones.add("9999999999");
    String smsText = "SMS Text";

    NotificationService service = new NotificationService();
    service.processSMS(userIds, phones, smsText, new RequestContext());
  }

  @Test
  public void processSMS() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    List<Map<String, Object>> users = new ArrayList<>();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE,"9999999999");
    user.put(JsonKey.FIRST_NAME,"FirstName");
    user.put(JsonKey.ID,"21315-456-7894");
    users.add(user);
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserIds(Mockito.anyList(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(users);

    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> phones = new ArrayList<>();
    phones.add("9999999999");
    String smsText = "SMS Text";

    NotificationService service = new NotificationService();
    boolean bool = service.processSMS(userIds, phones, smsText, new RequestContext());
    Assert.assertFalse(bool);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateAndGetEmailListWithInvalidEmailFormat() {
    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> emails = new ArrayList<>();
    emails.add("xyzxyz@com");
    Map<String,Object> recipientSearchQuery = new HashMap<>();
    Map<String,Object> filters =  new HashMap<>();
    filters.put(JsonKey.FIRST_NAME,"FirstName");
    recipientSearchQuery.put(JsonKey.FILTERS,filters);


    NotificationService service = new NotificationService();
    service.validateAndGetEmailList(userIds, emails, recipientSearchQuery, new RequestContext());
  }

  @Test(expected = ProjectCommonException.class)
  public void validateAndGetEmailListWithEmptyFilters() {
    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> emails = new ArrayList<>();
    emails.add("xyz@xyz.com");
    Map<String,Object> recipientSearchQuery = new HashMap<>();
    Map<String,Object> filters =  new HashMap<>();
    recipientSearchQuery.put(JsonKey.FILTERS,filters);

    NotificationService service = new NotificationService();
    service.validateAndGetEmailList(userIds, emails, recipientSearchQuery, new RequestContext());
  }

  @Test(expected = ProjectCommonException.class)
  public void validateAndGetEmailListWithInvalidUserIds() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    List<Map<String, Object>> users = new ArrayList<>();
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserIds(Mockito.anyList(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(users);
    PowerMockito.when(userService.getUserEmailsBySearchQuery(Mockito.anyMap(), Mockito.any(RequestContext.class))).thenReturn(users);
    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> emails = new ArrayList<>();
    emails.add("xyz@xyz.com");
    Map<String,Object> recipientSearchQuery = new HashMap<>();
    Map<String,Object> filters =  new HashMap<>();
    filters.put(JsonKey.FIRST_NAME,"FirstName");
    recipientSearchQuery.put(JsonKey.FILTERS,filters);


    NotificationService service = new NotificationService();
    service.validateAndGetEmailList(userIds, emails, recipientSearchQuery, new RequestContext());
  }

  @Test
  public void validateAndGetEmailList() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    List<Map<String, Object>> users = new ArrayList<>();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.EMAIL,"xyz@xyz.com");
    user.put(JsonKey.FIRST_NAME,"FirstName");
    user.put(JsonKey.ID,"21315-456-7894");
    users.add(user);
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserIds(Mockito.anyList(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(users);
    PowerMockito.when(userService.getUserEmailsBySearchQuery(Mockito.anyMap(), Mockito.any(RequestContext.class))).thenReturn(users);
    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> emails = new ArrayList<>();
    emails.add("xyz@xyz.com");
    String smsText = "SMS Text";
    Map<String,Object> recipientSearchQuery = new HashMap<>();
    Map<String,Object> filters =  new HashMap<>();
    filters.put(JsonKey.FIRST_NAME,"FirstName");
    recipientSearchQuery.put(JsonKey.FILTERS,filters);


    NotificationService service = new NotificationService();
    List<String> emailList = service.validateAndGetEmailList(userIds, emails, recipientSearchQuery, new RequestContext());
    Assert.assertEquals(1,emailList.size());
  }

  @Test
  public void updateFirstNameAndOrgNameInEmailContext() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    OrgService orgService = PowerMockito.mock(OrgService.class);
    PowerMockito.mockStatic(OrgServiceImpl.class);
    PowerMockito.when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    Map<String, Object> org = new HashMap<>();
    org.put(JsonKey.ID,"45642118798");
    org.put(JsonKey.ORG_NAME,"orgName");
    PowerMockito.when(orgService.getOrgById(Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(org);
    User user = new User();
    user.setId("1315-45-4546");
    user.setRootOrgId("45642118798");
    PowerMockito.when(userService.getUserById(Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(user);
    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> emails = new ArrayList<>();
    emails.add("xyz@xyz.com");
    String smsText = "SMS Text";
    Map<String,Object> recipientSearchQuery = new HashMap<>();
    Map<String,Object> filters =  new HashMap<>();
    filters.put(JsonKey.FIRST_NAME,"FirstName");
    recipientSearchQuery.put(JsonKey.FILTERS,filters);
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.FIRST_NAME,"firstName");
    request.put(JsonKey.ORG_NAME,"orgName");

    NotificationService service = new NotificationService();
    service.updateFirstNameAndOrgNameInEmailContext(userIds, emails, request, new RequestContext());
    Assert.assertNotNull(request);
  }

  @Test
  public void updateFirstNameAndOrgNameInEmailContext2() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    OrgService orgService = PowerMockito.mock(OrgService.class);
    PowerMockito.mockStatic(OrgServiceImpl.class);
    PowerMockito.when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    Map<String, Object> org = new HashMap<>();
    org.put(JsonKey.ID,"45642118798");
    org.put(JsonKey.ORG_NAME,"orgName");
    PowerMockito.when(orgService.getOrgById(Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(org);
    User user = new User();
    user.setId("1315-45-4546");
    user.setRootOrgId("45642118798");
    PowerMockito.when(userService.getUserById(Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(user);
    List<String> userIds = new ArrayList<>();
    userIds.add("21315-456-7894");
    List<String> emails = new ArrayList<>();
    emails.add("xyz@xyz.com");
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.FIRST_NAME,"firstName");

    NotificationService service = new NotificationService();
    service.updateFirstNameAndOrgNameInEmailContext(userIds, emails, request, new RequestContext());
    Assert.assertNotNull(request);
  }

  @Test
  public void updateFirstNameAndOrgNameInEmailContext3() {
    List<String> userIds = new ArrayList<>();
    List<String> emails = new ArrayList<>();
    emails.add("xyz@xyz.com");
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.FIRST_NAME,"firstName");

    NotificationService service = new NotificationService();
    service.updateFirstNameAndOrgNameInEmailContext(userIds, emails, request, new RequestContext());
    Assert.assertNotNull(request);
  }

  @Test
  public void getEmailTemplateFile() {
    EmailTemplateDao templateDao = PowerMockito.mock(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.when(EmailTemplateDaoImpl.getInstance()).thenReturn(templateDao);
    PowerMockito.when(templateDao.getTemplate(Mockito.anyString(),Mockito.any(RequestContext.class))).thenReturn("template");
    NotificationService service = new NotificationService();
    String template = service.getEmailTemplateFile("templateName", new RequestContext());
    Assert.assertNotNull(template);
  }

  @Test(expected = ProjectCommonException.class)
  public void getEmailTemplateFileWithInvalidTemplateId() {
    EmailTemplateDao templateDao = PowerMockito.mock(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.when(EmailTemplateDaoImpl.getInstance()).thenReturn(templateDao);
    PowerMockito.when(templateDao.getTemplate(Mockito.anyString(),Mockito.any(RequestContext.class))).thenReturn("");
    NotificationService service = new NotificationService();
    service.getEmailTemplateFile("templateName", new RequestContext());
  }

  @Test
  public void getV2NotificationRequest() {
    Set<String> phoneList = new HashSet<>();
    phoneList.add("9999999999");
    Map<String,Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.SUBJECT,"subject");
    reqMap.put(JsonKey.BODY,"body");
    NotificationService service = new NotificationService();
    Map<String, Object> notiReq = service.getV2NotificationRequest(phoneList,reqMap,JsonKey.SMS,"");
    Assert.assertNotNull(notiReq);
  }

  @Test
  public void getV2NotificationRequest2() {
    Set<String> emailList = new HashSet<>();
    emailList.add("xyz@xyz.com");
    Map<String,Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.SUBJECT,"subject");
    reqMap.put(JsonKey.BODY,"body");
    String template = "Welcome to $instanceName. Your user account has now been created. Click on the link below to #if ($setPasswordLink) set a password #else verify your email ID #end and start using your account:$newline$link";
    NotificationService service = new NotificationService();
    Map<String, Object> notiReq = service.getV2NotificationRequest(emailList,reqMap,JsonKey.EMAIL,template);
    Assert.assertNotNull(notiReq);
  }

}
