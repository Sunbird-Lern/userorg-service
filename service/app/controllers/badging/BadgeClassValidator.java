package controllers.badging;

import org.apache.cassandra.cql3.Json;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.List;
import java.util.Map;

public class BadgeClassValidator {
    private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

    public void validateParam(String value, ResponseCode error) {
        if (StringUtils.isBlank(value)) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    public void validateCreateBadgeClass(Request request) {
        Map<String, String> formParamsMap = (Map<String, String>) request.getRequest().get(JsonKey.FORM_PARAMS);

        validateParam(formParamsMap.get(BadgingJsonKey.ISSUER_ID), ResponseCode.issuerIdRequired);
        validateParam(formParamsMap.get(BadgingJsonKey.BADGE_CRITERIA), ResponseCode.badgeCriteriaRequired);
        validateParam(formParamsMap.get(JsonKey.NAME), ResponseCode.badgeNameRequired);
        validateParam(formParamsMap.get(JsonKey.DESCRIPTION), ResponseCode.badgeDescriptionRequired);
        validateParam(formParamsMap.get(JsonKey.ROOT_ORG_ID), ResponseCode.rootOrgIdRequired);
        validateParam(formParamsMap.get(JsonKey.TYPE), ResponseCode.badgeTypeRequired);
        validateParam(formParamsMap.get(JsonKey.ROLES), ResponseCode.badgeRolesRequired);

        Map<String, Object> fileParamsMap = (Map<String, Object>) request.getRequest().get(JsonKey.FILE_PARAMS);

        if (fileParamsMap.size() <= 0) {
            throw new ProjectCommonException(ResponseCode.badgeImageRequired.getErrorCode(),
                    ResponseCode.badgeImageRequired.getErrorMessage(), ERROR_CODE);
        }
    }

    public void validateGetBadgeClass(Request request) {
        String issuerId = (String) request.getRequest().get(BadgingJsonKey.ISSUER_ID);
        validateParam(issuerId, ResponseCode.issuerIdRequired);

        String badgeId = (String) request.getRequest().get(BadgingJsonKey.BADGE_ID);
        validateParam(badgeId, ResponseCode.badgeIdRequired);
    }

    public void validateListBadgeClass(Request request) {
        List<String> issuerList = (List<String>) request.getRequest().get(BadgingJsonKey.ISSUER_LIST);

        if (issuerList == null) {
            throw new ProjectCommonException(ResponseCode.issuerListRequired.getErrorCode(), ResponseCode.issuerListRequired.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    public void validateDeleteBadgeClass(Request request) {
        String issuerId = (String) request.getRequest().get(BadgingJsonKey.ISSUER_ID);
        validateParam(issuerId, ResponseCode.issuerIdRequired);

        String badgeId = (String) request.getRequest().get(BadgingJsonKey.BADGE_ID);
        validateParam(badgeId, ResponseCode.badgeIdRequired);
    }
}
