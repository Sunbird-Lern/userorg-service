package controllers.certmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.request.certificatevalidator.CertAddRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * This Controller is used for user-certificate related purposes
 */
public class CertificateController extends BaseController {

  /** This method helps in validating the access code and retrieves the certificate details
   * @return json and pdf links as response
   */
   public CompletionStage<Result> validateCertificate(Http.Request httpRequest) {
     return handleRequest(ActorOperations.VALIDATE_CERTIFICATE.getValue(),httpRequest.body().asJson(),
             req -> {
               Request request = (Request) req;
               new UserRequestValidator().validateCertValidationRequest(request);
               return null;
             }, 
             httpRequest);
   }

   public   CompletionStage<Result> addCertificate(Http.Request httpRequest){
       return handleRequest(ActorOperations.ADD_CERTIFICATE.getValue(),httpRequest.body().asJson(),req->{
           Request request=(Request)req;
           CertAddRequestValidator.getInstance(request).validate();
           return null;
       },null,
               null,
               true,
               httpRequest);
   }
   
	public CompletionStage<Result> getSignUrl(Http.Request httpRequest) {
		return handleRequest(ActorOperations.GET_SIGN_URL.getValue(), httpRequest.body().asJson(), req -> {
			Request request = (Request) req;
			CertAddRequestValidator.getInstance(request).validateDownlaodFileData();
			return null;
		}, null, null, true,
            httpRequest);
	}

   public CompletionStage<Result> mergeCertificate(Http.Request httpRequest) {
     return handleRequest(ActorOperations.MERGE_USER_CERTIFICATE.getValue(),httpRequest.body().asJson(), httpRequest);
   }
}
