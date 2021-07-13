package controllers.usermanagement.validator;

import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

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
		        (String) request.getRequest().get(JsonKey.KEY),
		        ResponseCode.mandatoryParamsMissing,
		        JsonKey.KEY);
		    validateParam(
			        (String) request.getRequest().get(JsonKey.TYPE),
			        ResponseCode.mandatoryParamsMissing,
			        JsonKey.TYPE);
	}

}
