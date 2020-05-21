/** */
package org.sunbird.common.models.util.fcm;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;

/**
 * Test cases for FCM notification service.
 *
 * @author Manzarul
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  HttpClients.class,
  URL.class,
  BufferedReader.class,
  HttpUtil.class,
  System.class,
  Notification.class
})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class FCMNotificationTest {

  @Test
  public void testSendNotificationSuccessWithListAndStringData() {
    Map<String, Object> map = new HashMap<>();
    map.put("title", "some title");
    map.put("summary", "some value");
    List<Object> list = new ArrayList<>();
    list.add("test12");
    list.add("test45");
    map.put("extra", list);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("title", "some value");
    innerMap.put("link", "https://google.com");
    map.put("map", innerMap);

    String val = Notification.sendNotification("nameOFTopic", map, Notification.FCM_URL);
    Assert.assertNotEquals(JsonKey.FAILURE, val);
  }

  @Test
  public void testSendNotificationSuccessWithStringData() {
    Map<String, Object> map = new HashMap<>();
    map.put("title", "some title");
    map.put("summary", "some value");
    String val = Notification.sendNotification("nameOFTopic", map, Notification.FCM_URL);
    Assert.assertNotEquals(JsonKey.FAILURE, val);
  }

  @Test
  public void testSendNotificationFailureWithEmptyFcmUrl() {
    Map<String, Object> map = new HashMap<>();
    map.put("title", "some title");
    map.put("summary", "some value");
    String val = Notification.sendNotification("nameOFTopic", map, "");
    Assert.assertEquals(JsonKey.FAILURE, val);
  }

  @Test
  public void testSendNotificationFailureWithNullData() {
    Map<String, Object> map = null;
    String val = Notification.sendNotification("nameOFTopic", map, "");
    Assert.assertEquals(JsonKey.FAILURE, val);
  }

  @Test
  public void testSendNotificationFailureWithEmptyTopic() {
    Map<String, Object> map = new HashMap<>();
    map.put("title", "some title");
    map.put("summary", "some value");
    String val = Notification.sendNotification("", map, "");
    Assert.assertEquals(JsonKey.FAILURE, val);
  }

  @Before
  public void addMockRules() {
    PowerMockito.mockStatic(System.class);
    URL url = mock(URL.class);
    HttpURLConnection connection = mock(HttpURLConnection.class);
    OutputStream outStream = mock(OutputStream.class);
    InputStream inStream = mock(InputStream.class);
    BufferedReader reader = mock(BufferedReader.class);
    try {
      when(System.getenv(JsonKey.SUNBIRD_FCM_ACCOUNT_KEY)).thenReturn("FCM_KEY");
      when(System.getenv(AdditionalMatchers.not(Mockito.eq(JsonKey.SUNBIRD_FCM_ACCOUNT_KEY))))
          .thenCallRealMethod();

      whenNew(URL.class).withAnyArguments().thenReturn(url);
      when(url.openConnection()).thenReturn(connection);
      when(connection.getOutputStream()).thenReturn(outStream);
      when(connection.getInputStream()).thenReturn(inStream);
      whenNew(BufferedReader.class).withAnyArguments().thenReturn(reader);
      when(reader.readLine()).thenReturn("{\"" + JsonKey.MESSAGE_Id + "\": 123}", (String) null);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Mock rules addition failed " + e.getMessage());
    }
  }
}
