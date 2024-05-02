package org.sunbird.service.notification;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.User;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.RequestContext;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.ProjectUtil;

import java.text.MessageFormat;
import java.util.*;

public class NotificationService {

  private final LoggerUtil logger = new LoggerUtil(NotificationService.class);
  private final UserService userService = UserServiceImpl.getInstance();
  private final OrgService orgService = OrgServiceImpl.getInstance();

  public boolean processSMS(
      List<String> userIds, List<String> phones, String smsText, RequestContext requestContext) {
    validatePhoneOrEmail(phones, JsonKey.PHONE);
    Set<String> phoneList = getEmailOrPhoneListByUserIds(userIds, JsonKey.PHONE, requestContext);
    // Merge All Phone
    if (CollectionUtils.isNotEmpty(phones)) {
      phoneList.addAll(phones);
    }
    validateRecipientsLimit(phoneList);

    return sendSMS(new ArrayList<>(phoneList), smsText, requestContext);
  }

  private void findMissingUserIds(
      List<String> requestedUserIds, List<Map<String, Object>> userListInDB) {
    // if requested userId list and cassandra user list size not same , means
    // requested userId
    // list
    // contains some invalid userId
    List<String> userIdFromDBList = new ArrayList<>();
    userListInDB.forEach(user -> userIdFromDBList.add((String) user.get(JsonKey.ID)));
    requestedUserIds.forEach(
        userId -> {
          if (!userIdFromDBList.contains(userId)) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.invalidParameterValue,
                MessageFormat.format(
                    ResponseCode.invalidParameterValue.getErrorMessage(),
                    userId,
                    JsonKey.RECIPIENT_USERIDS));
          }
        });
  }

  private void validateRecipientsLimit(Set<String> recipients) {
    int maxLimit = 100;
    try {
      if (StringUtils.isNotBlank(
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_EMAIL_MAX_RECEPIENT_LIMIT))) {
        maxLimit =
            Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_EMAIL_MAX_RECEPIENT_LIMIT));
      }
    } catch (Exception exception) {
      logger.error(
          "NotificationService:validateEmailRecipientsLimit: Exception occurred with error message = "
              + exception.getMessage(),
          exception);
      maxLimit = 100;
    }
    if (recipients.size() > maxLimit) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.emailNotSentRecipientsExceededMaxLimit,
          MessageFormat.format(
              ResponseCode.emailNotSentRecipientsExceededMaxLimit.getErrorMessage(), maxLimit));
    }
  }

  private boolean sendSMS(List<String> phones, String smsText, RequestContext context) {
    logger.info(
        context, "NotificationService:sendSMS: Sending sendSMS to = " + phones.size() + " phones");
    try {
      ISmsProvider smsProvider = SMSFactory.getInstance();
      return smsProvider.send(phones, smsText, context);
    } catch (Exception e) {
      logger.error(
          context,
          "NotificationService:sendSMS: Exception occurred with message = " + e.getMessage(),
          e);
      return false;
    }
  }

  public List<String> validateAndGetEmailList(
      List<String> userIds,
      List<String> emails,
      Map<String, Object> recipientSearchQuery,
      RequestContext requestContext) {
    validatePhoneOrEmail(emails, JsonKey.EMAIL);
    List<Map<String, Object>> searchQueryResult =
        getUserEmailsFromSearchQuery(recipientSearchQuery, requestContext);
    for (Map<String, Object> result : searchQueryResult) {
      if (StringUtils.isNotBlank((String) result.get(JsonKey.EMAIL))) {
        emails.add((String) result.get(JsonKey.EMAIL));
      }
    }
    Set<String> emailList = getEmailOrPhoneListByUserIds(userIds, JsonKey.EMAIL, requestContext);
    // Merge All Phone
    if (CollectionUtils.isNotEmpty(emails)) {
      emailList.addAll(emails);
    }
    validateRecipientsLimit(emailList);
    return new ArrayList<>(emailList);
  }

  public void updateFirstNameAndOrgNameInEmailContext(
      List<String> userIds,
      List<String> emails,
      Map<String, Object> request,
      RequestContext requestContext) {
    if ((userIds.size() == 1) && (emails.size() == 1)) {
      User user = userService.getUserById(userIds.get(0), requestContext);
      if (StringUtils.isNotBlank((String) request.get(JsonKey.FIRST_NAME))) {
        request.put(JsonKey.NAME, StringUtils.capitalize((String) request.get(JsonKey.FIRST_NAME)));
      }
      // fetch orgName inorder to set in the Template context
      String orgName = getOrgName(request, user.getRootOrgId(), requestContext);
      if (StringUtils.isNotBlank(orgName)) {
        request.put(JsonKey.ORG_NAME, orgName);
      }
      logger.info(
          requestContext,
          "NotificationService:updateFirstNameAndOrgNameInEmailContext: Sending email to = "
              + emails.size()
              + " email(s)");
    } else {
      logger.info(
          requestContext,
          "NotificationService:updateFirstNameAndOrgNameInEmailContext: Sending email to = "
              + emails.size()
              + " email(s)");
    }
  }

  public String getEmailTemplateFile(String templateName, RequestContext context) {
    EmailTemplateDao emailTemplateDao = EmailTemplateDaoImpl.getInstance();
    String template = emailTemplateDao.getTemplate(templateName, context);
    if (StringUtils.isBlank(template)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              templateName,
              JsonKey.EMAIL_TEMPLATE_TYPE));
    }
    return template;
  }

  private String getOrgName(Map<String, Object> request, String rootOrgId, RequestContext context) {
    String orgName = (String) request.get(JsonKey.ORG_NAME);
    if (StringUtils.isNotBlank(orgName)) {
      return orgName;
    }
    if (StringUtils.isNotBlank(rootOrgId)) {
      Map<String, Object> org = orgService.getOrgById(rootOrgId, context);
      if (MapUtils.isNotEmpty(org)) {
        orgName =
            (org.get(JsonKey.ORG_NAME) != null ? (String) org.get(JsonKey.ORGANISATION_NAME) : "");
      }
    }
    return orgName;
  }

  public Set<String> getEmailOrPhoneListByUserIds(
      List<String> userIds, String type, RequestContext requestContext) {
    Set<String> emailOrPhoneList = new HashSet<>();
    if (CollectionUtils.isNotEmpty(userIds)) {
      List<Map<String, Object>> dbUserPhoneEmailList =
          userService.getDecryptedEmailPhoneByUserIds(userIds, type, requestContext);
      if (userIds.size() != dbUserPhoneEmailList.size()) {
        findMissingUserIds(userIds, dbUserPhoneEmailList);
      }
      for (Map<String, Object> userMap : dbUserPhoneEmailList) {
        String emailOrPhone = (String) userMap.get(type);
        if (StringUtils.isNotBlank(emailOrPhone)) {
          emailOrPhoneList.add(emailOrPhone);
        }
      }
    }
    return emailOrPhoneList;
  }

  private List<Map<String, Object>> getUserEmailsFromSearchQuery(
      Map<String, Object> recipientSearchQuery, RequestContext context) {
    if (MapUtils.isNotEmpty(recipientSearchQuery)) {
      if (MapUtils.isEmpty((Map<String, Object>) recipientSearchQuery.get(JsonKey.FILTERS))) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                recipientSearchQuery,
                JsonKey.RECIPIENT_SEARCH_QUERY));
      }
      List<String> fields = new ArrayList<>();
      fields.add(JsonKey.USER_ID);
      fields.add(JsonKey.EMAIL);
      recipientSearchQuery.put(JsonKey.FIELDS, fields);
      return userService.getUserEmailsBySearchQuery(recipientSearchQuery, context);
    }
    return Collections.emptyList();
  }

  private void validatePhoneOrEmail(List<String> emailOrPhones, String type) {
    if (CollectionUtils.isNotEmpty(emailOrPhones)) {
      for (String emailOrPhone : emailOrPhones) {
        if (JsonKey.EMAIL.equalsIgnoreCase(type) && !ProjectUtil.isEmailvalid(emailOrPhone)) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.invalidParameterValue,
              MessageFormat.format(
                  ResponseCode.invalidParameterValue.getErrorMessage(),
                  emailOrPhone,
                  JsonKey.RECIPIENT_EMAILS));
        }

        if (JsonKey.PHONE.equalsIgnoreCase(type) && !ProjectUtil.validatePhone(emailOrPhone, "")) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.invalidParameterValue,
              MessageFormat.format(
                  ResponseCode.invalidParameterValue.getErrorMessage(),
                  emailOrPhone,
                  JsonKey.RECIPIENT_PHONES));
        }
      }
    }
  }

  public Map<String, Object> getV2NotificationRequest(
      Set<String> phoneOrEmailList, Map<String, Object> requestMap, String mode, String template) {
    Map<String, Object> notiReq = new HashMap<>();
    notiReq.put("deliveryType", "message");
    Map<String, Object> config = new HashMap<>(2);
    config.put("sender", System.getenv("sunbird_mail_server_from_email"));
    config.put(JsonKey.SUBJECT, requestMap.remove(JsonKey.SUBJECT));
    notiReq.put("config", config);
    Map<String, Object> templateMap = new HashMap<>(2);
    if (mode.equalsIgnoreCase(JsonKey.SMS)) {
      templateMap.put(JsonKey.DATA, requestMap.remove(JsonKey.BODY));
      templateMap.put(JsonKey.PARAMS, Collections.emptyMap());
      notiReq.put("template", templateMap);
      notiReq.put(JsonKey.MODE, JsonKey.PHONE);
    } else {
      templateMap.put(JsonKey.DATA, template);
      VelocityContext context = ProjectUtil.getContext(requestMap);
      Object[] keys = context.getKeys();
      for (Object obj : keys) {
        if (obj instanceof String) {
          String key = (String) obj;
          requestMap.put(key, context.get(key));
        }
      }
      templateMap.put(JsonKey.PARAMS, requestMap);
      notiReq.put("template", templateMap);
      notiReq.put(JsonKey.MODE, JsonKey.EMAIL);
    }
    notiReq.put("ids", new ArrayList<>(phoneOrEmailList));
    return notiReq;
  }
}
