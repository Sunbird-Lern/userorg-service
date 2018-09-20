package controllers;

import static controllers.TestUtil.mapToJson;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

public class BaseControllerTest {
  private static FakeApplication app;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);
  public static Map<String, String[]> headerMap;

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);
  }

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
  }

  public Result performTest(String url, String method, Map map) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (data != null) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    req.headers(headerMap);
    Result result = route(req);
    return result;
    //    assertEquals(expectedStatusCode, result.status());
  }
}
