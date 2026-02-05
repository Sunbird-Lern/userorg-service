package org.sunbird.validators;

import org.apache.commons.collections.CollectionUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

import java.util.List;

/**
 * Validator for Learner State related requests.
 * Handles validation logic for fetching content state.
 */
public class LearnerStateRequestValidator extends BaseRequestValidator {

  /**
   * Validates the 'get content state' request.
   * Checks for mandatory parameters and validates course/collection IDs.
   *
   * @param request The request object containing payload.
   */
  @SuppressWarnings("unchecked")
  public void validateGetContentState(Request request) {
    validateListParam(request.getRequest(), JsonKey.COURSE_IDS, JsonKey.CONTENT_IDS);
    
    if (request.getRequest().containsKey(JsonKey.COURSE_IDS)) {
      List<String> courseIds = (List<String>) request.getRequest().get(JsonKey.COURSE_IDS);
      request.getRequest().remove(JsonKey.COURSE_IDS);
      
      if (!request.getRequest().containsKey(JsonKey.COURSE_ID)
          && !request.getRequest().containsKey(JsonKey.COLLECTION_ID)
          && CollectionUtils.isNotEmpty(courseIds)) {
        request.getRequest().put(JsonKey.COURSE_ID, courseIds.get(0));
      }
    }
    
    String courseIdKey =
        request.getRequest().containsKey(JsonKey.COURSE_ID)
            ? JsonKey.COURSE_ID
            : JsonKey.COLLECTION_ID;
            
    // Ensure the key exists before putting it back to avoid null values if logic changes
    if (request.getRequest().containsKey(courseIdKey)) {
        request.getRequest().put(JsonKey.COURSE_ID, request.getRequest().get(courseIdKey));
    }
    
    checkMandatoryFieldsPresent(
        request.getRequest(), JsonKey.USER_ID, JsonKey.COURSE_ID, JsonKey.BATCH_ID);
  }
}
