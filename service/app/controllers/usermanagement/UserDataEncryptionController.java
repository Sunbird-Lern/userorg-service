package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserDataEncryptionRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserDataEncryptionController extends BaseController {

  public Promise<Result> encrypt() {
    return handleRequest(
        ActorOperations.ENCRYPT_USER_DATA.getValue(),
        request().body().asJson(),
        (request) -> {
          new UserDataEncryptionRequestValidator().validateEncryptRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> decrypt() {
    return handleRequest(
        ActorOperations.DECRYPT_USER_DATA.getValue(),
        request().body().asJson(),
        (request) -> {
          new UserDataEncryptionRequestValidator().validateDecryptRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }
}
