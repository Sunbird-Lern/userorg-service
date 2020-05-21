package org.sunbird.common.request.orgvalidator;

import java.text.MessageFormat;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OrgMemberRequestValidator extends BaseOrgRequestValidator {

  public void validateAddMemberRequest(Request request) {
    validateCommonParams(request);
    if (request.getRequest().containsKey(JsonKey.ROLES)
        && (!(request.getRequest().get(JsonKey.ROLES) instanceof List))) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          MessageFormat.format(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST),
          ERROR_CODE);
    }
  }

  private void validateCommonParams(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.USER_ID))) {
      ProjectLogger.log(
          "OrgMemberRequestValidator : validateCommonParams : UserId is missing. Validating userExternalId");
      validateCommonUserParams(request);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.ORGANISATION_ID))) {
      ProjectLogger.log(
          "OrgMemberRequestValidator : validateCommonParams : OrganizationId is missing. Validating ExternalId");
      validateCommonOrgParams(request);
    }
  }

  private void validateCommonOrgParams(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.EXTERNAL_ID))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          " Please provide organizationId or ExternalId,Provider ");
    }
    validateParam(
        (String) request.getRequest().get(JsonKey.PROVIDER),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.PROVIDER);
  }

  private void validateCommonUserParams(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.USER_EXTERNAL_ID))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          " Please provide userId or userExternalId,userProvider,userIdType ");
    }
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_PROVIDER),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_PROVIDER);
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID_TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID_TYPE);
  }

  public void validateCommon(Request request) {
    validateOrgReference(request);
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);
  }
}
