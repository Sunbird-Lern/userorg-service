package controllers.feed.validator;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;

public class FeedRequestValidatorTest {

  @Test(expected = ProjectCommonException.class)
  public void validateFeedRequestTestFailure() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.CREATE_USER_FEED.getValue());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.DATA, dataMap);
    reqObj.setRequest(requestMap);
    FeedRequestValidator.validateFeedRequest(reqObj);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateFeedUpdateRequestFailureTest() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.CREATE_USER_FEED.getValue());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.DATA, dataMap);
    reqObj.setRequest(requestMap);
    Assert.assertTrue(FeedRequestValidator.validateFeedUpdateRequest(reqObj));
  }

  @Test
  public void validateFeedUpdateRequestTest() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.CREATE_USER_FEED.getValue());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.FEED_ID, "someFeedId");
    reqObj.setRequest(requestMap);
    Assert.assertTrue(FeedRequestValidator.validateFeedUpdateRequest(reqObj));
  }

  @Test
  public void validateFeedDeleteRequestTest() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.CREATE_USER_FEED.getValue());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.FEED_ID, "someFeedId");
    reqObj.setRequest(requestMap);
    Assert.assertTrue(FeedRequestValidator.validateFeedDeleteRequest(reqObj));
  }
}
