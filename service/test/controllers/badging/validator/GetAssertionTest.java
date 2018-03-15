package controllers.badging.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Test class for Badge assertion get request data.
 * @author Manzarul
 *
 */
public class GetAssertionTest {

	@Test
	public void getAssertion() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			Assert.assertNull(e);
		}
		assertEquals(true, response);
	}

	@Test
	public void getAssertionWithOutIssuerId() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.issuerIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}

	@Test
	public void getAssertionWithEmptyIssuerId() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.issuerIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}

	@Test
	public void getAssertionWithEmptyBadgeId() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerId");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, null);
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.badgeIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}

	@Test
	public void getAssertionWithOutBadgeId() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerId");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.badgeIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}

	@Test
	public void getAssertionWithEmptyAssertionId() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerId");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "badgeId");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.assertionIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}

	@Test
	public void getAssertionWithEmptyAll() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "null");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, " ");
		request.setRequest(requestObj);
		try {
			BadgeAssertionValidator.validategetBadgeAssertion(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.issuerIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}
	
	
	@Test
	public void getAssertionList() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		List<Map<String,Object>> list = new ArrayList<>();
		 list.add(requestObj);
		request.getRequest().put(BadgingJsonKey.ASSERTIONS, list);
		try {
			BadgeAssertionValidator.validateGetAssertionList(request);
			response = true;
		} catch (ProjectCommonException e) {
			Assert.assertNull(e);
		}
		assertEquals(true, response);
	}
	
	@Test
	public void getAssertionListWithWrongKey() {
		Request request = new Request();
		boolean response = false;
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		List<Map<String,Object>> list = new ArrayList<>();
		 list.add(requestObj);
		request.getRequest().put("list", list);
		try {
			BadgeAssertionValidator.validateGetAssertionList(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.invalidRequestData.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}
	
	
	@Test
	public void getAssertionListWithExceedSize() {
		Request request = new Request();
		boolean response = false;
		List<Map<String,Object>> list = new ArrayList<>();
		for (int i = 0 ; i< 50 ;i++) {
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
		requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
		requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
		 list.add(requestObj);
		}
		request.getRequest().put(BadgingJsonKey.ASSERTIONS, list);
		try {
			BadgeAssertionValidator.validateGetAssertionList(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.sizeLimitExceed.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}

	@Test
	public void getAssertionListWithMaxSize() {
		Request request = new Request();
		boolean response = false;
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Map<String, Object> requestObj = new HashMap<>();
			requestObj.put(BadgingJsonKey.ISSUER_ID, "issuerSlug");
			requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
			requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
			list.add(requestObj);
		}
		request.getRequest().put(BadgingJsonKey.ASSERTIONS, list);
		try {
			BadgeAssertionValidator.validateGetAssertionList(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertNotNull(e);
		}
		assertEquals(true, response);
	}
	
	@Test
	public void getAssertionListWithEmptyIssuer() {
		Request request = new Request();
		boolean response = false;
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Map<String, Object> requestObj = new HashMap<>();
			requestObj.put(BadgingJsonKey.ISSUER_ID, "");
			requestObj.put(BadgingJsonKey.BADGE_CLASS_ID, "classslug");
			requestObj.put(BadgingJsonKey.ASSERTION_ID, "someAssertionId");
			list.add(requestObj);
		}
		request.getRequest().put(BadgingJsonKey.ASSERTIONS, list);
		try {
			BadgeAssertionValidator.validateGetAssertionList(request);
			response = true;
		} catch (ProjectCommonException e) {
			assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
			assertEquals(ResponseCode.issuerIdRequired.getErrorCode(), e.getCode());
		}
		assertEquals(false, response);
	}
}
