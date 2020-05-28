package org.sunbird.common.request;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.responsecode.ResponseCode;

public class UserRequestValidator extends BaseRequestValidator {

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateCreateUserRequest(Request userRequest) {
    externalIdsValidation(userRequest, JsonKey.CREATE);
    fieldsNotAllowed(
        Arrays.asList(
            JsonKey.REGISTERED_ORG_ID,
            JsonKey.ROOT_ORG_ID,
            JsonKey.PROVIDER,
            JsonKey.EXTERNAL_ID,
            JsonKey.EXTERNAL_ID_PROVIDER,
            JsonKey.EXTERNAL_ID_TYPE,
            JsonKey.ID_TYPE),
        userRequest);
    createUserBasicValidation(userRequest);
    validateUserType(userRequest);
    phoneValidation(userRequest);
    addressValidation(userRequest);
    educationValidation(userRequest);
    jobProfileValidation(userRequest);
    validateWebPages(userRequest);
    validateLocationCodes(userRequest);
    validatePassword((String) userRequest.getRequest().get(JsonKey.PASSWORD));
  }

  public static boolean isGoodPassword(String password) {
    return password.matches(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_PASS_REGEX));
  }

  private static void validatePassword(String password) {
    if (StringUtils.isNotBlank(password)) {
      boolean response = isGoodPassword(password);
      if (!response) {
        throw new ProjectCommonException(
            ResponseCode.passwordValidation.getErrorCode(),
            ResponseCode.passwordValidation.getErrorMessage(),
            ERROR_CODE);
      }
    }
  }

  private void validateLocationCodes(Request userRequest) {
    Object locationCodes = userRequest.getRequest().get(JsonKey.LOCATION_CODES);
    if ((locationCodes != null) && !(locationCodes instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.LOCATION_CODES, JsonKey.LIST),
          ERROR_CODE);
    }
    if (locationCodes != null) {
      List<String> set = new ArrayList(new HashSet<>((List<String>) locationCodes));
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
  }

  private void validateUserName(Request userRequest) {
    validateParam(
        (String) userRequest.getRequest().get(JsonKey.USERNAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USERNAME);
  }

  public void validateUserCreateV3(Request userRequest) {
    validateParam(
        (String) userRequest.getRequest().get(JsonKey.FIRST_NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.FIRST_NAME);
    if (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EMAIL))
        && StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.PHONE))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.emailorPhoneRequired);
    }
    phoneVerifiedValidation(userRequest);
    emailVerifiedValidation(userRequest);
    validatePassword((String) userRequest.getRequest().get(JsonKey.PASSWORD));
    if (StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.EMAIL))) {
      validateEmail((String) userRequest.getRequest().get(JsonKey.EMAIL));
    }
    if (StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.PHONE))) {
      validatePhone((String) userRequest.getRequest().get(JsonKey.PHONE));
    }
  }

  public void validateCreateUserV3Request(Request userRequest) {
    validateCreateUserRequest(userRequest);
  }

  public void validateCreateUserV1Request(Request userRequest) {
    validateUserName(userRequest);
    validateCreateUserV3Request(userRequest);
  }

  public void validateCreateUserV2Request(Request userRequest) {
    validateCreateUserRequest(userRequest);
  }

  public void fieldsNotAllowed(List<String> fields, Request userRequest) {
    for (String field : fields) {
      if (((userRequest.getRequest().get(field) instanceof String)
              && StringUtils.isNotBlank((String) userRequest.getRequest().get(field)))
          || (null != userRequest.getRequest().get(field))) {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestParameter.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidRequestParameter.getErrorMessage(), field),
            ERROR_CODE);
      }
    }
  }

  public void phoneValidation(Request userRequest) {
    if (!StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.COUNTRY_CODE))) {
      boolean bool =
          ProjectUtil.validateCountryCode(
              (String) userRequest.getRequest().get(JsonKey.COUNTRY_CODE));
      if (!bool) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidCountryCode);
      }
    }
    if (StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.PHONE))) {
      validatePhoneNo(
          (String) userRequest.getRequest().get(JsonKey.PHONE),
          (String) userRequest.getRequest().get(JsonKey.COUNTRY_CODE));
    }
    phoneVerifiedValidation(userRequest);
  }

  private void phoneVerifiedValidation(Request userRequest) {
    if (!StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.PHONE))) {
      if (null != userRequest.getRequest().get(JsonKey.PHONE_VERIFIED)) {
        if (userRequest.getRequest().get(JsonKey.PHONE_VERIFIED) instanceof Boolean) {
          if (!((boolean) userRequest.getRequest().get(JsonKey.PHONE_VERIFIED))) {
            ProjectCommonException.throwClientErrorException(ResponseCode.phoneVerifiedError);
          }
        } else {
          ProjectCommonException.throwClientErrorException(ResponseCode.phoneVerifiedError);
        }
      } else {
        ProjectCommonException.throwClientErrorException(ResponseCode.phoneVerifiedError);
      }
    }
  }

  /**
   * This method will do basic validation for user request object.
   *
   * @param userRequest
   */
  public void createUserBasicValidation(Request userRequest) {

    createUserBasicProfileFieldsValidation(userRequest);
    if (userRequest.getRequest().containsKey(JsonKey.ROLES)
        && null != userRequest.getRequest().get(JsonKey.ROLES)
        && !(userRequest.getRequest().get(JsonKey.ROLES) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST),
          ERROR_CODE);
    }
    if (userRequest.getRequest().containsKey(JsonKey.LANGUAGE)
        && null != userRequest.getRequest().get(JsonKey.LANGUAGE)
        && !(userRequest.getRequest().get(JsonKey.LANGUAGE) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.LANGUAGE, JsonKey.LIST),
          ERROR_CODE);
    }
  }

  private void createUserBasicProfileFieldsValidation(Request userRequest) {
    validateParam(
        (String) userRequest.getRequest().get(JsonKey.FIRST_NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.FIRST_NAME);
    if (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EMAIL))
        && StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.PHONE))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.emailorPhoneRequired);
    }

    if (null != userRequest.getRequest().get(JsonKey.DOB)) {
      boolean bool =
          ProjectUtil.isDateValidFormat(
              ProjectUtil.YEAR_MONTH_DATE_FORMAT,
              (String) userRequest.getRequest().get(JsonKey.DOB));
      if (!bool) {
        ProjectCommonException.throwClientErrorException(ResponseCode.dateFormatError);
      }
    }

    if (!StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EMAIL))
        && !ProjectUtil.isEmailvalid((String) userRequest.getRequest().get(JsonKey.EMAIL))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.emailFormatError);
    } else {
      emailVerifiedValidation(userRequest);
    }
  }

  private void emailVerifiedValidation(Request userRequest) {
    if (!StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EMAIL))) {
      if (null != userRequest.getRequest().get(JsonKey.EMAIL_VERIFIED)) {
        if (userRequest.getRequest().get(JsonKey.EMAIL_VERIFIED) instanceof Boolean) {
          if (!((boolean) userRequest.getRequest().get(JsonKey.EMAIL_VERIFIED))) {
            ProjectCommonException.throwClientErrorException(ResponseCode.emailVerifiedError);
          }
        } else {
          ProjectCommonException.throwClientErrorException(ResponseCode.emailVerifiedError);
        }
      } else {
        ProjectCommonException.throwClientErrorException(ResponseCode.emailVerifiedError);
      }
    }
  }

  /**
   * Method to validate Address
   *
   * @param userRequest
   */
  @SuppressWarnings("unchecked")
  private void addressValidation(Request userRequest) {
    Map<String, Object> addrReqMap;
    if (userRequest.getRequest().containsKey(JsonKey.ADDRESS)
        && null != userRequest.getRequest().get(JsonKey.ADDRESS)) {
      if (!(userRequest.getRequest().get(JsonKey.ADDRESS) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ADDRESS, JsonKey.LIST),
            ERROR_CODE);
      } else if (userRequest.getRequest().get(JsonKey.ADDRESS) instanceof List) {
        List<Map<String, Object>> reqList =
            (List<Map<String, Object>>) userRequest.get(JsonKey.ADDRESS);
        for (int i = 0; i < reqList.size(); i++) {
          addrReqMap = reqList.get(i);
          new AddressRequestValidator().validateAddress(addrReqMap, JsonKey.ADDRESS);
        }
      }
    }
  }

  /**
   * Method to validate educational details of the user
   *
   * @param userRequest
   */
  @SuppressWarnings("unchecked")
  private void educationValidation(Request userRequest) {
    Map<String, Object> addrReqMap;
    Map<String, Object> reqMap;
    if (userRequest.getRequest().containsKey(JsonKey.EDUCATION)
        && null != userRequest.getRequest().get(JsonKey.EDUCATION)) {
      if (!(userRequest.getRequest().get(JsonKey.EDUCATION) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.EDUCATION, JsonKey.LIST),
            ERROR_CODE);
      } else if (userRequest.getRequest().get(JsonKey.EDUCATION) instanceof List) {
        List<Map<String, Object>> reqList =
            (List<Map<String, Object>>) userRequest.get(JsonKey.EDUCATION);
        for (int i = 0; i < reqList.size(); i++) {
          reqMap = reqList.get(i);
          if (StringUtils.isBlank((String) reqMap.get(JsonKey.NAME))) {
            ProjectCommonException.throwClientErrorException(ResponseCode.educationNameError);
          }
          if (StringUtils.isBlank((String) reqMap.get(JsonKey.DEGREE))) {
            ProjectCommonException.throwClientErrorException(ResponseCode.educationDegreeError);
          }
          if (reqMap.containsKey(JsonKey.ADDRESS) && null != reqMap.get(JsonKey.ADDRESS)) {
            addrReqMap = (Map<String, Object>) reqMap.get(JsonKey.ADDRESS);
            new AddressRequestValidator().validateAddress(addrReqMap, JsonKey.EDUCATION);
          }
        }
      }
    }
  }

  /**
   * Method to validate jobProfile of a user
   *
   * @param userRequest
   */
  private void jobProfileValidation(Request userRequest) {
    if (userRequest.getRequest().containsKey(JsonKey.JOB_PROFILE)
        && null != userRequest.getRequest().get(JsonKey.JOB_PROFILE)) {
      if (!(userRequest.getRequest().get(JsonKey.JOB_PROFILE) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.JOB_PROFILE, JsonKey.LIST),
            ERROR_CODE);
      } else if (userRequest.getRequest().get(JsonKey.JOB_PROFILE) instanceof List) {
        validateJob(userRequest);
      }
    }
  }

  private void validateJob(Request userRequest) {

    Map<String, Object> reqMap = null;
    List<Map<String, Object>> reqList =
        (List<Map<String, Object>>) userRequest.get(JsonKey.JOB_PROFILE);
    for (int i = 0; i < reqList.size(); i++) {
      reqMap = reqList.get(i);
      validateJoinEndDate(reqMap);
      validateJobOrgNameAndAddress(reqMap);
    }
  }

  private void validateJoinEndDate(Map reqMap) {
    if (null != reqMap.get(JsonKey.JOINING_DATE)) {
      boolean bool =
          ProjectUtil.isDateValidFormat(
              ProjectUtil.YEAR_MONTH_DATE_FORMAT, (String) reqMap.get(JsonKey.JOINING_DATE));
      if (!bool) {
        ProjectCommonException.throwClientErrorException(ResponseCode.dateFormatError);
      }
    }
    if (null != reqMap.get(JsonKey.END_DATE)) {
      boolean bool =
          ProjectUtil.isDateValidFormat(
              ProjectUtil.YEAR_MONTH_DATE_FORMAT, (String) reqMap.get(JsonKey.END_DATE));
      if (!bool) {
        ProjectCommonException.throwClientErrorException(ResponseCode.dateFormatError);
      }
    }
  }

  private void validateJobOrgNameAndAddress(Map reqMap) {
    Map<String, Object> addrReqMap = null;
    if (StringUtils.isBlank((String) reqMap.get(JsonKey.JOB_NAME))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.jobNameError);
    }
    if (StringUtils.isBlank((String) reqMap.get(JsonKey.ORG_NAME))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.organisationNameError);
    }
    if (reqMap.containsKey(JsonKey.ADDRESS) && null != reqMap.get(JsonKey.ADDRESS)) {
      addrReqMap = (Map<String, Object>) reqMap.get(JsonKey.ADDRESS);
      new AddressRequestValidator().validateAddress(addrReqMap, JsonKey.JOB_PROFILE);
    }
  }

  @SuppressWarnings("unchecked")
  public void validateWebPages(Request request) {
    if (request.getRequest().containsKey(JsonKey.WEB_PAGES)) {
      List<Map<String, String>> data =
          (List<Map<String, String>>) request.getRequest().get(JsonKey.WEB_PAGES);
      if (null == data || data.isEmpty()) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidWebPageData);
      }
    }
  }

  private boolean validatePhoneNo(String phone, String countryCode) {
    if (phone.contains("+")) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidPhoneNumber);
    }
    if (ProjectUtil.validatePhone(phone, countryCode)) {
      return true;
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.phoneNoFormatError);
    }
    return false;
  }

  /**
   * This method will validate update user data.
   *
   * @param userRequest Request
   */
  public void validateUpdateUserRequest(Request userRequest) {
    externalIdsValidation(userRequest, JsonKey.UPDATE);
    phoneValidation(userRequest);
    updateUserBasicValidation(userRequest);
    validateAddressField(userRequest);
    validateJobProfileField(userRequest);
    validateEducationField(userRequest);
    validateUserType(userRequest);
    validateUserOrgField(userRequest);

    if (userRequest.getRequest().containsKey(JsonKey.ROOT_ORG_ID)
        && StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.ROOT_ORG_ID))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidRootOrganisationId);
    }
    validateLocationCodes(userRequest);
    validateExtIdTypeAndProvider(userRequest);
    validateFrameworkDetails(userRequest);
    validateRecoveryEmailOrPhone(userRequest);
  }

  private void validateUserOrgField(Request userRequest) {
    Map<String, Object> request = userRequest.getRequest();
    boolean isPrivate =
        BooleanUtils.isTrue((Boolean) userRequest.getContext().get(JsonKey.PRIVATE));
    if (isPrivate
        && StringUtils.isBlank((String) request.get(JsonKey.USER_ID))
        && request.containsKey(JsonKey.ORGANISATIONS)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.USER_ID));
    }

    if (!isPrivate && request.containsKey(JsonKey.ORGANISATIONS)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorUnsupportedField,
          ProjectUtil.formatMessage(
              ResponseCode.errorUnsupportedField.getErrorMessage(), JsonKey.ORGANISATIONS));
    }

    if (isPrivate
        && request.containsKey(JsonKey.ORGANISATIONS)
        && !(request.get(JsonKey.ORGANISATIONS) instanceof List)) {
      throwInvalidUserOrgData();
    }

    if (isPrivate && request.containsKey(JsonKey.ORGANISATIONS)) {
      List<Object> list = (List<Object>) request.get(JsonKey.ORGANISATIONS);
      for (Object map : list) {
        if (!(map instanceof Map)) {
          throwInvalidUserOrgData();
        } else {
          validRolesDataType((Map<String, Object>) map);
        }
      }
    }
  }

  private void validRolesDataType(Map<String, Object> map) {
    String organisationId = (String) map.get(JsonKey.ORGANISATION_ID);
    if (StringUtils.isBlank(organisationId)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ORGANISATION_ID));
    }
    if (map.containsKey(JsonKey.ROLES)) {
      if (!(map.get(JsonKey.ROLES) instanceof List)) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.dataTypeError,
            MessageFormat.format(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST));
      } else if (CollectionUtils.isEmpty((List) map.get(JsonKey.ROLES))) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.emptyRolesProvided, ResponseCode.emptyRolesProvided.getErrorMessage());
      }
    }
  }

  private void throwInvalidUserOrgData() {
    ProjectCommonException.throwClientErrorException(
        ResponseCode.dataTypeError,
        MessageFormat.format(
            ResponseCode.dataTypeError.getErrorMessage(),
            JsonKey.ORGANISATIONS,
            String.join(" ", JsonKey.LIST, " of ", JsonKey.MAP)));
  }

  private void validateAddressField(Request userRequest) {
    if (userRequest.getRequest().get(JsonKey.ADDRESS) != null
        && ((List) userRequest.getRequest().get(JsonKey.ADDRESS)).isEmpty()) {
      ProjectCommonException.throwClientErrorException(ResponseCode.addressRequired);
    }

    if (userRequest.getRequest().get(JsonKey.ADDRESS) != null
        && (!((List) userRequest.getRequest().get(JsonKey.ADDRESS)).isEmpty())) {
      validateUpdateUserAddress(userRequest);
    }
  }

  private void validateJobProfileField(Request userRequest) {
    if (userRequest.getRequest().get(JsonKey.JOB_PROFILE) != null
        && ((List) userRequest.getRequest().get(JsonKey.JOB_PROFILE)).isEmpty()) {
      ProjectCommonException.throwClientErrorException(ResponseCode.jobDetailsRequired);
    }

    if (userRequest.getRequest().get(JsonKey.JOB_PROFILE) != null
        && (!((List) userRequest.getRequest().get(JsonKey.JOB_PROFILE)).isEmpty())) {
      validateUpdateUserJobProfile(userRequest);
    }
  }

  private void validateEducationField(Request userRequest) {
    if (userRequest.getRequest().get(JsonKey.EDUCATION) != null
        && ((List) userRequest.getRequest().get(JsonKey.EDUCATION)).isEmpty()) {
      ProjectCommonException.throwClientErrorException(ResponseCode.educationRequired);
    }

    if (userRequest.getRequest().get(JsonKey.EDUCATION) != null
        && (!((List) userRequest.getRequest().get(JsonKey.EDUCATION)).isEmpty())) {
      validateUpdateUserEducation(userRequest);
    }
  }

  public void externalIdsValidation(Request userRequest, String operation) {
    if (userRequest.getRequest().containsKey(JsonKey.EXTERNAL_IDS)
        && (null != userRequest.getRequest().get(JsonKey.EXTERNAL_IDS))) {
      if (!(userRequest.getRequest().get(JsonKey.EXTERNAL_IDS) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.EXTERNAL_IDS, JsonKey.LIST),
            ERROR_CODE);
      }
      List<Map<String, String>> externalIds =
          (List<Map<String, String>>) userRequest.getRequest().get(JsonKey.EXTERNAL_IDS);
      validateIndividualExternalId(operation, externalIds);
      if (operation.equalsIgnoreCase(JsonKey.CREATE)) {
        checkForDuplicateExternalId(externalIds);
      }
    }
  }

  private void validateIndividualExternalId(
      String operation, List<Map<String, String>> externalIds) {
    // valid operation type for externalIds in user api.
    List<String> operationTypeList = Arrays.asList(JsonKey.ADD, JsonKey.REMOVE, JsonKey.EDIT);
    externalIds
        .stream()
        .forEach(
            identity -> {
              // check for invalid operation type
              if (StringUtils.isNotBlank(identity.get(JsonKey.OPERATION))
                  && (!operationTypeList.contains(
                      (identity.get(JsonKey.OPERATION)).toLowerCase()))) {
                throw new ProjectCommonException(
                    ResponseCode.invalidValue.getErrorCode(),
                    ProjectUtil.formatMessage(
                        ResponseCode.invalidValue.getErrorMessage(),
                        StringFormatter.joinByDot(JsonKey.EXTERNAL_IDS, JsonKey.OPERATION),
                        identity.get(JsonKey.OPERATION),
                        String.join(StringFormatter.COMMA, operationTypeList)),
                    ERROR_CODE);
              }
              // throw exception for invalid operation if other operation type is coming in
              // request
              // other than add or null for create user api
              if (JsonKey.CREATE.equalsIgnoreCase(operation)
                  && StringUtils.isNotBlank(identity.get(JsonKey.OPERATION))
                  && (!JsonKey.ADD.equalsIgnoreCase(((identity.get(JsonKey.OPERATION)))))) {
                throw new ProjectCommonException(
                    ResponseCode.invalidValue.getErrorCode(),
                    ProjectUtil.formatMessage(
                        ResponseCode.invalidValue.getErrorMessage(),
                        StringFormatter.joinByDot(JsonKey.EXTERNAL_IDS, JsonKey.OPERATION),
                        identity.get(JsonKey.OPERATION),
                        JsonKey.ADD),
                    ERROR_CODE);
              }
              validateExternalIdMandatoryParam(JsonKey.ID, identity.get(JsonKey.ID));
              validateExternalIdMandatoryParam(JsonKey.PROVIDER, identity.get(JsonKey.PROVIDER));
              validateExternalIdMandatoryParam(JsonKey.ID_TYPE, identity.get(JsonKey.ID_TYPE));
            });
  }

  private void validateExternalIdMandatoryParam(String param, String paramValue) {
    if (StringUtils.isBlank(paramValue)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              StringFormatter.joinByDot(JsonKey.EXTERNAL_IDS, param)),
          ERROR_CODE);
    }
  }

  private void validateUpdateUserEducation(Request userRequest) {
    List<Map<String, Object>> reqList =
        (List<Map<String, Object>>) userRequest.get(JsonKey.EDUCATION);
    for (int i = 0; i < reqList.size(); i++) {
      Map<String, Object> reqMap = reqList.get(i);
      if (reqMap.containsKey(JsonKey.IS_DELETED)
          && null != reqMap.get(JsonKey.IS_DELETED)
          && ((boolean) reqMap.get(JsonKey.IS_DELETED))
          && StringUtils.isBlank((String) reqMap.get(JsonKey.ID))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.idRequired);
      }
      if (!reqMap.containsKey(JsonKey.IS_DELETED)
          || (reqMap.containsKey(JsonKey.IS_DELETED)
              && (null == reqMap.get(JsonKey.IS_DELETED)
                  || !(boolean) reqMap.get(JsonKey.IS_DELETED)))) {
        educationValidation(userRequest);
      }
    }
  }

  private void validateUpdateUserJobProfile(Request userRequest) {
    List<Map<String, Object>> reqList =
        (List<Map<String, Object>>) userRequest.get(JsonKey.JOB_PROFILE);
    for (int i = 0; i < reqList.size(); i++) {
      Map<String, Object> reqMap = reqList.get(i);
      if (reqMap.containsKey(JsonKey.IS_DELETED)
          && null != reqMap.get(JsonKey.IS_DELETED)
          && ((boolean) reqMap.get(JsonKey.IS_DELETED))
          && StringUtils.isBlank((String) reqMap.get(JsonKey.ID))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.idRequired);
      }
      if (!reqMap.containsKey(JsonKey.IS_DELETED)
          || (reqMap.containsKey(JsonKey.IS_DELETED)
              && (null == reqMap.get(JsonKey.IS_DELETED)
                  || !(boolean) reqMap.get(JsonKey.IS_DELETED)))) {
        jobProfileValidation(userRequest);
      }
    }
  }

  private void validateUpdateUserAddress(Request userRequest) {
    List<Map<String, Object>> reqList =
        (List<Map<String, Object>>) userRequest.get(JsonKey.ADDRESS);
    for (int i = 0; i < reqList.size(); i++) {
      Map<String, Object> reqMap = reqList.get(i);

      if (reqMap.containsKey(JsonKey.IS_DELETED)
          && null != reqMap.get(JsonKey.IS_DELETED)
          && ((boolean) reqMap.get(JsonKey.IS_DELETED))
          && StringUtils.isBlank((String) reqMap.get(JsonKey.ID))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.idRequired);
      }
      if (!reqMap.containsKey(JsonKey.IS_DELETED)
          || (reqMap.containsKey(JsonKey.IS_DELETED)
              && (null == reqMap.get(JsonKey.IS_DELETED)
                  || !(boolean) reqMap.get(JsonKey.IS_DELETED)))) {
        new AddressRequestValidator().validateAddress(reqMap, JsonKey.ADDRESS);
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private void updateUserBasicValidation(Request userRequest) {
    fieldsNotAllowed(
        Arrays.asList(
            JsonKey.REGISTERED_ORG_ID,
            JsonKey.ROOT_ORG_ID,
            JsonKey.CHANNEL,
            JsonKey.USERNAME,
            JsonKey.PROVIDER,
            JsonKey.ID_TYPE),
        userRequest);
    validateUserIdOrExternalId(userRequest);
    if (userRequest.getRequest().containsKey(JsonKey.FIRST_NAME)
        && (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.FIRST_NAME)))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.firstNameRequired);
    }

    if ((userRequest.getRequest().containsKey(JsonKey.EMAIL)
            && userRequest.getRequest().get(JsonKey.EMAIL) != null)
        && !ProjectUtil.isEmailvalid((String) userRequest.getRequest().get(JsonKey.EMAIL))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.emailFormatError);
    }

    if (userRequest.getRequest().containsKey(JsonKey.ROLES)
        && null != userRequest.getRequest().get(JsonKey.ROLES)) {
      if (userRequest.getRequest().get(JsonKey.ROLES) instanceof List
          && ((List) userRequest.getRequest().get(JsonKey.ROLES)).isEmpty()) {
        ProjectCommonException.throwClientErrorException(ResponseCode.rolesRequired);
      } else if (!(userRequest.getRequest().get(JsonKey.ROLES) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST),
            ERROR_CODE);
      }
    }
    validateLangaugeFields(userRequest);
  }

  private void validateUserIdOrExternalId(Request userRequest) {
    if ((StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.USER_ID))
            && StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.ID)))
        && (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID))
            || StringUtils.isBlank(
                (String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER))
            || StringUtils.isBlank(
                (String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_TYPE)))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (StringFormatter.joinByOr(
                  JsonKey.USER_ID,
                  StringFormatter.joinByAnd(
                      StringFormatter.joinByComma(JsonKey.EXTERNAL_ID, JsonKey.EXTERNAL_ID_TYPE),
                      JsonKey.EXTERNAL_ID_PROVIDER)))),
          ERROR_CODE);
    }
  }

  private void validateLangaugeFields(Request userRequest) {
    if (userRequest.getRequest().containsKey(JsonKey.LANGUAGE)
        && null != userRequest.getRequest().get(JsonKey.LANGUAGE)) {
      if (userRequest.getRequest().get(JsonKey.LANGUAGE) instanceof List
          && ((List) userRequest.getRequest().get(JsonKey.LANGUAGE)).isEmpty()) {
        ProjectCommonException.throwClientErrorException(ResponseCode.languageRequired);
      } else if (!(userRequest.getRequest().get(JsonKey.LANGUAGE) instanceof List)) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.dataTypeError.getErrorMessage(), JsonKey.LANGUAGE, JsonKey.LIST),
            ERROR_CODE);
      }
    }
  }

  /**
   * This method will validate change password requested data.
   *
   * @param userRequest Request
   */
  public void validateChangePassword(Request userRequest) {
    if (userRequest.getRequest().get(JsonKey.PASSWORD) == null
        || (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.PASSWORD)))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.passwordRequired);
    }
    if (userRequest.getRequest().get(JsonKey.NEW_PASSWORD) == null) {
      ProjectCommonException.throwClientErrorException(ResponseCode.newPasswordRequired);
    }
    if (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.NEW_PASSWORD))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.newPasswordEmpty);
    }
  }

  /**
   * This method will validate verifyUser requested data.
   *
   * @param userRequest Request
   */
  public void validateVerifyUser(Request userRequest) {
    if (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.LOGIN_ID))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.loginIdRequired);
    }
  }

  /**
   * Either user will send UserId or (provider and externalId).
   *
   * @param request
   */
  public void validateAssignRole(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.USER_ID))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userIdRequired);
    }

    if (request.getRequest().get(JsonKey.ROLES) == null
        || !(request.getRequest().get(JsonKey.ROLES) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.ROLES, JsonKey.LIST),
          ERROR_CODE);
    }

    String organisationId = (String) request.getRequest().get(JsonKey.ORGANISATION_ID);
    String externalId = (String) request.getRequest().get(JsonKey.EXTERNAL_ID);
    String provider = (String) request.getRequest().get(JsonKey.PROVIDER);
    if (StringUtils.isBlank(organisationId)
        && (StringUtils.isBlank(externalId) || StringUtils.isBlank(provider))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (StringFormatter.joinByOr(
                  JsonKey.ORGANISATION_ID,
                  StringFormatter.joinByAnd(JsonKey.EXTERNAL_ID, JsonKey.PROVIDER)))),
          ERROR_CODE);
    }
  }

  /** @param request */
  public void validateForgotPassword(Request request) {
    if (request.getRequest().get(JsonKey.USERNAME) == null
        || StringUtils.isBlank((String) request.getRequest().get(JsonKey.USERNAME))) {
      throw new ProjectCommonException(
          ResponseCode.userNameRequired.getErrorCode(),
          ResponseCode.userNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * This method will validate bulk api user data.
   *
   * @param userRequest Request
   */
  public void validateBulkUserData(Request userRequest) {
    externalIdsValidation(userRequest, JsonKey.BULK_USER_UPLOAD);
    createUserBasicValidation(userRequest);
    phoneValidation(userRequest);
    validateWebPages(userRequest);
    validateExtIdTypeAndProvider(userRequest);
    if (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.USERNAME))
        && (StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER))
            || StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID))
            || StringUtils.isBlank(
                (String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_TYPE)))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (StringFormatter.joinByOr(
                  JsonKey.USERNAME,
                  StringFormatter.joinByAnd(
                      StringFormatter.joinByComma(JsonKey.EXTERNAL_ID, JsonKey.EXTERNAL_ID_TYPE),
                      JsonKey.EXTERNAL_ID_PROVIDER)))),
          ERROR_CODE);
    }
  }

  private void validateExtIdTypeAndProvider(Request userRequest) {
    if ((StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER))
        && StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID))
        && StringUtils.isNotBlank(
            (String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_TYPE)))) {
      return;
    } else if (StringUtils.isBlank(
            (String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER))
        && StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID))
        && StringUtils.isBlank((String) userRequest.getRequest().get(JsonKey.EXTERNAL_ID_TYPE))) {
      return;
    } else {
      throw new ProjectCommonException(
          ResponseCode.dependentParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dependentParamsMissing.getErrorMessage(),
              StringFormatter.joinByComma(
                  JsonKey.EXTERNAL_ID, JsonKey.EXTERNAL_ID_TYPE, JsonKey.EXTERNAL_ID_PROVIDER)),
          ERROR_CODE);
    }
  }

  private void checkForDuplicateExternalId(List<Map<String, String>> list) {
    List<Map<String, String>> checkedList = new ArrayList<>();
    for (Map<String, String> externalId : list) {
      for (Map<String, String> checkedExternalId : checkedList) {
        String provider = checkedExternalId.get(JsonKey.PROVIDER);
        String idType = checkedExternalId.get(JsonKey.ID_TYPE);
        if (provider.equalsIgnoreCase(externalId.get(JsonKey.PROVIDER))
            && idType.equalsIgnoreCase(externalId.get(JsonKey.ID_TYPE))) {
          String exceptionMsg =
              MessageFormat.format(
                  ResponseCode.duplicateExternalIds.getErrorMessage(), idType, provider);
          ProjectCommonException.throwClientErrorException(
              ResponseCode.duplicateExternalIds, exceptionMsg);
        }
      }
      checkedList.add(externalId);
    }
  }

  @SuppressWarnings("unchecked")
  private void validateFrameworkDetails(Request request) {
    if (request.getRequest().containsKey(JsonKey.FRAMEWORK)
        && (!(request.getRequest().get(JsonKey.FRAMEWORK) instanceof Map))) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE,
          JsonKey.FRAMEWORK,
          JsonKey.MAP);
    } else {
      Map<String, Object> framework =
          (Map<String, Object>) request.getRequest().get(JsonKey.FRAMEWORK);
      if (!MapUtils.isEmpty(framework)) {
        if (framework.get(JsonKey.ID) instanceof List) {
          List<String> frameworkId = (List<String>) framework.get(JsonKey.ID);
          if (CollectionUtils.isEmpty(frameworkId)) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.mandatoryParamsMissing,
                MessageFormat.format(
                    ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                    StringFormatter.joinByDot(JsonKey.FRAMEWORK, JsonKey.ID)));
          } else if (frameworkId.size() > 1) {
            throw new ProjectCommonException(
                ResponseCode.errorInvalidParameterSize.getErrorCode(),
                ResponseCode.errorInvalidParameterSize.getErrorMessage(),
                ERROR_CODE,
                StringFormatter.joinByDot(JsonKey.FRAMEWORK, JsonKey.ID),
                "1",
                String.valueOf(frameworkId.size()));
          }
        } else if (framework.get(JsonKey.ID) instanceof String) {
          String frameworkId = (String) framework.get(JsonKey.ID);
          if (StringUtils.isBlank(frameworkId)) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.mandatoryParamsMissing,
                MessageFormat.format(
                    ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                    StringFormatter.joinByDot(JsonKey.FRAMEWORK, JsonKey.ID)));
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void validateMandatoryFrameworkFields(
      Map<String, Object> userMap,
      List<String> frameworkFields,
      List<String> frameworkMandatoryFields) {
    if (userMap.containsKey(JsonKey.FRAMEWORK)) {
      Map<String, Object> frameworkRequest = (Map<String, Object>) userMap.get(JsonKey.FRAMEWORK);
      for (String field : frameworkFields) {
        if (CollectionUtils.isNotEmpty(frameworkMandatoryFields)
            && frameworkMandatoryFields.contains(field)) {
          if (!frameworkRequest.containsKey(field)) {
            validateParam(null, ResponseCode.mandatoryParamsMissing, field);
          }
          validateListParamWithPrefix(frameworkRequest, JsonKey.FRAMEWORK, field);
          List<String> fieldValue = (List) frameworkRequest.get(field);
          if (fieldValue.isEmpty()) {
            throw new ProjectCommonException(
                ResponseCode.errorMandatoryParamsEmpty.getErrorCode(),
                ResponseCode.errorMandatoryParamsEmpty.getErrorMessage(),
                ERROR_CODE,
                StringFormatter.joinByDot(JsonKey.FRAMEWORK, field));
          }
        } else {
          if (frameworkRequest.containsKey(field)
              && frameworkRequest.get(field) != null
              && !(frameworkRequest.get(field) instanceof List)) {
            throw new ProjectCommonException(
                ResponseCode.dataTypeError.getErrorCode(),
                ResponseCode.dataTypeError.getErrorMessage(),
                ERROR_CODE,
                field,
                JsonKey.LIST);
          }
        }
      }
      List<String> frameworkRequestFieldList =
          frameworkRequest.keySet().stream().collect(Collectors.toList());
      for (String frameworkRequestField : frameworkRequestFieldList) {
        if (!frameworkFields.contains(frameworkRequestField)) {
          throw new ProjectCommonException(
              ResponseCode.errorUnsupportedField.getErrorCode(),
              ResponseCode.errorUnsupportedField.getErrorMessage(),
              ERROR_CODE,
              StringFormatter.joinByDot(JsonKey.FRAMEWORK, frameworkRequestField));
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void validateFrameworkCategoryValues(
      Map<String, Object> userMap, Map<String, List<Map<String, String>>> frameworkMap) {
    Map<String, List<String>> fwRequest =
        (Map<String, List<String>>) userMap.get(JsonKey.FRAMEWORK);
    for (Map.Entry<String, List<String>> fwRequestFieldEntry : fwRequest.entrySet()) {
      if (!fwRequestFieldEntry.getValue().isEmpty()) {
        List<String> allowedFieldValues =
            getKeyValueFromFrameWork(fwRequestFieldEntry.getKey(), frameworkMap)
                .stream()
                .map(fieldMap -> fieldMap.get(JsonKey.NAME))
                .collect(Collectors.toList());

        List<String> fwRequestFieldList = fwRequestFieldEntry.getValue();

        for (String fwRequestField : fwRequestFieldList) {
          if (!allowedFieldValues.contains(fwRequestField)) {
            throw new ProjectCommonException(
                ResponseCode.invalidParameterValue.getErrorCode(),
                ResponseCode.invalidParameterValue.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                fwRequestField,
                StringFormatter.joinByDot(JsonKey.FRAMEWORK, fwRequestFieldEntry.getKey()));
          }
        }
      }
    }
  }

  private List<Map<String, String>> getKeyValueFromFrameWork(
      String key, Map<String, List<Map<String, String>>> frameworkMap) {
    if (frameworkMap.get(key) == null) {
      throw new ProjectCommonException(
          ResponseCode.errorUnsupportedField.getErrorCode(),
          MessageFormat.format(
              ResponseCode.errorUnsupportedField.getErrorMessage(),
              key + " in " + JsonKey.FRAMEWORK),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return frameworkMap.get(key);
  }

  private void validateUserType(Request userRequest) {
    String userType = (String) userRequest.getRequest().get(JsonKey.USER_TYPE);

    if (userType != null
        && (!JsonKey.OTHER.equalsIgnoreCase(userType))
        && (!JsonKey.TEACHER.equalsIgnoreCase(userType))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              new String[] {userType, JsonKey.USER_TYPE}));
    }
  }

  public void validateUserMergeRequest(
      Request request, String authUserToken, String sourceUserToken) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.FROM_ACCOUNT_ID))) {
      throw new ProjectCommonException(
          ResponseCode.fromAccountIdRequired.getErrorCode(),
          ResponseCode.fromAccountIdRequired.getErrorMessage(),
          ERROR_CODE);
    }

    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.TO_ACCOUNT_ID))) {
      throw new ProjectCommonException(
          ResponseCode.toAccountIdRequired.getErrorCode(),
          ResponseCode.toAccountIdRequired.getErrorMessage(),
          ERROR_CODE);
    }

    if (StringUtils.isBlank(authUserToken)) {
      createClientError(
          ResponseCode.mandatoryHeaderParamsMissing, JsonKey.X_AUTHENTICATED_USER_TOKEN);
    }

    if (StringUtils.isBlank(authUserToken)) {
      createClientError(
          ResponseCode.mandatoryHeaderParamsMissing, JsonKey.X_AUTHENTICATED_USER_TOKEN);
    }
    if (StringUtils.isBlank(sourceUserToken)) {
      createClientError(ResponseCode.mandatoryHeaderParamsMissing, JsonKey.X_SOURCE_USER_TOKEN);
    }
  }

  public void validateCertValidationRequest(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.CERT_ID))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.CERT_ID);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.ACCESS_CODE))) {
      createClientError(ResponseCode.mandatoryParamsMissing, JsonKey.ACCESS_CODE);
    }
  }

  private void createClientError(ResponseCode responseCode, String field) {
    throw new ProjectCommonException(
        responseCode.getErrorCode(),
        ProjectUtil.formatMessage(responseCode.getErrorMessage(), field),
        ERROR_CODE);
  }

  private void validateRecoveryEmailOrPhone(Request userRequest) {
    if (StringUtils.isNotBlank((String) userRequest.get(JsonKey.RECOVERY_EMAIL))) {
      validateEmail((String) userRequest.get(JsonKey.RECOVERY_EMAIL));
    }
    if (StringUtils.isNotBlank((String) userRequest.get(JsonKey.RECOVERY_PHONE))) {
      validatePhone((String) userRequest.get(JsonKey.RECOVERY_PHONE));
    }
  }
}
