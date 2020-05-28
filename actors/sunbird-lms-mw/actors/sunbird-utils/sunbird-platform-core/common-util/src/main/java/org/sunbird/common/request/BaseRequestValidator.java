package org.sunbird.common.request;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.EmailValidator;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.responsecode.ResponseCode;

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
          error.getErrorCode(),
          error.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
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
          error.getErrorCode(),
          MessageFormat.format(error.getErrorMessage(), errorMsgArgument),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Helper method which throws an exception if the given parameter list size exceeds the expected
   * size
   *
   * @param paramName Configuration parameter name
   * @param key Request parameter name
   * @param listValue Request parameter value
   */
  public void validateListParamSize(String paramName, String key, List<Object> listValue) {
    int maximumSizeAllowed = 0;
    try {
      maximumSizeAllowed = Integer.valueOf(ProjectUtil.getConfigValue(paramName).trim());
    } catch (NumberFormatException e) {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorInvalidConfigParamValue,
          MessageFormat.format(
              ResponseCode.errorInvalidConfigParamValue.getErrorMessage(),
              ProjectUtil.getConfigValue(key).trim(),
              key));
    }
    if (listValue.size() > maximumSizeAllowed) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorMaxSizeExceeded,
          MessageFormat.format(
              ResponseCode.errorMaxSizeExceeded.getErrorMessage(),
              key,
              String.valueOf(maximumSizeAllowed)));
    }
  }

  /**
   * This method will create the ProjectCommonException by reading ResponseCode and errorCode.
   * incase ResponseCode is null then it will throw invalidData error.
   *
   * @param code Error response code
   * @param errorCode (Http error code)
   * @return custom project exception
   */
  public ProjectCommonException createExceptionByResponseCode(ResponseCode code, int errorCode) {
    if (code == null) {
      ProjectLogger.log("ResponseCode object is coming as null", LoggerEnum.INFO.name());
      return new ProjectCommonException(
          ResponseCode.invalidData.getErrorCode(),
          ResponseCode.invalidData.getErrorMessage(),
          errorCode);
    }
    return new ProjectCommonException(code.getErrorCode(), code.getErrorMessage(), errorCode);
  }

  /**
   * This method will create the ProjectCommonException by reading ResponseCode and errorCode.
   * incase ResponseCode is null then it will throw invalidData error.
   *
   * @param code Error response code
   * @param errorCode (Http error code)
   * @return custom project exception
   */
  public ProjectCommonException createExceptionByResponseCode(
      ResponseCode code, int errorCode, String errorMsgArgument) {
    if (code == null) {
      ProjectLogger.log("ResponseCode object is coming as null", LoggerEnum.INFO.name());
      return new ProjectCommonException(
          ResponseCode.invalidData.getErrorCode(),
          ResponseCode.invalidData.getErrorMessage(),
          errorCode);
    }
    return new ProjectCommonException(
        code.getErrorCode(),
        MessageFormat.format(code.getErrorMessage(), errorMsgArgument),
        errorCode);
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
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Arrays.stream(keys)
        .forEach(
            key -> {
              if (StringUtils.isEmpty((String) data.get(key))) {
                throw new ProjectCommonException(
                    ResponseCode.mandatoryParamsMissing.getErrorCode(),
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
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    mandatoryParamsList.forEach(
        key -> {
          if (StringUtils.isEmpty((String) data.get(key))) {
            throw new ProjectCommonException(
                ResponseCode.mandatoryParamsMissing.getErrorCode(),
                ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                key);
          }
          if (!(data.get(key) instanceof String)) {
            throw new ProjectCommonException(
                ResponseCode.dataTypeError.getErrorCode(),
                MessageFormat.format(ResponseCode.dataTypeError.getErrorMessage(), key, "String"),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        });
  }

  /**
   * Method to check whether given mandatory fields is in given map or not .
   *
   * @param data Map contains the key value
   * @param keys List of string represents the mandatory fields
   * @param exceptionMsg Exception message
   */
  public void checkMandatoryParamsPresent(
      Map<String, Object> data, String exceptionMsg, String... keys) {
    if (MapUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Arrays.stream(keys)
        .forEach(
            key -> {
              if (StringUtils.isEmpty((String) data.get(key))) {
                throw new ProjectCommonException(
                    ResponseCode.mandatoryParamsMissing.getErrorCode(),
                    ProjectUtil.formatMessage(
                        ResponseCode.mandatoryParamsMissing.getErrorMessage(), exceptionMsg),
                    ResponseCode.CLIENT_ERROR.getResponseCode(),
                    key);
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
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Arrays.stream(keys)
        .forEach(
            key -> {
              if (data.containsKey(key)) {
                throw new ProjectCommonException(
                    ResponseCode.unupdatableField.getErrorCode(),
                    ResponseCode.unupdatableField.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode(),
                    key);
              }
            });
  }

  /**
   * Method to check whether given header fields present or not.
   *
   * @param data List of strings representing the header names in received request.
   * @param keys List of string represents the headers fields.
   */
  public void checkMandatoryHeadersPresent(Map<String, String[]> data, String... keys) {
    if (MapUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Arrays.stream(keys)
        .forEach(
            key -> {
              if (ArrayUtils.isEmpty(data.get(key))) {
                throw new ProjectCommonException(
                    ResponseCode.mandatoryHeadersMissing.getErrorCode(),
                    ResponseCode.mandatoryHeadersMissing.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode(),
                    key);
              }
            });
  }

  /**
   * Ensures not allowed fields are absent in given request.
   *
   * @param requestMap Request information
   * @param fields List of not allowed fields
   */
  public void checkForFieldsNotAllowed(Map<String, Object> requestMap, List<String> fields) {
    fields
        .stream()
        .forEach(
            field -> {
              if (requestMap.containsKey(field)) {
                throw new ProjectCommonException(
                    ResponseCode.invalidRequestParameter.getErrorCode(),
                    ProjectUtil.formatMessage(
                        ResponseCode.invalidRequestParameter.getErrorMessage(), field),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
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
                    ResponseCode.dataTypeError.getErrorCode(),
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
   * Helper method which throws an exception if given date is not in YYYY-MM-DD format.
   *
   * @param dob Date of birth.
   */
  public void validateDateParam(String dob) {
    if (StringUtils.isNotBlank(dob)) {
      boolean isValidDate = ProjectUtil.isDateValidFormat(ProjectUtil.YEAR_MONTH_DATE_FORMAT, dob);
      if (!isValidDate) {
        throw new ProjectCommonException(
            ResponseCode.dateFormatError.getErrorCode(),
            ResponseCode.dateFormatError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
  }

  /**
   * Helper method which throws an exception if given parameter value is blank (null or empty).
   *
   * @param error Error to be thrown in case of validation error.
   * @param errorMsg Error message.
   */
  public void validateParamValue(String value, ResponseCode error, String errorMsg) {
    if (StringUtils.isBlank(value)) {
      throw new ProjectCommonException(
          error.getErrorCode(),
          MessageFormat.format(error.getErrorMessage(), errorMsg),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
  /**
   * Helper method which throws an exception if user ID in request is not same as that in user
   * token.
   *
   * @param request API request
   * @param userIdKey Attribute name for user ID in API request
   */
  public static void validateUserId(Request request, String userIdKey) {
    if (!(request
        .getRequest()
        .get(userIdKey)
        .equals(request.getContext().get(JsonKey.REQUESTED_BY)))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          (String) request.getRequest().get(JsonKey.USER_ID),
          JsonKey.USER_ID);
    }
  }

  public void validateSearchRequest(Request request) {
    if (null == request.getRequest().get(JsonKey.FILTERS)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.FILTERS),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().containsKey(JsonKey.FILTERS)
        && (!(request.getRequest().get(JsonKey.FILTERS) instanceof Map))) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
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
          ResponseCode.dataTypeError.getErrorCode(),
          MessageFormat.format(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.FIELDS, "List"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().containsKey(JsonKey.FIELDS)
        && (request.getRequest().get(JsonKey.FIELDS) instanceof List)) {
      for (Object obj : (List) request.getRequest().get(JsonKey.FIELDS)) {
        if (!(obj instanceof String)) {
          throw new ProjectCommonException(
              ResponseCode.dataTypeError.getErrorCode(),
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
                  ResponseCode.invalidParameterValue.getErrorCode(),
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
                    ResponseCode.invalidParameterValue.getErrorCode(),
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
                ResponseCode.invalidParameterValue.getErrorCode(),
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
                ResponseCode.invalidParameterValue.getErrorCode(),
                MessageFormat.format(ResponseCode.invalidParameterValue.getErrorMessage(), v, key),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        });
  }

  public void validateEmail(String email) {
    if (!EmailValidator.isEmailValid(email)) {
      throw new ProjectCommonException(
          ResponseCode.emailFormatError.getErrorCode(),
          ResponseCode.emailFormatError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public void validatePhone(String phone) {
    if (!ProjectUtil.validatePhone(phone, null)) {
      throw new ProjectCommonException(
          ResponseCode.phoneNoFormatError.getErrorCode(),
          ResponseCode.phoneNoFormatError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
