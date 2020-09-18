package controllers.usermanagement.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import views.html.defaultpages.error;


public class UserConsentRequestValidator extends BaseRequestValidator {

    enum CONSENT_STATUS
    {
        ACTIVE, REVOKED;
    }
    /**
     * This method will validate the mandatory param in the read request.
     * @param request
     */
    public void validateReadConsentRequest(Request request){
        validateParam(
                (String) request.getRequest().get(JsonKey.USER_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.USER_ID);
        validateParam(
                (String) request.getRequest().get(JsonKey.CONSUMER_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.CONSUMER_ID);
        validateParam(
                (String) request.getRequest().get(JsonKey.OBJECT_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.OBJECT_ID);
    }

    /**
     * This method will validate the mandatory param in the update request.
     * @param request
     */
    public void validateUpdateConsentRequest(Request request){
        String userId = (String) request.getRequest().get(JsonKey.USER_ID);
        if (StringUtils.isNotEmpty(userId)) {
            validateUserId(request, userId);
        }
        validateParam(
                (String) request.getRequest().get(JsonKey.CONSUMER_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.CONSUMER_ID);
        validateParam(
                (String) request.getRequest().get(JsonKey.OBJECT_TYPE),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.OBJECT_TYPE);
        validateParam(
                (String) request.getRequest().get(JsonKey.OBJECT_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.OBJECT_ID);
        String status = (String) request.getRequest().get(JsonKey.STATUS);
        validateParam(
                status,
                ResponseCode.mandatoryParamsMissing,
                JsonKey.STATUS);
        try {
            CONSENT_STATUS.valueOf(status);
        }catch (IllegalArgumentException iae){
            throw new ProjectCommonException(
                    ResponseCode.invalidConsentStatus.getErrorCode(),
                    ResponseCode.invalidConsentStatus.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        request.getRequest().put(JsonKey.STATUS,status.toUpperCase()); //Store status in capital
    }
}
