package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import mapper.RequestMapper;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

public class RequestMapperTest {
  @Test
  public void testMapRequestSuccess() {
    Request request;
    JsonNode node =
        new ObjectMapper().convertValue(createRequestMap(JsonKey.REQUEST), JsonNode.class);
    request = (Request) RequestMapper.mapRequest(node, Request.class);
    Assert.assertNotNull(request);
    Assert.assertEquals("xyz", request.getRequest().get(JsonKey.FIRST_NAME));
  }

  @Test
  public void testMapRequestFailure() {
    try {
      RequestMapper.mapRequest(null, Request.class);
    } catch (ProjectCommonException e) {
      Assert.assertEquals(
          MessageFormat.format(
              ResponseCode.mandatoryHeaderParamsMissing.getErrorMessage(),
              "Content-Type with value application/json"),
          e.getMessage());
      Assert.assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getErrorResponseCode());
    }
  }

  @Test
  public void testMapRequestFailureWithInvalidKey() {
    Request request;
    JsonNode node = new ObjectMapper().convertValue(createRequestMap("invalidKey"), JsonNode.class);
    request = (Request) RequestMapper.mapRequest(node, Request.class);
    Assert.assertNotNull(request);
    Assert.assertEquals(null, request.getRequest().get(JsonKey.FIRST_NAME));
  }

  private Map<String, Object> createRequestMap(String requestKey) {
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.FIRST_NAME, "xyz");
    innerMap.put(JsonKey.PHONE, "1234567890");
    innerMap.put(JsonKey.EMAIL, "xyz@gmail.com");
    map.put(requestKey, innerMap);
    return map;
  }
}
