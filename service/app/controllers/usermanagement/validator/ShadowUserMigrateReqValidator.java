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


/**
 * @author anmolgupta
 * this class will be responsilbe to validate the Shadow user migration request.
 */
public class ShadowUserMigrateReqValidator extends BaseRequestValidator {

    private Request request;
    private String callerId;
    private static final List<String> allowedActions=new ArrayList<>(Arrays.asList("accept","reject"));
    private static List<String> mandatoryParamsList= new ArrayList<>(Arrays.asList(JsonKey.USER_ID,JsonKey.USER_EXT_ID,JsonKey.CHANNEL,JsonKey.ACTION));

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
        validateAction((String)request.getRequest().get(JsonKey.ACTION));
    }

    private void validateAction(String action){
        if(!allowedActions.contains(action)){
            throw new ProjectCommonException(ResponseCode.invalidParameterValue.getErrorCode(), MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), action, JsonKey.ACTION+" supported actions are:"+allowedActions), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }
}