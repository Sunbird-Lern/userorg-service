package controllers.badging.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates BadgeClass API requests.
 *
 * @author B Vinaya Kumar
 */
public class BadgeClassValidator {
    private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();
    private static PropertiesCache propertiesCache = PropertiesCache.getInstance();

    private void validateParam(String value, ResponseCode error) {
        if (StringUtils.isBlank(value)) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    private void validateListParam(String value, ResponseCode error) {
        if (value == null) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> list = mapper.readValue(value, ArrayList.class);

            if (list.size() <= 0) {
                throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
            }
        } catch (IOException e) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    private static void validateType(String type) {
        ResponseCode error = ResponseCode.badgeTypeRequired;

        if (StringUtils.isBlank(type)) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }

        error = ResponseCode.invalidBadgeType;
        if (! (type.equalsIgnoreCase(BadgingJsonKey.BADGE_TYPE_USER) || type.equalsIgnoreCase(BadgingJsonKey.BADGE_TYPE_CONTENT))) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    private static void validateSubtype(String subtype) {
        if (subtype != null) {
            String validSubtypes = System.getenv(BadgingJsonKey.VALID_BADGE_SUBTYPES);
            if (StringUtils.isBlank(validSubtypes)) {
                validSubtypes = propertiesCache.getProperty(BadgingJsonKey.VALID_BADGE_SUBTYPES);
            }

            String[] validSubtypesList = validSubtypes.split("\\,");
            for (String validSubtype : validSubtypesList) {
                if (validSubtype.equalsIgnoreCase(subtype)) return;
            }

            ResponseCode error = ResponseCode.invalidBadgeSubtype;
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }


    /**
     * Validates request of create badge class API.
     *
     * @param request Request containing following parameters:
     *                    issuerId: The ID of the Issuer to be owner of the new Badge Class
     *                    name: The name of the Badge Class
     *                    description: A short description of the new Badge Class.
     *                    image: An image to represent the Badge Class.
     *                    criteria: Either a text string or a URL of a remotely hosted page describing the criteria
     *                    rootOrgId: Root organisation ID
     *                    type: Badge class type (user / content)
     *                    subtype: Badge class subtype (e.g. award)
     *                    roles: JSON array of roles (e.g. [ "COURSE_MENTOR" ])
     */
    public void validateCreateBadgeClass(Request request) {
        Map<String, String> formParamsMap = (Map<String, String>) request.getRequest().get(JsonKey.FORM_PARAMS);

        if (formParamsMap == null) {
            throw new ProjectCommonException(ResponseCode.invalidRequestData.getErrorCode(),
                    ResponseCode.invalidRequestData.getErrorMessage(), ERROR_CODE);
        }

        validateParam(formParamsMap.get(BadgingJsonKey.ISSUER_ID), ResponseCode.issuerIdRequired);
        validateParam(formParamsMap.get(BadgingJsonKey.BADGE_CRITERIA), ResponseCode.badgeCriteriaRequired);
        validateParam(formParamsMap.get(JsonKey.NAME), ResponseCode.badgeNameRequired);
        validateParam(formParamsMap.get(JsonKey.DESCRIPTION), ResponseCode.badgeDescriptionRequired);
        validateParam(formParamsMap.get(JsonKey.ROOT_ORG_ID), ResponseCode.rootOrgIdRequired);

        validateType(formParamsMap.get(JsonKey.TYPE));
        validateSubtype(formParamsMap.get(JsonKey.SUBTYPE));
        validateListParam(formParamsMap.get(JsonKey.ROLES), ResponseCode.badgeRolesRequired);

        Map<String, Object> fileParamsMap = (Map<String, Object>) request.getRequest().get(JsonKey.FILE_PARAMS);

        if (fileParamsMap == null || fileParamsMap.size() <= 0) {
            throw new ProjectCommonException(ResponseCode.badgeImageRequired.getErrorCode(),
                    ResponseCode.badgeImageRequired.getErrorMessage(), ERROR_CODE);
        }
    }

    /**
     * Validates request of get badge class API.
     *
     * @param request Request containing following parameters:
     *                    issuerId: The ID of the Issuer who is owner of the Badge Class
     *                    badgeId: The ID of the Badge Class whose details to view
     */
    public void validateGetBadgeClass(Request request) {
        String issuerId = (String) request.getRequest().get(BadgingJsonKey.ISSUER_ID);
        validateParam(issuerId, ResponseCode.issuerIdRequired);

        String badgeId = (String) request.getRequest().get(BadgingJsonKey.BADGE_ID);
        validateParam(badgeId, ResponseCode.badgeIdRequired);
    }

    /**
     * Validates request of list badge class API.
     *
     * @param request Request containing following parameters:
     *                    issuerList: List of Issuer IDs whose badge classes are to be listed
     *                    context: JSON containing following parameters:
     *                                 rootOrgId: Root organisation ID
     *                                 type: Badge class type (user / content)
     *                                 subtype: Badge class subtype (e.g. award)
     *                                 roles: JSON array of roles (e.g. [ "COURSE_MENTOR" ])
     */
    public void validateListBadgeClass(Request request) {
        List<String> issuerList = (List<String>) request.getRequest().get(BadgingJsonKey.ISSUER_LIST);

        if (issuerList == null) {
            throw new ProjectCommonException(ResponseCode.issuerListRequired.getErrorCode(), ResponseCode.issuerListRequired.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    /**
     * Validates request of delete badge class API.
     *
     * @param request Request containing following parameters:
     *                    issuerId: The ID of the Issuer who is owner of the Badge Class
     *                    badgeId: The ID of the Badge Class whose details to view
     */
    public void validateDeleteBadgeClass(Request request) {
        String issuerId = (String) request.getRequest().get(BadgingJsonKey.ISSUER_ID);
        validateParam(issuerId, ResponseCode.issuerIdRequired);

        String badgeId = (String) request.getRequest().get(BadgingJsonKey.BADGE_ID);
        validateParam(badgeId, ResponseCode.badgeIdRequired);
    }
}
