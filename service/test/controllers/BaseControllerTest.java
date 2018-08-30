package controllers;

import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.common.request.HeaderParam;
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
}
