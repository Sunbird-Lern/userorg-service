package controllers.geolocation.validator;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class GeolocationRequestValidator extends BaseRequestValidator {
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateCreateGeolocationRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.ROOT_ORG_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ROOT_ORG_ID);
    List<Map<String, Object>> dataList =
        (List<Map<String, Object>>) request.getRequest().get(JsonKey.DATA);
    String rootOrgId = (String) request.getRequest().get(JsonKey.ROOT_ORG_ID);
    if (StringUtils.isEmpty(rootOrgId)) {
      throw new ProjectCommonException(
          ResponseCode.invalidOrgId.getErrorCode(),
          ResponseCode.invalidOrgId.getErrorMessage(),
          ERROR_CODE);
    }
    if (null == dataList) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    for (Map<String, Object> dataMap : dataList) {
      String location = (String) dataMap.get(JsonKey.LOCATION);
      String type = (String) dataMap.get(JsonKey.TYPE);
      validateLocationAndType(location, type);
    }
  }

  public void validateGetGeolocationRequest(Request request) {
    String id = (String) request.getRequest().get(JsonKey.ID);
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    validateGeolocationTypeValue(type);
  }

  public void valdiateUpdateGeolocationRequest(Request request) {
    String location = (String) request.getRequest().get(JsonKey.LOCATION);
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    validateLocationAndType(location, type);
  }

  public void validateLocationAndType(String location, String type) {
    validateParam(location, ResponseCode.mandatoryParamsMissing, JsonKey.LOCATION);
    validateParam(type, ResponseCode.mandatoryParamsMissing, JsonKey.TYPE);
    validateGeolocationTypeValue(type);
  }

  public void validateGeolocationTypeValue(String type) {
    if (!type.equalsIgnoreCase(JsonKey.ORGANISATION) && !type.equalsIgnoreCase(JsonKey.LOCATION)) {
      throw new ProjectCommonException(
          ResponseCode.invalidTypeValue.getErrorCode(),
          ResponseCode.invalidTypeValue.getErrorMessage(),
          ERROR_CODE);
    }
  }
}
