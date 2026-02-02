package controllers.usermanagement.validator;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.common.ProjectUtil;
import org.sunbird.utils.StringFormatter;
import org.sunbird.validators.BaseRequestValidator;

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
            ResponseCode.dataTypeError,
            ResponseCode.dataTypeError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode(),
            JsonKey.ROLES,
            JsonKey.LIST);
      } else if (CollectionUtils.isEmpty((List) request.getRequest().get(JsonKey.ROLES))) {
        throw new ProjectCommonException(
            ResponseCode.errorMandatoryParamsEmpty,
            MessageFormat.format(
                ResponseCode.errorMandatoryParamsEmpty.getErrorMessage(), JsonKey.ROLES),
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
                  ResponseCode.mandatoryParamsMissing,
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
            if (!roleObj.containsKey(JsonKey.OPERATION)
                || StringUtils.isEmpty((CharSequence) roleObj.get(JsonKey.OPERATION))) {
              throw new ProjectCommonException(
                  ResponseCode.mandatoryParamsMissing,
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
            if (!roleObj.containsKey(JsonKey.SCOPE)
                || CollectionUtils.isEmpty((List) roleObj.get(JsonKey.SCOPE))) {
              throw new ProjectCommonException(
                  ResponseCode.mandatoryParamsMissing,
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
            List<Map<String, String>> scopeList =
                (List<Map<String, String>>) roleObj.get(JsonKey.SCOPE);
            scopeList
                .stream()
                .forEach(
                    scope -> {
                      Object orgId = scope.get(JsonKey.ORGANISATION_ID);
                      if ((null == orgId)) {
                        throw new ProjectCommonException(
                            ResponseCode.invalidParameterValue,
                            ProjectUtil.formatMessage(
                                ResponseCode.invalidParameterValue.getErrorMessage(),
                                "null",
                                "scope.organisationId"),
                            ResponseCode.CLIENT_ERROR.getResponseCode());
                      }
                      if (!(orgId instanceof String)) {
                        throw new ProjectCommonException(
                            ResponseCode.dataTypeError,
                            ProjectUtil.formatMessage(
                                ResponseCode.dataTypeError.getErrorMessage(),
                                "scope.organisationId",
                                "String"),
                            ResponseCode.CLIENT_ERROR.getResponseCode());
                      }
                      if (StringUtils.isBlank((String) orgId)) {
                        throw new ProjectCommonException(
                            ResponseCode.invalidParameterValue,
                            ProjectUtil.formatMessage(
                                ResponseCode.invalidParameterValue.getErrorMessage(),
                                " ",
                                "scope.organisationId"),
                            ResponseCode.CLIENT_ERROR.getResponseCode());
                      }
                    });
          });
    }
  }
}
