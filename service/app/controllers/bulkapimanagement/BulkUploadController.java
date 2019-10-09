package controllers.bulkapimanagement;

import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class BulkUploadController extends BaseBulkUploadController {

  BaseRequestValidator baseRequestValidator = new BaseRequestValidator();

  public Promise<Result> userBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.USER_BULK_UPLOAD.getValue(), JsonKey.USER, true);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> userBulkMigrate() {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.USER_BULK_MIGRATION.getValue(), JsonKey.USER, true);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.ORG_BULK_UPLOAD.getValue(), JsonKey.ORGANISATION, true);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }


  public Promise<Result> locationBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.LOCATION_BULK_UPLOAD.getValue(), JsonKey.LOCATION, true);
      baseRequestValidator.checkMandatoryFieldsPresent(
          (Map<String, Object>) request.getRequest().get(JsonKey.DATA),
          GeoLocationJsonKey.LOCATION_TYPE);
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
