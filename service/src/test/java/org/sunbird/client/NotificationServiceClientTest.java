package org.sunbird.client;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.PropertiesCache;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesCache.class, HttpClientUtil.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class NotificationServiceClientTest {

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString()))
        .thenReturn("http://localhost:9000/")
        .thenReturn("v2/send");
    PowerMockito.mockStatic(HttpClientUtil.class);
    when(HttpClientUtil.post(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(
            "{\"id\":\"api.notification.send\",\"ver\":\"v2\",\"ts\":\"2022-08-04 03:11:03:589+0000\",\"params\":{\"resmsgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"msgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"err\":null,\"status\":\"SUCCESS\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":\"SUCCESS\"}}")
        .thenReturn(
            "{\"id\":\"api.notification.delete\",\"ver\":\"v2\",\"ts\":\"2022-08-04 03:11:03:589+0000\",\"params\":{\"resmsgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"msgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"err\":null,\"status\":\"SUCCESS\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":\"SUCCESS\"}}");
    when(HttpClientUtil.patch(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(
            "{\"id\":\"api.notification.update\",\"ver\":\"v2\",\"ts\":\"2022-08-04 03:11:03:589+0000\",\"params\":{\"resmsgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"msgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"err\":null,\"status\":\"SUCCESS\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":\"SUCCESS\"}}");
    when(HttpClientUtil.get(
            Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(
            "{\"id\":\"api.notification.update\",\"ver\":\"v2\",\"ts\":\"2022-08-04 03:11:03:589+0000\",\"params\":{\"resmsgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"msgid\":\"324c576b75ae778630ec31e8af0bc09f\",\"err\":null,\"status\":\"SUCCESS\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"notification\":\"some notification\"}}");
  }

  @Test
  public void testSendSyncV2Notification() {
    NotificationServiceClient client = new NotificationServiceClient();
    Response response = client.sendSyncV2Notification(new Request(), new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void testUpdateV1Notification() {
    NotificationServiceClient client = new NotificationServiceClient();
    Response response = client.updateV1Notification(new Request(), new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void testReadV1Notification() {
    NotificationServiceClient client = new NotificationServiceClient();
    Response response = client.readV1Notification(new Request(), new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void testDeleteV1Notification() {
    NotificationServiceClient client = new NotificationServiceClient();
    Response response = client.deleteV1Notification(new Request(), new RequestContext());
    Assert.assertNotNull(response);
  }
}
