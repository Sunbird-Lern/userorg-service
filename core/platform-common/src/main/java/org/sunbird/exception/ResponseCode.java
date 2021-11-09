package org.sunbird.exception;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;

/** @author Manzarul */
public enum ResponseCode {
  unAuthorized(ResponseMessage.Key.UNAUTHORIZED_USER, ResponseMessage.Message.UNAUTHORIZED_USER),
  operationTimeout(
      ResponseMessage.Key.OPERATION_TIMEOUT, ResponseMessage.Message.OPERATION_TIMEOUT),
  invalidOperationName(
      ResponseMessage.Key.INVALID_OPERATION_NAME, ResponseMessage.Message.INVALID_OPERATION_NAME),
  invalidRequestData(
      ResponseMessage.Key.INVALID_REQUESTED_DATA, ResponseMessage.Message.INVALID_REQUESTED_DATA),
  apiKeyRequired(
      ResponseMessage.Key.API_KEY_MISSING_ERROR, ResponseMessage.Message.API_KEY_MISSING_ERROR),
  internalError(ResponseMessage.Key.INTERNAL_ERROR, ResponseMessage.Message.INTERNAL_ERROR),
  dbInsertionError(
      ResponseMessage.Key.DB_INSERTION_FAIL, ResponseMessage.Message.DB_INSERTION_FAIL),
  dbUpdateError(ResponseMessage.Key.DB_UPDATE_FAIL, ResponseMessage.Message.DB_UPDATE_FAIL),
  success(ResponseMessage.Key.SUCCESS_MESSAGE, ResponseMessage.Message.SUCCESS_MESSAGE),
  emailFormatError(ResponseMessage.Key.EMAIL_FORMAT, ResponseMessage.Message.EMAIL_FORMAT),
  firstNameRequired(
      ResponseMessage.Key.FIRST_NAME_MISSING, ResponseMessage.Message.FIRST_NAME_MISSING),
  channelUniquenessInvalid(
      ResponseMessage.Key.CHANNEL_SHOULD_BE_UNIQUE,
      ResponseMessage.Message.CHANNEL_SHOULD_BE_UNIQUE),
  errorDuplicateEntry(
      ResponseMessage.Key.ERROR_DUPLICATE_ENTRY, ResponseMessage.Message.ERROR_DUPLICATE_ENTRY),
  unableToParseData(
      ResponseMessage.Key.UNABLE_TO_PARSE_DATA, ResponseMessage.Message.UNABLE_TO_PARSE_DATA),
  invalidOrgData(ResponseMessage.Key.INVALID_ORG_DATA, ResponseMessage.Message.INVALID_ORG_DATA),
  invalidRootOrganisationId(
      ResponseMessage.Key.INVALID_ROOT_ORGANIZATION,
      ResponseMessage.Message.INVALID_ROOT_ORGANIZATION),
  invalidUsrData(ResponseMessage.Key.INVALID_USR_DATA, ResponseMessage.Message.INVALID_USR_DATA),
  errorInvalidOTP(ResponseMessage.Key.ERROR_INVALID_OTP, ResponseMessage.Message.ERROR_INVALID_OTP),
  emailAlreadyExistError(ResponseMessage.Key.EMAIL_IN_USE, ResponseMessage.Message.EMAIL_IN_USE),
  userNameRequired(ResponseMessage.Key.USERNAME_MISSING, ResponseMessage.Message.USERNAME_MISSING),
  userNameAlreadyExistError(
      ResponseMessage.Key.USERNAME_IN_USE, ResponseMessage.Message.USERNAME_IN_USE),
  userIdRequired(ResponseMessage.Key.USERID_MISSING, ResponseMessage.Message.USERID_MISSING),
  authTokenRequired(
      ResponseMessage.Key.AUTH_TOKEN_MISSING, ResponseMessage.Message.AUTH_TOKEN_MISSING),
  emailANDUserNameAlreadyExistError(
      ResponseMessage.Key.USERNAME_EMAIL_IN_USE, ResponseMessage.Message.USERNAME_EMAIL_IN_USE),
  keyCloakDefaultError(
      ResponseMessage.Key.KEY_CLOAK_DEFAULT_ERROR, ResponseMessage.Message.KEY_CLOAK_DEFAULT_ERROR),
  invalidOrgId(ResponseMessage.Key.INVALID_ORG_ID, ResponseMessage.Key.INVALID_ORG_ID),
  invalidOrgStatus(ResponseMessage.Key.INVALID_ORG_STATUS, ResponseMessage.Key.INVALID_ORG_STATUS),
  invalidData(ResponseMessage.Key.INVALID_DATA, ResponseMessage.Message.INVALID_DATA),
  actorConnectionError(
      ResponseMessage.Key.ACTOR_CONNECTION_ERROR, ResponseMessage.Message.ACTOR_CONNECTION_ERROR),
  userAlreadyExists(
      ResponseMessage.Key.USER_ALREADY_EXISTS, ResponseMessage.Message.USER_ALREADY_EXISTS),
  invalidUserId(ResponseMessage.Key.INVALID_USER_ID, ResponseMessage.Message.INVALID_USER_ID),
  loginIdRequired(ResponseMessage.Key.LOGIN_ID_MISSING, ResponseMessage.Message.LOGIN_ID_MISSING),
  userNotFound(ResponseMessage.Key.USER_NOT_FOUND, ResponseMessage.Message.USER_NOT_FOUND),
  dataTypeError(ResponseMessage.Key.DATA_TYPE_ERROR, ResponseMessage.Message.DATA_TYPE_ERROR),
  errorAttributeConflict(
      ResponseMessage.Key.ERROR_ATTRIBUTE_CONFLICT,
      ResponseMessage.Message.ERROR_ATTRIBUTE_CONFLICT),
  rolesRequired(ResponseMessage.Key.ROLES_MISSING, ResponseMessage.Message.ROLES_MISSING),
  profileUserTypesRequired(ResponseMessage.Key.PROFILE_USER_TYPES_MISSING, ResponseMessage.Message.PROFILE_USER_TYPES_MISSING),
  emptyRolesProvided(
      ResponseMessage.Key.EMPTY_ROLES_PROVIDED, ResponseMessage.Message.EMPTY_ROLES_PROVIDED),
  contentTypeRequiredError(
      ResponseMessage.Key.CONTENT_TYPE_ERROR, ResponseMessage.Message.CONTENT_TYPE_ERROR),
  invalidPropertyError(
      ResponseMessage.Key.INVALID_PROPERTY_ERROR, ResponseMessage.Message.INVALID_PROPERTY_ERROR),
  dataSizeError(ResponseMessage.Key.DATA_SIZE_EXCEEDED, ResponseMessage.Message.DATA_SIZE_EXCEEDED),
  userAccountlocked(
      ResponseMessage.Key.USER_ACCOUNT_BLOCKED, ResponseMessage.Message.USER_ACCOUNT_BLOCKED),
  userAlreadyActive(
      ResponseMessage.Key.USER_ALREADY_ACTIVE, ResponseMessage.Message.USER_ALREADY_ACTIVE),
  userAlreadyInactive(
      ResponseMessage.Key.USER_ALREADY_INACTIVE, ResponseMessage.Message.USER_ALREADY_INACTIVE),
  dateFormatError(
      ResponseMessage.Key.DATE_FORMAT_ERRROR, ResponseMessage.Message.DATE_FORMAT_ERRROR),
  csvError(ResponseMessage.Key.INVALID_CSV_FILE, ResponseMessage.Message.INVALID_CSV_FILE),
  invalidObjectType(
      ResponseMessage.Key.INVALID_OBJECT_TYPE, ResponseMessage.Message.INVALID_OBJECT_TYPE),
  csvFileEmpty(ResponseMessage.Key.EMPTY_CSV_FILE, ResponseMessage.Message.EMPTY_CSV_FILE),
  invalidChannel(ResponseMessage.Key.INVALID_CHANNEL, ResponseMessage.Message.INVALID_CHANNEL),
  emailSubjectError(
      ResponseMessage.Key.EMAIL_SUBJECT_ERROR, ResponseMessage.Message.EMAIL_SUBJECT_ERROR),
  emailBodyError(ResponseMessage.Key.EMAIL_BODY_ERROR, ResponseMessage.Message.EMAIL_BODY_ERROR),
  storageContainerNameMandatory(
      ResponseMessage.Key.STORAGE_CONTAINER_NAME_MANDATORY,
      ResponseMessage.Message.STORAGE_CONTAINER_NAME_MANDATORY),
  invalidRole(ResponseMessage.Key.INVALID_ROLE, ResponseMessage.Message.INVALID_ROLE),
  saltValue(ResponseMessage.Key.INVALID_SALT, ResponseMessage.Message.INVALID_SALT),
  titleRequired(ResponseMessage.Key.TITLE_REQUIRED, ResponseMessage.Message.TITLE_REQUIRED),
  noteRequired(ResponseMessage.Key.NOTE_REQUIRED, ResponseMessage.Message.NOTE_REQUIRED),
  contentIdError(ResponseMessage.Key.CONTENT_ID_ERROR, ResponseMessage.Message.CONTENT_ID_ERROR),
  invalidTags(ResponseMessage.Key.INVALID_TAGS, ResponseMessage.Message.INVALID_TAGS),
  invalidNoteId(ResponseMessage.Key.NOTE_ID_INVALID, ResponseMessage.Message.NOTE_ID_INVALID),
  userDataEncryptionError(
      ResponseMessage.Key.USER_DATA_ENCRYPTION_ERROR,
      ResponseMessage.Message.USER_DATA_ENCRYPTION_ERROR),
  phoneNoFormatError(
      ResponseMessage.Key.INVALID_PHONE_NO_FORMAT, ResponseMessage.Message.INVALID_PHONE_NO_FORMAT),
  invalidOrgType(
      ResponseMessage.Key.INVALID_ORG_TYPE_ERROR, ResponseMessage.Message.INVALID_ORG_TYPE_ERROR),
  emailorPhoneorManagedByRequired(
      ResponseMessage.Key.EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING,
      ResponseMessage.Message.EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING),
  OnlyEmailorPhoneorManagedByRequired(
      ResponseMessage.Key.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED,
      ResponseMessage.Message.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED),
  PhoneNumberInUse(
      ResponseMessage.Key.PHONE_ALREADY_IN_USE, ResponseMessage.Message.PHONE_ALREADY_IN_USE),
  emailInUse(ResponseMessage.Key.EMAIL_IN_USE, ResponseMessage.Message.EMAIL_IN_USE),
  invalidPhoneNumber(
      ResponseMessage.Key.INVALID_PHONE_NUMBER, ResponseMessage.Message.INVALID_PHONE_NUMBER),
  invalidCountryCode(
      ResponseMessage.Key.INVALID_COUNTRY_CODE, ResponseMessage.Message.INVALID_COUNTRY_CODE),
  locationIdRequired(
      ResponseMessage.Key.LOCATION_ID_REQUIRED, ResponseMessage.Message.LOCATION_ID_REQUIRED),
  channelRegFailed(
      ResponseMessage.Key.CHANNEL_REG_FAILED, ResponseMessage.Message.CHANNEL_REG_FAILED),
  slugIsNotUnique(
      ResponseMessage.Key.SLUG_IS_NOT_UNIQUE, ResponseMessage.Message.SLUG_IS_NOT_UNIQUE),
  resourceNotFound(
      ResponseMessage.Key.RESOURCE_NOT_FOUND, ResponseMessage.Message.RESOURCE_NOT_FOUND),
  sizeLimitExceed(
      ResponseMessage.Key.MAX_ALLOWED_SIZE_LIMIT_EXCEED,
      ResponseMessage.Message.MAX_ALLOWED_SIZE_LIMIT_EXCEED),
  inactiveUser(ResponseMessage.Key.INACTIVE_USER, ResponseMessage.Message.INACTIVE_USER),
  orgDoesNotExist(ResponseMessage.Key.ORG_NOT_EXIST, ResponseMessage.Message.ORG_NOT_EXIST),
  alreadyExists(ResponseMessage.Key.ALREADY_EXISTS, ResponseMessage.Message.ALREADY_EXISTS),
  invalidValue(ResponseMessage.Key.INVALID_VALUE, ResponseMessage.Message.INVALID_VALUE),
  invalidParameter(
      ResponseMessage.Key.INVALID_PARAMETER, ResponseMessage.Message.INVALID_PARAMETER),
  invalidLocationDeleteRequest(
      ResponseMessage.Key.INVALID_LOCATION_DELETE_REQUEST,
      ResponseMessage.Message.INVALID_LOCATION_DELETE_REQUEST),
  mandatoryParamsMissing(
      ResponseMessage.Key.MANDATORY_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_PARAMETER_MISSING),
  errorMandatoryParamsEmpty(
      ResponseMessage.Key.ERROR_MANDATORY_PARAMETER_EMPTY,
      ResponseMessage.Message.ERROR_MANDATORY_PARAMETER_EMPTY),
  errorNoFrameworkFound(
      ResponseMessage.Key.ERROR_NO_FRAMEWORK_FOUND,
      ResponseMessage.Message.ERROR_NO_FRAMEWORK_FOUND),
  unupdatableField(
      ResponseMessage.Key.UPDATE_NOT_ALLOWED, ResponseMessage.Message.UPDATE_NOT_ALLOWED),
  invalidParameterValue(
      ResponseMessage.Key.INVALID_PARAMETER_VALUE, ResponseMessage.Message.INVALID_PARAMETER_VALUE),
  parentNotAllowed(
      ResponseMessage.Key.PARENT_NOT_ALLOWED, ResponseMessage.Message.PARENT_NOT_ALLOWED),
  missingFileAttachment(
      ResponseMessage.Key.MISSING_FILE_ATTACHMENT, ResponseMessage.Message.MISSING_FILE_ATTACHMENT),
  fileAttachmentSizeNotConfigured(
      ResponseMessage.Key.FILE_ATTACHMENT_SIZE_NOT_CONFIGURED,
      ResponseMessage.Message.FILE_ATTACHMENT_SIZE_NOT_CONFIGURED),
  emptyFile(ResponseMessage.Key.EMPTY_FILE, ResponseMessage.Message.EMPTY_FILE),
  invalidColumns(ResponseMessage.Key.INVALID_COLUMNS, ResponseMessage.Message.INVALID_COLUMNS),
  conflictingOrgLocations(
      ResponseMessage.Key.CONFLICTING_ORG_LOCATIONS,
      ResponseMessage.Message.CONFLICTING_ORG_LOCATIONS),
  unableToCommunicateWithActor(
      ResponseMessage.Key.UNABLE_TO_COMMUNICATE_WITH_ACTOR,
      ResponseMessage.Message.UNABLE_TO_COMMUNICATE_WITH_ACTOR),
  emptyHeaderLine(ResponseMessage.Key.EMPTY_HEADER_LINE, ResponseMessage.Message.EMPTY_HEADER_LINE),
  invalidRequestParameter(
      ResponseMessage.Key.INVALID_REQUEST_PARAMETER,
      ResponseMessage.Message.INVALID_REQUEST_PARAMETER),
  rootOrgAssociationError(
      ResponseMessage.Key.ROOT_ORG_ASSOCIATION_ERROR,
      ResponseMessage.Message.ROOT_ORG_ASSOCIATION_ERROR),
  dependentParameterMissing(
      ResponseMessage.Key.DEPENDENT_PARAMETER_MISSING,
      ResponseMessage.Message.DEPENDENT_PARAMETER_MISSING),
  externalIdNotFound(
      ResponseMessage.Key.EXTERNALID_NOT_FOUND, ResponseMessage.Message.EXTERNALID_NOT_FOUND),
  externalIdAssignedToOtherUser(
      ResponseMessage.Key.EXTERNALID_ASSIGNED_TO_OTHER_USER,
      ResponseMessage.Message.EXTERNALID_ASSIGNED_TO_OTHER_USER),
  dependentParamsMissing(
      ResponseMessage.Key.DEPENDENT_PARAMETER_MISSING,
      ResponseMessage.Message.DEPENDENT_PARAMS_MISSING),
  duplicateExternalIds(
      ResponseMessage.Key.DUPLICATE_EXTERNAL_IDS, ResponseMessage.Message.DUPLICATE_EXTERNAL_IDS),
  emailNotSentRecipientsExceededMaxLimit(
      ResponseMessage.Key.EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT,
      ResponseMessage.Message.EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT),
  parameterMismatch(
      ResponseMessage.Key.PARAMETER_MISMATCH, ResponseMessage.Message.PARAMETER_MISMATCH),
  errorForbidden(ResponseMessage.Key.FORBIDDEN, ResponseMessage.Message.FORBIDDEN),
  errorConfigLoadEmptyString(
      ResponseMessage.Key.ERROR_CONFIG_LOAD_EMPTY_STRING,
      ResponseMessage.Message.ERROR_CONFIG_LOAD_EMPTY_STRING),
  errorConfigLoadParseString(
      ResponseMessage.Key.ERROR_CONFIG_LOAD_PARSE_STRING,
      ResponseMessage.Message.ERROR_CONFIG_LOAD_PARSE_STRING),
  errorConfigLoadEmptyConfig(
      ResponseMessage.Key.ERROR_CONFIG_LOAD_EMPTY_CONFIG,
      ResponseMessage.Message.ERROR_CONFIG_LOAD_EMPTY_CONFIG),
  errorNoRootOrgAssociated(
      ResponseMessage.Key.ERROR_NO_ROOT_ORG_ASSOCIATED,
      ResponseMessage.Message.ERROR_NO_ROOT_ORG_ASSOCIATED),
  errorUnsupportedCloudStorage(
      ResponseMessage.Key.ERROR_UNSUPPORTED_CLOUD_STORAGE,
      ResponseMessage.Message.ERROR_UNSUPPORTED_CLOUD_STORAGE),
  errorUnsupportedField(
      ResponseMessage.Key.ERROR_UNSUPPORTED_FIELD, ResponseMessage.Message.ERROR_UNSUPPORTED_FIELD),
  errorCsvNoDataRows(
      ResponseMessage.Key.ERROR_CSV_NO_DATA_ROWS, ResponseMessage.Message.ERROR_CSV_NO_DATA_ROWS),
  errorInactiveOrg(
      ResponseMessage.Key.ERROR_INACTIVE_ORG, ResponseMessage.Message.ERROR_INACTIVE_ORG),
  errorDuplicateEntries(
      ResponseMessage.Key.ERROR_DUPLICATE_ENTRIES, ResponseMessage.Message.ERROR_DUPLICATE_ENTRIES),
  errorConflictingValues(
      ResponseMessage.Key.ERROR_CONFLICTING_VALUES,
      ResponseMessage.Message.ERROR_CONFLICTING_VALUES),
  errorConflictingRootOrgId(
      ResponseMessage.Key.ERROR_CONFLICTING_ROOT_ORG_ID,
      ResponseMessage.Message.ERROR_CONFLICTING_ROOT_ORG_ID),
  errorInvalidParameterSize(
      ResponseMessage.Key.ERROR_INVALID_PARAMETER_SIZE,
      ResponseMessage.Message.ERROR_INVALID_PARAMETER_SIZE),
  errorRateLimitExceeded(
      ResponseMessage.Key.ERROR_RATE_LIMIT_EXCEEDED,
      ResponseMessage.Message.ERROR_RATE_LIMIT_EXCEEDED),
  invalidRequestTimeout(
      ResponseMessage.Key.INVALID_REQUEST_TIMEOUT, ResponseMessage.Message.INVALID_REQUEST_TIMEOUT),
  errorUserMigrationFailed(
      ResponseMessage.Key.ERROR_USER_MIGRATION_FAILED,
      ResponseMessage.Message.ERROR_USER_MIGRATION_FAILED),
  invalidIdentifier(
      ResponseMessage.Key.VALID_IDENTIFIER_ABSENSE,
      ResponseMessage.Message.IDENTIFIER_VALIDATION_FAILED),
  fromAccountIdRequired(
      ResponseMessage.Key.FROM_ACCOUNT_ID_MISSING, ResponseMessage.Message.FROM_ACCOUNT_ID_MISSING),
  toAccountIdRequired(
      ResponseMessage.Key.TO_ACCOUNT_ID_MISSING, ResponseMessage.Message.TO_ACCOUNT_ID_MISSING),
  mandatoryHeaderParamsMissing(
      ResponseMessage.Key.MANDATORY_HEADER_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_HEADER_PARAMETER_MISSING),
  recoveryParamsMatchException(
      ResponseMessage.Key.RECOVERY_PARAM_MATCH_EXCEPTION,
      ResponseMessage.Message.RECOVERY_PARAM_MATCH_EXCEPTION),
  accountNotFound(ResponseMessage.Key.ACCOUNT_NOT_FOUND, ResponseMessage.Message.ACCOUNT_NOT_FOUND),
  invalidElementInList(
      ResponseMessage.Key.INVALID_ELEMENT_IN_LIST, ResponseMessage.Message.INVALID_ELEMENT_IN_LIST),
  passwordValidation(
      ResponseMessage.Key.INVALID_PASSWORD, ResponseMessage.Message.INVALID_PASSWORD),
  otpVerificationFailed(
      ResponseMessage.Key.OTP_VERIFICATION_FAILED, ResponseMessage.Message.OTP_VERIFICATION_FAILED),
  serviceUnAvailable(
      ResponseMessage.Key.SERVICE_UNAVAILABLE, ResponseMessage.Message.SERVICE_UNAVAILABLE),
  managedByNotAllowed(
      ResponseMessage.Key.MANAGED_BY_NOT_ALLOWED, ResponseMessage.Message.MANAGED_BY_NOT_ALLOWED),
  managedUserLimitExceeded(
      ResponseMessage.Key.MANAGED_USER_LIMIT_EXCEEDED,
      ResponseMessage.Message.MANAGED_USER_LIMIT_EXCEEDED),
  unableToConnectToAdminUtil(
      ResponseMessage.Key.UNABLE_TO_CONNECT_TO_ADMINUTIL,
      ResponseMessage.Message.UNABLE_TO_CONNECT_TO_ADMINUTIL),
  dataEncryptionError(
      ResponseMessage.Key.DATA_ENCRYPTION_ERROR, ResponseMessage.Message.DATA_ENCRYPTION_ERROR),
  invalidCaptcha(ResponseMessage.Key.INVALID_CAPTCHA, ResponseMessage.Message.INVALID_CAPTCHA),
  preferenceAlreadyExists(
      ResponseMessage.Key.PREFERENCE_ALREADY_EXIST,
      ResponseMessage.Message.PREFERENCE_ALREADY_EXIST),
  declaredUserErrorStatusNotUpdated(
      ResponseMessage.Key.DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED,
      ResponseMessage.Message.DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED),
  declaredUserValidatedStatusNotUpdated(
      ResponseMessage.Key.DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED,
      ResponseMessage.Message.DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED),
  preferenceNotFound(
      ResponseMessage.Key.PREFERENCE_NOT_FOUND, ResponseMessage.Message.PREFERENCE_NOT_FOUND),
  userConsentNotFound(
      ResponseMessage.Key.USER_CONSENT_NOT_FOUND, ResponseMessage.Message.USER_CONSENT_NOT_FOUND),
  InvalidUserInfoValue(
      ResponseMessage.Key.INVALID_USER_INFO_VALUE, ResponseMessage.Message.INVALID_USER_INFO_VALUE),
  invalidConsentStatus(
      ResponseMessage.Key.INVALID_CONSENT_STATUS, ResponseMessage.Message.INVALID_CONSENT_STATUS),
  roleSaveError(ResponseMessage.Key.ROLE_SAVE_ERROR, ResponseMessage.Message.ROLE_SAVE_ERROR),
  OK(200),
  CLIENT_ERROR(400),
  SERVER_ERROR(500),
  RESOURCE_NOT_FOUND(404),
  UNAUTHORIZED(401),
  FORBIDDEN(403),
  REDIRECTION_REQUIRED(302),
  TOO_MANY_REQUESTS(429),
  SERVICE_UNAVAILABLE(503),
  PARTIAL_SUCCESS_RESPONSE(206),
  IM_A_TEAPOT(418);
  private int responseCode;
  /** error code contains String value */
  private String errorCode;
  /** errorMessage contains proper error message. */
  private String errorMessage;

