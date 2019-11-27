package controllers.feed.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;

/** This call will validate the Feed API request */
public class FeedRequestValidator {
  public static boolean userIdValidation(String accessTokenUserId, String requestUserId) {
    if (!StringUtils.equalsIgnoreCase(accessTokenUserId, requestUserId)) {
      ProjectCommonException.throwUnauthorizedErrorException();
    }
    return true;
  }
}
