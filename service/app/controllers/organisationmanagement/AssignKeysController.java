package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.AssignKeyValidator;
import play.libs.F;
import play.mvc.Result;

public class AssignKeysController extends BaseController {


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
