/** */
package org.sunbird.validator;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.util.PropertiesCache;
import org.sunbird.validator.orgvalidator.OrgRequestValidator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesCache.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class OrgValidatorTest {

  @Test
  public void validateCreateOrgSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    requestObj.put(JsonKey.ORG_TYPE, "board");
    requestObj.put(JsonKey.IS_TENANT, false);
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateCreateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", requestObj.get("ext"));
  }

  @Test
  public void validateCreateRootOrgWithLicenseSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    requestObj.put(JsonKey.ORG_TYPE, "board");
    requestObj.put(JsonKey.IS_TENANT, false);
    requestObj.put(JsonKey.LICENSE, "Test license");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateCreateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", requestObj.get("ext"));
  }

  @Test
  public void validateCreateRootOrgWithEmptyLicenseFailure() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    requestObj.put(JsonKey.LICENSE, "");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateCreateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNotNull(e);
    }
    assertEquals(requestObj.get("ext"), null);
  }

  @Test
  public void validateCreateOrgWithOutName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateCreateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, requestObj.get("ext"));
  }

  @Test
  public void validateCreateOrgWithRootOrgTrueAndWithOutChannel() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, false);
    requestObj.put(JsonKey.CHANNEL, "");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateCreateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, requestObj.get("ext"));
  }

  @Test
  public void validateUpdateCreateOrgSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.ORGANISATION_ID, "test12344");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateUpdateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", requestObj.get("ext"));
  }

  @Test
  public void validateUpdateOrgFailure() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORGANISATION_ID, "test2344");
    requestObj.put(JsonKey.ROOT_ORG_ID, "");
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateUpdateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.invalidParameterValue.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, requestObj.get("ext"));
  }

  @Test
  public void validateUpdateOrgWithStatus() {
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");

    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.STATUS, "true");
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "tpp");
    requestObj.put(JsonKey.ORGANISATION_ID, "test123444");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateUpdateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.invalidRequestParameter.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, requestObj.get("ext"));
  }

  @Test
  public void validateUpdateOrgWithEmptyChannel() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.IS_TENANT, true);
    requestObj.put(JsonKey.CHANNEL, "");
    requestObj.put(JsonKey.ORGANISATION_ID, "test123444");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateUpdateOrgRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.dependentParamsMissing.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, requestObj.get("ext"));
  }

  @Test
  public void validateUpdateOrgStatus() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.STATUS, 2);
    requestObj.put(JsonKey.ORGANISATION_ID, "test-12334");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateUpdateOrgStatusRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", requestObj.get("ext"));
  }

  @Test
  public void validateUpdateOrgStatusWithInvalidStatus() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ORG_NAME, "test");
    requestObj.put(JsonKey.STATUS, "true");
    requestObj.put(JsonKey.ORGANISATION_ID, "test-12334");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      new OrgRequestValidator().validateUpdateOrgStatusRequest(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.invalidRequestData.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, requestObj.get("ext"));
  }
}
