package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class UserConsentController extends BaseController {

    public CompletionStage<Result> updateUserConsent(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.UPDATE_USER_CONSENT.getValue(),
                httpRequest.body().asJson(),
                req -> {
                    Request request = (Request) req;
                    return null;
                },
                null,
                null,
                true,
                httpRequest);
    }

    public CompletionStage<Result> getUserConsent(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.GET_USER_CONSENT.getValue(),
                httpRequest.body().asJson(),
                req -> {
                    Request request = (Request) req;
                    return null;
                },
                null,
                null,
                true,
                httpRequest);
    }
}
