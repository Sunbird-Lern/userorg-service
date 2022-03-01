package org.sunbird.util;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, CassandraOperation.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UtilTest {
  private static CassandraOperation cassandraOperation;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  @Ignore
  public void initializeContextTest() {

    List<Map<String, Object>> userList = new ArrayList<>();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.ROOT_ORG_ID, "rootorgid1");
    userList.add(user);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, userList);

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);

    Request req = new Request();
    req.getContext().put(JsonKey.ACTOR_TYPE, JsonKey.USER);
    req.getContext().put(JsonKey.REQUESTED_BY, "user1");
    Util.initializeContext(req, null);
  }

  @Test
  public void addMaskEmailAndPhoneTest() {
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.EMAIL, "test@test.com");
    userMap.put(JsonKey.PHONE, "999999999");
    try {
      Util.addMaskEmailAndPhone(userMap);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Assert.assertEquals("test@test.com", userMap.get(JsonKey.ENC_EMAIL));
    Assert.assertEquals("999999999", userMap.get(JsonKey.ENC_PHONE));
  }
}
