package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.ResetPasswordRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

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
	public Promise<Result> resetPassword() {
	    return handleRequest(
	        ActorOperations.RESET_PASSWORD.getValue(),
	        request().body().asJson(),
	        (request) -> {
	          new ResetPasswordRequestValidator().validateResetPasswordRequest((Request) request);
	          return null;
	        },
	        getAllRequestHeaders(request()));
	  }
}
