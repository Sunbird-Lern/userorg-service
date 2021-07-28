package org.sunbird.http;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@PrepareForTest({
  HttpClients.class,
  CloseableHttpClient.class,
  ConnectionKeepAliveStrategy.class,
  PoolingHttpClientConnectionManager.class,
  CloseableHttpResponse.class,
  HttpGet.class,
  HttpPost.class,
  UrlEncodedFormEntity.class,
  HttpPatch.class,
  EntityUtils.class
})
public class HttpClientUtilTest {

  private Map<String, String> headers() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return headers;
  }

  @Test
  public void testGetFailure() throws IOException {
    PowerMockito.mockStatic(HttpClients.class);
    HttpClientBuilder clientBuilder = PowerMockito.mock(HttpClientBuilder.class);
    CloseableHttpClient httpclient = PowerMockito.mock(CloseableHttpClient.class);
    PowerMockito.when(HttpClients.custom()).thenReturn(clientBuilder);
    PowerMockito.when(clientBuilder.build()).thenReturn(httpclient);
    CloseableHttpResponse response = PowerMockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = PowerMockito.mock(StatusLine.class);
    PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(400);
    HttpEntity entity = PowerMockito.mock(HttpEntity.class);
    PowerMockito.when(response.getEntity()).thenReturn(entity);
    PowerMockito.mockStatic(EntityUtils.class);
    byte[] bytes = "{\"message\":\"success\"}".getBytes();
    PowerMockito.when(EntityUtils.toByteArray(Mockito.any(HttpEntity.class))).thenReturn(bytes);
    PowerMockito.when(httpclient.execute(Mockito.any(HttpGet.class))).thenReturn(response);
    HttpClientUtil.getInstance();
    String res = HttpClientUtil.get("http://localhost:80/user/read", headers());
    assertNotNull(res);
  }

  @Test
  public void testPostSuccess() throws IOException {
    PowerMockito.mockStatic(HttpClients.class);
    HttpClientBuilder clientBuilder = PowerMockito.mock(HttpClientBuilder.class);
    CloseableHttpClient httpclient = PowerMockito.mock(CloseableHttpClient.class);
    PowerMockito.when(HttpClients.custom()).thenReturn(clientBuilder);
    PowerMockito.when(clientBuilder.build()).thenReturn(httpclient);
    CloseableHttpResponse response = PowerMockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = PowerMockito.mock(StatusLine.class);
    PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
    HttpEntity entity = PowerMockito.mock(HttpEntity.class);
    PowerMockito.when(response.getEntity()).thenReturn(entity);
    PowerMockito.mockStatic(EntityUtils.class);
    byte[] bytes = "{\"message\":\"success\"}".getBytes();
    PowerMockito.when(EntityUtils.toByteArray(Mockito.any(HttpEntity.class))).thenReturn(bytes);
    PowerMockito.when(httpclient.execute(Mockito.any(HttpPost.class))).thenReturn(response);
    HttpClientUtil.getInstance();
    String res =
        HttpClientUtil.post(
            "http://localhost:80/user/read", "{\"message\":\"success\"}", headers());
    assertNotNull(res);
  }

  @Test
  public void testPostFormSuccess() throws IOException {
    PowerMockito.mockStatic(HttpClients.class);
    HttpClientBuilder clientBuilder = PowerMockito.mock(HttpClientBuilder.class);
    CloseableHttpClient httpclient = PowerMockito.mock(CloseableHttpClient.class);
    PowerMockito.when(HttpClients.custom()).thenReturn(clientBuilder);
    PowerMockito.when(clientBuilder.build()).thenReturn(httpclient);
    CloseableHttpResponse response = PowerMockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = PowerMockito.mock(StatusLine.class);
    PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
    HttpEntity entity = PowerMockito.mock(HttpEntity.class);
    PowerMockito.when(response.getEntity()).thenReturn(entity);
    PowerMockito.mockStatic(EntityUtils.class);
    byte[] bytes = "{\"message\":\"success\"}".getBytes();
    PowerMockito.when(EntityUtils.toByteArray(Mockito.any(HttpEntity.class))).thenReturn(bytes);
    PowerMockito.when(httpclient.execute(Mockito.any(HttpPost.class))).thenReturn(response);
    Map<String, String> fields = new HashMap<>();
    fields.put("message", "success");
    HttpClientUtil.getInstance();
    String res = HttpClientUtil.postFormData("http://localhost:80/user/read", fields, headers());
    assertNotNull(res);
  }

  @Test
  public void testPatchFailure() throws Exception {
    PowerMockito.mockStatic(HttpClients.class);
    HttpClientBuilder clientBuilder = PowerMockito.mock(HttpClientBuilder.class);
    CloseableHttpClient httpclient = PowerMockito.mock(CloseableHttpClient.class);
    PowerMockito.when(HttpClients.custom()).thenReturn(clientBuilder);
    PowerMockito.when(clientBuilder.build()).thenReturn(httpclient);
    CloseableHttpResponse response = PowerMockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = PowerMockito.mock(StatusLine.class);
    PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(400);
    HttpEntity entity = PowerMockito.mock(HttpEntity.class);
    PowerMockito.when(response.getEntity()).thenReturn(entity);
    PowerMockito.mockStatic(EntityUtils.class);
    byte[] bytes = "{\"message\":\"success\"}".getBytes();
    PowerMockito.when(EntityUtils.toByteArray(Mockito.any(HttpEntity.class))).thenReturn(bytes);
    PowerMockito.when(httpclient.execute(Mockito.any(HttpPatch.class))).thenReturn(response);
    HttpClientUtil.getInstance();
    String res =
        HttpClientUtil.patch(
            "http://localhost:80/user/read", "{\"message\":\"success\"}", headers());
    assertNotNull(res);
  }

  @Test
  public void testDeleteSuccess() throws Exception {
    PowerMockito.mockStatic(HttpClients.class);
    HttpClientBuilder clientBuilder = PowerMockito.mock(HttpClientBuilder.class);
    CloseableHttpClient httpclient = PowerMockito.mock(CloseableHttpClient.class);
    PowerMockito.when(HttpClients.custom()).thenReturn(clientBuilder);
    PowerMockito.when(clientBuilder.build()).thenReturn(httpclient);
    CloseableHttpResponse response = PowerMockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = PowerMockito.mock(StatusLine.class);
    PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
    HttpEntity entity = PowerMockito.mock(HttpEntity.class);
    PowerMockito.when(response.getEntity()).thenReturn(entity);
    PowerMockito.mockStatic(EntityUtils.class);
    byte[] bytes = "{\"message\":\"success\"}".getBytes();
    PowerMockito.when(EntityUtils.toByteArray(Mockito.any(HttpEntity.class))).thenReturn(bytes);
    PowerMockito.when(httpclient.execute(Mockito.any(HttpPatch.class))).thenReturn(response);
    HttpClientUtil.getInstance();
    String res = HttpClientUtil.delete("http://localhost:80/user/read", headers());
    assertNotNull(res);
  }
}
