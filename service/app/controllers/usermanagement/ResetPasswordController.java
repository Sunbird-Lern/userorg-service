package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.ResetPasswordRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
<<<<<<< HEAD
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

=======
import play.libs.F.Promise;
import play.mvc.Result;

>>>>>>> upstream/master
/**
 * 
 * This controller contains method for reset the user password.
 *
 */
public class ResetPasswordController extends BaseController{

	/**
	 * This method will reset the password for given userId in request.
	 * @return Promise
	 */
<<<<<<< HEAD
	public CompletionStage<Result> resetPassword(Http.Request httpRequest) {
	    return handleRequest(
	        ActorOperations.RESET_PASSWORD.getValue(),
	        httpRequest.body().asJson(),
=======
	public Promise<Result> resetPassword() {
	    return handleRequest(
	        ActorOperations.RESET_PASSWORD.getValue(),
	        request().body().asJson(),
>>>>>>> upstream/master
	        (request) -> {
	          new ResetPasswordRequestValidator().validateResetPasswordRequest((Request) request);
	          return null;
	        },
<<<<<<< HEAD
	        getAllRequestHeaders(httpRequest), 
							httpRequest);
=======
	        getAllRequestHeaders(request()));
>>>>>>> upstream/master
	  }
}
