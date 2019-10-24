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

public class ShadowUserMigrateReqValidator extends BaseRequestValidator {

    private Request request;
    private String callerId;
    private static List<String> mandatoryParamsList= new ArrayList<>(Arrays.asList(JsonKey.USER_ID,JsonKey.USER_EXT_ID));

    private ShadowUserMigrateReqValidator(Request request,String tokenUserId) {
        this.request = request;
        this.callerId=tokenUserId;
    }

    public static  ShadowUserMigrateReqValidator getInstance(Request request,String callerId){
        return new ShadowUserMigrateReqValidator(request,callerId);
    }
    public void validate(){
        checkMandatoryFieldsPresent(request.getRequest(),mandatoryParamsList);
        String userId=(String)request.getRequest().get(JsonKey.USER_ID);
        if(!StringUtils.equalsIgnoreCase(userId,callerId)){
            throw new ProjectCommonException(ResponseCode.invalidParameterValue.getErrorCode(), MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }
}