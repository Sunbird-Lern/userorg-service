/** */
package org.sunbird.common.request;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

/** @author Manzarul */
public class PageSectionValidatorTest {

  @Test
  public void testValidateGetPageDataSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SOURCE, "web");
    requestObj.put(JsonKey.PAGE_NAME, "resource");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateGetPageData(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateGetPageDataFailureWithoutSource() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PAGE_NAME, "resource");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateGetPageData(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.sourceRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateGetPageDataFailureWithoutPageName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SOURCE, "web");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateGetPageData(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.pageNameRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateCreateSectionSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SECTION_NAME, "latest resource");
    requestObj.put(
        JsonKey.SECTION_DATA_TYPE, "{\"request\": { \"search\": {\"contentType\": [\"Story\"] }}}");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateCreateSection(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateCreateSectionFailureWithoutSectionName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(
        JsonKey.SECTION_DATA_TYPE, "{\"request\": { \"search\": {\"contentType\": [\"Story\"] }}}");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateCreateSection(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.sectionNameRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateCreateSectionFailureWithoutSectionDataType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SECTION_NAME, "latest resource");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateCreateSection(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.sectionDataTypeRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateUpdateSectionSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SECTION_NAME, "latest resource");
    requestObj.put(
        JsonKey.SECTION_DATA_TYPE, "{\"request\": { \"search\": {\"contentType\": [\"Story\"] }}}");
    requestObj.put(JsonKey.ID, "some section id");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdateSection(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateUpdateSectionFailureWithoutId() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SECTION_NAME, "latest resource");
    requestObj.put(
        JsonKey.SECTION_DATA_TYPE, "{\"request\": { \"search\": {\"contentType\": [\"Story\"] }}}");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdateSection(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.sectionIdRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateUpdateSectionFailureWithoutSectioName() {
    Request request = new Request();
    boolean reqSuccess = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(
        JsonKey.SECTION_DATA_TYPE, "{\"request\": { \"search\": {\"contentType\": [\"Story\"] }}}");
    requestObj.put(JsonKey.ID, "some section id");
    requestObj.put(JsonKey.SECTION_NAME, "");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdateSection(request);
      reqSuccess = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.sectionNameRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, reqSuccess);
  }

  @Test
  public void testValidateUpdateSectionFailureWithoutSectioData() {
    Request request = new Request();
    boolean reqSuccess = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SECTION_NAME, "latest resource");
    requestObj.put(JsonKey.SECTION_DATA_TYPE, "");
    requestObj.put(JsonKey.ID, "some section id");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdateSection(request);
      reqSuccess = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.sectionDataTypeRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, reqSuccess);
  }

  @Test
  public void testValidateCreatePageSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PAGE_NAME, "some page name that need to be build");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateCreatePage(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateCreatePageFailureWithoutPageName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateCreatePage(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.pageNameRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }

  @Test
  public void testValidateUpdatePageSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PAGE_NAME, "some page name that need to be build");
    requestObj.put(JsonKey.ID, "identifier of the page");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdatepage(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals("success", requestObj.get("ext"));
  }

  @Test
  public void testValidateUpdatePageFailureWithoutPageName() {
    boolean reqSuccess = false;
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.ID, "identifier of the page");
    requestObj.put(JsonKey.PAGE_NAME, null);
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdatepage(request);
      reqSuccess = false;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.pageNameRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, reqSuccess);
  }

  @Test
  public void testValidateUpdatePageFailureWithoutId() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.PAGE_NAME, "some page name that need to be build");
    request.setRequest(requestObj);
    try {
      // this method will either throw projectCommonException or it return void
      RequestValidator.validateUpdatepage(request);
      requestObj.put("ext", "success");
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.pageIdRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(null, (String) requestObj.get("ext"));
  }
}
