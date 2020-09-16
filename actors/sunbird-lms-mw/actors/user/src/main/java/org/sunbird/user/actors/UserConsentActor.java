package org.sunbird.user.actors;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.learner.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@ActorConfig(
        tasks = {"updateUserConsent", "getUserConsent" },
        asyncTasks = {}
)
public class UserConsentActor extends BaseActor {

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
        response.put("consents", new ArrayList<>());
        sender().tell(response, self());
    }

    private void updateUserConsent(Request request) {
        Response response = new Response();
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault("consent", new HashMap<String, Object>());
        String userId = (String) consent.getOrDefault("userId", "");
        response.put("consent", new HashMap<String, Object>() {{ put("userId", userId);}});
        response.put("message", "User Consent updated successfully.");
        sender().tell(response, self());
    }

}
