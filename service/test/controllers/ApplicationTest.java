package controllers;

import static org.junit.Assert.assertEquals;

import modules.OnRequestHandler;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

/**
 * Simple (JUnit) tests that can call all parts of a play app. If you are interested in mocking a
 * whole application, see the wiki for more details. extends WithApplication
 */
@PrepareForTest({SunbirdMWService.class, OnRequestHandler.class})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class ApplicationTest {

  @Test
  public void testGetApiVersionSuccess() {
    String apiPath = "/v1/learner/getenrolledcoures";
    String version = BaseController.getApiVersion(apiPath);
    assertEquals("v1", version);
  }

  @Test
  public void testCreateResponseOnExceptionSuccess() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.courseIdRequiredError.getErrorCode(),
            ResponseCode.courseIdRequiredError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    Response response =
        BaseController.createResponseOnException("v1/user/create", "POST", exception);
    assertEquals(ResponseCode.courseIdRequiredError.getErrorCode(), response.getParams().getErr());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateCommonExceptionResponseSuccess() {
    ResponseCode code = ResponseCode.getResponse(ResponseCode.authTokenRequired.getErrorCode());
    code.setResponseCode(ResponseCode.CLIENT_ERROR.getResponseCode());
    Result result = new BaseController().createCommonExceptionResponse(new Exception(), null);
    assertEquals(ResponseCode.OK.getResponseCode(), result.status());
  }
}
