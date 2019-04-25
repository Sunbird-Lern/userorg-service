package controllers;

import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.KeyCloakConnectionProvider;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.learner.util.Util;
import org.sunbird.middleware.Application;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.services.sso.impl.KeyCloakServiceImpl;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.AuthenticationHelper;
import util.RequestInterceptor;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  RequestInterceptor.class,
  AuthenticationHelper.class,
  Application.class,
  SSOServiceFactory.class,
  KeyCloakConnectionProvider.class,
  Util.class
})
@SuppressStaticInitializationFor({"util.AuthenticationHelper", "util.Global"})
@PowerMockIgnore("javax.management.*")
// @Ignore
public class BaseControllerTest {

  public static FakeApplication app;
  public static Map<String, String[]> headerMap;
  public static ActorSystem system;
  public static final Props props = Props.create(DummyActor.class);

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Some consumer ID"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some device ID"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Some authenticated user ID"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Some message ID"});
    headerMap.put(HeaderParam.X_APP_ID.getName(), new String[] {"Some app Id"});

    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);
  }

  @Before
  public void mockKeycloakAndCassandra() {
    SSOManager impl = Mockito.mock(KeyCloakServiceImpl.class);
    Keycloak keycloak = Mockito.mock(Keycloak.class);
    PowerMockito.mockStatic(SSOServiceFactory.class);
    PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(impl);
    PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
    try {
      Mockito.when(KeyCloakConnectionProvider.initialiseConnection()).thenReturn(keycloak);
    } catch (Exception e) {
      e.printStackTrace();
    }
    PowerMockito.mockStatic(AuthenticationHelper.class);
    Mockito.when(AuthenticationHelper.verifyUserAccesToken(Mockito.anyString()))
        .thenReturn("userId");
    PowerMockito.mockStatic(RequestInterceptor.class);
    Mockito.when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");

    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(Application.class);
    try {
      PowerMockito.doNothing().when(Util.class, "checkCassandraDbConnections");
      PowerMockito.doNothing().when(Application.class, "checkCassandraConnection");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Result performTest(String url, String method, Map map) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (StringUtils.isNotBlank(data)) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    req.headers(headerMap);
    Result result = route(req);
    return result;
  }

  public String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        ProjectLogger.log(e.getMessage(), e);
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);

      if (response != null) {
        ResponseParams params = response.getParams();
        return params.getStatus();
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseControllerTest:getResponseCode: Exception occurred with error message = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }

  @Test
  public void testGetResponseSizeSuccess() throws UnsupportedEncodingException {
    String jsonResponse =
        "\"{\n"
            + "    \\\"id\\\": \\\"api.org.search\\\",\n"
            + "    \\\"ver\\\": \\\"v1\\\",\n"
            + "    \\\"ts\\\": \\\"2019-04-24 08:56:28:795+0000\\\",\n"
            + "    \\\"params\\\": {\n"
            + "        \\\"resmsgid\\\": null,\n"
            + "        \\\"msgid\\\": \\\"8e27cbf5-e299-43b0-bca7-8347f7e5abcf\\\",\n"
            + "        \\\"err\\\": null,\n"
            + "        \\\"status\\\": \\\"success\\\",\n"
            + "        \\\"errmsg\\\": null\n"
            + "    },\n"
            + "    \\\"responseCode\\\": \\\"OK\\\",\n"
            + "    \\\"result\\\": {\n"
            + "        \\\"response\\\": {\n"
            + "            \\\"count\\\": 1,\n"
            + "            \\\"content\\\": [\n"
            + "                {\n"
            + "                    \\\"dateTime\\\": null,\n"
            + "                    \\\"preferredLanguage\\\": null,\n"
            + "                    \\\"approvedBy\\\": null,\n"
            + "                    \\\"channel\\\": \\\"ORG_001\\\",\n"
            + "                    \\\"description\\\": null,\n"
            + "                    \\\"updatedDate\\\": null,\n"
            + "                    \\\"addressId\\\": null,\n"
            + "                    \\\"orgType\\\": null,\n"
            + "                    \\\"provider\\\": null,\n"
            + "                    \\\"locationId\\\": null,\n"
            + "                    \\\"orgCode\\\": null,\n"
            + "                    \\\"theme\\\": null,\n"
            + "                    \\\"id\\\": \\\"0126322873849692160\\\",\n"
            + "                    \\\"communityId\\\": null,\n"
            + "                    \\\"isApproved\\\": null,\n"
            + "                    \\\"slug\\\": \\\"org_001\\\",\n"
            + "                    \\\"identifier\\\": \\\"0126322873849692160\\\",\n"
            + "                    \\\"thumbnail\\\": null,\n"
            + "                    \\\"orgName\\\": \\\"Sunbird_Framework_Testing\\\",\n"
            + "                    \\\"updatedBy\\\": null,\n"
            + "                    \\\"address\\\": {},\n"
            + "                    \\\"locationIds\\\": [],\n"
            + "                    \\\"externalId\\\": null,\n"
            + "                    \\\"isRootOrg\\\": true,\n"
            + "                    \\\"rootOrgId\\\": \\\"0126322873849692160\\\",\n"
            + "                    \\\"approvedDate\\\": null,\n"
            + "                    \\\"imgUrl\\\": null,\n"
            + "                    \\\"homeUrl\\\": null,\n"
            + "                    \\\"orgTypeId\\\": null,\n"
            + "                    \\\"isDefault\\\": null,\n"
            + "                    \\\"contactDetail\\\": [],\n"
            + "                    \\\"createdDate\\\": \\\"2018-11-12 12:47:22:133+0000\\\",\n"
            + "                    \\\"createdBy\\\": null,\n"
            + "                    \\\"parentOrgId\\\": null,\n"
            + "                    \\\"hashTagId\\\": \\\"0126322873849692160\\\",\n"
            + "                    \\\"noOfMembers\\\": null,\n"
            + "                    \\\"status\\\": 1\n"
            + "                }\n"
            + "            ]\n"
            + "        }\n"
            + "    }\n"
            + "}\"";

    String jsonResponseSize = BaseController.getResponseSize(jsonResponse);
    Assert.assertEquals("2138", jsonResponseSize);
  }

  @Test
  public void testGetResponseSizeFailure() throws UnsupportedEncodingException {
    String jsonResponse = "";
    String jsonResponseSize = BaseController.getResponseSize(jsonResponse);
    Assert.assertEquals("0.0", jsonResponseSize);
  }
}
