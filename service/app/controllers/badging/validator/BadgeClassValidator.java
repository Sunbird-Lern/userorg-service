package controllers.badging.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
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

    private void validateRole(String role, ResponseCode error) {
        String validRoles = System.getenv(BadgingJsonKey.VALID_BADGE_ROLES);
        if (StringUtils.isBlank(validRoles)) {
            validRoles = propertiesCache.getProperty(BadgingJsonKey.VALID_BADGE_ROLES);
        }

        String[] validRolesList = validRoles.split("\\,");
        for (String validRole : validRolesList) {
            if (role.equalsIgnoreCase(validRole)) return;
        }

        throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    private void validateRoles(String value, ResponseCode error) {
        if (value == null) {
            throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }

        try {
            String trimmedValue = value.trim();
            if (trimmedValue.startsWith("[") && trimmedValue.endsWith("]")) {
                ObjectMapper mapper = new ObjectMapper();
                List<String> list = mapper.readValue(value, ArrayList.class);

                if (list.size() <= 0) {
                    throw new ProjectCommonException(error.getErrorCode(), error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
                }

                for (String role: list) {
                    validateRole(role, ResponseCode.invalidBadgeRole);
                }
            } else {
                validateRole(trimmedValue, ResponseCode.invalidBadgeRole);
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
        Map<String, Object> requestMap = request.getRequest();

        if (requestMap == null) {
            throw new ProjectCommonException(ResponseCode.invalidRequestData.getErrorCode(),
                    ResponseCode.invalidRequestData.getErrorMessage(), ERROR_CODE);
        }

        validateParam((String) requestMap.get(BadgingJsonKey.ISSUER_ID), ResponseCode.issuerIdRequired);
        validateParam((String) requestMap.get(BadgingJsonKey.BADGE_CRITERIA), ResponseCode.badgeCriteriaRequired);
        validateParam((String) requestMap.get(JsonKey.NAME), ResponseCode.badgeNameRequired);
        validateParam((String) requestMap.get(JsonKey.DESCRIPTION), ResponseCode.badgeDescriptionRequired);
        validateParam((String) requestMap.get(JsonKey.ROOT_ORG_ID), ResponseCode.rootOrgIdRequired);

        validateType((String) requestMap.get(JsonKey.TYPE));
        validateSubtype((String) requestMap.get(JsonKey.SUBTYPE));
        validateRoles((String) requestMap.get(JsonKey.ROLES), ResponseCode.badgeRolesRequired);

        Object image = request.getRequest().get(JsonKey.IMAGE);

        if (image == null) {
            throw new ProjectCommonException(ResponseCode.badgeImageRequired.getErrorCode(),
                    ResponseCode.badgeImageRequired.getErrorMessage(), ERROR_CODE);
        }
    }

    /**
     * Validates request of get badge class API.
     *
     * @param request Request containing following parameters:
     *                    badgeId: The ID of the Badge Class whose details to view
     */
    public void validateGetBadgeClass(Request request) {
        String badgeId = (String) request.getRequest().get(BadgingJsonKey.BADGE_ID);
        validateParam(badgeId, ResponseCode.badgeIdRequired);
    }

    /**
     * Validates request of search badge class API.
     *
     * @param request Request containing following filters:
     *                    issuerList: List of Issuer IDs whose badge classes are to be listed
     *                    rootOrgId: Root organisation ID
     *                    type: Badge class type (user / content)
     *                    subtype: Badge class subtype (e.g. award)
     *                    roles: JSON array of roles (e.g. [ "COURSE_MENTOR" ])
     */
    public void validateSearchBadgeClass(Request request) {
        Map<String, Object> filtersMap = (Map<String, Object>) request.getRequest().get(JsonKey.FILTERS);

        if (filtersMap == null) {
            throw new ProjectCommonException(ResponseCode.invalidRequestData.getErrorCode(), ResponseCode.invalidRequestData.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }

        List<String> issuerList = (List<String>) filtersMap.get(BadgingJsonKey.ISSUER_LIST);

        if (issuerList == null) {
            throw new ProjectCommonException(ResponseCode.issuerListRequired.getErrorCode(), ResponseCode.issuerListRequired.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }

    /**
     * Validates request of delete badge class API.
     *
     * @param request Request containing following parameters:
     *                    badgeId: The ID of the Badge Class to delete
     */
    public void validateDeleteBadgeClass(Request request) {
        String badgeId = (String) request.getRequest().get(BadgingJsonKey.BADGE_ID);
        validateParam(badgeId, ResponseCode.badgeIdRequired);
    }
}
