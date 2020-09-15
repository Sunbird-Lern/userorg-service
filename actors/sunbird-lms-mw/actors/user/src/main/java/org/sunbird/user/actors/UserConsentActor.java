package org.sunbird.user.actors;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.learner.util.Util;


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

    private Response getUserConsent(Request request) {
        Response response = new Response();
        return response;
    }

    private Response updateUserConsent(Request request) {
        Response response = new Response();
        return response;
    }

}
