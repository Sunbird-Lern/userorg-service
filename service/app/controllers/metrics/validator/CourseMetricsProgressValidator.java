package controllers.metrics.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by rajatgupta on 11/02/19. */
public class CourseMetricsProgressValidator {

  public void courseProgressMetricsV2Validator(String limit, String Offset) {

    if (!StringUtils.isNumeric(limit)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.LIMIT,
          "NUMERIC");
    }

    if (!StringUtils.isNumeric(Offset)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.OFFSET,
          "NUMERIC");
    }
  }
}
