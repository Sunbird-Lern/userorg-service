package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserDataEncryptionRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class UserDataEncryptionController extends BaseController {

  public CompletionStage<Result> encrypt(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.ENCRYPT_USER_DATA.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new UserDataEncryptionRequestValidator().validateEncryptRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> decrypt(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.DECRYPT_USER_DATA.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new UserDataEncryptionRequestValidator().validateDecryptRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }
}
