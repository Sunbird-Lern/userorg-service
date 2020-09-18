package org.sunbird.user.actors;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;
import org.sunbird.models.UserConsent;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.service.UserConsentService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserConsentServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.DateUtil;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ActorConfig(
        tasks = {"updateUserConsent", "getUserConsent" },
        asyncTasks = {}
)
public class UserConsentActor extends BaseActor {

    Map<String, Object> consentCache = new HashMap<String, Object>();
    private UserService userService = UserServiceImpl.getInstance();
    private UserConsentService userConsentService = UserConsentServiceImpl.getInstance();

    enum CONSENT_CONSUMER_TYPE
    {
        ORGANISATION;
    }
    @Override
    public void onReceive(Request request) throws Throwable {
        Util.initializeContext(request, TelemetryEnvKey.USER);
        RequestContext context = request.getRequestContext();
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
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault("consent", new HashMap<String, Object>());
        Map<String, Object> filters = (Map<String, Object>) consent.getOrDefault("filters", new HashMap<String, Object>());
        String userId = (String) filters.getOrDefault("userId", "");
        String consumerId = (String) filters.getOrDefault("consumerId", "");
        String objectId = (String) filters.getOrDefault("objectId", "");

        String key = getKey(userId, consumerId, objectId);
        //Object cacheData = consentCache.get(key);

        UserConsent consentObj = userConsentService.getConsent(key, request.getRequestContext());

        Response response = new Response();
        if (consentObj != null)
            response.put("consents", Arrays.asList(consentObj));
        else {
            throw new ProjectCommonException(
                    ResponseCode.userConsentNotFound.getErrorCode(),
                    ResponseCode.userConsentNotFound.getErrorMessage(),
                    ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
        }
        sender().tell(response, self());
    }

    private void updateUserConsent(Request request) {
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault("consent", new HashMap<String, Object>());
        String userId = (String) consent.getOrDefault("userId", "");
        if(StringUtils.isEmpty(userId)){
            userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
        }
        String managedFor = (String)request.getContext().get(JsonKey.MANAGED_FOR);
        if(StringUtils.isNotEmpty(managedFor)){
            userId = managedFor;
        }
        userService.validateUserId(request, null, request.getRequestContext());

        userConsentService.updateConsent(createConsentRequestObj(consent), request.getRequestContext());

        Response response = new Response();
        Map consentMap = new HashMap<String, Object>();
        consentMap.put("userId", userId);
        response.put("consent", consentMap);
        response.put("message", "User Consent updated successfully.");

        sender().tell(response, self());

        Map<String, Object> targetObject = null;
        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        targetObject =
                TelemetryUtil.generateTargetObject(
                        userId, TelemetryEnvKey.USER, JsonKey.UPDATE, null);
        TelemetryUtil.telemetryProcessingCall(
                consent, targetObject, correlatedObject, request.getContext());
    }

    private String getKey (String userId, String consumerId, String objectId) {
        return "usr-consent:" + userId + ":" + consumerId + ":" + objectId;
    }

    private UserConsent createConsentRequestObj(Map<String, Object> consent){
        String userId = (String) consent.getOrDefault("userId", "");
        String consumerId = (String) consent.getOrDefault("consumerId", "");
        String consumerType = (String) consent.getOrDefault("consumerType", CONSENT_CONSUMER_TYPE.ORGANISATION.toString());
        String objectId = (String) consent.getOrDefault("objectId", "");
        String objectType = (String) consent.getOrDefault("objectType", "");
        String status = (String) consent.getOrDefault("status", "");

        String key = getKey(userId, consumerId, objectId);
        //consentCache.put(key, consent);

        UserConsent consentObj = new UserConsent();
        consentObj.setId(key);
        consentObj.setUserId(userId);
        consentObj.setConsumerId(consumerId);
        consentObj.setConsumerType(consumerType);
        consentObj.setObjectId(objectId);
        consentObj.setObjectType(objectType);
        consentObj.setStatus(status);
        consentObj.setCreatedOn(DateUtil.getCurrentDateTimestamp());
        consentObj.setLastUpdatedOn(DateUtil.getCurrentDateTimestamp());
        consentObj.setExpiry(new Timestamp(calculateConsentExpiryDate()));
        return consentObj;
    }

    private long calculateConsentExpiryDate(){
        int consentExpiry = Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.CONSENT_EXPIRY_IN_DAYS));
        return DateUtil.addDaysToDate(new Date(), consentExpiry).getTime();
    }
}
