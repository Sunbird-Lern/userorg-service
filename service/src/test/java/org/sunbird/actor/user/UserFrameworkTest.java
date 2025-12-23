package org.sunbird.actor.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.Props;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.contentstore.ContentStoreUtil;

@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@PrepareForTest({DataCacheHandler.class, ContentStoreUtil.class})
public class UserFrameworkTest extends UserManagementActorTestBase {

  public final Props props = Props.create(UserUpdateActor.class);

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.mockStatic(ContentStoreUtil.class);
    mockForUpdateTest();
  }

  @Test
  public void testUpdateUserFrameworkSuccess() {
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    Request reqObj = getRequest(null, null);
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    reqObj.getRequest().remove(JsonKey.USER);
    boolean res = testScenario(reqObj, null, props);
    assertTrue(res);
  }

  @Test
  public void testUpdateUserFrameworkFailureInvalidGradeLevel() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    Request reqObj = getRequest(null, null);
    boolean res = testScenario(reqObj, null, props);
    assertTrue(res);
  }

  @SuppressWarnings("unchecked")
  public void mockForUpdateTest() {
    mockUtilsForOrgDetails();
    mockContentStoreUtil();
  }

  private void mockUtilsForOrgDetails() {
    Map<String, Object> rootOrgMap = new HashMap<>();
    String hashTagId = "someHashTagId";
    rootOrgMap.put(JsonKey.HASHTAGID, hashTagId);
    when(orgService.getOrgById(Mockito.anyString(), Mockito.any())).thenReturn(rootOrgMap);
  }

  private void mockContentStoreUtil() {
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.RESPONSE, null);
    when(ContentStoreUtil.readFramework("invalidFrameworkId", new RequestContext()))
        .thenReturn(contentMap);
  }

  private Request getRequest(String key, String value) {
    Request reqObj = new Request();
    String userId = "userId";
    reqObj.getRequest().put(JsonKey.USER_ID, userId);
    reqObj.setOperation(ActorOperations.UPDATE_USER.getValue());
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, userId);
    Map<String, Object> frameworkMap = getFrameworkDetails(key, value);
    reqObj.getRequest().put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.FRAMEWORK, frameworkMap);
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER, innerMap);
    request.put(JsonKey.USER_ID, userId);
    request.put(JsonKey.FRAMEWORK, frameworkMap);
    getUpdateRequestWithDefaultFlags(request);
    reqObj.setRequest(request);
    Map<String, Object> context = new HashMap<>();
    context.put(JsonKey.REQUESTED_BY, "someValue");
    context.put(JsonKey.USER_ID, userId);
    reqObj.setContext(context);
    return reqObj;
  }

  private List<Map<String, String>> getListForCategoryMap(String value) {
    Map<String, String> map2 = new HashMap<>();
    map2.put(JsonKey.NAME, value);
    List<Map<String, String>> list2 = new ArrayList<>();
    list2.add(map2);
    return list2;
  }

  private Map<String, Object> getFrameworkDetails(String key, String value) {
    Map<String, Object> frameworkMap = new HashMap<>();
    List<String> medium = new ArrayList<>();
    medium.add("English");
    List<String> gradeLevel = new ArrayList<>();
    gradeLevel.add("Grade 3");
    List<String> board = new ArrayList<>();
    board.add("NCERT");
    List<String> frameId = new ArrayList<>();
    frameId.add("NCF");
    frameworkMap.put(JsonKey.ID, frameId);
    frameworkMap.put("gradeLevel", gradeLevel);
    frameworkMap.put("board", board);
    frameworkMap.put("medium", medium);
    if (key != null) {
      List<String> wrongValue = new ArrayList<>();
      wrongValue.add(value);
      frameworkMap.put(key, wrongValue);
    }
    return frameworkMap;
  }
}
