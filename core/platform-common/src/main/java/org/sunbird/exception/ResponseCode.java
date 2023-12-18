package org.sunbird.exception;

import java.util.HashMap;
import java.util.Map;

/** @author Manzarul */
public enum ResponseCode {
  unAuthorized(ResponseMessage.Key.UNAUTHORIZED_USER, ResponseMessage.Message.UNAUTHORIZED_USER),
  invalidOperationName(
      ResponseMessage.Key.INVALID_OPERATION_NAME, ResponseMessage.Message.INVALID_OPERATION_NAME),
  invalidRequestData(
      ResponseMessage.Key.INVALID_REQUESTED_DATA, ResponseMessage.Message.INVALID_REQUESTED_DATA),
  success(ResponseMessage.Key.SUCCESS_MESSAGE, ResponseMessage.Message.SUCCESS_MESSAGE),
  errorDuplicateEntry(
      ResponseMessage.Key.ERROR_DUPLICATE_ENTRY, ResponseMessage.Message.ERROR_DUPLICATE_ENTRY),
  errorParamExists(
      ResponseMessage.Key.ERROR_PARAM_EXISTS, ResponseMessage.Message.ERROR_PARAM_EXISTS),
  errorInvalidOTP(ResponseMessage.Key.ERROR_INVALID_OTP, ResponseMessage.Message.ERROR_INVALID_OTP),
  dataTypeError(ResponseMessage.Key.DATA_TYPE_ERROR, ResponseMessage.Message.DATA_TYPE_ERROR),
  errorAttributeConflict(
      ResponseMessage.Key.ERROR_ATTRIBUTE_CONFLICT,
      ResponseMessage.Message.ERROR_ATTRIBUTE_CONFLICT),
  invalidPropertyError(
      ResponseMessage.Key.INVALID_PROPERTY_ERROR, ResponseMessage.Message.INVALID_PROPERTY_ERROR),
  dataSizeError(ResponseMessage.Key.DATA_SIZE_EXCEEDED, ResponseMessage.Message.DATA_SIZE_EXCEEDED),
  userAccountlocked(
      ResponseMessage.Key.USER_ACCOUNT_BLOCKED, ResponseMessage.Message.USER_ACCOUNT_BLOCKED),
  userStatusError(ResponseMessage.Key.USER_STATUS_MSG, ResponseMessage.Message.USER_STATUS_MSG),
  csvError(ResponseMessage.Key.INVALID_CSV_FILE, ResponseMessage.Message.INVALID_CSV_FILE),
  invalidObjectType(
      ResponseMessage.Key.INVALID_OBJECT_TYPE, ResponseMessage.Message.INVALID_OBJECT_TYPE),
  csvFileEmpty(ResponseMessage.Key.EMPTY_CSV_FILE, ResponseMessage.Message.EMPTY_CSV_FILE),
  dataFormatError(ResponseMessage.Key.DATA_FORMAT_ERROR, ResponseMessage.Message.DATA_FORMAT_ERROR),

