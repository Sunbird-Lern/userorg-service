package org.sunbird.badge.service.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

/** Created by arvind on 15/3/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class BadgrServiceImplBadgeIssuerTest {

  BadgingService badgrServiceImpl;
  private Request request;
  private static final String BADGE_ISSUER_CREATE_SUCCESS_RESPONSE =
      "{    \"created_at\": \"2018-03-15T08:28:50.319695Z\",    \"json\": {        \"description\": \"Best certificate for teaching way and content\",        \"url\": \"http://localhost:8000/abcdhe\",        \"id\": \"http://localhost:8000/public/issuers/swarn-35\",        \"@context\": \"https://w3id.org/openbadges/v1\",        \"type\": \"Issuer\",        \"email\": \"abc123.xyz@gmail.com\",        \"name\": \"Swarn\"    },    \"name\": \"Swarn\",    \"slug\": \"swarn\",    \"image\": null,    \"created_by\": \"http://localhost:8000/user/1\",    \"description\": \"Best certificate for teaching way and content\",    \"owner\": \"http://localhost:8000/user/1\",    \"editors\": [],    \"staff\": []}";
  private static final String BADGE_ISSUER_LIST_SUCCESS_RESPONSE =
      "[    {        \"created_at\": \"2018-03-05T11:55:53.966947Z\",        \"json\": {            \"description\": \"Best certificate for teaching way and content\",            \"url\": \"http://localhost:8000/v1/issuer/issuers\",            \"id\": \"http://localhost:8000/public/issuers/swarn-2\",            \"@context\": \"https://w3id.org/openbadges/v1\",            \"type\": \"Issuer\",            \"email\": \"abc123.glaitm@gmail.com\",            \"name\": \"Swarn\"        },        \"name\": \"Swarn\",        \"slug\": \"swarn\",        \"image\": null,        \"created_by\": \"http://localhost:8000/user/1\",        \"description\": \"Best certificate for teaching way and content\",        \"owner\": \"http://localhost:8000/user/1\",        \"editors\": [],        \"staff\": []    }]";

  @Before
  public void setUp() {
    PowerMockito.mockStatic(HttpUtil.class);
    badgrServiceImpl = new BadgrServiceImpl();
    request = new Request();
  }

  @Test
  public void testCreateBadgeIssuerSuccess() throws IOException {
    PowerMockito.when(
            HttpUtil.postFormData(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_ISSUER_CREATE_SUCCESS_RESPONSE, 200));

    Map<String, Object> formParams = new HashMap<>();
    formParams.put(JsonKey.NAME, "Swarn");
    formParams.put(JsonKey.DESCRIPTION, "Best certificate for teaching way and content");
    formParams.put(JsonKey.EMAIL, "abc123.xyz@gmail.com");
    formParams.put(JsonKey.URL, "http://localhost:8000/abcdhe");

    request.getRequest().putAll(formParams);

    Response response = badgrServiceImpl.createIssuer(request);
    assertEquals(response.getResult().get(BadgingJsonKey.ISSUER_ID), "swarn");
  }

  @Test
  public void testGetBadgeIssuerSuccess() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_ISSUER_CREATE_SUCCESS_RESPONSE, 200));

    Map<String, Object> formParams = new HashMap<>();
    formParams.put(JsonKey.SLUG, "swarn");
    request.getRequest().putAll(formParams);
    Response response = badgrServiceImpl.getIssuerDetails(request);
    assertEquals(response.getResult().get(BadgingJsonKey.ISSUER_ID), "swarn");
  }

  @Test
  public void testGetBadgeIssuersListSuccess() throws IOException {
    PowerMockito.when(HttpUtil.doGetRequest(Mockito.any(), Mockito.any()))
        .thenReturn(new HttpUtilResponse(BADGE_ISSUER_LIST_SUCCESS_RESPONSE, 200));

    Map<String, Object> formParams = new HashMap<>();
    request.getRequest().putAll(formParams);
    Response response = badgrServiceImpl.getIssuerList(request);
    assertEquals(
        (((List<Map<String, Object>>) response.getResult().get(BadgingJsonKey.ISSUERS))
            .get(0)
            .get(BadgingJsonKey.ISSUER_ID)),
        "swarn");
  }
}
