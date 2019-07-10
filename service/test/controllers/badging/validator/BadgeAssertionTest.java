package controllers.badging.validator;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Test class for Badge assertion request data.
 *
 * @author Manzarul
 */
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class BadgeAssertionTest {

  @Test
  public void validateAssertionSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "slug12");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "UserId or contnet id");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    requestObj.put(BadgingJsonKey.EVIDENCE, "http://127.0.0.1:8000/public/badges/db-design-expert");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateAssertionWithEmptyIssuer() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.issuerIdRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateAssertionWithOutIssuer() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userId");
    requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.issuerIdRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateAssertionWithEmptyBadgeSlug() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "contentId");
    requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.badgeIdRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateAssertionWithOutBadgeSlug() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userId");
    requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.badgeIdRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateAssertionWithEmptyRecipient() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "");
    requestObj.put(BadgingJsonKey.EVIDENCE, "http://dev.open-sunbird.org");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.recipientIdRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void assertionWithOutRecipientId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.recipientIdRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void assertionWithEmptyEvidence() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "UserId or contnet id");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "content");
    requestObj.put(BadgingJsonKey.EVIDENCE, "some data");
    requestObj.put(BadgingJsonKey.NOTIFY, false);
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateBadgeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.evidenceRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }
}