  OnlyEmailorPhoneorManagedByRequired(
      ResponseMessage.Key.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED,
      ResponseMessage.Message.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED),
  channelRegFailed(
      ResponseMessage.Key.CHANNEL_REG_FAILED, ResponseMessage.Message.CHANNEL_REG_FAILED),
  resourceNotFound(
      ResponseMessage.Key.RESOURCE_NOT_FOUND, ResponseMessage.Message.RESOURCE_NOT_FOUND),
  sizeLimitExceed(
      ResponseMessage.Key.MAX_ALLOWED_SIZE_LIMIT_EXCEED,
      ResponseMessage.Message.MAX_ALLOWED_SIZE_LIMIT_EXCEED),
  inactiveUser(ResponseMessage.Key.INACTIVE_USER, ResponseMessage.Message.INACTIVE_USER),
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
  externalIdAssignedToOtherUser(
      ResponseMessage.Key.EXTERNALID_ASSIGNED_TO_OTHER_USER,
      ResponseMessage.Message.EXTERNALID_ASSIGNED_TO_OTHER_USER),
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
  mandatoryHeaderParamsMissing(
      ResponseMessage.Key.MANDATORY_HEADER_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_HEADER_PARAMETER_MISSING),
  recoveryParamsMatchException(
      ResponseMessage.Key.RECOVERY_PARAM_MATCH_EXCEPTION,
      ResponseMessage.Message.RECOVERY_PARAM_MATCH_EXCEPTION),
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
  invalidCaptcha(ResponseMessage.Key.INVALID_CAPTCHA, ResponseMessage.Message.INVALID_CAPTCHA),
  declaredUserErrorStatusNotUpdated(
      ResponseMessage.Key.DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED,
      ResponseMessage.Message.DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED),
  declaredUserValidatedStatusNotUpdated(
      ResponseMessage.Key.DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED,
      ResponseMessage.Message.DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED),
  invalidConsentStatus(
      ResponseMessage.Key.INVALID_CONSENT_STATUS, ResponseMessage.Message.INVALID_CONSENT_STATUS),
  serverError(ResponseMessage.Key.SERVER_ERROR, ResponseMessage.Message.SERVER_ERROR),
  invalidFileExtension(
      ResponseMessage.Key.INVALID_FILE_EXTENSION, ResponseMessage.Message.INVALID_FILE_EXTENSION),
  invalidEncryptionFile(
      ResponseMessage.Key.INVALID_ENCRYPTION_FILE, ResponseMessage.Message.INVALID_ENCRYPTION_FILE),
  invalidSecurityLevel(
      ResponseMessage.Key.INVALID_SECURITY_LEVEL, ResponseMessage.Message.INVALID_SECURITY_LEVEL),
  invalidSecurityLevelLower(
      ResponseMessage.Key.INVALID_SECURITY_LEVEL_LOWER,
      ResponseMessage.Message.INVALID_SECURITY_LEVEL_LOWER),
  defaultSecurityLevelConfigMissing(
      ResponseMessage.Key.MISSING_DEFAULT_SECURITY_LEVEL,
      ResponseMessage.Message.MISSING_DEFAULT_SECURITY_LEVEL),
  invalidTenantSecurityLevelLower(
      ResponseMessage.Key.INVALID_TENANT_SECURITY_LEVEL_LOWER,
      ResponseMessage.Message.INVALID_TENANT_SECURITY_LEVEL_LOWER),
  cannotDeleteUser(
      ResponseMessage.Key.CANNOT_DELETE_USER, ResponseMessage.Message.CANNOT_DELETE_USER),

  cannotTransferOwnership(
          ResponseMessage.Key.CANNOT_TRANSFER_OWNERSHIP, ResponseMessage.Message.CANNOT_TRANSFER_OWNERSHIP),
  OK(200),
  SUCCESS(200),
  CLIENT_ERROR(400),
  SERVER_ERROR(500),
  RESOURCE_NOT_FOUND(404),
  UNAUTHORIZED(401),
  FORBIDDEN(403),
  REDIRECTION_REQUIRED(302),
  TOO_MANY_REQUESTS(429),
  SERVICE_UNAVAILABLE(503),
  PARTIAL_SUCCESS_RESPONSE(206),
  IM_A_TEAPOT(418),
  extendUserProfileNotLoaded(
      ResponseMessage.Key.EXTENDED_USER_PROFILE_NOT_LOADED,
      ResponseMessage.Message.EXTENDED_USER_PROFILE_NOT_LOADED),
  roleProcessingInvalidOrgError(
      ResponseMessage.Key.ROLE_PROCESSING_INVALID_ORG,
      ResponseMessage.Message.ROLE_PROCESSING_INVALID_ORG);
  private int responseCode;
  /** error code contains String value */
  private String errorCode;
  /** errorMessage contains proper error message. */
  private String errorMessage;

  /**
   * @param errorCode String
   * @param errorMessage String
   */
  ResponseCode(String errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
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

  ResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  private static final Map<Integer, ResponseCode> responseCodeByCode = new HashMap<>();

  static {
    responseCodeByCode.put(200, ResponseCode.OK);
    responseCodeByCode.put(400, ResponseCode.CLIENT_ERROR);
    responseCodeByCode.put(500, ResponseCode.SERVER_ERROR);
    responseCodeByCode.put(404, ResponseCode.RESOURCE_NOT_FOUND);
    responseCodeByCode.put(401, ResponseCode.UNAUTHORIZED);
    responseCodeByCode.put(403, ResponseCode.FORBIDDEN);
    responseCodeByCode.put(302, ResponseCode.REDIRECTION_REQUIRED);
    responseCodeByCode.put(429, ResponseCode.TOO_MANY_REQUESTS);
    responseCodeByCode.put(503, ResponseCode.SERVICE_UNAVAILABLE);
    responseCodeByCode.put(206, ResponseCode.PARTIAL_SUCCESS_RESPONSE);
    responseCodeByCode.put(418, ResponseCode.IM_A_TEAPOT);
  }

  public static ResponseCode getResponseCodeByCode(Integer code) {
    ResponseCode responseCode = responseCodeByCode.get(code);
    if (null != responseCode) {
      return responseCode;
    } else {
      return ResponseCode.OK;
    }
  }
}
