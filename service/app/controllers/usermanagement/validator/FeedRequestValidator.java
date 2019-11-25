package controllers.usermanagement.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;

/** This call will validate the Feed API request */
public class FeedRequestValidator {
  public static void userIdValidation(String accessTokenUserId, String requestUserId) {
    if (!StringUtils.endsWithIgnoreCase(accessTokenUserId, requestUserId)) {
      ProjectCommonException.throwUnauthorizedErrorException();
    }
  }
}
