package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.validator.orgvalidator.KeyManagementValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;


/**
 * this Class is responsible for managing the signIn and enc keys for organisation
 */
public class KeyManagementController extends BaseController {


    /**
     * this action method will validate and save the enc and signIn keys into organisation db.
     * @return Result
     */
    public CompletionStage<Result> assignKeys(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.ASSIGN_KEYS.getValue(),
                httpRequest.body().asJson(),
                orgRequest -> {
                    KeyManagementValidator.getInstance((Request) orgRequest).validate();
                    return null;
                },
                null, null, true,
                httpRequest);
    }
}
