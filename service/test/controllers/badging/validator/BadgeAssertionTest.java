package controllers.badging.validator;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Test class for Badge assertion request data.
 * @author Manzarul
 *
 */
public class BadgeAssertionTest {
	
	
	@Test
	public void validateAssertionSuccess () {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "slug12");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
		requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
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
	public void validateAssertionWithEmptyIssuer () {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
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
	public void validateAssertionWithOutIssuer () {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
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
	public void validateAssertionWithEmptyBadgeSlug () {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
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
	public void validateAssertionWithOutBadgeSlug () {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "issuerSlug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
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
	public void validateAssertionWithEmptyRecipient () {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "");
		requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
		requestObj.put(BadgingJsonKey.NOTIFY, false);
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validateBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.recipientEmailRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}
	
	@Test
	public void assertionWithInvalidRecipient() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul");
		requestObj.put(BadgingJsonKey.EVIDENCE, "http://localhost:8000/public/badges/db-design-expert");
		requestObj.put(BadgingJsonKey.NOTIFY, false);
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validateBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.emailFormatError.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}
	
	@Test
	public void assertionWithOutEvidence() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
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
	
	@Test
	public void assertionWithEmptyEvidence() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_SLUG, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_SLUG, "classslug");
		requestObj.put(BadgingJsonKey.RECIPIENT_EMAIL, "manzarul.haque@tarento.com");
		requestObj.put(BadgingJsonKey.EVIDENCE, "  ");
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
