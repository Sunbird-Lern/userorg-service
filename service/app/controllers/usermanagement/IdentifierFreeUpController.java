package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserFreeUpRequestValidator;
<<<<<<< HEAD
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

=======
import play.libs.F.Promise;
import play.mvc.Result;

>>>>>>> upstream/master
/**
 * this action method is responsible to free Up the user account attributes.
 *
 * @author anmolgupta
 */
public class IdentifierFreeUpController extends BaseController {


    /**
     * this action method will be used to free Up user Identifier from user DB
     * @return
     */
<<<<<<< HEAD
    public CompletionStage<Result> freeUpIdentifier(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.FREEUP_USER_IDENTITY.getValue(),
                httpRequest.body().asJson(),
=======
    public Promise<Result> freeUpIdentifier() {
        return handleRequest(
                ActorOperations.FREEUP_USER_IDENTITY.getValue(),
                request().body().asJson(),
>>>>>>> upstream/master
                req -> {
                    Request request = (Request) req;
                    UserFreeUpRequestValidator.getInstance(request).validate();
                    return null;
                },
                null,
                null,
<<<<<<< HEAD
                true,
                httpRequest);
=======
                true);
>>>>>>> upstream/master
    }
}
