package controllers.audit;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.BaseTestHelper;
import controllers.LearnerControllerTest;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;

/** Created by arvind on 6/12/17. */

public class AuditLogControllerTest extends BaseTestHelper {
	@Test
	public void testSearchAuditHistorySuccess() {
		Map<String, Object> requestMap = new HashMap<>();
		Map<String, Object> innerMap = new HashMap<>();
		requestMap.put(JsonKey.REQUEST, innerMap);
		String data = LearnerControllerTest.mapToJson(requestMap);
		JsonNode json = Json.parse(data);
		RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/audit/history").method("POST");
		req.headers(headerMap);
		Result result = route(req);
		assertEquals(200, result.status());
	}
}
