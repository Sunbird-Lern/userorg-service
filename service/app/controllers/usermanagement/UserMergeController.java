package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
<<<<<<< HEAD
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UserMergeController extends BaseController {

    public CompletionStage<Result> mergeUser(Http.Request httpRequest) {
      Optional<String> authUserToken = httpRequest.getHeaders().get(JsonKey.X_AUTHENTICATED_USER_TOKEN);
      Optional<String> sourceUserToken = httpRequest.getHeaders().get(JsonKey.X_SOURCE_USER_TOKEN);
        return handleRequest(ActorOperations.MERGE_USER.getValue(),
                httpRequest.body().asJson(),
                req -> {
                    Request request = (Request) req;
                    new UserRequestValidator().validateUserMergeRequest(request, authUserToken.get(), sourceUserToken.get());
                    return null;
                },
                getAllRequestHeaders(httpRequest),
                httpRequest);
=======
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
>>>>>>> upstream/master
    }
}
