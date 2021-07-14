package org.sunbird.learner.actors.notificationservice;

import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.mail.SendEmail;
import org.sunbird.mail.SendgridConnection;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.notificationservice.dao.EmailTemplateDao;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.util.Util;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"emailService"},
  asyncTasks = {"emailService"},
  dispatcher = "notification-dispatcher"
)
public class EmailServiceActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private DecryptionService decryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private SendgridConnection connection = new SendgridConnection();
  private String resetInterval = ProjectUtil.getConfigValue("sendgrid_connection_reset_interval");
  private volatile long timer;

  @Override
  public void onReceive(Request request) throws Throwable {
    if (null == connection.getTransport()) {
      connection.createConnection();
      // set timer value
      timer = System.currentTimeMillis();
    }
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

    List<String> userIds = (List<String>) request.get(JsonKey.RECIPIENT_USERIDS);
    if (CollectionUtils.isEmpty(userIds)) {
      userIds = new ArrayList<>();
    }

    if (request.get(JsonKey.MODE) != null
        && "sms".equalsIgnoreCase((String) request.get(JsonKey.MODE))) {
      // Sending sms
      List<String> phones = (List<String>) request.get(JsonKey.RECIPIENT_PHONES);
      if (CollectionUtils.isNotEmpty(phones)) {
        Iterator<String> itr = phones.iterator();
        while (itr.hasNext()) {
          String phone = itr.next();
          if (!ProjectUtil.validatePhone(phone, "")) {
            logger.info(
                actorMessage.getRequestContext(),
                "EmailServiceActor:sendMail: Removing invalid phone number =" + phone);
            itr.remove();
          }
        }
      }
      sendSMS(
          phones, userIds, (String) request.get(JsonKey.BODY), actorMessage.getRequestContext());
    } else {
      // Sending email
      List<String> emails = (List<String>) request.get(JsonKey.RECIPIENT_EMAILS);
      if (CollectionUtils.isNotEmpty(emails)) {
        checkEmailValidity(emails);
      } else {
        emails = new ArrayList<>();
      }
      // Fetch public user emails from Elastic Search based on recipient search query given in
      // request.
      getUserEmailsFromSearchQuery(request, emails, userIds, actorMessage.getRequestContext());
      List<Map<String, Object>> userList = null;

      if (CollectionUtils.isNotEmpty(userIds)) {
        userList = getUsersFromDB(userIds, actorMessage.getRequestContext());
        validateUserIds(userIds, userList, emails, actorMessage.getRequestContext());
      }

      validateRecipientsLimit(emails);

      if ((userIds.size() == 1) && (emails.size() == 1) && (CollectionUtils.isNotEmpty(userList))) {
        Map<String, Object> user = userList.get(0);
        if (StringUtils.isNotBlank((String) request.get(JsonKey.FIRST_NAME))) {
          request.put(
              JsonKey.NAME, StringUtils.capitalize((String) request.get(JsonKey.FIRST_NAME)));
        }
        // fetch orgname inorder to set in the Template context
        String orgName =
            getOrgName(
                request, (String) user.get(JsonKey.ROOT_ORG_ID), actorMessage.getRequestContext());
        if (StringUtils.isNotBlank(orgName)) {
          request.put(JsonKey.ORG_NAME, orgName);
        }
        logger.info(
            actorMessage.getRequestContext(),
            "EmailServiceActor:sendMail: Sending email to = " + emails + " emails");
      } else {
        logger.info(
            actorMessage.getRequestContext(),
            "EmailServiceActor:sendMail: Sending email to = " + emails.size() + " emails");
      }
      if (CollectionUtils.isNotEmpty(emails)) {
        String template =
            getEmailTemplateFile(
                (String) request.get(JsonKey.EMAIL_TEMPLATE_TYPE),
                actorMessage.getRequestContext());
        sendMail(request, emails, template, actorMessage.getRequestContext());
      }
    }

    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(res, self());
  }

  private void sendMail(
      Map<String, Object> request,
      List<String> emails,
      String template,
      RequestContext requestContext) {
    try {
      SendEmail sendEmail = new SendEmail();
      Velocity.init();
      VelocityContext context = ProjectUtil.getContext(request);
      StringWriter writer = new StringWriter();
      Velocity.evaluate(context, writer, "SimpleVelocity", template);
      long interval = 60000L;
      if (StringUtils.isNotBlank(resetInterval)) {
        interval = Long.parseLong(resetInterval);
      }
      if (null == connection.getTransport()
          || ((System.currentTimeMillis()) - timer >= interval)
          || (!connection.getTransport().isConnected())) {
        resetConnection();
      }
      sendEmail.send(
          emails.toArray(new String[emails.size()]),
          (String) request.get(JsonKey.SUBJECT),
          context,
          writer,
          connection.getSession(),
          connection.getTransport());
    } catch (Exception e) {
      logger.error(
          requestContext,
          "EmailServiceActor:sendMail: Exception occurred with message = " + e.getMessage(),
          e);
    }
  }

  private void resetConnection() {
    logger.info("SMTP Transport client connection is closed or timed out. Create new connection.");
    connection.createConnection();
    // set timer value
    timer = System.currentTimeMillis();
  }

  /**
   * This method will send sms to targeted user.
   *
   * @param phones list of phone numbers
   * @param userIds list of userIds
   * @param smsText message
   */
  private void sendSMS(
      List<String> phones, List<String> userIds, String smsText, RequestContext context) {
    logger.info(context, "EmailServiceActor:sendSMS: method started  = " + smsText);
    if (CollectionUtils.isEmpty(phones)) {
      phones = new ArrayList<String>();
    }
    if (CollectionUtils.isNotEmpty(userIds)) {
      List<Map<String, Object>> userList = getUsersFromDB(userIds, context);
      if (userIds.size() != userList.size()) {
        findMissingUserIds(userIds, userList);
      } else {
        for (Map<String, Object> userMap : userList) {
          String phone = (String) userMap.get(JsonKey.PHONE);
          if (StringUtils.isNotBlank(phone)) {
            String decryptedPhone = decryptionService.decryptData(phone, context);
            if (!phones.contains(decryptedPhone)) {
              phones.add(decryptedPhone);
            }
          }
        }
      }
    }

    validateRecipientsLimit(phones);

    logger.info(
        context, "EmailServiceActor:sendSMS: Sending sendSMS to = " + phones.size() + " phones");
    try {
      ISmsProvider smsProvider = SMSFactory.getInstance();
      smsProvider.send(phones, smsText);
    } catch (Exception e) {
      logger.error(
          context,
          "EmailServiceActor:sendSMS: Exception occurred with message = " + e.getMessage(),
          e);
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
      logger.error(
          "EmailServiceActor:validateEmailRecipientsLimit: Exception occurred with error message = "
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

  private void validateUserIds(
      List<String> userIds,
      List<Map<String, Object>> userList,
      List<String> emails,
      RequestContext context) {
    // Fetch private (masked in Elastic Search) user emails from Cassandra DB
    if (userIds.size() != userList.size()) {
      findMissingUserIds(userIds, userList);
    } else {
      for (Map<String, Object> userMap : userList) {
        String email = (String) userMap.get(JsonKey.EMAIL);
        if (StringUtils.isNotBlank(email)) {
          String decryptedEmail = decryptionService.decryptData(email, context);
          emails.add(decryptedEmail);
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

  private String getEmailTemplateFile(String templateName, RequestContext context) {
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

  private List<Map<String, Object>> getUsersFromDB(List<String> userIds, RequestContext context) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    List<String> userIdList = new ArrayList<>(userIds);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.ID);
    fields.add(JsonKey.FIRST_NAME);
    fields.add(JsonKey.EMAIL);
    fields.add(JsonKey.PHONE);
    fields.add(JsonKey.ROOT_ORG_ID);
    Response response =
        cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), fields, userIdList, context);
    List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    return userList;
  }

  private void getUserEmailsFromSearchQuery(
      Map<String, Object> request,
      List<String> emails,
      List<String> userIds,
      RequestContext context) {
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
                ProjectUtil.EsType.user.getTypeName(),
                context);
        esResult = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
      } catch (Exception ex) {
        logger.error(
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
                String email =
                    decryptionService.decryptData((String) user.get(JsonKey.EMAIL), context);
                if (ProjectUtil.isEmailvalid(email)) {
                  emails.add(email);
                } else {
                  logger.info(
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

  private String getOrgName(Map<String, Object> request, String rootOrgId, RequestContext context) {
    String orgName = (String) request.get(JsonKey.ORG_NAME);
    if (StringUtils.isNotBlank(orgName)) {
      return orgName;
    }
    if (StringUtils.isNotBlank(rootOrgId)) {
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      Response response =
          cassandraOperation.getRecordById(
              orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), rootOrgId, context);
      List<Map<String, Object>> orgList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      Map<String, Object> org = null;
      if (CollectionUtils.isNotEmpty(orgList)) {
        org = orgList.get(0);
      }
      if (MapUtils.isNotEmpty(org)) {
        orgName =
            (org.get(JsonKey.ORG_NAME) != null ? (String) org.get(JsonKey.ORGANISATION_NAME) : "");
      }
    }
    return orgName;
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
