package org.sunbird.validators.orgvalidator;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validators.BaseRequestValidator;

public class BaseOrgRequestValidator extends BaseRequestValidator {

  public static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateOrgReference(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.ORGANISATION_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ORGANISATION_ID);
  }

  public void validateTenantOrgChannel(Request request) {
    if ((null != request.getRequest().get(JsonKey.IS_TENANT)
            && (Boolean) request.getRequest().get(JsonKey.IS_TENANT))
        && StringUtils.isEmpty((String) request.getRequest().get(JsonKey.CHANNEL))) {
      throw new ProjectCommonException(
          ResponseCode.dependentParameterMissing,
          MessageFormat.format(
              ResponseCode.dependentParameterMissing.getErrorMessage(),
              JsonKey.CHANNEL,
              JsonKey.IS_TENANT),
          ERROR_CODE);
    }
  }
}
