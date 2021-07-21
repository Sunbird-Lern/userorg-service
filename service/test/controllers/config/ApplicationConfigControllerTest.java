package controllers.config;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.util.*;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.PropertiesCache;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 6/12/17. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@PrepareForTest(OnRequestHandler.class)
public class ApplicationConfigControllerTest extends BaseApplicationTest {

  private static Map<String, String[]> headerMap;

  @Before
  public void before() {
    setup(DummyActor.class);
  }

  @Test
  public void testupdateSystemSettingsFailure() {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject())).thenReturn(userAuthentication);
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    List<String> list =
        new ArrayList(
            Arrays.asList(
                PropertiesCache.getInstance()
                    .getProperty("system_settings_properties")
                    .split(",")));

    if (list.size() > 0) {
      innerMap.put(list.get(0), list.get(0));
      requestMap.put(JsonKey.REQUEST, innerMap);
      String data = mapToJson(requestMap);

      JsonNode json = Json.parse(data);
      RequestBuilder req =
          new RequestBuilder().bodyJson(json).uri("/v1/system/settings/set").method("POST");
      /*req.headers(headerMap);*/
      Result result = Helpers.route(application, req);
      assertEquals(400, result.status());
    }
  }
}
