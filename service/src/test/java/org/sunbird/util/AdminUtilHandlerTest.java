package org.sunbird.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.adminutil.AdminUtilRequestData;
import org.sunbird.model.adminutil.AdminUtilRequestPayload;
import org.sunbird.request.RequestContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, AdminUtilHandlerTest.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class AdminUtilHandlerTest {

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
  }

  @Test
  public void testPrepareAdminUtilPayload() {
    List<AdminUtilRequestData> reqData = new ArrayList<AdminUtilRequestData>();
    reqData.add(new AdminUtilRequestData("parentId", "childId1"));
    reqData.add(new AdminUtilRequestData("parentId", "childId2"));
    AdminUtilRequestPayload payload = AdminUtilHandler.prepareAdminUtilPayload(reqData);
    assertEquals(2, payload.getRequest().getData().size());
  }

  @Test
  public void testFetchEncryptedToken() throws IOException {
    List<AdminUtilRequestData> reqData = new ArrayList<AdminUtilRequestData>();
    reqData.add(new AdminUtilRequestData("parentId", "childId1"));
    reqData.add(new AdminUtilRequestData("parentId", "childId2"));

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.any()))
        .thenReturn(
            "{\"id\": \"ekstep.api.am.adminutil.sign.payload\",\"ver\": \"1.0\",\"ets\":1591589862198,\"params\": {\"status\": \"successful\",\"err\": null,\"errmsg\": null,\"msgid\": \"\",\"resmsgid\": \"328749cb-45e3-4b26-aea6-b7f4b97d548b\"}, \"result\": {\"data\": [{\"parentId\": \"parentId\", \"sub\":\"childId1\",\"token\":\"encryptedtoken1\"},{\"parentId\": \"parentId\",\"sub\": \"childId2\",\"token\":\"encryptedtoken2\"}]}}");
    Map<String, Object> encryptedTokenList =
        AdminUtilHandler.fetchEncryptedToken(
            AdminUtilHandler.prepareAdminUtilPayload(reqData), null);

    ArrayList<Map<String, Object>> data =
        (ArrayList<Map<String, Object>>) encryptedTokenList.get(JsonKey.DATA);
    for (Object object : data) {
      Map<String, Object> tempMap = (Map<String, Object>) object;
      assertNotNull(tempMap.get(JsonKey.TOKEN));
    }
  }

  @Test
  public void testFetchEncryptedTokenFailure() {
    List<AdminUtilRequestData> reqData = new ArrayList<AdminUtilRequestData>();
    reqData.add(new AdminUtilRequestData("parentId", "childId1"));
    reqData.add(new AdminUtilRequestData("parentId", "childId2"));

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.any()))
        .thenThrow(new RuntimeException("Exception Messsage"));
    try {
      Map<String, Object> encryptedTokenList =
          AdminUtilHandler.fetchEncryptedToken(
              AdminUtilHandler.prepareAdminUtilPayload(reqData), null);
    } catch (Exception e) {
      assertNotNull(e);
    }
  }
}
