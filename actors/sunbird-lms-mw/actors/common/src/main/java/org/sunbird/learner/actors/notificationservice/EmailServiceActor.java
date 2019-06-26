package org.sunbird.learner.actors.notificationservice;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.mail.SendMail;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.notificationservice.dao.EmailTemplateDao;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.util.Util;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"emailService"},
  asyncTasks = {"emailService"}
)
public class EmailServiceActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private static final String NOTIFICATION_MODE = "sms";

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase(BackgroundOperations.emailService.name())) {
      sendMail(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  @SuppressWarnings({"unchecked"})
  private void sendMail(Request actorMessage) {
    Map<String, Object> request =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.EMAIL_REQUEST);
    List<String> emails = (List<String>) request.get(JsonKey.RECIPIENT_EMAILS);
    if (CollectionUtils.isNotEmpty(emails)) {
      checkEmailValidity(emails);
    } else {
      emails = new ArrayList<>();
    }

    List<String> userIds = (List<String>) request.get(JsonKey.RECIPIENT_USERIDS);
    if (CollectionUtils.isEmpty(userIds)) {
      userIds = new ArrayList<>();
    }

    if (request.get(JsonKey.MODE) != null
        && NOTIFICATION_MODE.equalsIgnoreCase((String) request.get(JsonKey.MODE))) {
      List<String> phones = (List<String>) request.get(JsonKey.RECIPIENT_PHONES);
      if (CollectionUtils.isNotEmpty(phones)) {
        Iterator<String> itr = phones.iterator();
        while (itr.hasNext()) {
          String phone = itr.next();
          if (!ProjectUtil.validatePhone(phone, "")) {
            ProjectLogger.log(
                "EmailServiceActor:sendMail: Removing invalid phone number =" + phone,
                LoggerEnum.INFO.name());
            itr.remove();
          }
        }
      }
      sendSMS(phones, userIds, (String) request.get(JsonKey.BODY));
      return;
    }

    // Fetch public user emails from Elastic Search based on recipient search query given in
    // request.
    getUserEmailsFromSearchQuery(request, emails, userIds);

    validateUserIds(userIds, emails);
    validateRecipientsLimit(emails);

    Map<String, Object> user = null;
    if (CollectionUtils.isNotEmpty(emails)) {
      user = getUserInfo(emails.get(0));
    }

    String name = "";
    if (emails.size() == 1) {
      name = StringUtils.capitalize((String) user.get(JsonKey.FIRST_NAME));
    }

    // fetch orgname inorder to set in the Template context
    String orgName = getOrgName(request, (String) user.get(JsonKey.USER_ID));

    request.put(JsonKey.NAME, name);
    if (orgName != null) {
      request.put(JsonKey.ORG_NAME, orgName);
    }

    String template = getEmailTemplateFile((String) request.get(JsonKey.EMAIL_TEMPLATE_TYPE));

    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(res, self());
    ProjectLogger.log(
        "EmailServiceActor:sendMail: Sending email to = " + emails.size() + " emails",
        LoggerEnum.INFO.name());
    try {
      SendMail.sendMailWithBody(
          emails.toArray(new String[emails.size()]),
          (String) request.get(JsonKey.SUBJECT),
          ProjectUtil.getContext(request),
          template);
    } catch (Exception e) {
      ProjectLogger.log(
          "EmailServiceActor:sendMail: Exception occurred with message = " + e.getMessage(), e);
    }
  }

  /**
   * This method will send sms to targeted user.
   *
   * @param phones list of phone numbers
   * @param userIds list of userIds
   * @param smsText message
   */
  private void sendSMS(List<String> phones, List<String> userIds, String smsText) {
    ProjectLogger.log(
        "EmailServiceActor:sendSMS: method started  = " + smsText, LoggerEnum.INFO.name());
    if (CollectionUtils.isEmpty(phones)) {
      phones = new ArrayList<String>();
    }
    if (CollectionUtils.isNotEmpty(userIds)) {
      List<Map<String, Object>> userList = getUsersFromDB(userIds);
      if (userIds.size() != userList.size()) {
        findMissingUserIds(userIds, userList);
      } else {
        for (Map<String, Object> userMap : userList) {
          String phone = (String) userMap.get(JsonKey.PHONE);
          if (StringUtils.isNotBlank(phone)) {
            String decryptedPhone = decryptionService.decryptData(phone);
            if (!phones.contains(decryptedPhone)) {
              phones.add(decryptedPhone);
            }
          }
        }
      }
    }

    validateRecipientsLimit(phones);
    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(res, self());
    ProjectLogger.log(
        "EmailServiceActor:sendSMS: Sending sendSMS to = " + phones.size() + " phones",
        LoggerEnum.INFO.name());
    try {
      ISmsProvider smsProvider = SMSFactory.getInstance("91SMS");
      smsProvider.send(phones, smsText);
    } catch (Exception e) {
      ProjectLogger.log(
          "EmailServiceActor:sendSMS: Exception occurred with message = " + e.getMessage(), e);
    }
  }

  private void validateRecipientsLimit(List<String> recipients) {
    if (CollectionUtils.isEmpty(recipients)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.emailNotSentRecipientsZero,
          ResponseCode.emailNotSentRecipientsZero.getErrorMessage());
    }
    int maxLimit = 100;
    try {
      maxLimit =
          Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_EMAIL_MAX_RECEPIENT_LIMIT));
    } catch (Exception exception) {
      ProjectLogger.log(
          "EmailServiceActor:validateEmailRecipientsLimit: Exception occurred with error message = "
              + exception.getMessage(),
          LoggerEnum.INFO);
      maxLimit = 100;
    }
    if (recipients.size() > maxLimit) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.emailNotSentRecipientsExceededMaxLimit,
          MessageFormat.format(
              ResponseCode.emailNotSentRecipientsExceededMaxLimit.getErrorMessage(), maxLimit));
    }
  }

  private void validateUserIds(List<String> userIds, List<String> emails) {
    // Fetch private (masked in Elastic Search) user emails from Cassandra DB
    if (CollectionUtils.isNotEmpty(userIds)) {
      List<Map<String, Object>> userList = getUsersFromDB(userIds);
      if (userIds.size() != userList.size()) {
        findMissingUserIds(userIds, userList);
      } else {
        for (Map<String, Object> userMap : userList) {
          String email = (String) userMap.get(JsonKey.EMAIL);
          if (StringUtils.isNotBlank(email)) {
            String decryptedEmail = decryptionService.decryptData(email);
            emails.add(decryptedEmail);
          }
        }
      }
    }
  }

  private void findMissingUserIds(
      List<String> requestedUserIds, List<Map<String, Object>> userListInDB) {
    // if requested userId list and cassandra user list size not same , means
    // requested userId
    // list
    // contains some invalid userId
    List<String> userIdFromDBList = new ArrayList<>();
    userListInDB.forEach(
        user -> {
          userIdFromDBList.add((String) user.get(JsonKey.ID));
        });
    requestedUserIds.forEach(
        userId -> {
          if (!userIdFromDBList.contains(userId)) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.invalidParameterValue,
                MessageFormat.format(
                    ResponseCode.invalidParameterValue.getErrorMessage(),
                    userId,
                    JsonKey.RECIPIENT_USERIDS));
            return;
          }
        });
  }

  private String getEmailTemplateFile(String templateName) {
    EmailTemplateDao emailTemplateDao = EmailTemplateDaoImpl.getInstance();
    String template = emailTemplateDao.getTemplate(templateName);
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

  private List<Map<String, Object>> getUsersFromDB(List<String> userIds) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    List<String> userIdList = new ArrayList<>(userIds);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.ID);
    fields.add(JsonKey.FIRST_NAME);
    fields.add(JsonKey.EMAIL);
    fields.add(JsonKey.PHONE);
    Response response =
        cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), fields, userIdList);
    List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    return userList;
  }

  private void getUserEmailsFromSearchQuery(
      Map<String, Object> request, List<String> emails, List<String> userIds) {
    Map<String, Object> recipientSearchQuery =
        (Map<String, Object>) request.get(JsonKey.RECIPIENT_SEARCH_QUERY);
    if (MapUtils.isNotEmpty(recipientSearchQuery)) {

      if (MapUtils.isEmpty((Map<String, Object>) recipientSearchQuery.get(JsonKey.FILTERS))) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                recipientSearchQuery,
                JsonKey.RECIPIENT_SEARCH_QUERY));
        return;
      }

      List<String> fields = new ArrayList<>();
      fields.add(JsonKey.USER_ID);
      fields.add(JsonKey.EMAIL);
      recipientSearchQuery.put(JsonKey.FIELDS, fields);
      Map<String, Object> esResult = Collections.emptyMap();
      try {
        Future<Map<String, Object>> esResultF =
            esService.search(
                ElasticSearchHelper.createSearchDTO(recipientSearchQuery),
                EsType.user.getTypeName());
        esResult = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
      } catch (Exception ex) {
        ProjectLogger.log(
            "EmailServiceActor:getUserEmailsFromSearchQuery: Exception occurred with error message = "
                + ex.getMessage(),
            ex);
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                recipientSearchQuery,
                JsonKey.RECIPIENT_SEARCH_QUERY));
        return;
      }
      if (MapUtils.isNotEmpty(esResult)
          && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
        List<Map<String, Object>> usersList =
            (List<Map<String, Object>>) esResult.get(JsonKey.CONTENT);
        usersList.forEach(
            user -> {
              if (StringUtils.isNotBlank((String) user.get(JsonKey.EMAIL))) {
                String email = decryptionService.decryptData((String) user.get(JsonKey.EMAIL));
                if (ProjectUtil.isEmailvalid(email)) {
                  emails.add(email);
                } else {
                  ProjectLogger.log(
                      "EmailServiceActor:sendMail: Email decryption failed for userId = "
                          + user.get(JsonKey.USER_ID));
                }
              } else {
                // If email is blank (or private) then fetch email from cassandra
                userIds.add((String) user.get(JsonKey.USER_ID));
              }
            });
      }
    }
  }

  private String getOrgName(Map<String, Object> request, String usrId) {
    String orgName = (String) request.get(JsonKey.ORG_NAME);
    if (StringUtils.isNotBlank(orgName)) {
      return orgName;
    }
    Future<Map<String, Object>> esUserResultF =
        esService.getDataByIdentifier(EsType.user.getTypeName(), usrId);
    Map<String, Object> esUserResult =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esUserResultF);
    if (null != esUserResult) {
      String rootOrgId = (String) esUserResult.get(JsonKey.ROOT_ORG_ID);
      if (!(StringUtils.isBlank(rootOrgId))) {

        Future<Map<String, Object>> esOrgResultF =
            esService.getDataByIdentifier(EsType.organisation.getTypeName(), rootOrgId);
        Map<String, Object> esOrgResult =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esOrgResultF);
        if (null != esOrgResult) {
          orgName =
              (esOrgResult.get(JsonKey.ORG_NAME) != null
                  ? (String) esOrgResult.get(JsonKey.ORGANISATION_NAME)
                  : "");
        }
      }
    }
    return orgName;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getUserInfo(String email) {
    String encryptedMail = "";
    try {
      encryptedMail = encryptionService.encryptData(email);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    SearchDTO searchDTO = new SearchDTO();
    Map<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put(JsonKey.EMAIL, encryptedMail);
    searchDTO.addAdditionalProperty(JsonKey.FILTERS, additionalProperties);
    Future<Map<String, Object>> esResultF = esService.search(searchDTO, EsType.user.getTypeName());
    Map<String, Object> esResult =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    if (MapUtils.isNotEmpty(esResult)
        && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
      return ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT)).get(0);
    } else {
      return Collections.EMPTY_MAP;
    }
  }

  private void checkEmailValidity(List<String> emails) {
    for (String email : emails) {
      if (!ProjectUtil.isEmailvalid(email)) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                email,
                JsonKey.RECIPIENT_EMAILS));
        return;
      }
    }
  }
}
