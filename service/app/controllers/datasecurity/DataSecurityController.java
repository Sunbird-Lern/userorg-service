package controllers.datasecurity;

import controllers.BaseController;
import controllers.datasecurity.validator.DataSecurityRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class DataSecurityController extends BaseController {

  public Promise<Result> userDataEncryption() {
    return handleRequest(
        ActorOperations.ENCRYPT_USER_DATA.getValue(),
        request().body().asJson(),
        (request) -> {
          new DataSecurityRequestValidator().validateEncryptRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> userDataDecryption() {
    return handleRequest(
        ActorOperations.DECRYPT_USER_DATA.getValue(),
        request().body().asJson(),
        (request) -> {
          new DataSecurityRequestValidator().validateDecryptRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }
}
