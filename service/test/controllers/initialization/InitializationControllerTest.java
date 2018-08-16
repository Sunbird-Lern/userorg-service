package controllers.initialization;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import controllers.DummyActor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

/**
 * This class has test case specs for Initialisation Controller
 *
 * @author Loganathan
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class InitializationControllerTest {

  private static FakeApplication app;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);
  }

  @Test
  public void testInitalizeRootOrg() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORG_NAME, "org123");
    innerMap.put(JsonKey.CHANNEL, "123456");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/init/rootOrg").method("POST");
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testInitalizeRootOrgWithoutChannel() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORG_NAME, "org123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/init/rootOrg").method("POST");
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testInitalizeRootOrgWithoutOrgName() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CHANNEL, "123456");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/init/rootOrg").method("POST");
    Result result = route(req);
    assertEquals(400, result.status());
  }

  private static String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(map);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonResp;
  }
}
