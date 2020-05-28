package org.sunbird.user.actors;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.KeycloakRequiredActionLinkUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.user.util.UserActorOperations;

@ActorConfig(
  tasks = {},
  asyncTasks = {"processOnBoardingMailAndSms"}
)
public class UserOnboardingNotificationActor extends BaseActor {

  private SSOManager ssoManager = SSOServiceFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (UserActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      sendEmailAndSms(request);
    } else {
      onReceiveUnsupportedOperation("ProcessOnBoardingMailAndSmsActor");
    }
  }

  private void sendEmailAndSms(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    // generate required action link and shorten the url
    UserUtility.decryptUserData(requestMap);
    requestMap.put(JsonKey.USERNAME, requestMap.get(JsonKey.USERNAME));
    requestMap.put(JsonKey.REDIRECT_URI, Util.getSunbirdWebUrlPerTenent(requestMap));
    Util.getUserRequiredActionLink(requestMap);
    // user created successfully send the onboarding mail
    Request welcomeMailReqObj = Util.sendOnboardingMail(requestMap);
    if (null != welcomeMailReqObj) {
      tellToAnother(welcomeMailReqObj);
    }

    if (StringUtils.isNotBlank((String) requestMap.get(JsonKey.PHONE))) {
      Util.sendSMS(requestMap);
    }

    if (StringUtils.isBlank((String) requestMap.get(JsonKey.PASSWORD))) {
      ssoManager.setRequiredAction(
          (String) requestMap.get(JsonKey.USER_ID), KeycloakRequiredActionLinkUtil.UPDATE_PASSWORD);
    }

    if (StringUtils.isNotBlank((String) requestMap.get(JsonKey.EMAIL))) {
      ssoManager.setRequiredAction(
          (String) requestMap.get(JsonKey.USER_ID), KeycloakRequiredActionLinkUtil.VERIFY_EMAIL);
    }
  }
}
