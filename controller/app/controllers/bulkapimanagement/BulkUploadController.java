package controllers.bulkapimanagement;

import org.apache.pekko.actor.ActorRef;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.BaseRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

public class BulkUploadController extends BaseBulkUploadController {
  @Inject
  @Named("user_bulk_upload_actor")
  private ActorRef userBulkUploadActor;

  @Inject
  @Named("org_bulk_upload_actor")
  private ActorRef orgBulkUploadActor;

  @Inject
  @Named("location_bulk_upload_actor")
  private ActorRef locationBulkUploadActor;

  @Inject
  @Named("bulk_upload_management_actor")
  private ActorRef bulkUploadManagementActor;

  private BaseRequestValidator baseRequestValidator = new BaseRequestValidator();

  public CompletionStage<Result> userBulkUpload(Http.Request httpRequest) {
    Request request = new Request();
    try {
      request =
          createAndInitBulkRequest(
              ActorOperations.USER_BULK_UPLOAD.getValue(), JsonKey.USER, true, httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      return actorResponseHandler(userBulkUploadActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  public CompletionStage<Result> orgBulkUpload(Http.Request httpRequest) {
    Request request = new Request();
    try {
      request =
          createAndInitBulkRequest(
              ActorOperations.ORG_BULK_UPLOAD.getValue(), JsonKey.ORGANISATION, true, httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      return actorResponseHandler(orgBulkUploadActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  public CompletionStage<Result> locationBulkUpload(Http.Request httpRequest) {
    Request request = new Request();
    try {
      request =
          createAndInitBulkRequest(
              ActorOperations.LOCATION_BULK_UPLOAD.getValue(), JsonKey.LOCATION, true, httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      baseRequestValidator.checkMandatoryFieldsPresent(
          (Map<String, Object>) request.getRequest().get(JsonKey.DATA), JsonKey.LOCATION_TYPE);
      return actorResponseHandler(locationBulkUploadActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  public CompletionStage<Result> getUploadStatus(String processId, Http.Request httpRequest) {
    return handleRequest(
        bulkUploadManagementActor,
        ActorOperations.GET_BULK_OP_STATUS.getValue(),
        null,
        null,
        processId,
        JsonKey.PROCESS_ID,
        false,
        httpRequest);
  }
}
