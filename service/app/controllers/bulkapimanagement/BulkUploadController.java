package controllers.bulkapimanagement;

import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the request related to bulk api's for user management.
 *
 * @author Amit Kumar
 */
public class BulkUploadController extends BaseBulkUploadController {

  BaseRequestValidator baseRequestValidator = new BaseRequestValidator();

  /**
   * This method will allow to upload bulk user.
   *
   * @return Promise<Result>
   */
  public Promise<Result> uploadUser() {

    try {
      Request request =
          createAndInitBulkRequest(ActorOperations.BULK_UPLOAD.getValue(), JsonKey.USER, false);
      RequestValidator.validateUploadUser(
          (Map<String, Object>) request.getRequest().get(JsonKey.DATA));
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide the status of bulk operation by their processId.
   *
   * @param processId Stirng
   * @return Promise<Result>
   */
  public Promise<Result> getUploadStatus(String processId) {
    try {
      ProjectLogger.log("get bulk operation status =" + processId, LoggerEnum.INFO.name());
      Request request = createAndInitRequest(ActorOperations.GET_BULK_OP_STATUS.getValue());
      request.getRequest().put(JsonKey.PROCESS_ID, processId);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /*
   * This method will allow to upload bulk organisation.
   *
   * @return Promise<Result>
   */
  public Promise<Result> uploadOrg() {

    try {
      Request request =
          createAndInitBulkRequest(
              ActorOperations.BULK_UPLOAD.getValue(), JsonKey.ORGANISATION, false);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow to upload bulk user.
   *
   * @return Promise<Result>
   */
  public Promise<Result> bulkBatchEnrollment() {

    try {
      Request request =
          createAndInitBulkRequest(ActorOperations.BULK_UPLOAD.getValue(), JsonKey.BATCH, false);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
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

  /*
   * This method will allow to upload bulk location.
   *
   * @return Promise<Result>
   */
  public Promise<Result> uploadLocation() {

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
}
