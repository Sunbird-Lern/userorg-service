package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

import java.util.Map;

public class UserMergeController extends BaseController {

    public Promise<Result> mergeUser() {
      String authUserToken = ctx().request().getHeader(JsonKey.X_AUTHENTICATED_USER_TOKEN);
      String sourceUserToken = ctx().request().getHeader(JsonKey.X_SOURCE_USER_TOKEN);
        return handleRequest(ActorOperations.MERGE_USER.getValue(),
                request().body().asJson(),
                req -> {
                    Request request = (Request) req;
                    new UserRequestValidator().validateUserMergeRequest(request, authUserToken, sourceUserToken);
                    return null;
                },(Map)request().headers());
    }
}
