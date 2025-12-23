package controllers.systemsettings;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.systemsettings.SystemSettingsRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

public class SystemSettingsController extends BaseController {

  @Inject
  @Named("system_settings_actor")
  private ActorRef systemSettingsActor;

  private static final SystemSettingsRequestValidator systemSettingsRequestValidator =
      new SystemSettingsRequestValidator();

  @SuppressWarnings("unchecked")
  public CompletionStage<Result> getSystemSetting(String field, Http.Request httpRequest) {
    try {
      return handleRequest(
          systemSettingsActor,
          ActorOperations.GET_SYSTEM_SETTING.getValue(),
          field,
          JsonKey.FIELD,
          httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(
                  ActorOperations.GET_SYSTEM_SETTING.getValue()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  @SuppressWarnings("unchecked")
  public CompletionStage<Result> setSystemSetting(Http.Request httpRequest) {
    try {
      return handleRequest(
          systemSettingsActor,
          ActorOperations.SET_SYSTEM_SETTING.getValue(),
          httpRequest.body().asJson(),
          (request) -> {
            systemSettingsRequestValidator.validateSetSystemSetting((Request) request);
            return null;
          },
          httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(
                  ActorOperations.SET_SYSTEM_SETTING.getValue()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  @SuppressWarnings("unchecked")
  public CompletionStage<Result> getAllSystemSettings(Http.Request httpRequest) {
    try {
      return handleRequest(
          systemSettingsActor, ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue(), httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(
                  ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }
}
