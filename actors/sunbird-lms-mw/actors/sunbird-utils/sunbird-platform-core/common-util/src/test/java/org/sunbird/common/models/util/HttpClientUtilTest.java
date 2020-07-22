package org.sunbird.common.models.util;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
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


  private CloseableHttpClient httpclient;

  private Map<String, String> headers (){
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type","application/json");
    return headers;
  }

  @Before
  public void init() {
    PowerMockito.mockStatic(HttpClients.class);
    HttpClientBuilder clientBuilder = PowerMockito.mock(HttpClientBuilder.class);
    httpclient = PowerMockito.mock(CloseableHttpClient.class);
    PowerMockito.when(HttpClients.custom()).thenReturn(clientBuilder);
    PowerMockito.when(clientBuilder.build()).thenReturn(httpclient);
    HttpClientUtil.getInstance();
  }

  @Test
  public void testGetSuccess() throws IOException {
    CloseableHttpResponse response = PowerMockito.mock(CloseableHttpResponse.class);
    StatusLine statusLine = PowerMockito.mock(StatusLine.class);
    PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
    PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
    HttpEntity entity = PowerMockito.mock(HttpEntity.class);
    PowerMockito.when(response.getEntity()).thenReturn(entity);
    PowerMockito.mockStatic(EntityUtils.class);
    byte[] bytes = "{\"message\":\"success\"}".getBytes();
    PowerMockito.when(EntityUtils.toByteArray(Mockito.any(HttpEntity.class))).thenReturn(bytes);
    PowerMockito.when(httpclient.execute(Mockito.any(HttpGet.class))).thenReturn(response);
    String res = HttpClientUtil.get("http://localhost:80/user/read",headers());
    assertTrue("{\"message\":\"success\"}".equals(res));
  }

  @Test
  public void testPostSuccess() throws IOException {
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
    String res = HttpClientUtil.post("http://localhost:80/user/read","{\"message\":\"success\"}",headers());
    assertTrue("{\"message\":\"success\"}".equals(res));
  }

  @Test
  public void testPostFormSuccess() throws IOException {
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
    Map<String,String> fields = new HashMap<>();
    fields.put("message", "success");
    String res = HttpClientUtil.postFormData("http://localhost:80/user/read",fields,headers());
    assertTrue("{\"message\":\"success\"}".equals(res));
  }

  @Test
  public void testPatchSuccess() throws IOException {
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
    String res = HttpClientUtil.patch("http://localhost:80/user/read","{\"message\":\"success\"}",headers());
    assertTrue("{\"message\":\"success\"}".equals(res));
  }

}
