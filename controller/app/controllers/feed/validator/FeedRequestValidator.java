package controllers.feed.validator;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validator.BaseRequestValidator;

/** This call will validate the Feed API request */
public class FeedRequestValidator extends BaseRequestValidator {
  public static void validateFeedRequest(Request request) {
    Map<String, Object> feedReq = request.getRequest();
    if (StringUtils.isBlank((String) feedReq.get(JsonKey.USER_ID))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.USER_ID);
    }
    if (StringUtils.isBlank((String) feedReq.get(JsonKey.CATEGORY))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.CATEGORY);
    }
    if ((feedReq.get(JsonKey.PRIORITY)) == null || ((Integer) feedReq.get(JsonKey.PRIORITY)) <= 0) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.PRIORITY);
    }
  }

  public static boolean validateFeedDeleteRequest(Request request) {
    return validateFeedUpdateRequest(request);
  }

  public static boolean validateFeedUpdateRequest(Request request) {
    Map<String, Object> feedReq = request.getRequest();
    if (StringUtils.isBlank((String) feedReq.get(JsonKey.USER_ID))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.USER_ID);
    }
    if (StringUtils.isBlank((String) feedReq.get(JsonKey.CATEGORY))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.CATEGORY);
    }
    if (StringUtils.isBlank((String) feedReq.get(JsonKey.FEED_ID))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.FEED_ID);
    }
    return true;
  }
}
