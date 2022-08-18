package org.sunbird.service.urlaction;

import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class})
public class UrlActionServiceTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @BeforeClass
  public static void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
  }

  @Test
  public void getUrlActionMapTest() {
    PowerMockito.when(
            cassandraOperationImpl.getAllRecords(
                Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getUrlActionRecords());
    Map<String, Object> resp = UrlActionService.getUrlActionMap("URL_ID");
    Assert.assertNotNull(resp);
  }

  private Response getUrlActionRecords() {
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put(JsonKey.ID, "URL_ID");
    record.put(JsonKey.NAME, "ACTION_NAME");
    List<String> urlList = new ArrayList<>();
    urlList.add("http://localhost:9000/new/url1");
    urlList.add("http://localhost:9000/new/url2");
    record.put(JsonKey.URL, urlList);
    results.add(record);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, results);
    return response;
  }
}
