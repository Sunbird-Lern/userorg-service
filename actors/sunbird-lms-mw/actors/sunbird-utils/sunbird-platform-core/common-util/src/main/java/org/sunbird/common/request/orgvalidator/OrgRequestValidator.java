package org.sunbird.common.request.orgvalidator;

import java.text.MessageFormat;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.AddressRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OrgRequestValidator extends BaseOrgRequestValidator {

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateCreateOrgRequest(Request orgRequest) {

    validateParam(
        (String) orgRequest.getRequest().get(JsonKey.ORG_NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ORG_NAME);
    validateRootOrgChannel(orgRequest);
    validateLicense(orgRequest);

    Map<String, Object> address =
        (Map<String, Object>) orgRequest.getRequest().get(JsonKey.ADDRESS);
    if (MapUtils.isNotEmpty(address)) {
      new AddressRequestValidator().validateAddress(address, JsonKey.ORGANISATION);
    }
    validateLocationIdOrCode(orgRequest);
  }

  private void validateLicense(Request orgRequest) {
    if (orgRequest.getRequest().containsKey(JsonKey.IS_ROOT_ORG)
        && (boolean) orgRequest.getRequest().get(JsonKey.IS_ROOT_ORG)
        && orgRequest.getRequest().containsKey(JsonKey.LICENSE)
        && StringUtils.isBlank((String) orgRequest.getRequest().get(JsonKey.LICENSE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              (String) orgRequest.getRequest().get(JsonKey.LICENSE),
              JsonKey.LICENSE),
          ERROR_CODE);
    }
  }

  public void validateUpdateOrgRequest(Request request) {
    validateOrgReference(request);
    if (request.getRequest().containsKey(JsonKey.ROOT_ORG_ID)
        && StringUtils.isEmpty((String) request.getRequest().get(JsonKey.ROOT_ORG_ID))) {
      throw new ProjectCommonException(
          ResponseCode.invalidRootOrganisationId.getErrorCode(),
          ResponseCode.invalidRootOrganisationId.getErrorMessage(),
          ERROR_CODE);
    }
    if (request.getRequest().get(JsonKey.STATUS) != null) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestParameter.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.invalidRequestParameter.getErrorMessage(), JsonKey.STATUS),
          ERROR_CODE);
    }

    validateRootOrgChannel(request);
    validateLocationIdOrCode(request);
    Map<String, Object> address = (Map<String, Object>) request.getRequest().get(JsonKey.ADDRESS);
    if (MapUtils.isNotEmpty(address)) {
      new AddressRequestValidator().validateAddress(address, JsonKey.ORGANISATION);
    }
  }

  public void validateUpdateOrgStatusRequest(Request request) {
    validateOrgReference(request);

    if (!request.getRequest().containsKey(JsonKey.STATUS)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }

    if (!(request.getRequest().get(JsonKey.STATUS) instanceof Integer)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private void validateLocationIdOrCode(Request orgRequest) {
    validateListParam(orgRequest.getRequest(), JsonKey.LOCATION_IDS, JsonKey.LOCATION_CODE);
    if (orgRequest.getRequest().get(JsonKey.LOCATION_IDS) != null
        && orgRequest.getRequest().get(JsonKey.LOCATION_CODE) != null) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorAttributeConflict,
          MessageFormat.format(
              ResponseCode.errorAttributeConflict.getErrorMessage(),
              JsonKey.LOCATION_CODE,
              JsonKey.LOCATION_IDS));
    }
  }
}
