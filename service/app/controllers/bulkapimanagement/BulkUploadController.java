package controllers.bulkapimanagement;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class BulkUploadController extends BaseBulkUploadController {

  BaseRequestValidator baseRequestValidator = new BaseRequestValidator();

  public Promise<Result> batchEnrollmentBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              ActorOperations.BULK_UPLOAD.getValue(), JsonKey.BATCH_LEARNER_ENROL, false);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> batchUnEnrollmentBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              ActorOperations.BULK_UPLOAD.getValue(), JsonKey.BATCH_LEARNER_UNENROL, false);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getUploadStatus(String processId) {
    return handleRequest(
        ActorOperations.GET_BULK_OP_STATUS.getValue(),
        null,
        null,
        processId,
        JsonKey.PROCESS_ID,
        false);
  }

  public Promise<Result> getStatusDownloadLink(String processId) {
    return handleRequest(
        ActorOperations.GET_BULK_UPLOAD_STATUS_DOWNLOAD_LINK.getValue(),
        null,
        null,
        processId,
        JsonKey.PROCESS_ID,
        false);
  }
}
