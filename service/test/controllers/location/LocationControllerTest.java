package controllers.location;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** @author arvind on 19/4/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(OnRequestHandler.class)
public class LocationControllerTest extends BaseApplicationTest {

  private static Map<String, String[]> headerMap;
  private static final String LOCATION_NAME = "12345";
  private static final String LOCATION_CODE = "LOC_01";
  private static final String LOCATION_TYPE = "State";
  private static final String LOCATION_ID = "123";
  private static final String PARENT_ID = "1a234bc5-dee6-78f9-01g2-h3ij456k7890";
  private static final String CREATE_LOCATION_URL = "/v1/location/create";
  private static final String UPDATE_LOCATION_URL = "/v1/location/update";
  private static final String DELETE_LOCATION_URL = "/v1/location/delete";
  private static final String SEARCH_LOCATION_URL = "/v1/location/search";

  @Before
  public void before() {
    setup(DummyActor.class);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(
        HeaderParam.X_Authenticated_User_Token.getName(),
        new String[] {"Authenticated user token"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
  }

  @Test
  public void testCreateLocation() {
    Map userAuthentication = new HashMap<String, String>();  //create new hashmap - userAuthentication
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");   //giving the userid as key value pair for user-authentication
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject())).thenReturn(userAuthentication);  // authenticate the user
    Map<String, Object> requestMap = new HashMap<>();  //created new hashmap - requestMap
    Map<String, Object> locationData = new HashMap<>();     //create new Hashmap - locationData
    locationData.put(JsonKey.NAME, LOCATION_NAME);    // added the location name into locationData as name
    locationData.put(JsonKey.CODE, LOCATION_CODE);   //added the location code into locationData as code
    locationData.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);    //added the location type into locationData as location_type
    requestMap.put(JsonKey.REQUEST, locationData);      // passed the locationData to requestMap as request
    String data = TestUtil.mapToJson(requestMap);     // convert the requestMap value into Json and moved to data parameter
    JsonNode json = Json.parse(data);   // data is converted into javascript object and added to json parameter
    RequestBuilder req =      // creating the request with url and method as post along with the json body
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);   // passing the request
    assertEquals(200, result.status());   //checking whether the result.status and expected are 200
  }

  @Test
  public void testCreateLocationWithoutType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    requestMap.put(JsonKey.REQUEST, locationData);   //passes only name and code
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateLocationWithoutName() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    locationData.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateLocationWithValidParentId() {

    Map userAuthentication = new HashMap<String, String>();
    userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME,LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    locationData.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    locationData.put(GeoLocationJsonKey.PARENT_ID,PARENT_ID);   //not sure with geolocationjsonkey or jsonkey
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateLocationWithoutCode() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME,LOCATION_NAME);
    locationData.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }


  @Test
  public void testUpdateLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, LOCATION_NAME);
    requestBody.put(JsonKey.CODE, LOCATION_CODE);
    requestBody.put(JsonKey.ID, LOCATION_ID);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithoutId() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, LOCATION_NAME);
    requestBody.put(JsonKey.CODE, LOCATION_CODE);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test    //can avoid this case
  public void testUpdateLocationWithoutNameOrCode() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.CODE, LOCATION_CODE);
    requestBody.put(JsonKey.ID, LOCATION_ID);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }


  @Test
  public void testUpdateLocationWithType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.ID, LOCATION_ID);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testDeleteLocation() {
    RequestBuilder req =
        new RequestBuilder().uri(DELETE_LOCATION_URL + "/" + LOCATION_ID).method("DELETE");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, LOCATION_NAME);
    filters.put(JsonKey.CODE, LOCATION_CODE);
    filters.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }
}
