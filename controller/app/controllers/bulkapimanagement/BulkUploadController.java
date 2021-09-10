package controllers.bulkapimanagement;

import akka.actor.ActorRef;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.operations.BulkUploadActorOperation;
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
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.USER_BULK_UPLOAD.getValue(),
              JsonKey.USER,
              true,
              httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      return actorResponseHandler(userBulkUploadActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> orgBulkUpload(Http.Request httpRequest) {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.ORG_BULK_UPLOAD.getValue(),
              JsonKey.ORGANISATION,
              true,
              httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      return actorResponseHandler(orgBulkUploadActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> locationBulkUpload(Http.Request httpRequest) {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.LOCATION_BULK_UPLOAD.getValue(),
              JsonKey.LOCATION,
              true,
              httpRequest);
      setContextAndPrintEntryLog(httpRequest, request);
      baseRequestValidator.checkMandatoryFieldsPresent(
          (Map<String, Object>) request.getRequest().get(JsonKey.DATA), JsonKey.LOCATION_TYPE);
      return actorResponseHandler(locationBulkUploadActor, request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
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
