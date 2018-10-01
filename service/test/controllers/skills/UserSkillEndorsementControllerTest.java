package controllers.skills;

import static controllers.TestUtil.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.DummyActor;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@Ignore
public class UserSkillEndorsementControllerTest {

  private static FakeApplication app;
  private static Map<String, String[]> headerMap;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some Device Id"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
    headerMap.put(
        HeaderParam.X_Authenticated_User_Token.getName(),
        new String[] {"{userId} uuiuhcf784508 8y8c79-fhh"});
    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);
  }

  @Test
  public void testAddSkillEndorsement() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, "{userId} uuiuhcf784508 8y8c79-fhh");
    innerMap.put(JsonKey.ENDORSED_USER_ID, "1223");
    innerMap.put(JsonKey.SKILL_NAME, "C");

    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    Http.RequestBuilder req =
        new Http.RequestBuilder().bodyJson(json).uri("/v1/user/skill/endorse/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }
}
