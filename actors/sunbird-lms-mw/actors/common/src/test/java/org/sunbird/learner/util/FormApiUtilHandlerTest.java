package org.sunbird.learner.util;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.FormUtil.FormApiUtilRequestPayload;
import org.sunbird.models.FormUtil.FormUtilRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, FormApiUtilHandlerTest.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class FormApiUtilHandlerTest {

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
  }

  @Test
  public void testGetFormApiEmptyConfig() {

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
        .thenReturn("");
    Map<String, Object> dataConfigMap =
        FormApiUtil.getProfileConfig("locationCode", new RequestContext());
    Assert.assertEquals(0, dataConfigMap.size());
  }

  @Test
  public void testGetFormApiEmptyResponseConfig() {

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
        .thenReturn(getFormApiEmptyResponse());
    Map<String, Object> dataConfigMap =
        FormApiUtil.getProfileConfig("locationCode", new RequestContext());
    Assert.assertEquals(0, dataConfigMap.size());
  }

  @Test
  public void testprepareFormApiUtilPayload() {
    FormUtilRequest req = new FormUtilRequest();
    req.setComponent("component");
    req.setType("profileConfig");
    FormApiUtilRequestPayload payload = FormApiUtilHandler.prepareFormApiUtilPayload(req);
    assertEquals("profileConfig", payload.getRequest().getType());
  }

  @Test
  public void testGetFormApiConfig() {

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
        .thenReturn(getFormApiResponse());
    Map<String, Object> dataConfigMap =
        FormApiUtil.getProfileConfig("locationCode", new RequestContext());
    Assert.assertEquals(
        "profileconfig", ((Map<String, Object>) dataConfigMap.get(JsonKey.FORM)).get(JsonKey.TYPE));
  }

  public String getFormApiResponse() {
    String formData =
        "{ \"id\": \"api.form.read\", \"params\": { \"resmsgid\": \"5ebb6cb5-07a0-4407-8013-b45043270d7a\", \"msgid\": \"3af660bf-fc92-4c93-acd1-36ad81cb8f35\", \"status\": \"successful\" }, \"responseCode\": \"OK\", \"result\":"
            + " { \"form\": { \"type\": \"profileconfig\", \"subtype\": \"28\", \"action\": \"get\", \"component\": \"*\", "
            + "\"framework\": \"*\", \"created_on\": \"2021-01-12T06:12:51.284Z\", \"last_modified_on\": null, \"rootOrgId\": "
            + "\"*\" } }, \"ts\": \"2021-01-13T22:29:26.748Z\", \"ver\": \"1.0\" }";
    return formData;
  }

  public String getFormApiEmptyResponse() {
    String formData =
        "{ \"id\": \"api.form.read\", \"params\": { \"resmsgid\": \"5ebb6cb5-07a0-4407-8013-b45043270d7a\", \"msgid\": \"3af660bf-fc92-4c93-acd1-36ad81cb8f35\", \"status\": \"successful\" }, \"responseCode\": \"OK\", "
            + "\"result\":"
            + ", \"ts\": \"2021-01-13T22:29:26.748Z\", \"ver\": \"1.0\" }";
    return formData;
  }
}
