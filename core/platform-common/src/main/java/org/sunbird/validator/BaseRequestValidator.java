package org.sunbird.validator;

import com.typesafe.config.ConfigFactory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base request validator class to house common validation methods.
 *
 * @author B Vinaya Kumar
 */
public class BaseRequestValidator {
  /**
   * Helper method which throws an exception if given parameter value is blank (null or empty).
   *
   * @param value Request parameter value.
   * @param error Error to be thrown in case of validation error.
   */
  public void validateParam(String value, ResponseCode error) {
    if (StringUtils.isBlank(value)) {
      throw new ProjectCommonException(
          error, error.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Helper method which throws an exception if given parameter value is blank (null or empty).
   *
   * @param value Request parameter value.
   * @param error Error to be thrown in case of validation error.
   * @param errorMsgArgument Argument for error message.
   */
  public void validateParam(String value, ResponseCode error, String errorMsgArgument) {
    if (StringUtils.isBlank(value)) {
      throw new ProjectCommonException(
          error,
          MessageFormat.format(error.getErrorMessage(), errorMsgArgument),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Method to check whether given mandatory fields is in given map or not.
   *
   * @param data Map contains the key value,
   * @param keys List of string represents the mandatory fields.
   */
  public void checkMandatoryFieldsPresent(Map<String, Object> data, String... keys) {
    if (MapUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Arrays.stream(keys)
        .forEach(
            key -> {
              if (StringUtils.isEmpty((String) data.get(key))) {
                throw new ProjectCommonException(
                    ResponseCode.mandatoryParamsMissing,
                    ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode(),
                    key);
              }
            });
  }
  /**
   * Method to check whether given mandatory fields is in given map or not. also check the instance
   * of request attributes
   *
   * @param data Map contains the key value,
   * @param mandatoryParamsList List of string represents the mandatory fields.
   */
  public void checkMandatoryFieldsPresent(
      Map<String, Object> data, List<String> mandatoryParamsList) {
    if (MapUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    mandatoryParamsList.forEach(
        key -> {
          if (StringUtils.isEmpty((String) data.get(key))) {
            throw new ProjectCommonException(
                ResponseCode.mandatoryParamsMissing,
                ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                key);
          }
          if (!(data.get(key) instanceof String)) {
            throw new ProjectCommonException(
                ResponseCode.dataTypeError,
                MessageFormat.format(ResponseCode.dataTypeError.getErrorMessage(), key, "String"),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        });
  }

  /**
   * Method to check whether given fields is in given map or not .If it is there throw exception.
   * because in some update request cases we don't want to update some props to , if it is there in
   * request , throw exception.
   *
   * @param data Map contains the key value
   * @param keys List of string represents the must not present fields.
   */
  public void checkReadOnlyAttributesAbsent(Map<String, Object> data, String... keys) {

    if (MapUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Arrays.stream(keys)
        .forEach(
            key -> {
              if (data.containsKey(key)) {
                throw new ProjectCommonException(
                    ResponseCode.unupdatableField,
                    ResponseCode.unupdatableField.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode(),
                    key);
              }
            });
  }

  /**
   * Helper method which throws an exception if each field is not of type List.
   *
   * @param requestMap Request information
   * @param fieldPrefix Field prefix
   * @param fields List of fields
   */
  public void validateListParamWithPrefix(
      Map<String, Object> requestMap, String fieldPrefix, String... fields) {
    Arrays.stream(fields)
        .forEach(
            field -> {
              if (requestMap.containsKey(field)
                  && null != requestMap.get(field)
                  && !(requestMap.get(field) instanceof List)) {

                String fieldWithPrefix =
                    fieldPrefix != null ? StringFormatter.joinByDot(fieldPrefix, field) : field;

                throw new ProjectCommonException(
                    ResponseCode.dataTypeError,
                    ProjectUtil.formatMessage(
                        ResponseCode.dataTypeError.getErrorMessage(),
                        fieldWithPrefix,
                        JsonKey.LIST),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
              }
            });
  }

  /**
   * Helper method which throws an exception if each field is not of type List.
   *
   * @param requestMap Request information
   * @param fields List of fields
   */
  public void validateListParam(Map<String, Object> requestMap, String... fields) {
    validateListParamWithPrefix(requestMap, null, fields);
  }

  /**
   * Helper method which throws an exception if user ID in request is not same as that in user
   * token.
   *
   * @param request API request
   * @param userIdKey Attribute name for user ID in API request
   */
  public static void validateUserId(Request request, String userIdKey) {
      if (ConfigFactory.load().getBoolean(JsonKey.AUTH_ENABLED) && !(request
              .getRequest()
              .get(userIdKey)
              .equals(request.getContext().get(JsonKey.REQUESTED_BY)))) {
        throw new ProjectCommonException(
                ResponseCode.invalidParameterValue,
                ResponseCode.invalidParameterValue.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                (String) request.getRequest().get(JsonKey.USER_ID),
                JsonKey.USER_ID);
      }
  }

  public void validateSearchRequest(Request request) {
    if (null == request.getRequest().get(JsonKey.FILTERS)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.FILTERS),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().containsKey(JsonKey.FILTERS)
        && (!(request.getRequest().get(JsonKey.FILTERS) instanceof Map))) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError,
          MessageFormat.format(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.FILTERS, "Map"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    validateSearchRequestFiltersValues(request);
    validateSearchRequestFieldsValues(request);
  }

  private void validateSearchRequestFieldsValues(Request request) {
    if (request.getRequest().containsKey(JsonKey.FIELDS)
        && (!(request.getRequest().get(JsonKey.FIELDS) instanceof List))) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError,
          MessageFormat.format(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.FIELDS, "List"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().containsKey(JsonKey.FIELDS)
        && (request.getRequest().get(JsonKey.FIELDS) instanceof List)) {
      for (Object obj : (List) request.getRequest().get(JsonKey.FIELDS)) {
        if (!(obj instanceof String)) {
          throw new ProjectCommonException(
              ResponseCode.dataTypeError,
              MessageFormat.format(
                  ResponseCode.dataTypeError.getErrorMessage(), JsonKey.FIELDS, "List of String"),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }
  }

  private void validateSearchRequestFiltersValues(Request request) {
    if (request.getRequest().containsKey(JsonKey.FILTERS)
        && ((request.getRequest().get(JsonKey.FILTERS) instanceof Map))) {
      Map<String, Object> map = (Map<String, Object>) request.getRequest().get(JsonKey.FILTERS);

      map.forEach(
          (key, val) -> {
            if (key == null) {
              throw new ProjectCommonException(
                  ResponseCode.invalidParameterValue,
                  MessageFormat.format(
                      ResponseCode.invalidParameterValue.getErrorMessage(), key, JsonKey.FILTERS),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
            if (val instanceof List) {
              validateListValues((List) val, key);
            } else if (val instanceof Map) {
              validateMapValues((Map) val);
            } else if (val == null)
              if (StringUtils.isEmpty((String) val)) {
                throw new ProjectCommonException(
                    ResponseCode.invalidParameterValue,
                    MessageFormat.format(
                        ResponseCode.invalidParameterValue.getErrorMessage(), val, key),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
              }
          });
    }
  }

  private void validateMapValues(Map val) {
    val.forEach(
        (k, v) -> {
          if (k == null || v == null) {
            throw new ProjectCommonException(
                ResponseCode.invalidParameterValue,
                MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), v, k),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        });
  }

  private void validateListValues(List val, String key) {
    val.forEach(
        v -> {
          if (v == null) {
            throw new ProjectCommonException(
                ResponseCode.invalidParameterValue,
                MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), v, key),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        });
  }

  public void validateEmail(String email) {
    if (!EmailValidator.isEmailValid(email)) {
      throw new ProjectCommonException(
          ResponseCode.dataFormatError,
          ResponseCode.dataFormatError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public void validatePhone(String phone) {
    if (!ProjectUtil.validatePhone(phone, null)) {
      throw new ProjectCommonException(
          ResponseCode.dataFormatError,
          String.format(ResponseCode.dataFormatError.getErrorMessage(), JsonKey.PHONE),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public static void createClientError(ResponseCode responseCode, String field) {
    throw new ProjectCommonException(
        responseCode,
        ProjectUtil.formatMessage(responseCode.getErrorMessage(), field),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }
}
