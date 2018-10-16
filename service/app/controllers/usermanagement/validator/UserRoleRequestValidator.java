package controllers.usermanagement.validator;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserRoleRequestValidator extends BaseRequestValidator {

  public void validateAssignRoles(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);

    String organisationId = (String) request.getRequest().get(JsonKey.ORGANISATION_ID);
    String externalId = (String) request.getRequest().get(JsonKey.EXTERNAL_ID);
    String provider = (String) request.getRequest().get(JsonKey.PROVIDER);
    if (StringUtils.isBlank(organisationId)
        && (StringUtils.isBlank(externalId) || StringUtils.isBlank(provider))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              StringFormatter.joinByOr(
                  JsonKey.ORGANISATION_ID,
                  StringFormatter.joinByAnd(JsonKey.EXTERNAL_ID, JsonKey.PROVIDER))));
    }

    validateParam(
        (String) request.getRequest().get(JsonKey.ROLES),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ROLES);

    if (request.getRequest().containsKey(JsonKey.ROLES)
        && !(request.getRequest().get(JsonKey.ROLES) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.ROLES,
          JsonKey.LIST);
    }
  }
}
