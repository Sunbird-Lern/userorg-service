package controllers.usermanagement;

import static org.junit.Assert.*;

import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
import java.util.*;
import modules.OnRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.HeaderParam;
import play.mvc.Result;
import util.ACTORS;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@Ignore
public class IdentifierFreeUpControllerTest extends BaseApplicationTest {

  public static Map<String, List<String>> headerMap;

  @Before
  public void before() {
    setup(ACTORS.IDENTIFIER_FREE_UP_ACTOR, DummyActor.class);
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), Arrays.asList("Some consumer ID"));
    headerMap.put(HeaderParam.X_Device_ID.getName(), Arrays.asList("Some device ID"));
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), Arrays.asList("Some authenticated user ID"));
    headerMap.put(JsonKey.MESSAGE_ID, Arrays.asList("Some message ID"));
    headerMap.put(HeaderParam.X_APP_ID.getName(), Arrays.asList("Some app Id"));
  }

  @After
  public void tearDown() throws Exception {
    headerMap.clear();
  }

  @Test
  public void tesIdentifierFreeUpUserSuccess() {
    Result result =
        TestUtil.performTest(
            "/private/user/v1/identifier/freeup", "POST", getRequest(), application);
    assertEquals(ResponseCode.SUCCESS.name(), TestUtil.getResponseCode(result));
  }

  @Test
  public void tesIdentifierFreeUpUserFailure() {
    Result result =
        TestUtil.performTest(
            "/private/user/v1/identifier/freeup", "POST", getFailureReq(), application);
    assertEquals(ResponseCode.CLIENT_ERROR.name(), TestUtil.getResponseCode(result));
  }

  private Map<String, Object> getRequest() {
    List<String> identifiers = new ArrayList<>();
    identifiers.add(JsonKey.EMAIL);
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "userId");
    reqMap.put(JsonKey.IDENTIFIER, identifiers);
    request.put(JsonKey.REQUEST, reqMap);
    return request;
  }

  private Map<String, Object> getFailureReq() {
    List<String> identifiers = new ArrayList<>();
    identifiers.add(JsonKey.EMAIL);
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.IDENTIFIER, identifiers);
    request.put(JsonKey.REQUEST, reqMap);
    return request;
  }
}
