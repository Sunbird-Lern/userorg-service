package controllers.usermanagement.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import views.html.defaultpages.error;

import java.util.HashMap;
import java.util.Map;


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
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault(JsonKey.CONSENT_BODY, new HashMap<String, Object>());
        Map<String, Object> filters = (Map<String, Object>) consent.getOrDefault(JsonKey.FILTERS, new HashMap<String, Object>());
        validateParam(
                (String) filters.get(JsonKey.USER_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.USER_ID);
        validateParam(
                (String) filters.get(JsonKey.CONSENT_CONSUMERID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.CONSENT_CONSUMERID);
        validateParam(
                (String) filters.get(JsonKey.CONSENT_OBJECTID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.CONSENT_OBJECTID);
    }

    /**
     * This method will validate the mandatory param in the update request.
     * @param request
     */
    public void validateUpdateConsentRequest(Request request){
        Map<String, Object> consent = (Map<String, Object>) request.getRequest().getOrDefault(JsonKey.CONSENT_BODY, new HashMap<String, Object>());
        validateParam(
                (String) consent.get(JsonKey.USER_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.USER_ID);
        validateParam(
                (String) consent.get(JsonKey.CONSENT_CONSUMERID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.CONSENT_CONSUMERID);
        validateParam(
                (String) consent.get(JsonKey.OBJECT_TYPE),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.OBJECT_TYPE);
        validateParam(
                (String) consent.get(JsonKey.CONSENT_OBJECTID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.CONSENT_OBJECTID);
        String status = (String) consent.get(JsonKey.STATUS);
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
        consent.put(JsonKey.STATUS,status.toUpperCase()); //Store status in capital
    }
}
