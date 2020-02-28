package controllers.bulkapimanagement;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class BulkUploadController extends BaseBulkUploadController {

  BaseRequestValidator baseRequestValidator = new BaseRequestValidator();

  public CompletionStage<Result> userBulkUpload(Http.Request httpRequest) {
    try {
      Request request =
              createAndInitBulkRequest(
                      BulkUploadActorOperation.USER_BULK_UPLOAD.getValue(), JsonKey.USER, true, httpRequest);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> userBulkMigrate(Http.Request httpRequest) {
    try {
      Request request =
              createAndInitBulkRequest(
                      BulkUploadActorOperation.USER_BULK_MIGRATION.getValue(), JsonKey.USER, true, httpRequest);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> orgBulkUpload(Http.Request httpRequest) {
    try {
      Request request =
              createAndInitBulkRequest(
                      BulkUploadActorOperation.ORG_BULK_UPLOAD.getValue(), JsonKey.ORGANISATION, true, httpRequest);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }


  public CompletionStage<Result> locationBulkUpload(Http.Request httpRequest) {
    try {
      Request request =
              createAndInitBulkRequest(
                      BulkUploadActorOperation.LOCATION_BULK_UPLOAD.getValue(), JsonKey.LOCATION, true, httpRequest);
      baseRequestValidator.checkMandatoryFieldsPresent(
              (Map<String, Object>) request.getRequest().get(JsonKey.DATA),
              GeoLocationJsonKey.LOCATION_TYPE);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> getUploadStatus(String processId, Http.Request httpRequest) {
    return handleRequest(
            ActorOperations.GET_BULK_OP_STATUS.getValue(),
            null,
            null,
            processId,
            JsonKey.PROCESS_ID,
            false,
            httpRequest);
  }

  public CompletionStage<Result> getStatusDownloadLink(String processId, Http.Request httpRequest) {
    return handleRequest(
            ActorOperations.GET_BULK_UPLOAD_STATUS_DOWNLOAD_LINK.getValue(),
            null,
            null,
            processId,
            JsonKey.PROCESS_ID,
            false,
            httpRequest);
  }
}
