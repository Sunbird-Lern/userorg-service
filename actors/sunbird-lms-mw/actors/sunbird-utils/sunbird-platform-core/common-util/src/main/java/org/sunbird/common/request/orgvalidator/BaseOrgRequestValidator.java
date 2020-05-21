package org.sunbird.common.request.orgvalidator;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class BaseOrgRequestValidator extends BaseRequestValidator {

  public static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateOrgReference(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.ORGANISATION_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ORGANISATION_ID);
  }

  public void validateRootOrgChannel(Request request) {
    if ((null != request.getRequest().get(JsonKey.IS_ROOT_ORG)
            && (Boolean) request.getRequest().get(JsonKey.IS_ROOT_ORG))
        && StringUtils.isEmpty((String) request.getRequest().get(JsonKey.CHANNEL))) {
      throw new ProjectCommonException(
          ResponseCode.dependentParameterMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.dependentParameterMissing.getErrorMessage(),
              JsonKey.CHANNEL,
              JsonKey.IS_ROOT_ORG),
          ERROR_CODE);
    }
  }
}
