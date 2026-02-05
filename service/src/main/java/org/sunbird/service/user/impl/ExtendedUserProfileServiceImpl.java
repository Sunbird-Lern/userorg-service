package org.sunbird.service.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.service.user.ExtendedUserProfileService;
import org.sunbird.util.user.UserExtendedProfileSchemaValidator;

import java.util.Map;

public class ExtendedUserProfileServiceImpl implements ExtendedUserProfileService {
    private static final String SCHEMA = "profileDetails.json";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static ExtendedUserProfileServiceImpl instance = null;
    private LoggerUtil logger = new LoggerUtil(UserExtendedProfileSchemaValidator.class);

    public static ExtendedUserProfileService getInstance() {
        if(instance == null) {
            instance = new ExtendedUserProfileServiceImpl();
        }
        return instance;
    }

    @Override
    public void validateProfile(Request userRequest) {
        if (userRequest!=null && userRequest.get(JsonKey.PROFILE_DETAILS)!=null) {
            try{
                String userProfile = mapper.writeValueAsString(userRequest.getRequest().get(JsonKey.PROFILE_DETAILS));
                JSONObject obj = new JSONObject(userProfile);
                UserExtendedProfileSchemaValidator.validate(SCHEMA, obj);
                ((Map)userRequest.getRequest().get(JsonKey.PROFILE_DETAILS)).put(JsonKey.MANDATORY_FIELDS_EXISTS, obj.get(JsonKey.MANDATORY_FIELDS_EXISTS));
            } catch (Exception e){
                logger.error(userRequest.getRequestContext(), "ExtendedUserProfileServiceImpl.validateProfile :: failed to validate profile.", e);
                throw new ProjectCommonException(
                        ResponseCode.extendUserProfileNotLoaded,
                        ResponseCode.extendUserProfileNotLoaded.getErrorMessage(),
                        ResponseCode.extendUserProfileNotLoaded.getResponseCode());
            }
        }
    }
}
