package controllers.usermanagement.validator;


import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.impl.KeyCloakServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MigrationRejectRequestValidator extends BaseRequestValidator {

    private Request request;
    private String authToken;
    private static List<String> mandatoryParamsList = new ArrayList<>(Arrays.asList(JsonKey.ACTION, JsonKey.USER_ID, JsonKey.USER_EXT_ID));
    private static SSOManager ssoManager = new KeyCloakServiceImpl();

    private MigrationRejectRequestValidator(Request request, String authToken) {
        this.request = request;
        this.authToken = authToken;
    }

    public static MigrationRejectRequestValidator getInstance(Request request, String authToken) {
        return new MigrationRejectRequestValidator(request, authToken);
    }

    public void validate() {
        validateAuthToken();
    }

    private void validateAuthToken() {
        String userId = ssoManager.verifyToken(authToken);
        if (!StringUtils.equalsIgnoreCase(userId, (String) request.getContext().get(JsonKey.USER_ID))) {
            throw new ProjectCommonException(ResponseCode.invalidAuthToken.getErrorCode(), ResponseCode.invalidAuthToken.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }

    }
}