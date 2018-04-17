package controllers.datapersistence;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import controllers.DummyActor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.learner.util.Util;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 5/12/17. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class DbOperationControllerTest {

  private static FakeApplication app;
  private static Map<String, String[]> headerMap;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);
  /* private static List<String> tableList = null;
  private static CassandraConnectionManager manager = CassandraConnectionMngrFactory
      .getObject(PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE));*/
  private static String entityName = null;
  private static final String PAYLOAD = "payload";
  private static final String ENTITY_NAME = "entityName";
  private static final String INDEXED = "indexed";
  private static final String REQUIRED_FIELDS = "requiredFields";

  @BeforeClass
  public static void startApp() {
    // createtableList();
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD_PLUGIN);
    entityName = "announcement";
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some Device Id"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});

    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);
  }

  /*private static void createtableList(){
    try{
      tableList = manager.getTableList(JsonKey.SUNBIRD_PLUGIN);
    }catch (Exception e) {
      ProjectLogger.log("Error occured" + e.getMessage(), e);
    }
  }*/

  @Test
  public void testAll() {
    testCreate();
    testCreateWithWrongEntityName();
    testupdate();
    testread();
    testreadAll();
    testsearch();
    testdelete();

    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    List<String> requiredFields = new ArrayList<>();
    requiredFields.add("userid");
    innerMap.put(REQUIRED_FIELDS, requiredFields);
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, "usergt78y4ry85464");
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/search").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  // @Test
  public void testCreate() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    innerMap.put(INDEXED, true);
    Map<String, Object> payLoad = new HashMap<>();
    payLoad.put(JsonKey.USER_ID, "usergt78y4ry8");
    payLoad.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    innerMap.put(PAYLOAD, payLoad);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
  }

  public void testCreateWithWrongEntityName() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName + "-wrong");
    innerMap.put(INDEXED, true);
    Map<String, Object> payLoad = new HashMap<>();
    payLoad.put(JsonKey.USER_ID, "usergt78y4ry8");
    payLoad.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    innerMap.put(PAYLOAD, payLoad);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  // @Test
  public void testupdate() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    innerMap.put(INDEXED, true);
    Map<String, Object> payLoad = new HashMap<>();
    payLoad.put(JsonKey.USER_ID, "usergt78y4ry85464");
    payLoad.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    innerMap.put(PAYLOAD, payLoad);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/update").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  // @Test
  public void testdelete() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    innerMap.put(INDEXED, true);
    innerMap.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    /*Map<String , Object> payLoad = new HashMap<>();
    payLoad.put(JsonKey.USER_ID , "usergt78y4ry8");
    payLoad.put(JsonKey.ID , "ggudy8d8ydyy8ddy9");
    innerMap.put(PAYLOAD , payLoad);*/
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/delete").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  // @Test
  public void testread() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    // innerMap.put(INDEXED , true);
    // innerMap.put(JsonKey.USER_ID , "usergt78y4ry8");
    innerMap.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    /*Map<String , Object> payLoad = new HashMap<>();
    payLoad.put(JsonKey.USER_ID , "usergt78y4ry8");
    payLoad.put(JsonKey.ID , "ggudy8d8ydyy8ddy9");
    innerMap.put(PAYLOAD , payLoad);*/
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/object/read").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  // @Test
  public void testreadAll() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    innerMap.put(INDEXED, true);

    /*Map<String , Object> payLoad = new HashMap<>();
    payLoad.put(JsonKey.USER_ID , "usergt78y4ry8");
    payLoad.put(JsonKey.ID , "ggudy8d8ydyy8ddy9");
    innerMap.put(PAYLOAD , payLoad);*/
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/read/list").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  public void testsearch() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    List<String> requiredFields = new ArrayList<>();
    requiredFields.add("userid");
    innerMap.put(REQUIRED_FIELDS, requiredFields);
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, "usergt78y4ry85464");
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/search").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  public void testgetMetrics() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(ENTITY_NAME, entityName);
    List<String> requiredFields = new ArrayList<>();
    requiredFields.add("userid");
    innerMap.put(REQUIRED_FIELDS, requiredFields);
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, "usergt78y4ry85464");
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.ID, "ggudy8d8ydyy8ddy9");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/object/metrics").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
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
