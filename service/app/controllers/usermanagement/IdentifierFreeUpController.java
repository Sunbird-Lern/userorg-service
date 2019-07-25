package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserFreeUpRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * this action method is responsible to free Up the user account attributes.
 *
 * @author anmolgupta
 */
public class IdentifierFreeUpController extends BaseController {

    public Promise<Result> freeUpIdentifier() {
        return handleRequest(
                ActorOperations.FREEUP_USER.getValue(),
                request().body().asJson(),
                req -> {
                    Request request = (Request) req;
                    UserFreeUpRequestValidator.getInstance(request).validate();
                    return null;
                },
                null,
                null,
                true);
    }
}
