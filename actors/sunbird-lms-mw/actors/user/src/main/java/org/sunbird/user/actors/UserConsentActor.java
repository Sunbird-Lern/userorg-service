package org.sunbird.user.actors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.service.UserConsentService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserConsentServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.DateUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ActorConfig(
        tasks = {"updateUserConsent", "getUserConsent" },
        asyncTasks = {}
)
public class UserConsentActor extends BaseActor {

    private UserService userService = UserServiceImpl.getInstance();
    private UserConsentService userConsentService = UserConsentServiceImpl.getInstance();

    enum CONSENT_CONSUMER_TYPE
    {
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
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault(JsonKey.CONSENT, new HashMap<String, Object>());
        Map<String, Object> filters = (Map<String, Object>) consent.getOrDefault(JsonKey.FILTERS, new HashMap<String, Object>());
        String userId = (String) filters.getOrDefault(JsonKey.USER_ID, "");
        String consumerId = (String) filters.getOrDefault(JsonKey.CONSUMERID, "");
        String objectId = (String) filters.getOrDefault(JsonKey.OBJECTID, "");

        String key = getKey(userId, consumerId, objectId);

        Map<String, Object> consentMap = userConsentService.getConsent(key, request.getRequestContext());

        Response response = new Response();
        if (MapUtils.isNotEmpty(consentMap))
            response.put(JsonKey.CONSENTS, Arrays.asList(consentMap));
        else {
            throw new ProjectCommonException(
                    ResponseCode.userConsentNotFound.getErrorCode(),
                    ResponseCode.userConsentNotFound.getErrorMessage(),
                    ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
        }
        sender().tell(response, self());
    }

    private void updateUserConsent(Request request) {
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault(JsonKey.CONSENT, new HashMap<String, Object>());
        String userId = (String) consent.getOrDefault(JsonKey.USER_ID, "");
        if(StringUtils.isEmpty(userId)){
            userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
        }
        String managedFor = (String)request.getContext().get(JsonKey.MANAGED_FOR);
        if(StringUtils.isNotEmpty(managedFor)){
            userId = managedFor;
        }
        consent.put(JsonKey.USER_ID,userId);
        userService.validateUserId(request, null, request.getRequestContext());

        Map consentReq = createConsentRequest(consent, request.getRequestContext());

        userConsentService.updateConsent(consentReq, request.getRequestContext());

        Response response = new Response();
        Map consentResponse = new HashMap<String, Object>();
        consentResponse.put(JsonKey.USER_ID, userId);
        response.put(JsonKey.CONSENT, consentResponse);
        response.put(JsonKey.MESSAGE, "User Consent updated successfully.");

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

    private Map<String, Object> createConsentRequest(Map<String, Object> consent, RequestContext context){
        String userId = (String) consent.getOrDefault(JsonKey.USER_ID, "");
        String consumerId = (String) consent.getOrDefault(JsonKey.CONSUMERID, "");
        String consumerType = (String) consent.getOrDefault(JsonKey.CONSUMERTYPE, CONSENT_CONSUMER_TYPE.ORGANISATION.toString());
        String objectId = (String) consent.getOrDefault(JsonKey.OBJECTID, "");
        String objectType = (String) consent.getOrDefault(JsonKey.OBJECTTYPE, "");
        String status = (String) consent.getOrDefault(JsonKey.STATUS, "");


        String key = getKey(userId, consumerId, objectId);
        Map<String, Object> consentReq = new HashMap<String, Object>();
        consentReq.put(JsonKey.ID, key);
        consentReq.put(JsonKey.CONSENT_USER_ID, userId);
        consentReq.put(JsonKey.CONSUMER_ID, consumerId);
        consentReq.put(JsonKey.CONSUMER_TYPE, consumerType);
        consentReq.put(JsonKey.OBJECT_ID, objectId);
        consentReq.put(JsonKey.CONSENT_OBJECT_TYPE, objectType);
        consentReq.put(JsonKey.STATUS, status);
        consentReq.put(JsonKey.EXPIRY, new Timestamp(calculateConsentExpiryDate()));

        //Check if consent is already existing
        Map<String, Object> dbCconsentMap = userConsentService.getConsent((String)consentReq.get(JsonKey.ID), context);
        if (MapUtils.isNotEmpty(dbCconsentMap)){
            consentReq.put(JsonKey.CONSENT_LAST_UPDATED_ON, DateUtil.getCurrentDateTimestamp());
        } else {
            consentReq.put(JsonKey.CONSENT_CREATED_ON, DateUtil.getCurrentDateTimestamp());
            consentReq.put(JsonKey.CONSENT_LAST_UPDATED_ON, DateUtil.getCurrentDateTimestamp());
        }

        return consentReq;
    }

    private long calculateConsentExpiryDate(){
        int consentExpiry = Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.CONSENT_EXPIRY_IN_DAYS));
        return DateUtil.addDaysToDate(new Date(), consentExpiry).getTime();
    }
}
