package org.sunbird.actor.user;

import org.apache.pekko.actor.ActorRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.ResetPasswordService;
import org.sunbird.sso.KeycloakRequiredActionLinkUtil;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.SMSTemplateProvider;
import org.sunbird.util.UserUtility;

public class UserOnboardingNotificationActor extends BaseActor {

  private final ResetPasswordService resetPasswordService = new ResetPasswordService();

  @Inject
  @Named("email_service_actor")
  private ActorRef emailServiceActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    if ((ActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS
            .getValue()
            .equalsIgnoreCase(request.getOperation()))
        || (ActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS
            .getValue()
            .equalsIgnoreCase(request.getOperation()))) {
      sendEmailAndSms(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void sendEmailAndSms(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    // generate required action link and shorten the url
    UserUtility.decryptUserData(requestMap);
    requestMap.put(JsonKey.USERNAME, requestMap.get(JsonKey.USERNAME));
    requestMap.put(
        JsonKey.REDIRECT_URI, getSunbirdWebUrlPerTenant(requestMap, request.getRequestContext()));
    resetPasswordService.getUserRequiredActionLink(requestMap, true, request.getRequestContext());
    if (request.getOperation().equals(ActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS.getValue())) {
      // user created successfully send the onboarding mail
      Request welcomeMailReqObj = sendOnboardingMail(requestMap);
      if (null != welcomeMailReqObj && null != emailServiceActor) {
        welcomeMailReqObj.setRequestContext(request.getRequestContext());
        emailServiceActor.tell(welcomeMailReqObj, self());
      }
    } else if (request
        .getOperation()
        .equals(ActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue())) {
      Request resetMailReqObj = sendResetPassMail(requestMap);
      if (null != resetMailReqObj && null != emailServiceActor) {
        resetMailReqObj.setRequestContext(request.getRequestContext());
        emailServiceActor.tell(resetMailReqObj, self());
      }
    }

    if (StringUtils.isNotBlank((String) requestMap.get(JsonKey.PHONE))) {
      sendSMS(requestMap, request.getRequestContext());
    }
    if (StringUtils.isBlank((String) requestMap.get(JsonKey.PASSWORD))) {
      SSOManager ssoManager = SSOServiceFactory.getInstance();
      ssoManager.setRequiredAction(
          (String) requestMap.get(JsonKey.USER_ID), KeycloakRequiredActionLinkUtil.UPDATE_PASSWORD);
    }
  }

  private Request sendOnboardingMail(Map<String, Object> emailTemplateMap) {
    Request request = null;
    if ((StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.EMAIL)))) {
      String envName = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      String welcomeSubject = ProjectUtil.getConfigValue(JsonKey.ONBOARDING_MAIL_SUBJECT);
      emailTemplateMap.put(JsonKey.SUBJECT, ProjectUtil.formatMessage(welcomeSubject, envName));
      List<String> reciptientsMail = new ArrayList<>();
      reciptientsMail.add((String) emailTemplateMap.get(JsonKey.EMAIL));
      emailTemplateMap.put(JsonKey.RECIPIENT_EMAILS, reciptientsMail);
      emailTemplateMap.put(
          JsonKey.BODY, ProjectUtil.getConfigValue(JsonKey.ONBOARDING_WELCOME_MAIL_BODY));
      emailTemplateMap.put(JsonKey.NOTE, ProjectUtil.getConfigValue(JsonKey.MAIL_NOTE));
      emailTemplateMap.put(JsonKey.ORG_NAME, envName);
      String welcomeMessage = ProjectUtil.getConfigValue(JsonKey.ONBOARDING_MAIL_MESSAGE);
      emailTemplateMap.put(
          JsonKey.WELCOME_MESSAGE, ProjectUtil.formatMessage(welcomeMessage, envName));

      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "welcome");
      setRequiredActionLink(emailTemplateMap);
      if (StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK))
          && StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.VERIFY_EMAIL_LINK))) {
        logger.info("send On-boarding Mail: Email not sent as generated link is empty");
        return null;
      }

      request = new Request();
      request.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
      request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    }
    return request;
  }

  private void setRequiredActionLink(Map<String, Object> templateMap) {
    String setPasswordLink = (String) templateMap.get(JsonKey.SET_PASSWORD_LINK);
    String verifyEmailLink = (String) templateMap.get(JsonKey.VERIFY_EMAIL_LINK);
    if (StringUtils.isNotBlank(setPasswordLink)) {
      templateMap.put(JsonKey.LINK, setPasswordLink);
      templateMap.put(JsonKey.SET_PW_LINK, "true");
    } else if (StringUtils.isNotBlank(verifyEmailLink)) {
      templateMap.put(JsonKey.LINK, verifyEmailLink);
      templateMap.put(JsonKey.SET_PW_LINK, null);
    }
  }

  private void sendSMS(Map<String, Object> userMap, RequestContext context) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PHONE))) {
      String envName = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      setRequiredActionLink(userMap);
      if (StringUtils.isBlank((String) userMap.get(JsonKey.SET_PASSWORD_LINK))
          && StringUtils.isBlank((String) userMap.get(JsonKey.VERIFY_EMAIL_LINK))) {
        logger.info(context, "sendSMS: SMS not sent as generated link is empty");
        return;
      }
      Map<String, String> smsTemplate = new HashMap<>();
      smsTemplate.put("instanceName", envName);
      smsTemplate.put("newline", " ");
      smsTemplate.put(JsonKey.LINK, (String) userMap.get(JsonKey.LINK));
      smsTemplate.put(JsonKey.SET_PW_LINK, (String) userMap.get(JsonKey.SET_PW_LINK));
      String sms =
          SMSTemplateProvider.getSMSBody(JsonKey.WELCOME_SMS_TEMPLATE, smsTemplate, context);
      if (StringUtils.isBlank(sms)) {
        sms = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_WELCOME_MSG);
      }
      logger.debug(context, "SMS text : " + sms);
      String countryCode;
      if (StringUtils.isBlank((String) userMap.get(JsonKey.COUNTRY_CODE))) {
        countryCode = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE);
      } else {
        countryCode = (String) userMap.get(JsonKey.COUNTRY_CODE);
      }
      ISmsProvider smsProvider = SMSFactory.getInstance();
      logger.debug(context, "SMS text : " + sms + " with phone " + userMap.get(JsonKey.PHONE));
      boolean response =
          smsProvider.send((String) userMap.get(JsonKey.PHONE), countryCode, sms, context);
      logger.info(context, "Response from smsProvider : " + response);
    }
  }

  private Request sendResetPassMail(Map<String, Object> emailTemplateMap) {
    Request request = null;
    if (StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK))) {
      logger.info("Util:sendResetPassMail: Email not sent as generated link is empty");
      return null;
    } else if ((StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.EMAIL)))) {
      String envName = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      String welcomeSubject = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_RESET_PASS_MAIL_SUBJECT);
      emailTemplateMap.put(JsonKey.SUBJECT, ProjectUtil.formatMessage(welcomeSubject, envName));
      List<String> reciptientsMail = new ArrayList<>();
      reciptientsMail.add((String) emailTemplateMap.get(JsonKey.EMAIL));
      emailTemplateMap.put(JsonKey.RECIPIENT_EMAILS, reciptientsMail);
      emailTemplateMap.put(JsonKey.ORG_NAME, envName);
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "resetPassword");
      setRequiredActionLink(emailTemplateMap);
    } else if (StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.PHONE))) {
      emailTemplateMap.put(
          JsonKey.BODY,
          ProjectUtil.formatMessage(
              ProjectUtil.getConfigValue("sunbird_reset_pass_msg"),
              emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK)));
      emailTemplateMap.put(JsonKey.MODE, "SMS");
      List<String> phoneList = new ArrayList<>();
      phoneList.add((String) emailTemplateMap.get(JsonKey.PHONE));
      emailTemplateMap.put(JsonKey.RECIPIENT_PHONES, phoneList);
    } else {
      logger.info("sendResetPassMail: requested data is neither having email nor phone ");
      return null;
    }
    request = new Request();
    request.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
    request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    return request;
  }

  private String getSunbirdWebUrlPerTenant(Map<String, Object> userMap, RequestContext context) {
    StringBuilder webUrl = new StringBuilder();
    String slug = "";
    webUrl.append(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_WEB_URL));
    if (!StringUtils.isBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
      Map<String, Object> orgMap =
          OrgServiceImpl.getInstance()
              .getOrgById((String) userMap.get(JsonKey.ROOT_ORG_ID), context);
      slug = (String) orgMap.get(JsonKey.SLUG);
    }
    if (!StringUtils.isBlank(slug)) {
      webUrl.append("/" + slug);
    }
    return webUrl.toString();
  }
}