  /**
   * @param errorCode String
   * @param errorMessage String
   */
  private ResponseCode(String errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  private ResponseCode(String errorCode, String errorMessage, int responseCode) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.responseCode = responseCode;
  }

  /**
   * @param errorCode
   * @return
   */
  public String getMessage(int errorCode) {
    return "";
  }

  /** @return */
  public String getErrorCode() {
    return errorCode;
  }

  /** @param errorCode */
  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  /** @return */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** @param errorMessage */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * This method will provide status message based on code
   *
   * @param code
   * @return String
   */
  public static String getResponseMessage(String code) {
    if (StringUtils.isBlank(code)) {
      return "";
    }
    ResponseCode responseCodes[] = ResponseCode.values();
    for (ResponseCode actionState : responseCodes) {
      if (actionState.getErrorCode().equals(code)) {
        return actionState.getErrorMessage();
      }
    }
    return "";
  }

  private ResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  /**
   * This method will take header response code as int value and it provide matched enum value, if
   * code is not matched or exception occurs then it will provide SERVER_ERROR
   *
   * @param code int
   * @return HeaderResponseCode
   */
  public static ResponseCode getHeaderResponseCode(int code) {
    if (code > 0) {
      try {
        ResponseCode[] arr = ResponseCode.values();
        if (null != arr) {
          for (ResponseCode rc : arr) {
            if (rc.getResponseCode() == code) return rc;
          }
        }
      } catch (Exception e) {
        return ResponseCode.SERVER_ERROR;
      }
    }
    return ResponseCode.SERVER_ERROR;
  }

  /**
   * This method will provide ResponseCode enum based on error code
   *
   * @param errorCode
   * @return String
   */
  public static ResponseCode getResponse(String errorCode) {
    if (StringUtils.isBlank(errorCode)) {
      return null;
    } else if (JsonKey.UNAUTHORIZED.equals(errorCode)) {
      return ResponseCode.unAuthorized;
    } else {
      ResponseCode value = null;
      ResponseCode responseCodes[] = ResponseCode.values();
      for (ResponseCode response : responseCodes) {
        if (response.getErrorCode().equals(errorCode)) {
          return response;
        }
      }
      return value;
    }
  }
}
