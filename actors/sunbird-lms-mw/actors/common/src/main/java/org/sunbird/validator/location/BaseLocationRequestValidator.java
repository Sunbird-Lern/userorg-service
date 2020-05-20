package org.sunbird.validator.location;

import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by arvind on 25/4/18. */
public class BaseLocationRequestValidator extends BaseRequestValidator {

  /**
   * Method to validate the create location request . Mandatory fields are as - name , type, code.
   *
   * @param req Request .
   */
  public void validateCreateLocationRequest(Request req) {
    checkMandatoryFieldsPresent(
        req.getRequest(), JsonKey.NAME, JsonKey.CODE, GeoLocationJsonKey.LOCATION_TYPE);
  }

  /**
   * Method to validate the update location request . Mandatory fields are as - id, type.
   *
   * @param req Request.
   */
  public void validateUpdateLocationRequest(Request req) {
    checkMandatoryFieldsPresent(req.getRequest(), JsonKey.ID);
    checkReadOnlyAttributesAbsent(req.getRequest(), GeoLocationJsonKey.LOCATION_TYPE);
  }

  /**
   * Method to validate the delete location request . Mandatory field - locationId.
   *
   * @param locationId
   */
  public void validateDeleteLocationRequest(String locationId) {
    if (StringUtils.isEmpty(locationId)) {
      throw new ProjectCommonException(
          ResponseCode.locationIdRequired.getErrorCode(),
          ResponseCode.locationIdRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Method to validate the search location request . Request body can not be null and If filter is
   * coming in request body it should be of type map.
   *
   * @param req
   */
  public void validateSearchLocationRequest(Request req) {
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
