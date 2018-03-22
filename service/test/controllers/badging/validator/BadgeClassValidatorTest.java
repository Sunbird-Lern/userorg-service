package controllers.badging.validator;

import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test class for BadgeClassValidator.
 *
 * @author B Vinaya Kumar
 */
public class BadgeClassValidatorTest {
    @Test
    public void testValidateCreateBadgeClassIssuerIdRequired() {
        Request request = new Request();
        Map<String, Object> requestMap = new HashMap<>();
        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.issuerIdRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassCriteriaRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeCriteriaRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassNameRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeNameRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassDescriptionRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeDescriptionRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassRootOrgIdRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.rootOrgIdRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassTypeRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeTypeRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassInvalidType() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "invalid");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.invalidBadgeType.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassInvalidSubtype() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "user");
        requestMap.put(JsonKey.SUBTYPE, "invalid");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.invalidBadgeSubtype.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassRolesRequiredNull() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "user");
        requestMap.put(JsonKey.SUBTYPE, "award");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeRolesRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassRolesRequiredEmpty() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "user");
        requestMap.put(JsonKey.SUBTYPE, "award");
        requestMap.put(JsonKey.ROLES, "[]");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeRolesRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassSingleRoleImageRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "user");
        requestMap.put(JsonKey.SUBTYPE, "award");
        requestMap.put(JsonKey.ROLES, "OFFICIAL_BADGE_ISSUER");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeImageRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassMultipleRolesImageRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "user");
        requestMap.put(JsonKey.SUBTYPE, "award");
        requestMap.put(JsonKey.ROLES, "[ \"OFFICIAL_BADGE_ISSUER\", \"TEACHER_BADGE_ISSUER\" ]");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeImageRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassSuccess() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        requestMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        requestMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        requestMap.put(JsonKey.ROOT_ORG_ID, "AP");
        requestMap.put(JsonKey.TYPE, "user");
        requestMap.put(JsonKey.SUBTYPE, "award");
        requestMap.put(JsonKey.ROLES, "[ \"OFFICIAL_BADGE_ISSUER\" ]");
        requestMap.put(JsonKey.IMAGE, "something");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            fail();
        }
    }

    @Test
    public void testValidateGetBadgeIssuerIdRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateGetBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.issuerIdRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateGetBadgeBadgeIdRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateGetBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeIdRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateGetBadgeClassSuccess() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_ID, "java-se-8-programmer");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateGetBadgeClass(request);
        } catch (ProjectCommonException e) {
            fail();
        }
    }

    @Test
    public void testValidateSearchBadgeInvalidRequest() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateSearchBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.invalidRequestData.getErrorCode());
        }
    }

    @Test
    public void testValidateSearchBadgeIssuerListRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> filtersMap = new HashMap<>();

        requestMap.put(JsonKey.FILTERS, filtersMap);

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateSearchBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.issuerListRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateSearchBadgeIssuerListSuccess() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> filtersMap = new HashMap<>();

        filtersMap.put(BadgingJsonKey.ISSUER_LIST, new ArrayList<>());
        requestMap.put(JsonKey.FILTERS, filtersMap);

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateSearchBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.issuerListRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateDeleteBadgeIssuerIdRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateDeleteBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.issuerIdRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateDeleteBadgeBadgeIdRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateDeleteBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeIdRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateDeleteBadgeClassSuccess() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        requestMap.put(BadgingJsonKey.BADGE_ID, "java-se-8-programmer");

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateDeleteBadgeClass(request);
        } catch (ProjectCommonException e) {
            fail();
        }
    }
}
