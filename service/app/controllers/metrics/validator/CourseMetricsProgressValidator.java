package controllers.metrics.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

public class CourseMetricsProgressValidator {

  public void validateCourseProgressMetricsV2Request(
      String limit, String offset, String sortOrder) {

    if (!StringUtils.isNumeric(limit)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.LIMIT,
          JsonKey.NUMERIC);
    }

    if (!StringUtils.isNumeric(offset)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.OFFSET,
          JsonKey.NUMERIC);
    }

    if (!StringUtils.isEmpty(sortOrder)
        && !(JsonKey.ASC.equalsIgnoreCase(sortOrder) || JsonKey.DESC.equalsIgnoreCase(sortOrder))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          sortOrder,
          JsonKey.SORT_ORDER);
    }
  }
}
