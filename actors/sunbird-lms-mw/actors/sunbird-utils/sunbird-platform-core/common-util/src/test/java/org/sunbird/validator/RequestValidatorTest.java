/** */
package org.sunbird.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

/** @author Manzarul */
public class RequestValidatorTest {

  @Test
  public void testValidateFileUploadFailureWithoutContainerName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.CONTAINER, "");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateFileUpload(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.storageContainerNameMandatory.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateSendEmailSuccess() {
    boolean response = false;
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SUBJECT, "test123");
    requestObj.put(JsonKey.BODY, "test");
    List<String> data = new ArrayList<>();
    data.add("test123@gmail.com");
    requestObj.put(JsonKey.RECIPIENT_EMAILS, data);
    requestObj.put(JsonKey.RECIPIENT_USERIDS, new ArrayList<>());
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendMail(request);
      response = true;
    } catch (ProjectCommonException e) {

    }
    assertTrue(response);
  }

  @Test
  public void testValidateSendMailFailureWithNullRecipients() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SUBJECT, "test123");
    requestObj.put(JsonKey.BODY, "test");
    requestObj.put(JsonKey.RECIPIENT_EMAILS, null);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendMail(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.mandatoryParamsMissing.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateSendMailFailureWithEmptyBody() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SUBJECT, "test123");
    requestObj.put(JsonKey.BODY, "");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendMail(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailBodyError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateSendMailFailureWithEmptySubject() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.SUBJECT, "");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendMail(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.emailSubjectError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testValidateSyncRequestSuccess() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.OPERATION_FOR, "keycloak");
    requestObj.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    request.setRequest(requestObj);
    boolean response = false;
    try {
      RequestValidator.validateSyncRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testValidateSyncRequestFailureWithNullObjectType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.OPERATION_FOR, "not keycloack");
    requestObj.put(JsonKey.OBJECT_TYPE, null);
    request.setRequest(requestObj);
    boolean response = false;
    try {
      RequestValidator.validateSyncRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dataTypeError.getErrorCode(), e.getCode());
    }
    Assert.assertFalse(response);
  }

  @Test
  public void testValidateSyncRequestFailureWithInvalidObjectType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.OPERATION_FOR, "not keycloack");
    List<String> objectLsit = new ArrayList<>();
    objectLsit.add("testval");
    requestObj.put(JsonKey.OBJECT_TYPE, objectLsit);
    request.setRequest(requestObj);
    boolean response = false;
    try {
      RequestValidator.validateSyncRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidObjectType.getErrorCode(), e.getCode());
    }
    Assert.assertFalse(response);
  }
}
