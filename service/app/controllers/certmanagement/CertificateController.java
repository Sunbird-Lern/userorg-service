package controllers.certmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import play.libs.F;
import play.mvc.Result;

public class CertificateController extends BaseController {

   public F.Promise<Result> validateCertificate() {
     return handleRequest(ActorOperations.VALIDATE_CERTIFICATE.getValue(),request().body().asJson(),
             req -> {
               Request request = (Request) req;
               new UserRequestValidator().validateCertValidationRequest(request);
               return null;
             });
   }
}
