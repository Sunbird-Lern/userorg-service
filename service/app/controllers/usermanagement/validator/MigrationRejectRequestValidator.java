package controllers.usermanagement.validator;


import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.responsecode.ResponseCode;

import java.text.MessageFormat;

public class MigrationRejectRequestValidator extends BaseRequestValidator {


    private String tokenUserId,userId;

    private MigrationRejectRequestValidator(String tokenUserId, String userId) {

        this.tokenUserId=tokenUserId;
        this.userId=userId;
    }

    public static MigrationRejectRequestValidator getInstance(String tokenUserId, String authToken) {
        return new MigrationRejectRequestValidator(tokenUserId, authToken);
    }

    public void validate() {
        if(!StringUtils.equalsIgnoreCase(tokenUserId,userId)){
            throw new ProjectCommonException(ResponseCode.invalidParameterValue.getErrorCode(), MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID), ResponseCode.CLIENT_ERROR.getResponseCode());
        }

    }

}