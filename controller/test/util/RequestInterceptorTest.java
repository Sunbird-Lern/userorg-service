package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigFactory;

import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.auth.verifier.AccessTokenValidator;
import org.sunbird.keys.JsonKey;
import play.mvc.Http;
import play.test.Helpers;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AccessTokenValidator.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
public class RequestInterceptorTest {

  private static AccessTokenValidator tokenValidator;

  @Test
  public void testIsRequestInExcludeListWithResAnonymous() {
    tokenValidator = mock(AccessTokenValidator.class);
    PowerMockito.mockStatic(AccessTokenValidator.class);
    Http.RequestBuilder requestBuilder =
        Helpers.fakeRequest(Helpers.GET, "http://localhost:9000/service/health")
            .header(
                "x-authenticated-user-token",
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyZUNvWGlZRHFDbHRaX1F1ZXNRMEhtNkNYVF91emJiN2d3bXlMZXhsN1JnIn0.eyJqdGkiOiIwMTVmNmRlOC1jODRiLTRkNmUtOGRkYy1mNzZmNTk3NTViNjgiLCJleHAiOjE1OTQxMDg0MjUsIm5iZiI6MCwiaWF0IjoxNTk0MDIyMDI1LCJpc3MiOiJodHRwczovL3N0YWdpbmcubnRwLm5ldC5pbi9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiZjo5MzFhOWRjOS00NTk0LTQ4MzktYWExNi1jZjBjYWMwOTYzODE6M2Y0YmYzMTEtOTNkMy00ODY3LTgxMGMtZGViMDYzYjQzNzg5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGFmYzU0YmUtNmZkOS00MDRlLTljYzctN2FkYzYxYzVjYzI1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7fSwibmFtZSI6Ik5hdjIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuYXYyMzMzNyIsImdpdmVuX25hbWUiOiJOYXYyIiwiZW1haWwiOiJuYXYyQHlvcG1haWwuY29tIn0.HAG5Uv7F7J82HCmNsM9NjzMKEW_65nsJX-P_SC5XNfSoz9w5FkQQ4Xlx9elw5vbvtG9UU5Jn5TDMRGAnjdCZ-FMgMv0BLGy3uRKq6Xu6drf6oN9kYMIgTGYuf946EX3pelXQtL6kXwi5_OQ6OQT7Ie94l525BEn09SkeiKJsUrrxShLMlCaX3ERt83MwNdxLkuJ0tI8Jx22leksaf8cxGteC3iF31eLVxIe3ioIexUpbbTI-zBZHHURX_5tAIZvq91kV7Laibngqg4RDluaBltmbBWufFBPAYHqwFRvhix2E78t3d6cb7mx4xRNDrbTJCxHQCL2kE-VXkPGBDEHa3g");
    ;
    assertEquals(
        (String)
            RequestInterceptor.verifyRequestData(requestBuilder.build(), new HashMap<>())
                .get(JsonKey.USER_ID),
        "Anonymous");
  }

  @Test
  public void testIsRequestInExcludeLisWithReqUrlNotExists() {
    boolean isurlexits = RequestInterceptor.isRequestInExcludeList("/v1/group/create");
    assertFalse(isurlexits);
  }

  @Test
  public void testVerifyRequestDataReturnsAuthorizedUser() {
    tokenValidator = mock(AccessTokenValidator.class);
    PowerMockito.mockStatic(AccessTokenValidator.class);
    ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
    ObjectNode userNode = JsonNodeFactory.instance.objectNode();
    userNode.put(JsonKey.USER_ID, "56c2d9a3-fae9-4341-9862-4eeeead2e9a1");
    requestNode.put(JsonKey.REQUEST, userNode);
    Http.RequestBuilder requestBuilder =
        Helpers.fakeRequest(Helpers.POST, "http://localhost:9000/v1/group/create")
            .header(
                "x-authenticated-user-token",
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyZUNvWGlZRHFDbHRaX1F1ZXNRMEhtNkNYVF91emJiN2d3bXlMZXhsN1JnIn0.eyJqdGkiOiIwMTVmNmRlOC1jODRiLTRkNmUtOGRkYy1mNzZmNTk3NTViNjgiLCJleHAiOjE1OTQxMDg0MjUsIm5iZiI6MCwiaWF0IjoxNTk0MDIyMDI1LCJpc3MiOiJodHRwczovL3N0YWdpbmcubnRwLm5ldC5pbi9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiZjo5MzFhOWRjOS00NTk0LTQ4MzktYWExNi1jZjBjYWMwOTYzODE6M2Y0YmYzMTEtOTNkMy00ODY3LTgxMGMtZGViMDYzYjQzNzg5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGFmYzU0YmUtNmZkOS00MDRlLTljYzctN2FkYzYxYzVjYzI1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7fSwibmFtZSI6Ik5hdjIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuYXYyMzMzNyIsImdpdmVuX25hbWUiOiJOYXYyIiwiZW1haWwiOiJuYXYyQHlvcG1haWwuY29tIn0.HAG5Uv7F7J82HCmNsM9NjzMKEW_65nsJX-P_SC5XNfSoz9w5FkQQ4Xlx9elw5vbvtG9UU5Jn5TDMRGAnjdCZ-FMgMv0BLGy3uRKq6Xu6drf6oN9kYMIgTGYuf946EX3pelXQtL6kXwi5_OQ6OQT7Ie94l525BEn09SkeiKJsUrrxShLMlCaX3ERt83MwNdxLkuJ0tI8Jx22leksaf8cxGteC3iF31eLVxIe3ioIexUpbbTI-zBZHHURX_5tAIZvq91kV7Laibngqg4RDluaBltmbBWufFBPAYHqwFRvhix2E78t3d6cb7mx4xRNDrbTJCxHQCL2kE-VXkPGBDEHa3g")
            .header(
                "x-authenticated-for",
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyZUNvWGlZRHFDbHRaX1F1ZXNRMEhtNkNYVF91emJiN2d3bXlMZXhsN1JnIn0.eyJqdGkiOiIwMTVmNmRlOC1jODRiLTRkNmUtOGRkYy1mNzZmNTk3NTViNjgiLCJleHAiOjE1OTQxMDg0MjUsIm5iZiI6MCwiaWF0IjoxNTk0MDIyMDI1LCJpc3MiOiJodHRwczovL3N0YWdpbmcubnRwLm5ldC5pbi9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiZjo5MzFhOWRjOS00NTk0LTQ4MzktYWExNi1jZjBjYWMwOTYzODE6M2Y0YmYzMTEtOTNkMy00ODY3LTgxMGMtZGViMDYzYjQzNzg5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGFmYzU0YmUtNmZkOS00MDRlLTljYzctN2FkYzYxYzVjYzI1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7fSwibmFtZSI6Ik5hdjIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuYXYyMzMzNyIsImdpdmVuX25hbWUiOiJOYXYyIiwiZW1haWwiOiJuYXYyQHlvcG1haWwuY29tIn0.HAG5Uv7F7J82HCmNsM9NjzMKEW_65nsJX-P_SC5XNfSoz9w5FkQQ4Xlx9elw5vbvtG9UU5Jn5TDMRGAnjdCZ-FMgMv0BLGy3uRKq6Xu6drf6oN9kYMIgTGYuf946EX3pelXQtL6kXwi5_OQ6OQT7Ie94l525BEn09SkeiKJsUrrxShLMlCaX3ERt83MwNdxLkuJ0tI8Jx22leksaf8cxGteC3iF31eLVxIe3ioIexUpbbTI-zBZHHURX_5tAIZvq91kV7Laibngqg4RDluaBltmbBWufFBPAYHqwFRvhix2E78t3d6cb7mx4xRNDrbTJCxHQCL2kE-VXkPGBDEHa3g")
            .bodyJson(requestNode);
    Http.Request req = requestBuilder.build();
    when(tokenValidator.verifyUserToken(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("authorized-user");
    when(tokenValidator.verifyManagedUserToken(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("authorized-user");
    assertEquals(
        (String)
            RequestInterceptor.verifyRequestData(requestBuilder.build(), new HashMap<>())
                .get(JsonKey.USER_ID),
        "authorized-user");
  }

  @Test
  public void testVerifyRequestDataReturnsAuthorizedUser2() {
    tokenValidator = mock(AccessTokenValidator.class);
    PowerMockito.mockStatic(AccessTokenValidator.class);
    ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
    ObjectNode userNode = JsonNodeFactory.instance.objectNode();
    userNode.put(JsonKey.USER_ID, "56c2d9a3-fae9-4341-9862-4eeeead2e9a1");
    requestNode.put(JsonKey.REQUEST, userNode);
    Http.RequestBuilder requestBuilder =
        Helpers.fakeRequest(Helpers.POST, "http://localhost:9000/v1/group/create")
            .bodyJson(requestNode);
    Http.Request req = requestBuilder.build();
    when(tokenValidator.verifyUserToken(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("authorized-user");
    when(tokenValidator.verifyManagedUserToken(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("authorized-user");
    //checking auth enabled condition
    if(ConfigFactory.load().getBoolean(JsonKey.AUTH_ENABLED)){
      assertEquals(
              RequestInterceptor.verifyRequestData(requestBuilder.build(), new HashMap<>())
                      .get(JsonKey.USER_ID),
              JsonKey.UNAUTHORIZED);
    }
  }

  @Test
  public void testVerifyRequestDataReturnsAuthorizedUserForGET() {
    tokenValidator = mock(AccessTokenValidator.class);
    PowerMockito.mockStatic(AccessTokenValidator.class);
    ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
    ObjectNode userNode = JsonNodeFactory.instance.objectNode();
    userNode.put(JsonKey.USER_ID, "56c2d9a3-fae9-4341-9862-4eeeead2e9a1");
    requestNode.put(JsonKey.REQUEST, userNode);
    Http.RequestBuilder requestBuilder =
        Helpers.fakeRequest(
                Helpers.GET,
                "http://localhost:9000/v1/user/read/56c2d9a3-fae9-4341-9862-4eeeead2e9a1")
            .header(
                "x-authenticated-user-token",
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyZUNvWGlZRHFDbHRaX1F1ZXNRMEhtNkNYVF91emJiN2d3bXlMZXhsN1JnIn0.eyJqdGkiOiIwMTVmNmRlOC1jODRiLTRkNmUtOGRkYy1mNzZmNTk3NTViNjgiLCJleHAiOjE1OTQxMDg0MjUsIm5iZiI6MCwiaWF0IjoxNTk0MDIyMDI1LCJpc3MiOiJodHRwczovL3N0YWdpbmcubnRwLm5ldC5pbi9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiZjo5MzFhOWRjOS00NTk0LTQ4MzktYWExNi1jZjBjYWMwOTYzODE6M2Y0YmYzMTEtOTNkMy00ODY3LTgxMGMtZGViMDYzYjQzNzg5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGFmYzU0YmUtNmZkOS00MDRlLTljYzctN2FkYzYxYzVjYzI1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7fSwibmFtZSI6Ik5hdjIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuYXYyMzMzNyIsImdpdmVuX25hbWUiOiJOYXYyIiwiZW1haWwiOiJuYXYyQHlvcG1haWwuY29tIn0.HAG5Uv7F7J82HCmNsM9NjzMKEW_65nsJX-P_SC5XNfSoz9w5FkQQ4Xlx9elw5vbvtG9UU5Jn5TDMRGAnjdCZ-FMgMv0BLGy3uRKq6Xu6drf6oN9kYMIgTGYuf946EX3pelXQtL6kXwi5_OQ6OQT7Ie94l525BEn09SkeiKJsUrrxShLMlCaX3ERt83MwNdxLkuJ0tI8Jx22leksaf8cxGteC3iF31eLVxIe3ioIexUpbbTI-zBZHHURX_5tAIZvq91kV7Laibngqg4RDluaBltmbBWufFBPAYHqwFRvhix2E78t3d6cb7mx4xRNDrbTJCxHQCL2kE-VXkPGBDEHa3g")
            .header(
                "x-authenticated-for",
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyZUNvWGlZRHFDbHRaX1F1ZXNRMEhtNkNYVF91emJiN2d3bXlMZXhsN1JnIn0.eyJqdGkiOiIwMTVmNmRlOC1jODRiLTRkNmUtOGRkYy1mNzZmNTk3NTViNjgiLCJleHAiOjE1OTQxMDg0MjUsIm5iZiI6MCwiaWF0IjoxNTk0MDIyMDI1LCJpc3MiOiJodHRwczovL3N0YWdpbmcubnRwLm5ldC5pbi9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiZjo5MzFhOWRjOS00NTk0LTQ4MzktYWExNi1jZjBjYWMwOTYzODE6M2Y0YmYzMTEtOTNkMy00ODY3LTgxMGMtZGViMDYzYjQzNzg5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGFmYzU0YmUtNmZkOS00MDRlLTljYzctN2FkYzYxYzVjYzI1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7fSwibmFtZSI6Ik5hdjIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuYXYyMzMzNyIsImdpdmVuX25hbWUiOiJOYXYyIiwiZW1haWwiOiJuYXYyQHlvcG1haWwuY29tIn0.HAG5Uv7F7J82HCmNsM9NjzMKEW_65nsJX-P_SC5XNfSoz9w5FkQQ4Xlx9elw5vbvtG9UU5Jn5TDMRGAnjdCZ-FMgMv0BLGy3uRKq6Xu6drf6oN9kYMIgTGYuf946EX3pelXQtL6kXwi5_OQ6OQT7Ie94l525BEn09SkeiKJsUrrxShLMlCaX3ERt83MwNdxLkuJ0tI8Jx22leksaf8cxGteC3iF31eLVxIe3ioIexUpbbTI-zBZHHURX_5tAIZvq91kV7Laibngqg4RDluaBltmbBWufFBPAYHqwFRvhix2E78t3d6cb7mx4xRNDrbTJCxHQCL2kE-VXkPGBDEHa3g");
    Http.Request req = requestBuilder.build();
    when(tokenValidator.verifyUserToken(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("authorized-user");
    when(tokenValidator.verifyManagedUserToken(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("authorized-user");
    assertEquals(
        (String)
            RequestInterceptor.verifyRequestData(requestBuilder.build(), new HashMap<>())
                .get(JsonKey.USER_ID),
        "authorized-user");
  }

  @Test
  public void testVerifyRequestDataReturingUnAuthorizedUser() {
    tokenValidator = mock(AccessTokenValidator.class);
    Http.RequestBuilder requestBuilder =
        Helpers.fakeRequest(Helpers.GET, "http://localhost:9000/v1/group/read")
            .header(
                "x-authenticated-user-token",
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIyZUNvWGlZRHFDbHRaX1F1ZXNRMEhtNkNYVF91emJiN2d3bXlMZXhsN1JnIn0.eyJqdGkiOiIwMTVmNmRlOC1jODRiLTRkNmUtOGRkYy1mNzZmNTk3NTViNjgiLCJleHAiOjE1OTQxMDg0MjUsIm5iZiI6MCwiaWF0IjoxNTk0MDIyMDI1LCJpc3MiOiJodHRwczovL3N0YWdpbmcubnRwLm5ldC5pbi9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiZjo5MzFhOWRjOS00NTk0LTQ4MzktYWExNi1jZjBjYWMwOTYzODE6M2Y0YmYzMTEtOTNkMy00ODY3LTgxMGMtZGViMDYzYjQzNzg5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGFmYzU0YmUtNmZkOS00MDRlLTljYzctN2FkYzYxYzVjYzI1IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7fSwibmFtZSI6Ik5hdjIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuYXYyMzMzNyIsImdpdmVuX25hbWUiOiJOYXYyIiwiZW1haWwiOiJuYXYyQHlvcG1haWwuY29tIn0.HAG5Uv7F7J82HCmNsM9NjzMKEW_65nsJX-P_SC5XNfSoz9w5FkQQ4Xlx9elw5vbvtG9UU5Jn5TDMRGAnjdCZ-FMgMv0BLGy3uRKq6Xu6drf6oN9kYMIgTGYuf946EX3pelXQtL6kXwi5_OQ6OQT7Ie94l525BEn09SkeiKJsUrrxShLMlCaX3ERt83MwNdxLkuJ0tI8Jx22leksaf8cxGteC3iF31eLVxIe3ioIexUpbbTI-zBZHHURX_5tAIZvq91kV7Laibngqg4RDluaBltmbBWufFBPAYHqwFRvhix2E78t3d6cb7mx4xRNDrbTJCxHQCL2kE-VXkPGBDEHa3g");
    Http.Request req = requestBuilder.build();
    assertEquals(
        (String)
            RequestInterceptor.verifyRequestData(requestBuilder.build(), new HashMap<>())
                .get(JsonKey.USER_ID),
        JsonKey.UNAUTHORIZED);
  }
}
