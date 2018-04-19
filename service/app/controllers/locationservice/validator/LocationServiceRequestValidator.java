package controllers.locationservice.validator;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by arvind on 18/4/18. */
public class LocationServiceRequestValidator {

  public static void validateCreateLocationRequest(Request req) {

    Map<String, Object> requestBody = req.getRequest();
    if (MapUtils.isEmpty(requestBody)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    validateLocationType((String) requestBody.get(JsonKey.OBJECT_TYPE));
    List<Map<String, Object>> data = (List<Map<String, Object>>) requestBody.get(JsonKey.DATA);
    validateDataForCreateLocation(data);
  }

  private static void validateDataForCreateLocation(List<Map<String, Object>> data) {

    if (CollectionUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    data.stream()
        .forEach(
            x -> {
              if (StringUtils.isEmpty((String) x.get(JsonKey.NAME))
                  || StringUtils.isEmpty((String) x.get(JsonKey.CODE))
                  || StringUtils.isEmpty((String) x.get(GeoLocationJsonKey.PARENT_CODE))) {
                throw new ProjectCommonException(
                    ResponseCode.invalidRequestData.getErrorCode(),
                    ResponseCode.invalidRequestData.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
              }
            });
  }

  private static void validateLocationType(String type) {
    if (StringUtils.isEmpty(type)) {
      throw new ProjectCommonException(
          ResponseCode.locationTypeRequired.getErrorCode(),
          ResponseCode.locationTypeRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public static void validateUpdateLocationRequest(Request reqObj) {}

  public static void validateDeleteLocationRequest(String locationId) {

    if (StringUtils.isEmpty(locationId)) {
      throw new ProjectCommonException(
          ResponseCode.locationIdRequired.getErrorCode(),
          ResponseCode.locationIdRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public static void validateSearchLocationRequest(Request req) {

    Map<String, Object> requestBody = req.getRequest();
    if (MapUtils.isEmpty(requestBody)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (requestBody.containsKey(JsonKey.FILTERS)
        && !(requestBody.get(JsonKey.FILTERS) instanceof Map)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
