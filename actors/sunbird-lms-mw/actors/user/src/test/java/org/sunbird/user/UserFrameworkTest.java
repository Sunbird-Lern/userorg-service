package org.sunbird.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.content.store.util.ContentStoreUtil;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

@PrepareForTest({DataCacheHandler.class, ContentStoreUtil.class})
public class UserFrameworkTest extends UserManagementActorTestBase {

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.mockStatic(ContentStoreUtil.class);
    mockForUpdateTest();
  }

  @Test
  public void testUpdateUserFrameworkSuccess() {
    Request reqObj = getRequest(null, null);
    boolean res = testScenario(reqObj, null);
    assertTrue(res);
  }

  @Test
  public void testUpdateUserFrameworkFailureInvalidGradeLevel() {
    Request reqObj = getRequest("gradeLevel", "SomeWrongGrade");
    boolean res = testScenario(reqObj, ResponseCode.invalidParameterValue);
    assertTrue(res);
  }

  @Test
  public void testUpdateUserFrameworkFailureInvalidMedium() {
    Request reqObj = getRequest("medium", "glish");
    boolean res = testScenario(reqObj, ResponseCode.invalidParameterValue);
    assertTrue(res);
  }

  @Test
  public void testUpdateUserFrameworkFailureInvalidBoard() {
    Request reqObj = getRequest("board", "RBCS");
    boolean res = testScenario(reqObj, ResponseCode.invalidParameterValue);
    assertTrue(res);
  }

  @Test
  public void testUpdateUserFrameworkFailureInvalidFrameworkId() {
    Request reqObj = getRequest(JsonKey.ID, "invalidFrameworkId");
    boolean res = testScenario(reqObj, ResponseCode.errorNoFrameworkFound);
    assertTrue(res);
  }

  @SuppressWarnings("unchecked")
  public void mockForUpdateTest() {
    mockUtilsForOrgDetails();
    mockDataCacheHandler();
    mockContentStoreUtil();
  }

  private void mockUtilsForOrgDetails() {
    Map<String, Object> rootOrgMap = new HashMap<>();
    String hashTagId = "someHashTagId";
    rootOrgMap.put(JsonKey.HASHTAGID, hashTagId);
    when(Util.getOrgDetails(Mockito.anyString())).thenReturn(rootOrgMap);
  }

  private void mockContentStoreUtil() {
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.RESPONSE, null);
    when(ContentStoreUtil.readFramework("invalidFrameworkId")).thenReturn(contentMap);
  }

  private void mockDataCacheHandler() {
    Map<String, List<String>> frameworkFieldsConfigMap = new HashMap<>();
    List<String> frameworkFieldConfig =
        Arrays.asList("id", "medium", "gradeLevel", "board", "subject");
    List<String> frameworkFieldConfigMan = Arrays.asList("id", "medium", "gradeLevel", "board");
    frameworkFieldsConfigMap.put(JsonKey.FIELDS, frameworkFieldConfig);
    frameworkFieldsConfigMap.put(JsonKey.MANDATORY_FIELDS, frameworkFieldConfigMan);
    Mockito.when(DataCacheHandler.getFrameworkFieldsConfig()).thenReturn(frameworkFieldsConfigMap);

    Map<String, List<Map<String, String>>> frameworkCategoriesMap = new HashMap<>();
    frameworkCategoriesMap.put("medium", getListForCategoryMap("English"));
    frameworkCategoriesMap.put("gradeLevel", getListForCategoryMap("Grade 3"));
    frameworkCategoriesMap.put("board", getListForCategoryMap("NCERT"));

    Map<String, Map<String, List<Map<String, String>>>> frameworkCategory = new HashMap<>();
    frameworkCategory.put("NCF", frameworkCategoriesMap);
    when(DataCacheHandler.getFrameworkCategoriesMap()).thenReturn(frameworkCategory);
    Map<String, List<String>> map1 = new HashMap<>();
    List<String> list1 = Arrays.asList("NCF");
    map1.put("someHashTagId", list1);
    when(DataCacheHandler.getHashtagIdFrameworkIdMap()).thenReturn(map1);
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
    frameworkMap.put(JsonKey.ID, "NCF");
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
