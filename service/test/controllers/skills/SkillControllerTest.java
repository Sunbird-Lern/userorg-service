package controllers.skills;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.DummyActor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

/**
 * Created by arvind on 30/11/17.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class SkillControllerTest {

  public static FakeApplication app;
  @Mock
  private Http.Request request;
  private static Map<String,String[]> headerMap;
  static ActorSystem system;
  final static Props props = Props.create(DummyActor.class);
  static ActorRef subject ;

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[]{"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[]{"Some Device Id"});
    headerMap.put(HeaderParam.X_Authenticated_Userid.getName(), new String[]{"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[]{"Unique Message id"});

    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    //BaseController.setActorRef(subject);
  }

  @Test
  public void testAddSkill() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when( RequestInterceptor.verifyRequestData(Mockito.anyObject()) ).thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");

    Map<String , Object> requestMap = new HashMap<>();
    Map<String , Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID , "7687584");
    requestMap.put(JsonKey.REQUEST , innerMap);
    String data = mapToJson(requestMap);
    System.out.println(data);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/skill/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetUserSkill() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when( RequestInterceptor.verifyRequestData(Mockito.anyObject()) ).thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");

    Map<String , Object> requestMap = new HashMap<>();
    Map<String , Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID , "7687584");
    requestMap.put(JsonKey.REQUEST , innerMap);
    String data = mapToJson(requestMap);
    System.out.println(data);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/skill/read").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetSkills() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when( RequestInterceptor.verifyRequestData(Mockito.anyObject()) ).thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");


    RequestBuilder req = new RequestBuilder().uri("/v1/skills").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }


  private static String mapToJson(Map map){
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(map);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return jsonResp;
  }


}
