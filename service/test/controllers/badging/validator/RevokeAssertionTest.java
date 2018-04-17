package controllers.badging.validator;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Test class for Badge assertion revoke request data.
 *
 * @author Manzarul
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class RevokeAssertionTest {

  @Test
  public void revokeAssertion() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
    requestObj.put(BadgingJsonKey.REVOCATION_REASON, "some reason");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userIdorContentId");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateRevokeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void revoeWithemptyReason() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
    requestObj.put(BadgingJsonKey.REVOCATION_REASON, " ");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userIdorContentId");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateRevokeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.revocationReasonRequired.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void revokeWithEmptyBadgeId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "");
    requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
    requestObj.put(BadgingJsonKey.REVOCATION_REASON, "some reason");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userIdorContentId");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateRevokeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void revokeWithOutIssuer() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
    requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
    requestObj.put(BadgingJsonKey.REVOCATION_REASON, "some reason");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userIdorContentId");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateRevokeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void revokeWithInvalidRecipient() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
    requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "badgeId");
    requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
    requestObj.put(BadgingJsonKey.REVOCATION_REASON, "some reason");
    requestObj.put(BadgingJsonKey.RECIPIENT_ID, "userIdorContentId");
    requestObj.put(BadgingJsonKey.RECIPIENT_TYPE, "user-1");
    request.setRequest(requestObj);
    try {
      BadgeAssertionValidator.validateRevokeAssertion(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidRecipientType.getErrorCode(), e.getCode());
    }
    assertEquals(false, response);
  }
}
