package controllers.bulkapimanagement;

import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class BulkUploadController extends BaseBulkUploadController {

  BaseRequestValidator baseRequestValidator = new BaseRequestValidator();

  public Promise<Result> userBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.USER_BULK_UPLOAD.getValue(), JsonKey.USER, false);
      Map<String, Object> reqObj = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
      RequestValidator.validateUploadUser(reqObj);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgBulkUpload() {
    try {
      Request request =
          createAndInitBulkRequest(
              BulkUploadActorOperation.ORG_BULK_UPLOAD.getValue(), JsonKey.ORGANISATION, false);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> batchEnrolmentBulkUpload() {=
    try {
      Request request =
          createAndInitBulkRequest(ActorOperations.BULK_UPLOAD.getValue(), JsonKey.BATCH, false);
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

  public Promise<Result> userDataEncryption() {
    try {
      Request request = createAndInitRequest(ActorOperations.ENCRYPT_USER_DATA.getValue());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> userDataDecryption() {
    try {
      Request request = createAndInitRequest(ActorOperations.DECRYPT_USER_DATA.getValue());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
