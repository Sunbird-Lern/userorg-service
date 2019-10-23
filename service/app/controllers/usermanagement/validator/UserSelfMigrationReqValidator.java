package controllers.usermanagement.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserSelfMigrationReqValidator extends BaseRequestValidator {

    private Request request;
    private String tokenUserid;
    private static List<String> mandatoryParamsList= new ArrayList<>(Arrays.asList(JsonKey.USER_ID,JsonKey.USER_EXT_ID));

    private UserSelfMigrationReqValidator(Request request,String tokenUserId) {
        this.request = request;
        this.tokenUserid=tokenUserId;
    }

    public static  UserSelfMigrationReqValidator getInstance(Request request,String tokenUserId){
        return new UserSelfMigrationReqValidator(request,tokenUserId);
    }
    public void validate(){
        checkMandatoryFieldsPresent(request.getRequest(),mandatoryParamsList);
        String userId=(String)request.getRequest().get(JsonKey.USER_ID);
        if(!StringUtils.equalsIgnoreCase(userId,tokenUserid)){
            throw new ProjectCommonException(ResponseCode.invalidParameterValue.getErrorCode(), MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID), ResponseCode.CLIENT_ERROR.getResponseCode());

        }
    }
}