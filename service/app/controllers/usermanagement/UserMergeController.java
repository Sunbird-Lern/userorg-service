package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserMergeController extends BaseController {

    public Promise<Result> mergeUser() {
        return handleRequest(ActorOperations.MERGE_USER.getValue(),
                request().body().asJson(),
                req -> {
                    Request request = (Request) req;
                    new UserRequestValidator().validateUserMergeRequest(request);
                    return null;
                });
    }
}
