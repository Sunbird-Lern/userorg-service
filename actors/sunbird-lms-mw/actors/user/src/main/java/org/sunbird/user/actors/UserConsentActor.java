package org.sunbird.user.actors;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@ActorConfig(
        tasks = {"updateUserConsent", "getUserConsent" },
        asyncTasks = {}
)
public class UserConsentActor extends BaseActor {

    Map<String, Object> consentCache = new HashMap<String, Object>();

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
        Response response = new Response();
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault("consent", new HashMap<String, Object>());
        Map<String, Object> filters = (Map<String, Object>) consent.getOrDefault("filters", new HashMap<String, Object>());
        String userId = (String) filters.getOrDefault("userId", "");
        String consumerId = (String) filters.getOrDefault("consumerId", "");
        String objectId = (String) filters.getOrDefault("objectId", "");
        String key = getKey(userId, consumerId, objectId);
        Object cacheData = consentCache.get(key);
        if (cacheData != null)
            response.put("consents", Arrays.asList(cacheData));
        else {
            throw new ProjectCommonException(
                    ResponseCode.userConsentNotFound.getErrorCode(),
                    ResponseCode.userConsentNotFound.getErrorMessage(),
                    ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
        }
        sender().tell(response, self());
    }

    private void updateUserConsent(Request request) {
        Response response = new Response();
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault("consent", new HashMap<String, Object>());
        String userId = (String) consent.getOrDefault("userId", "");
        String consumerId = (String) consent.getOrDefault("consumerId", "");
        String objectId = (String) consent.getOrDefault("objectId", "");
        consentCache.put(getKey(userId, consumerId, objectId), consent);
        response.put("consent", new HashMap<String, Object>() {{ put("userId", userId);}});
        response.put("message", "User Consent updated successfully.");
        sender().tell(response, self());
    }

    private String getKey (String userId, String consumerId, String objectId) {
        return "usr-consent:" + userId + ":" + consumerId + ":" + objectId;
    }

}
