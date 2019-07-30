package controllers.usermanagement.validator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * 
 *This class will validate the reset password request
 *
 */
public class ResetPasswordRequestValidator extends BaseRequestValidator {
	/**
	 * This method will validate the mandatory param in the request.
	 * @param request
	 */
	public void validateResetPasswordRequest(Request request){
		validateParam(
		        (String) request.getRequest().get(JsonKey.USER_ID),
		        ResponseCode.mandatoryParamsMissing,
		        JsonKey.USER_ID);
		    validateParam(
		        (String) request.getRequest().get(JsonKey.PASSWORD),
		        ResponseCode.mandatoryParamsMissing,
		        JsonKey.PASSWORD);
	}

}
