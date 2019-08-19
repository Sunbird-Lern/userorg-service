package controllers.certmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.request.certificatevalidator.CertAddRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This Controller is used for user-certificate related purposes
 */
public class CertificateController extends BaseController {

  /** This method helps in validating the access code and retrieves the certificate details
   * @return json and pdf links as response
   */
   public Promise<Result> validateCertificate() {
     return handleRequest(ActorOperations.VALIDATE_CERTIFICATE.getValue(),request().body().asJson(),
             req -> {
               Request request = (Request) req;
               new UserRequestValidator().validateCertValidationRequest(request);
               return null;
             });
   }

   public   Promise<Result> addCertificate(){
       return handleRequest(ActorOperations.ADD_CERTIFICATE.getValue(),request().body().asJson(),req->{
           Request request=(Request)req;
           CertAddRequestValidator.getInstance(request).validate();
           return null;
       },null,
               null,
               true);
   }
   
	public Promise<Result> getSignUrl() {
		return handleRequest(ActorOperations.GET_SIGN_URL.getValue(), request().body().asJson(), req -> {
			Request request = (Request) req;
			CertAddRequestValidator.getInstance(request).validateDownlaodFileData();
			return null;
		}, null, null, true);
	}

   public Promise<Result> mergeCertificate() {
     return handleRequest(ActorOperations.MERGE_USER_CERTIFICATE.getValue(),request().body().asJson());
   }
}
