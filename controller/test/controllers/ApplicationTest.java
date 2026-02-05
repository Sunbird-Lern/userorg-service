package controllers;

import static org.junit.Assert.assertEquals;

import modules.OnRequestHandler;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import play.mvc.Result;

/**
 * Simple (JUnit) tests that can call all parts of a play app. If you are interested in mocking a
 * whole application, see the wiki for more details. extends WithApplication
 */
@PrepareForTest({OnRequestHandler.class})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class ApplicationTest {

  @Test
  public void testGetApiVersionSuccess() {
    String apiPath = "/v1/user/read";
    String version = BaseController.getApiVersion(apiPath);
    assertEquals("v1", version);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateCommonExceptionResponseSuccess() {
    ResponseCode code = ResponseCode.mandatoryParamsMissing;
    code.setResponseCode(ResponseCode.CLIENT_ERROR.getResponseCode());
    Result result = new BaseController().createCommonExceptionResponse(new Exception(), null);
    assertEquals(ResponseCode.OK.getResponseCode(), result.status());
  }
}
