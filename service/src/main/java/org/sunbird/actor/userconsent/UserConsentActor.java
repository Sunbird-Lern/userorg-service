package org.sunbird.actor.userconsent;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.Util;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.service.userconsent.UserConsentService;
import org.sunbird.service.userconsent.impl.UserConsentServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.user.DateUtil;

@ActorConfig(
  tasks = {"updateUserConsent", "getUserConsent"},
  asyncTasks = {}
)
public class UserConsentActor extends BaseActor {

  private UserService userService = UserServiceImpl.getInstance();
  private UserConsentService userConsentService = UserConsentServiceImpl.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private OrgDao orgDao = OrgDaoImpl.getInstance();

  enum CONSENT_CONSUMER_TYPE {
    ORGANISATION;
  }

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
        onReceiveUnsupportedOperation("UserConsentActor");
        break;
    }
  }

  private void getUserConsent(Request request) {
    Map<String, Object> consent =
        (Map<String, Object>)
            request.getRequest().getOrDefault(JsonKey.CONSENT_BODY, new HashMap<String, Object>());
    Map<String, Object> filters =
        (Map<String, Object>) consent.getOrDefault(JsonKey.FILTERS, new HashMap<String, Object>());

    Map<String, Object> getConsentFromDBReq = new HashMap<String, Object>();
    getConsentFromDBReq.put(JsonKey.CONSENT_USER_ID, (String) filters.get(JsonKey.USER_ID));
    getConsentFromDBReq.put(
        JsonKey.CONSENT_CONSUMER_ID, (String) filters.get(JsonKey.CONSENT_CONSUMERID));
    getConsentFromDBReq.put(
        JsonKey.CONSENT_OBJECT_ID, (String) filters.get(JsonKey.CONSENT_OBJECTID));

    List<Map<String, Object>> consentList =
        userConsentService.getConsent(getConsentFromDBReq, request.getRequestContext());

    Response response = new Response();
    if (CollectionUtils.isNotEmpty(consentList)) {
      List<Map<String, Object>> consentResponseList =
          constructConsentResponse(consentList, request.getRequestContext());
      response.put(JsonKey.CONSENT_RESPONSE, consentResponseList);
    } else {
      throw new ProjectCommonException(
          ResponseCode.userConsentNotFound.getErrorCode(),
          ResponseCode.userConsentNotFound.getErrorMessage(),
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

    Map consentReq = constructConsentRequest(consent, context);
    userConsentService.updateConsent(consentReq, context);

    Response response = new Response();
    Map consentResponse = new HashMap<String, Object>();
    consentResponse.put(JsonKey.USER_ID, userId);
    response.put(JsonKey.CONSENT_BODY, consentResponse);
    response.put(JsonKey.MESSAGE, JsonKey.CONSENT_SUCCESS_MESSAGE);

    sender().tell(response, self());

    logAuditTelemetry((String) consentReq.get(JsonKey.ID), consent, request.getContext());
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

  private String getKey(String userId, String consumerId, String objectId) {
    return "usr-consent:" + userId + ":" + consumerId + ":" + objectId;
  }

  private Map<String, Object> constructConsentRequest(
      Map<String, Object> consent, RequestContext context) {
    String userId = (String) consent.get(JsonKey.USER_ID);
    String consumerId = (String) consent.get(JsonKey.CONSENT_CONSUMERID);
    String consumerType =
        (String)
            consent.getOrDefault(
                JsonKey.CONSENT_CONSUMERTYPE, CONSENT_CONSUMER_TYPE.ORGANISATION.toString());
    String objectId = (String) consent.get(JsonKey.CONSENT_OBJECTID);
    String objectType = (String) consent.getOrDefault(JsonKey.CONSENT_OBJECTTYPE, "");
    String status = (String) consent.get(JsonKey.STATUS);
    validateConsumerId(consumerId, context);

    String key = getKey(userId, consumerId, objectId);
    Map<String, Object> consentReq = new HashMap<String, Object>();
    consentReq.put(JsonKey.ID, key);
    consentReq.put(JsonKey.CONSENT_USER_ID, userId);
    consentReq.put(JsonKey.CONSENT_CONSUMER_ID, consumerId);
    consentReq.put(JsonKey.CONSENT_OBJECT_ID, objectId);
    consentReq.put(JsonKey.CONSENT_CONSUMER_TYPE, consumerType);
    consentReq.put(JsonKey.CONSENT_OBJECT_TYPE, objectType);
    consentReq.put(JsonKey.STATUS, status);
    consentReq.put(JsonKey.CONSENT_EXPIRY, new Timestamp(calculateConsentExpiryDate()));

    Map<String, Object> getConsentFromDBReq = new HashMap<String, Object>();
    getConsentFromDBReq.put(JsonKey.CONSENT_USER_ID, userId);
    getConsentFromDBReq.put(JsonKey.CONSENT_CONSUMER_ID, consumerId);
    getConsentFromDBReq.put(JsonKey.CONSENT_OBJECT_ID, objectId);
    // Check if consent is already existing
    List<Map<String, Object>> dbCconsentMap =
        userConsentService.getConsent(getConsentFromDBReq, context);
    if (CollectionUtils.isNotEmpty(dbCconsentMap) && MapUtils.isNotEmpty(dbCconsentMap.get(0))) {
      consentReq.put(JsonKey.CONSENT_LAST_UPDATED_ON, DateUtil.getCurrentDateTimestamp());
    } else {
      consentReq.put(JsonKey.CONSENT_CREATED_ON, DateUtil.getCurrentDateTimestamp());
      consentReq.put(JsonKey.CONSENT_LAST_UPDATED_ON, DateUtil.getCurrentDateTimestamp());
    }

    return consentReq;
  }

  private void validateConsumerId(String consumerId, RequestContext context) {
    Map<String, Object> org = null;
    try {
      org = orgDao.getOrgById(consumerId, context);
    } catch (Exception ex) {
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    if (MapUtils.isEmpty(org)) {
      throw new ProjectCommonException(
          ResponseCode.invalidOrgId.getErrorCode(),
          ResponseCode.invalidOrgId.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }

  private List<Map<String, Object>> constructConsentResponse(
      List<Map<String, Object>> consentDBList, RequestContext context) {
    List<Map<String, Object>> consentResponseList =
        consentDBList
            .stream()
            .map(
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

  private long calculateConsentExpiryDate() {
    int consentExpiry =
        Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.CONSENT_EXPIRY_IN_DAYS));
    return DateUtil.addDaysToDate(new Date(), consentExpiry).getTime();
  }
}
