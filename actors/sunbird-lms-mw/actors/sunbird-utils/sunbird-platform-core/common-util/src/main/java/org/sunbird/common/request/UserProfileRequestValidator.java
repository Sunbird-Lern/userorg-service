package org.sunbird.common.request;

import java.util.List;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

public class UserProfileRequestValidator extends BaseRequestValidator {

  @SuppressWarnings("unchecked")
  public void validateProfileVisibility(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);
    validateUserId(request, JsonKey.USER_ID);
    validatePublicAndPrivateFields(request);
  }

  private void validatePublicAndPrivateFields(Request request) {
    List<String> publicList = (List<String>) request.getRequest().get(JsonKey.PUBLIC);
    List<String> privateList = (List<String>) request.getRequest().get(JsonKey.PRIVATE);

    if (publicList == null && privateList == null) {
      throw new ProjectCommonException(
          ResponseCode.invalidData.getErrorCode(),
          ResponseCode.invalidData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    validateListElementsAreDisjoint(publicList, privateList);
  }

  private void validateListElementsAreDisjoint(List<String> list1, List<String> list2) {
    if (list1 == null || list2 == null) {
      return;
    }
    for (String field : list2) {
      if (list1.contains(field)) {
        throw new ProjectCommonException(
            ResponseCode.visibilityInvalid.getErrorCode(),
            ResponseCode.visibilityInvalid.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
  }
}
