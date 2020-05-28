package org.sunbird.common.request;

import org.apache.commons.collections.CollectionUtils;
import org.sunbird.common.models.util.JsonKey;

import java.util.List;

/** @author arvind */
public class LearnerStateRequestValidator extends BaseRequestValidator {

  /**
   * Method to validate the get content state request.
   *
   * @param request Representing the request object.
   */
  public void validateGetContentState(Request request) {
  	validateListParam(request.getRequest(), JsonKey.COURSE_IDS, JsonKey.CONTENT_IDS);
  	if (request.getRequest().containsKey(JsonKey.COURSE_IDS)) {
        List courseIds = (List) request.getRequest().get(JsonKey.COURSE_IDS);
        request.getRequest().remove(JsonKey.COURSE_IDS);
        if (!request.getRequest().containsKey(JsonKey.COURSE_ID) && CollectionUtils.isNotEmpty(courseIds)) {
            request.getRequest().put(JsonKey.COURSE_ID, courseIds.get(0));
        }
    }
    checkMandatoryFieldsPresent(request.getRequest(), JsonKey.USER_ID, JsonKey.COURSE_ID, JsonKey.BATCH_ID);
  }
}
