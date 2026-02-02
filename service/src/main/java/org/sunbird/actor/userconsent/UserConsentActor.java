package org.sunbird.actor.userconsent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.service.userconsent.UserConsentService;
import org.sunbird.service.userconsent.impl.UserConsentServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.Util;

public class UserConsentActor extends BaseActor {

  private final UserService userService = UserServiceImpl.getInstance();
  private final UserConsentService userConsentService = UserConsentServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "updateUserConsent":
        updateUserConsent(request);
        break;
      case "getUserConsent":
        getUserConsent(request);
        break;
      default:
        onReceiveUnsupportedOperation();
        break;
    }
  }

  private void getUserConsent(Request request) {

    List<Map<String, Object>> consentList = userConsentService.getConsent(request);

    Response response = new Response();
    if (CollectionUtils.isNotEmpty(consentList)) {
      //Remove revoked consent from the list
      List<Map<String, Object>> consentResponseList = constructConsentResponse(consentList);
      consentList=consentResponseList;
    }
    if(CollectionUtils.isNotEmpty(consentList)){
      response.put(JsonKey.CONSENT_RESPONSE, consentList);
    }else {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(), JsonKey.USER_CONSENT_TEXT),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    sender().tell(response, self());
  }

  private void updateUserConsent(Request request) {
    RequestContext context = request.getRequestContext();
    Map<String, Object> consent =
        (Map<String, Object>)
            request.getRequest().getOrDefault(JsonKey.CONSENT_BODY, new HashMap<String, Object>());
    String userId = (String) consent.get(JsonKey.USER_ID);

    request.getRequest().put(JsonKey.USER_ID, userId); // Important for userid validation
    userService.validateUserId(request, null, context);

    String consumerId = (String) consent.get(JsonKey.CONSENT_CONSUMERID);
    userConsentService.validateConsumerId(consumerId, context);

    Response consentRes = userConsentService.updateConsent(consent, context);

    Response response = new Response();
    Map consentResponse = new HashMap<String, Object>();
    consentResponse.put(JsonKey.USER_ID, userId);
    response.put(JsonKey.CONSENT_BODY, consentResponse);
    response.put(JsonKey.MESSAGE, JsonKey.CONSENT_SUCCESS_MESSAGE);

    sender().tell(response, self());

    logAuditTelemetry(
        (String) consentRes.getResult().get(JsonKey.ID), consent, request.getContext());
  }

  private void logAuditTelemetry(
      String consentId, Map<String, Object> consent, Map<String, Object> contextMap) {
    Map cdata1 = new HashMap<>();
    cdata1.put(JsonKey.ID, consent.get(JsonKey.CONSENT_CONSUMERID));
    cdata1.put(JsonKey.TYPE, JsonKey.CONSUMER);
    Map cdata2 = new HashMap<>();
    cdata2.put(JsonKey.ID, consent.get(JsonKey.CONSENT_OBJECTID));
    cdata2.put(JsonKey.TYPE, JsonKey.CONSENT_OBJECT);

    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    correlatedObject.add(cdata1);
    correlatedObject.add(cdata2);

    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) contextMap.get(JsonKey.CHANNEL));
    TelemetryUtil.addTargetObjectRollUp(rollUp, contextMap);

    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(
            consentId, TelemetryEnvKey.USER_CONSENT, (String) consent.get(JsonKey.STATUS), null);
    TelemetryUtil.telemetryProcessingCall(
        TelemetryEnvKey.EDATA_TYPE_USER_CONSENT,
        consent,
        targetObject,
        correlatedObject,
        contextMap);
  }

  private List<Map<String, Object>> constructConsentResponse(
      List<Map<String, Object>> consentDBList) {
    List<Map<String, Object>> consentResponseList =
        consentDBList
            .stream()
            .filter(
                consents ->
                    !((String) consents.get(JsonKey.STATUS)).equalsIgnoreCase(JsonKey.CONSENT_STATUS_DELETED)
            ).map(
                consent -> {
                  Map<String, Object> consentRes = new HashMap<String, Object>();
                  consentRes.put(JsonKey.ID, consent.get(JsonKey.ID));
                  consentRes.put(JsonKey.USER_ID, consent.get(JsonKey.CONSENT_USER_ID));
                  consentRes.put(
                      JsonKey.CONSENT_CONSUMERID, consent.get(JsonKey.CONSENT_CONSUMER_ID));
                  consentRes.put(JsonKey.CONSENT_OBJECTID, consent.get(JsonKey.CONSENT_OBJECT_ID));
                  consentRes.put(
                      JsonKey.CONSENT_CONSUMERTYPE, consent.get(JsonKey.CONSENT_CONSUMER_TYPE));
                  consentRes.put(
                      JsonKey.CONSENT_OBJECTTYPE, consent.get(JsonKey.CONSENT_OBJECT_TYPE));
                  consentRes.put(JsonKey.STATUS, consent.get(JsonKey.STATUS));
                  consentRes.put(JsonKey.CONSENT_EXPIRY, consent.get(JsonKey.CONSENT_EXPIRY));
                  consentRes.put(JsonKey.CATEGORIES, consent.get(JsonKey.CATEGORIES));
                  consentRes.put(JsonKey.CREATED_ON, consent.get(JsonKey.CONSENT_CREATED_ON));
                  consentRes.put(
                      JsonKey.LAST_UPDATED_ON, consent.get(JsonKey.CONSENT_LAST_UPDATED_ON));
                  return consentRes;
                })
            .collect(Collectors.toList());
    return consentResponseList;
  }
}
