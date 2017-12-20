import static org.junit.Assert.assertEquals;
import static play.test.Helpers.GET;
import static play.test.Helpers.route;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;

//@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntegrationTest {

	  public static FakeApplication app;
	  @Mock
	  private Http.Request request;
      private static Map<String,String[]> headerMap;
	  
	  @BeforeClass
	  public static void startApp() {
	      app = Helpers.fakeApplication();
	      Helpers.start(app);
          headerMap = new HashMap<String, String[]>();
          headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[]{"Service test consumer"});
          headerMap.put(HeaderParam.X_Device_ID.getName(), new String[]{"Some Device Id"});
          headerMap.put(HeaderParam.X_Authenticated_Userid.getName(), new String[]{"Authenticated user id"});
          headerMap.put(JsonKey.MESSAGE_ID, new String[]{"Unique Message id"});
	  }

	  @Before
	  public void setUp() throws Exception {
	      Map<String, String> flashData = Collections.emptyMap();
	  }
	 
	  @AfterClass
	  public static void stopApp() {

	  }
}
