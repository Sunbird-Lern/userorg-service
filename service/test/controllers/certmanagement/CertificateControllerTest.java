package controllers.certmanagement;

import static org.junit.Assert.*;

import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
import java.util.*;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;
import play.test.Helpers;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class CertificateControllerTest extends BaseApplicationTest {

  public static Map<String, List<String>> headerMap;

  @Before
  public void before() {
    setup(DummyActor.class);
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
  public void tesCertificateDownloadSuccess() {
    Result result =
        TestUtil.performTest("/v1/user/certs/download", "POST", getRequest(), application);
    System.out.println("dfkd" + Helpers.contentAsString(result));
    assertEquals(
        ResponseCode.success.getErrorCode().toLowerCase(), TestUtil.getResponseCode(result));
  }

  @Test
  public void testCertificateDownloadFailure() {
    Result result =
        TestUtil.performTest("/v1/user/certs/download", "POST", getFailureReq(), application);
    assertEquals(
        ResponseCode.mandatoryParamsMissing.getErrorCode(), TestUtil.getResponseCode(result));
  }

  @Test
  public void tesCertificateValidateSuccess() {
    Result result =
        TestUtil.performTest(
            "/private/user/v1/certs/validate", "POST", getValidateReq(null), application);
    System.out.println("dfkd" + Helpers.contentAsString(result));
    assertEquals(
        ResponseCode.success.getErrorCode().toLowerCase(), TestUtil.getResponseCode(result));
  }

  @Test
  public void tesCertificateValidateFailure() {
    Result result =
        TestUtil.performTest(
            "/private/user/v1/certs/validate",
            "POST",
            getValidateReq(JsonKey.CERT_ID),
            application);
    System.out.println("dfkd" + Helpers.contentAsString(result));
    assertEquals(
        ResponseCode.mandatoryParamsMissing.getErrorCode(), TestUtil.getResponseCode(result));
  }

  private Map<String, Object> getRequest() {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put("pdfUrl", "pdf_url");
    request.put(JsonKey.REQUEST, reqMap);
    return request;
  }

  private Map<String, Object> getFailureReq() {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.PDF_URL, StringUtils.EMPTY);
    request.put(JsonKey.REQUEST, reqMap);
    return request;
  }

  private Map<String, Object> getValidateReq(String param) {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ACCESS_CODE, "pdf_url");
    reqMap.put(JsonKey.CERT_ID, "certificateId");
    reqMap.remove(param);
    request.put(JsonKey.REQUEST, reqMap);
    return request;
  }
}
