package controllers.tenantpreference;

import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validators.BaseRequestValidator;

public class TenantPreferenceValidator extends BaseRequestValidator {

  public void validateCreatePreferenceRequest(Request request) {
    validateMandatoryParamsWithType(
        request.getRequest(),
        Lists.newArrayList(JsonKey.ORG_ID, JsonKey.KEY),
        String.class,
        true,
        JsonKey.REQUEST);
    validateMandatoryParamsWithType(
        request.getRequest(), Lists.newArrayList(JsonKey.DATA), Map.class, true, JsonKey.REQUEST);
  }

  public void validateUpdatePreferenceRequest(Request request) {
    validateMandatoryParamsWithType(
        request.getRequest(),
        Lists.newArrayList(JsonKey.ORG_ID, JsonKey.KEY),
        String.class,
        true,
        JsonKey.REQUEST);
    validateMandatoryParamsWithType(
        request.getRequest(), Lists.newArrayList(JsonKey.DATA), Map.class, false, JsonKey.REQUEST);
  }

  public void validateGetPreferenceRequest(Request request) {
    validateMandatoryParamsWithType(
        request.getRequest(),
        Lists.newArrayList(JsonKey.ORG_ID, JsonKey.KEY),
        String.class,
        true,
        JsonKey.REQUEST);
  }

  public static void validateMandatoryParamsWithType(
      Map<String, Object> reqMap,
      List<String> mandatoryParamsList,
      Class<?> type,
      boolean validatePresence,
      String parentKey) {
    for (String param : mandatoryParamsList) {
      if (!reqMap.containsKey(param)) {
        throw new ProjectCommonException(
            ResponseCode.mandatoryParamsMissing,
            MessageFormat.format(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), parentKey + "." + param),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      if (!(isInstanceOf(reqMap.get(param).getClass(), type))) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError,
            MessageFormat.format(
                ResponseCode.dataTypeError.getErrorMessage(),
                parentKey + "." + param,
                type.getName()),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      if (validatePresence) {
        validatePresence(param, reqMap.get(param), type, parentKey);
      }
    }
  }

  private static void validatePresence(String key, Object value, Class<?> type, String parentKey) {
    if (type == String.class) {
      if (StringUtils.isBlank((String) value)) {
        throw new ProjectCommonException(
            ResponseCode.mandatoryParamsMissing,
            MessageFormat.format(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), parentKey + "." + key),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else if (type == Map.class) {
      Map<String, Object> map = (Map<String, Object>) value;
      if (MapUtils.isEmpty(map)) {
        throw new ProjectCommonException(
            ResponseCode.mandatoryParamsMissing,
            MessageFormat.format(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), parentKey + "." + key),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
  }

  private static boolean isInstanceOf(Class objClass, Class targetClass) {
    return targetClass.isAssignableFrom(objClass);
  }
}
