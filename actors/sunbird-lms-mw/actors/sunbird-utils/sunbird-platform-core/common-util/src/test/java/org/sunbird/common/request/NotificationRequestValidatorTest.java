/** */
package org.sunbird.common.request;

import static org.junit.Assert.assertEquals;

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
public class NotificationRequestValidatorTest {

  @Test
  public void validateSendNotificationSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TO, "test");
    requestObj.put(JsonKey.TYPE, "FCM");
    Map<String, Object> data = new HashMap<>();
    data.put("url", "www.google.com");
    requestObj.put(JsonKey.DATA, data);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateSendNotificationWithOutTOParam() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TYPE, "FCM");
    Map<String, Object> data = new HashMap<>();
    data.put("url", "www.google.com");
    requestObj.put(JsonKey.DATA, data);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidTopic.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateSendNotificationWithOutType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TO, "test");
    Map<String, Object> data = new HashMap<>();
    data.put("url", "www.google.com");
    requestObj.put(JsonKey.DATA, data);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidNotificationType.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateSendNotificationWithWrongType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TO, "test");
    requestObj.put(JsonKey.TYPE, "GCM");
    Map<String, Object> data = new HashMap<>();
    data.put("url", "www.google.com");
    requestObj.put(JsonKey.DATA, data);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.notificationTypeSupport.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateSendNotificationWithEmptyData() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TO, "test");
    requestObj.put(JsonKey.TYPE, "FCM");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidTopicData.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateSendNotificationWithWrongObjectType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TO, "test");
    requestObj.put(JsonKey.TYPE, "FCM");
    List<String> data = new ArrayList<String>();
    data.add("www.google.com");
    requestObj.put(JsonKey.DATA, data);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidTopicData.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateSendNotificationWithEmptyDataMap() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.TO, "test");
    requestObj.put(JsonKey.TYPE, "FCM");
    Map<String, Object> data = new HashMap<>();
    requestObj.put(JsonKey.DATA, data);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateSendNotification(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidTopicData.getErrorCode(), e.getCode());
    }
  }
}
