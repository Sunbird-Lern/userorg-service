package controllers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

/**
 * Simple (JUnit) tests that can call all parts of a play app. If you are interested in mocking a
 * whole application, see the wiki for more details. extends WithApplication
 */
public class ApplicationTest {

  @Test
  public void checkGetApiVersion() {
    String apiPath = "/v1/learner/getenrolledcoures";
    String version = BaseController.getApiVersion(apiPath);
    assertEquals("v1", version);
  }

  @Test
  public void checkCreateRequestParam() {
    ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
    code.setResponseCode(ResponseCode.OK.getResponseCode());
    ResponseParams params = BaseController.createResponseParamObj(code);
    assertEquals(ResponseCode.success.name(), params.getStatus());
  }

  // @Test
  public void checkExceptionResponse() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.courseIdRequiredError.getErrorCode(),
            ResponseCode.courseIdRequiredError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    Response response = BaseController.createResponseOnException(null, exception);
    assertEquals(ResponseCode.courseIdRequiredError.getErrorCode(), response.getParams().getErr());
  }

  // @Test
  public void testSuccessResponse() {
    Response response = new Response();
    response = BaseController.createSuccessResponse(null, response);
    assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void checkFailureResponse() {
    ResponseCode code = ResponseCode.getResponse(ResponseCode.authTokenRequired.getErrorCode());
    code.setResponseCode(ResponseCode.CLIENT_ERROR.getResponseCode());
    ResponseParams params = BaseController.createResponseParamObj(code);
    assertEquals(ResponseCode.authTokenRequired.name(), params.getStatus());
  }

  @Test(expected = RuntimeException.class)
  public void testcreateCommonExceptionResponse() {
    ResponseCode code = ResponseCode.getResponse(ResponseCode.authTokenRequired.getErrorCode());
    code.setResponseCode(ResponseCode.CLIENT_ERROR.getResponseCode());
    Result result = new BaseController().createCommonExceptionResponse(new Exception(), null);
    assertEquals(ResponseCode.OK.getResponseCode(), result.status());
  }
}
