package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.AssignKeyValidator;
import play.libs.F;
import play.mvc.Result;


/**
 * this Class is responsible for managing the signIn and enc keys for organisation
 */
public class KeyManagementController extends BaseController {


    /**
     * this action method will validate and save the enc and signIn keys into organisation db.
     * @return Result
     */
    public F.Promise<Result> assignKeys() {
        return handleRequest(
                ActorOperations.ASSIGN_KEYS.getValue(),
                request().body().asJson(),
                orgRequest -> {
                    AssignKeyValidator.getInstance((Request) orgRequest).validate();
                    return null;
                },
                null, null, true);
    }
}
