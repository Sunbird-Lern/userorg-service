package controllers.feed.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

/** This call will validate the Feed API request */
public class FeedRequestValidator {
  public static boolean userIdValidation(String requestUserId) {
    if (StringUtils.isEmpty(requestUserId)) {
      throw new ProjectCommonException(
              ResponseCode.invalidRequestParameter.getErrorCode(),
              ResponseCode.invalidRequestParameter.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return true;
  }
}
