package util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Common.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
public class PrintEntryExitLogTest {
  @Test
  public void testPrintExitLogOnFailure() {
    try {
      ResponseParams params = new ResponseParams();
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.serverError,
              ResponseCode.serverError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      ResponseCode code = exception.getResponseCode();
      params.setErr(code.getErrorCode());
      params.setErrmsg(code.getErrorMessage());
      params.setStatus(JsonKey.FAILED);
      params.setMsgid("123-456-789");
      PowerMockito.mockStatic(Common.class);
      PowerMockito.when(
              Common.createResponseParamObj(
                  Mockito.any(ResponseCode.class), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(params);
      Request request = new Request();
      request.getContext().put(JsonKey.METHOD, "POST");
      request.getContext().put(JsonKey.URL, "/private/user/v1/lookup");
      request.setOperation("searchUser");
      RequestContext requestContext = new RequestContext();
      requestContext.setReqId("123-456-789");
      request.setRequestContext(requestContext);
      PrintEntryExitLog.printExitLogOnFailure(request, null);
      Assert.assertNotNull(request);
    } catch (Exception e) {
      Assert.assertNull(e);
    }
  }

  @Test
  public void testPrintExitLogOnSuccess() {
    try {
      Request request = new Request();
      request.getContext().put(JsonKey.METHOD, "GET");
      request.setOperation("healthCheck");
      RequestContext requestContext = new RequestContext();
      requestContext.setReqId("123-456-789");
      request.setRequestContext(requestContext);
      PrintEntryExitLog.printExitLogOnSuccessResponse(request, new Response());
      Assert.assertNotNull(request);
    } catch (Exception e) {
      Assert.assertNull(e);
    }
  }
}
