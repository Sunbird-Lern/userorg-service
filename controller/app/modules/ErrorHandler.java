package modules;

import com.typesafe.config.Config;
import controllers.BaseController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.response.Response;
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
  private LoggerUtil logger = new LoggerUtil(ErrorHandler.class);

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
    logger.error(
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
    } else if (t instanceof org.apache.pekko.pattern.AskTimeoutException) {
      commonException =
          new ProjectCommonException(
              ResponseCode.serverError,
              ResponseCode.serverError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    } else {
      commonException =
          new ProjectCommonException(
              ResponseCode.serverError,
              ResponseCode.serverError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    response =
        BaseController.createResponseOnException(request.path(), request.method(), commonException);
    return CompletableFuture.completedFuture(Results.internalServerError(Json.toJson(response)));
  }
}
