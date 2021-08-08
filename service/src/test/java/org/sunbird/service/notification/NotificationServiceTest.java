package org.sunbird.service.notification;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.RequestContext;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.ProjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    Assert.assertTrue(emailList.size() == 1);
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

  //@Test
  public void getEmailTemplateFile() {
    EmailTemplateDao templateDao = PowerMockito.mock(EmailTemplateDao.class);
    PowerMockito.mockStatic(EmailTemplateDao.class);
    //PowerMockito.when(EmailTemplateDaoImpl.getInstance()).thenReturn(templateDao);
    //PowerMockito.when(templateDao.getTemplate(Mockito.anyString(),Mockito.any(RequestContext.class))).thenReturn("template");
    NotificationService service = new NotificationService();
    String template = service.getEmailTemplateFile("templateName", new RequestContext());
    Assert.assertNotNull(template);
  }

}
