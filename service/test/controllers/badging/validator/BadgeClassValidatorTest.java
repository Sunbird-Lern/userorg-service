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
        Map<String, Object> formParamsMap = new HashMap<>();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "invalid");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "user");
        formParamsMap.put(JsonKey.SUBTYPE, "invalid");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "user");
        formParamsMap.put(JsonKey.SUBTYPE, "award");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "user");
        formParamsMap.put(JsonKey.SUBTYPE, "award");
        formParamsMap.put(JsonKey.ROLES, "[]");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeRolesRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassImageRequiredNull() {
        Request request = new Request();
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "user");
        formParamsMap.put(JsonKey.SUBTYPE, "award");
        formParamsMap.put(JsonKey.ROLES, "[ \"roleId1\" ]");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateCreateBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.badgeImageRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateCreateBadgeClassImageRequiredEmpty() {
        Request request = new Request();
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "user");
        formParamsMap.put(JsonKey.SUBTYPE, "award");
        formParamsMap.put(JsonKey.ROLES, "[ \"roleId1\" ]");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);

        Map<String, Object> fileParamsMap = new HashMap<>();
        requestMap.put(JsonKey.FILE_PARAMS, fileParamsMap);

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
        Map<String, Object> formParamsMap = new HashMap<>();
        formParamsMap.put(BadgingJsonKey.ISSUER_ID, "oracle-university");
        formParamsMap.put(BadgingJsonKey.BADGE_CRITERIA, "https://education.oracle.com/pls/web_prod-plq-dad/db_pages.getpage?page_id=5001&get_params=p_exam_id:1Z0-808");
        formParamsMap.put(JsonKey.NAME, "Java SE 8 Programmer");
        formParamsMap.put(JsonKey.DESCRIPTION, "A basic Java SE 8 certification.");
        formParamsMap.put(JsonKey.ROOT_ORG_ID, "AP");
        formParamsMap.put(JsonKey.TYPE, "user");
        formParamsMap.put(JsonKey.SUBTYPE, "award");
        formParamsMap.put(JsonKey.ROLES, "[ \"roleId1\" ]");

        Map<String, Object> fileParamsMap = new HashMap<>();
        fileParamsMap.put("image", "something");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.FORM_PARAMS, formParamsMap);
        requestMap.put(JsonKey.FILE_PARAMS, fileParamsMap);

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
    public void testValidateListBadgeIssuerListRequired() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateListBadgeClass(request);
        } catch (ProjectCommonException e) {
            assertEquals(e.getCode(), ResponseCode.issuerListRequired.getErrorCode());
        }
    }

    @Test
    public void testValidateListBadgeIssuerListSuccess() {
        Request request = new Request();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(BadgingJsonKey.ISSUER_LIST, new ArrayList<>());

        request.setRequest(requestMap);

        try {
            new BadgeClassValidator().validateListBadgeClass(request);
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
