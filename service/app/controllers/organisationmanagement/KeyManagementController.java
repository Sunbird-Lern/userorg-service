package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.KeyManagementValidator;
<<<<<<< HEAD
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

=======
import play.libs.F;
import play.mvc.Result;

>>>>>>> upstream/master

/**
 * this Class is responsible for managing the signIn and enc keys for organisation
 */
public class KeyManagementController extends BaseController {


    /**
     * this action method will validate and save the enc and signIn keys into organisation db.
     * @return Result
     */
<<<<<<< HEAD
    public CompletionStage<Result> assignKeys(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.ASSIGN_KEYS.getValue(),
                httpRequest.body().asJson(),
=======
    public F.Promise<Result> assignKeys() {
        return handleRequest(
                ActorOperations.ASSIGN_KEYS.getValue(),
                request().body().asJson(),
>>>>>>> upstream/master
                orgRequest -> {
                    KeyManagementValidator.getInstance((Request) orgRequest).validate();
                    return null;
                },
<<<<<<< HEAD
                null, null, true,
                httpRequest);
=======
                null, null, true);
>>>>>>> upstream/master
    }
}
