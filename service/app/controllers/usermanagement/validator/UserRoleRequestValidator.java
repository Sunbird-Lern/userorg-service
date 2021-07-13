package controllers.usermanagement.validator;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;
import org.sunbird.validator.BaseRequestValidator;
import org.sunbird.request.Request;

public class UserRoleRequestValidator extends BaseRequestValidator {

  public void validateAssignRolesRequest(Request request) {
    validateCommonParams(request);

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
  }

  private void validateCommonParams(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);

    if (request.getRequest().get(JsonKey.ROLES) == null) {
      validateParam(null, ResponseCode.mandatoryParamsMissing, JsonKey.ROLES);
    }
    if (request.getRequest().containsKey(JsonKey.ROLES)) {
      if (!(request.getRequest().get(JsonKey.ROLES) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ResponseCode.dataTypeError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode(),
            JsonKey.ROLES,
            JsonKey.LIST);
      } else if (CollectionUtils.isEmpty((List) request.getRequest().get(JsonKey.ROLES))) {
        throw new ProjectCommonException(
            ResponseCode.emptyRolesProvided.getErrorCode(),
            ResponseCode.emptyRolesProvided.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
  }

  public void validateAssignRolesRequestV2(Request request) {
    validateCommonParams(request);

    if (!CollectionUtils.isEmpty((List) request.getRequest().get(JsonKey.ROLES))) {
      List<Map> rolesList = (List<Map>) request.getRequest().get(JsonKey.ROLES);
      rolesList.forEach(
          roleObj -> {
            if (!roleObj.containsKey(JsonKey.ROLE)
                || StringUtils.isEmpty((CharSequence) roleObj.get(JsonKey.ROLE))) {
              throw new ProjectCommonException(
                  ResponseCode.mandatoryParamsMissing.getErrorCode(),
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
            if (!roleObj.containsKey(JsonKey.OPERATION)
                || StringUtils.isEmpty((CharSequence) roleObj.get(JsonKey.OPERATION))) {
              throw new ProjectCommonException(
                  ResponseCode.mandatoryParamsMissing.getErrorCode(),
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
            if (!roleObj.containsKey(JsonKey.SCOPE)
                || CollectionUtils.isEmpty((List) roleObj.get(JsonKey.SCOPE))) {
              throw new ProjectCommonException(
                  ResponseCode.mandatoryParamsMissing.getErrorCode(),
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
          });
    }
  }
}
