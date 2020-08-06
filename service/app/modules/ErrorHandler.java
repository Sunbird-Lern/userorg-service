package modules;

import com.typesafe.config.Config;
import controllers.BaseController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

  @Inject
  public ErrorHandler(
      Config config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes) {
    super(config, environment, sourceMapper, routes);
  }

  @Override
  public CompletionStage<Result> onServerError(Http.RequestHeader request, Throwable t) {
    ProjectLogger.log(
        "Global: onError called for path = "
            + request.path()
            + ", headers = "
            + request.getHeaders().toMap(),
        t);
    Response response = null;
    ProjectCommonException commonException = null;
    if (t instanceof ProjectCommonException) {
      commonException = (ProjectCommonException) t;
      response =
          BaseController.createResponseOnException(
              request.path(), request.method(), (ProjectCommonException) t);
    } else if (t instanceof akka.pattern.AskTimeoutException) {
      commonException =
          new ProjectCommonException(
              ResponseCode.actorConnectionError.getErrorCode(),
              ResponseCode.actorConnectionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    } else {
      commonException =
          new ProjectCommonException(
              ResponseCode.internalError.getErrorCode(),
              ResponseCode.internalError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    response =
        BaseController.createResponseOnException(request.path(), request.method(), commonException);
    return CompletableFuture.completedFuture(Results.internalServerError(Json.toJson(response)));
  }
}
