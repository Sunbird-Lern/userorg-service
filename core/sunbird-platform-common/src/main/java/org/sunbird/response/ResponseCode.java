package org.sunbird.response;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;

/**
 * Enum indicating the response code of the API.
 *
 * <p>This enum holds all the response codes used across the application, logically grouped by functionality.
 * It maps internal error codes (Strings) to their corresponding error messages and HTTP status codes.
 */
public enum ResponseCode {

  // -------------------------------------------------------------------------
  // Generic / Common
  // -------------------------------------------------------------------------
  success(ResponseMessage.Key.SUCCESS_MESSAGE, ResponseMessage.Message.SUCCESS_MESSAGE),
  internalError(ResponseMessage.Key.INTERNAL_ERROR, ResponseMessage.Message.INTERNAL_ERROR),
  operationTimeout(
      ResponseMessage.Key.OPERATION_TIMEOUT, ResponseMessage.Message.OPERATION_TIMEOUT),
  invalidOperationName(
      ResponseMessage.Key.INVALID_OPERATION_NAME, ResponseMessage.Message.INVALID_OPERATION_NAME),
  invalidRequestData(
      ResponseMessage.Key.INVALID_REQUESTED_DATA, ResponseMessage.Message.INVALID_REQUESTED_DATA),
  invalidData(ResponseMessage.Key.INVALID_DATA, ResponseMessage.Message.INVALID_DATA),
  invalidParameter(
      ResponseMessage.Key.INVALID_PARAMETER, ResponseMessage.Message.INVALID_PARAMETER),
  invalidParameterValue(
      ResponseMessage.Key.INVALID_PARAMETER_VALUE, ResponseMessage.Message.INVALID_PARAMETER_VALUE),
  mandatoryParamsMissing(
      ResponseMessage.Key.MANDATORY_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_PARAMETER_MISSING),
  errorMandatoryParamsEmpty(
      ResponseMessage.Key.ERROR_MANDATORY_PARAMETER_EMPTY,
      ResponseMessage.Message.ERROR_MANDATORY_PARAMETER_EMPTY),
  idRequired(ResponseMessage.Key.ID_REQUIRED_ERROR, ResponseMessage.Message.ID_REQUIRED_ERROR),
  dataTypeError(ResponseMessage.Key.DATA_TYPE_ERROR, ResponseMessage.Message.DATA_TYPE_ERROR),
  invalidValue(ResponseMessage.Key.INVALID_VALUE, ResponseMessage.Message.INVALID_VALUE),
  alreadyExists(ResponseMessage.Key.ALREADY_EXISTS, ResponseMessage.Message.ALREADY_EXISTS),
  resourceNotFound(
      ResponseMessage.Key.RESOURCE_NOT_FOUND, ResponseMessage.Message.RESOURCE_NOT_FOUND),
  serverError(
      ResponseMessage.Key.INTERNAL_ERROR, ResponseMessage.Message.INTERNAL_ERROR),
  customServerError(
      ResponseMessage.Key.CUSTOM_SERVER_ERROR, ResponseMessage.Message.CUSTOM_SERVER_ERROR),
  serviceUnAvailable(
      ResponseMessage.Key.SERVICE_UNAVAILABLE, ResponseMessage.Message.SERVICE_UNAVAILABLE),
  dataAlreadyExist(
      ResponseMessage.Key.DATA_ALREADY_EXIST, ResponseMessage.Message.DATA_ALREADY_EXIST),
  notSupported(ResponseMessage.Key.NOT_SUPPORTED, ResponseMessage.Message.NOT_SUPPORTED),
  functionalityMissing(ResponseMessage.Key.NOT_SUPPORTED, ResponseMessage.Message.NOT_SUPPORTED),
  invalidElementInList(
      ResponseMessage.Key.INVALID_ELEMENT_IN_LIST, ResponseMessage.Message.INVALID_ELEMENT_IN_LIST),
  errorRateLimitExceeded(
      ResponseMessage.Key.ERROR_RATE_LIMIT_EXCEEDED,
      ResponseMessage.Message.ERROR_RATE_LIMIT_EXCEEDED),
  invalidRequestTimeout(
      ResponseMessage.Key.INVALID_REQUEST_TIMEOUT, ResponseMessage.Message.INVALID_REQUEST_TIMEOUT),
  invalidObjectType(
      ResponseMessage.Key.INVALID_OBJECT_TYPE, ResponseMessage.Message.INVALID_OBJECT_TYPE),
  invalidPropertyError(
      ResponseMessage.Key.INVALID_PROPERTY_ERROR, ResponseMessage.Message.INVALID_PROPERTY_ERROR),
  invalidDateFormat(
       ResponseMessage.Key.INVALID_DATE_FORMAT, ResponseMessage.Message.INVALID_DATE_FORMAT),
  passwordValidation(
       ResponseMessage.Key.INVALID_PASSWORD, ResponseMessage.Message.INVALID_PASSWORD),
  dataFormatError(
       ResponseMessage.Key.DATA_FORMAT_ERROR, ResponseMessage.Message.DATA_FORMAT_ERROR),
  OnlyEmailorPhoneorManagedByRequired(
       ResponseMessage.Key.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED,
       ResponseMessage.Message.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED),
  managedByNotAllowed(
       ResponseMessage.Key.MANAGED_BY_NOT_ALLOWED, ResponseMessage.Message.MANAGED_BY_NOT_ALLOWED),
  managedUserLimitExceeded(
       ResponseMessage.Key.MANAGED_USER_LIMIT_EXCEEDED, ResponseMessage.Message.MANAGED_USER_LIMIT_EXCEEDED),
  errorConflictingValues(
      ResponseMessage.Key.ERROR_CONFLICTING_VALUES, ResponseMessage.Message.ERROR_CONFLICTING_VALUES),
  errorConflictingRootOrgId(
      ResponseMessage.Key.ERROR_CONFLICTING_ROOT_ORG_ID, ResponseMessage.Message.ERROR_CONFLICTING_ROOT_ORG_ID),
  errorInvalidParameterSize(
      ResponseMessage.Key.ERROR_INVALID_PARAMETER_SIZE, ResponseMessage.Message.ERROR_INVALID_PARAMETER_SIZE),
  declaredUserErrorStatusNotUpdated(
      ResponseMessage.Key.DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED, ResponseMessage.Message.DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED),
  invalidEncryptionFile(
      ResponseMessage.Key.INVALID_ENCRYPTION_FILE, ResponseMessage.Message.INVALID_ENCRYPTION_FILE),
  errorParamExists(
      ResponseMessage.Key.ERROR_PARAM_EXISTS, ResponseMessage.Message.ERROR_PARAM_EXISTS),
  recoveryParamsMatchException(
      ResponseMessage.Key.RECOVERY_PARAM_MATCH_EXCEPTION,
      ResponseMessage.Message.RECOVERY_PARAM_MATCH_EXCEPTION),
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
  errorUserMigrationFailed(
      ResponseMessage.Key.ERROR_USER_MIGRATION_FAILED,
      ResponseMessage.Message.ERROR_USER_MIGRATION_FAILED),
  declaredUserValidatedStatusNotUpdated(
      ResponseMessage.Key.DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED,
      ResponseMessage.Message.DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED),
  userStatusError(ResponseMessage.Key.USER_STATUS_MSG, ResponseMessage.Message.USER_STATUS_MSG),
  extendUserProfileNotLoaded(
      ResponseMessage.Key.EXTENDED_USER_PROFILE_NOT_LOADED,
      ResponseMessage.Message.EXTENDED_USER_PROFILE_NOT_LOADED),
  inactiveUser(ResponseMessage.Key.INACTIVE_USER, ResponseMessage.Message.INACTIVE_USER),
  cannotDeleteUser(
      ResponseMessage.Key.CANNOT_DELETE_USER, ResponseMessage.Message.CANNOT_DELETE_USER),
  roleProcessingInvalidOrgError(
      ResponseMessage.Key.ROLE_PROCESSING_INVALID_ORG,
      ResponseMessage.Message.ROLE_PROCESSING_INVALID_ORG),
  dateFormatError(
       ResponseMessage.Key.DATE_FORMAT_ERRROR, ResponseMessage.Message.DATE_FORMAT_ERRROR),
  unableToParseData(
      ResponseMessage.Key.UNABLE_TO_PARSE_DATA, ResponseMessage.Message.UNABLE_TO_PARSE_DATA),
  invalidJsonData(ResponseMessage.Key.INVALID_JSON, ResponseMessage.Message.INVALID_JSON),
  noDataForConsumption(ResponseMessage.Key.NO_DATA, ResponseMessage.Message.NO_DATA),
  invalidIdentifier(
      ResponseMessage.Key.VALID_IDENTIFIER_ABSENSE,
      ResponseMessage.Message.IDENTIFIER_VALIDATION_FAILED),
  parameterMismatch(
      ResponseMessage.Key.PARAMETER_MISMATCH, ResponseMessage.Message.PARAMETER_MISMATCH),

  // -------------------------------------------------------------------------
  // Authentication & Authorization
  // -------------------------------------------------------------------------
  unAuthorized(ResponseMessage.Key.UNAUTHORIZED_USER, ResponseMessage.Message.UNAUTHORIZED_USER),
  invalidUserCredentials(
      ResponseMessage.Key.INVALID_USER_CREDENTIALS,
      ResponseMessage.Message.INVALID_USER_CREDENTIALS),
  apiKeyRequired(
      ResponseMessage.Key.API_KEY_MISSING_ERROR, ResponseMessage.Message.API_KEY_MISSING_ERROR),
  invalidApiKey(
      ResponseMessage.Key.API_KEY_INVALID_ERROR, ResponseMessage.Message.API_KEY_INVALID_ERROR),
  authTokenRequired(
      ResponseMessage.Key.AUTH_TOKEN_MISSING, ResponseMessage.Message.AUTH_TOKEN_MISSING),
  invalidAuthToken(
      ResponseMessage.Key.INVALID_AUTH_TOKEN, ResponseMessage.Message.INVALID_AUTH_TOKEN),
  sessionIdRequiredError(
      ResponseMessage.Key.SESSION_ID_MISSING, ResponseMessage.Message.SESSION_ID_MISSING),
  invalidRole(ResponseMessage.Key.INVALID_ROLE, ResponseMessage.Message.INVALID_ROLE),
  invalidSalt(ResponseMessage.Key.INVALID_SALT, ResponseMessage.Message.INVALID_SALT),
  keyCloakDefaultError(
      ResponseMessage.Key.KEY_CLOAK_DEFAULT_ERROR, ResponseMessage.Message.KEY_CLOAK_DEFAULT_ERROR),
  otpVerificationFailed(
      ResponseMessage.Key.OTP_VERIFICATION_FAILED, ResponseMessage.Message.OTP_VERIFICATION_FAILED),
  errorInvalidOTP(ResponseMessage.Key.ERROR_INVALID_OTP, ResponseMessage.Message.ERROR_INVALID_OTP),
  errorForbidden(ResponseMessage.Key.FORBIDDEN, ResponseMessage.Message.FORBIDDEN),

  // -------------------------------------------------------------------------
  // User Management
  // -------------------------------------------------------------------------
  userNotFound(ResponseMessage.Key.USER_NOT_FOUND, ResponseMessage.Message.USER_NOT_FOUND),
  userAlreadyExists(
      ResponseMessage.Key.USER_ALREADY_EXISTS, ResponseMessage.Message.USER_ALREADY_EXISTS),
  invalidUserId(ResponseMessage.Key.INVALID_USER_ID, ResponseMessage.Message.INVALID_USER_ID),
  userIdRequired(ResponseMessage.Key.USERID_MISSING, ResponseMessage.Message.USERID_MISSING),
  userNameRequired(ResponseMessage.Key.USERNAME_MISSING, ResponseMessage.Message.USERNAME_MISSING),
  firstNameRequired(
      ResponseMessage.Key.FIRST_NAME_MISSING, ResponseMessage.Message.FIRST_NAME_MISSING),
  emailRequired(ResponseMessage.Key.EMAIL_MISSING, ResponseMessage.Message.EMAIL_MISSING),
  phoneNoRequired(
      ResponseMessage.Key.PHONE_NO_REQUIRED_ERROR, ResponseMessage.Message.PHONE_NO_REQUIRED_ERROR),
  passwordRequired(ResponseMessage.Key.PASSWORD_MISSING, ResponseMessage.Message.PASSWORD_MISSING),
  invalidPassword(ResponseMessage.Key.INVALID_PASSWORD, ResponseMessage.Message.INVALID_PASSWORD),
  passwordMinLengthError(
      ResponseMessage.Key.PASSWORD_MIN_LENGHT, ResponseMessage.Message.PASSWORD_MIN_LENGHT),
  passwordMaxLengthError(
      ResponseMessage.Key.PASSWORD_MAX_LENGHT, ResponseMessage.Message.PASSWORD_MAX_LENGHT),
  userNameAlreadyExistError(
      ResponseMessage.Key.USERNAME_IN_USE, ResponseMessage.Message.USERNAME_IN_USE),
  emailAlreadyExistError(ResponseMessage.Key.EMAIL_IN_USE, ResponseMessage.Message.EMAIL_IN_USE),
  PhoneNumberInUse(
      ResponseMessage.Key.PHONE_ALREADY_IN_USE, ResponseMessage.Message.PHONE_ALREADY_IN_USE),
  userAccountlocked(
      ResponseMessage.Key.USER_ACCOUNT_BLOCKED, ResponseMessage.Message.USER_ACCOUNT_BLOCKED),
  userAlreadyActive(
      ResponseMessage.Key.USER_ALREADY_ACTIVE, ResponseMessage.Message.USER_ALREADY_ACTIVE),
  userAlreadyInactive(
      ResponseMessage.Key.USER_ALREADY_INACTIVE, ResponseMessage.Message.USER_ALREADY_INACTIVE),
  userRegUnSuccessfull(
      ResponseMessage.Key.USER_REG_UNSUCCESSFUL, ResponseMessage.Message.USER_REG_UNSUCCESSFUL),
  userUpdationUnSuccessfull(
      ResponseMessage.Key.USER_UPDATE_UNSUCCESSFUL,
      ResponseMessage.Message.USER_UPDATE_UNSUCCESSFUL),
  userPhoneUpdateFailed(
      ResponseMessage.Key.USER_PHONE_UPDATE_FAILED,
      ResponseMessage.Message.USER_PHONE_UPDATE_FAILED),
  userMigrationFiled(
      ResponseMessage.Key.USER_MIGRATION_FAILED, ResponseMessage.Message.USER_MIGRATION_FAILED),
  userDataEncryptionError(
      ResponseMessage.Key.USER_DATA_ENCRYPTION_ERROR,
      ResponseMessage.Message.USER_DATA_ENCRYPTION_ERROR),
  invalidUserExternalId(
      ResponseMessage.Key.INVALID_EXT_USER_ID, ResponseMessage.Message.INVALID_EXT_USER_ID),
  externalIdNotFound(
      ResponseMessage.Key.EXTERNALID_NOT_FOUND, ResponseMessage.Message.EXTERNALID_NOT_FOUND),
  externalIdAssignedToOtherUser(
      ResponseMessage.Key.EXTERNALID_ASSIGNED_TO_OTHER_USER,
      ResponseMessage.Message.EXTERNALID_ASSIGNED_TO_OTHER_USER),
  duplicateExternalIds(
      ResponseMessage.Key.DUPLICATE_EXTERNAL_IDS, ResponseMessage.Message.DUPLICATE_EXTERNAL_IDS),
  emailANDUserNameAlreadyExistError(
      ResponseMessage.Key.USERNAME_EMAIL_IN_USE, ResponseMessage.Message.USERNAME_EMAIL_IN_USE),
  userNameCanntBeUpdated(
      ResponseMessage.Key.USERNAME_CANNOT_BE_UPDATED,
      ResponseMessage.Message.USERNAME_CANNOT_BE_UPDATED),
  newPasswordRequired(
      ResponseMessage.Key.CONFIIRM_PASSWORD_MISSING,
      ResponseMessage.Message.CONFIIRM_PASSWORD_MISSING),
  newPasswordEmpty(
      ResponseMessage.Key.CONFIIRM_PASSWORD_EMPTY, ResponseMessage.Message.CONFIIRM_PASSWORD_EMPTY),
  samePasswordError(
      ResponseMessage.Key.SAME_PASSWORD_ERROR, ResponseMessage.Message.SAME_PASSWORD_ERROR),
  emailVerifiedError(
      ResponseMessage.Key.EMAIL_VERIFY_ERROR, ResponseMessage.Message.EMAIL_VERIFY_ERROR),
  phoneVerifiedError(
      ResponseMessage.Key.PHONE_VERIFY_ERROR, ResponseMessage.Message.PHONE_VERIFY_ERROR),
  loginTypeRequired(
      ResponseMessage.Key.LOGIN_TYPE_MISSING, ResponseMessage.Message.LOGIN_TYPE_MISSING),
  loginTypeError(ResponseMessage.Key.LOGIN_TYPE_ERROR, ResponseMessage.Message.LOGIN_TYPE_ERROR),
  loginIdRequired(ResponseMessage.Key.LOGIN_ID_MISSING, ResponseMessage.Message.LOGIN_ID_MISSING),
  userNameOrUserIdRequired(
      ResponseMessage.Key.USERNAME_USERID_MISSING, ResponseMessage.Message.USERNAME_USERID_MISSING),
  usernameOrUserIdError(
      ResponseMessage.Key.USER_NAME_OR_ID_ERROR, ResponseMessage.Message.USER_NAME_OR_ID_ERROR),
  rolesRequired(ResponseMessage.Key.ROLES_MISSING, ResponseMessage.Message.ROLES_MISSING),
  roleRequired(ResponseMessage.Key.ROLE_MISSING, ResponseMessage.Message.ROLE_MISSING),
  emptyRolesProvided(
      ResponseMessage.Key.EMPTY_ROLES_PROVIDED, ResponseMessage.Message.EMPTY_ROLES_PROVIDED),
  visibilityInvalid(
      ResponseMessage.Key.INVALID_VISIBILITY_REQUEST,
      ResponseMessage.Message.INVALID_VISIBILITY_REQUEST),
  addressRequired(
      ResponseMessage.Key.ADDRESS_REQUIRED_ERROR, ResponseMessage.Message.ADDRESS_REQUIRED_ERROR),
  educationRequired(
      ResponseMessage.Key.EDUCATION_REQUIRED_ERROR,
      ResponseMessage.Message.EDUCATION_REQUIRED_ERROR),
  jobDetailsRequired(
      ResponseMessage.Key.JOBDETAILS_REQUIRED_ERROR,
      ResponseMessage.Message.JOBDETAILS_REQUIRED_ERROR),
  addressError(ResponseMessage.Key.ADDRESS_ERROR, ResponseMessage.Message.ADDRESS_ERROR),
  addressTypeError(
      ResponseMessage.Key.ADDRESS_TYPE_ERROR, ResponseMessage.Message.ADDRESS_TYPE_ERROR),
  educationNameError(
      ResponseMessage.Key.NAME_OF_INSTITUTION_ERROR,
      ResponseMessage.Message.NAME_OF_INSTITUTION_ERROR),
  educationDegreeError(
      ResponseMessage.Key.EDUCATION_DEGREE_ERROR, ResponseMessage.Message.EDUCATION_DEGREE_ERROR),
  jobNameError(ResponseMessage.Key.JOB_NAME_ERROR, ResponseMessage.Message.JOB_NAME_ERROR),
  invalidUsrData(ResponseMessage.Key.INVALID_USR_DATA, ResponseMessage.Message.INVALID_USR_DATA),
  usrValidationError(
      ResponseMessage.Key.USR_DATA_VALIDATION_ERROR,
      ResponseMessage.Message.USR_DATA_VALIDATION_ERROR),
  invalidUsrOrgData(
      ResponseMessage.Key.INVALID_USR_ORG_DATA, ResponseMessage.Message.INVALID_USR_ORG_DATA),
  userNotAssociatedToOrg(
      ResponseMessage.Key.USER_NOT_BELONGS_TO_ANY_ORG,
      ResponseMessage.Message.USER_NOT_BELONGS_TO_ANY_ORG),
  userOrgAssociationError(
      ResponseMessage.Key.USER_ORG_ASSOCIATION_ERROR,
      ResponseMessage.Message.USER_ORG_ASSOCIATION_ERROR),
  errorUserHasNotCreatedAnyCourse(
      ResponseMessage.Key.ERROR_USER_HAS_NOT_CREATED_ANY_COURSE,
      ResponseMessage.Message.ERROR_USER_HAS_NOT_CREATED_ANY_COURSE),
  userNotAssociatedToRootOrg(
      ResponseMessage.Key.USER_NOT_ASSOCIATED_TO_ROOT_ORG,
      ResponseMessage.Message.USER_NOT_ASSOCIATED_TO_ROOT_ORG),
  invalidCredentials(
      ResponseMessage.Key.INVALID_CREDENTIAL, ResponseMessage.Message.INVALID_CREDENTIAL),
  cannotTransferOwnership(
      ResponseMessage.Key.CANNOT_TRANSFER_OWNERSHIP,
      ResponseMessage.Message.CANNOT_TRANSFER_OWNERSHIP),
  emailFormatError(ResponseMessage.Key.EMAIL_FORMAT, ResponseMessage.Message.EMAIL_FORMAT),
  urlFormatError(ResponseMessage.Key.URL_FORMAT_ERROR, ResponseMessage.Message.URL_FORMAT_ERROR),
  languageRequired(ResponseMessage.Key.LANGUAGE_MISSING, ResponseMessage.Message.LANGUAGE_MISSING),
  timeStampRequired(
      ResponseMessage.Key.TIMESTAMP_REQUIRED, ResponseMessage.Message.TIMESTAMP_REQUIRED),
  phoneNoFormatError(
      ResponseMessage.Key.INVALID_PHONE_NO_FORMAT, ResponseMessage.Message.INVALID_PHONE_NO_FORMAT),
  invalidPhoneNumber(
      ResponseMessage.Key.INVALID_PHONE_NUMBER, ResponseMessage.Message.INVALID_PHONE_NUMBER),
  invalidCountryCode(
      ResponseMessage.Key.INVALID_COUNTRY_CODE, ResponseMessage.Message.INVALID_COUNTRY_CODE),
  emailorPhoneRequired(
      ResponseMessage.Key.EMAIL_OR_PHONE_MISSING, ResponseMessage.Message.EMAIL_OR_PHONE_MISSING),
  accountNotFound(ResponseMessage.Key.ACCOUNT_NOT_FOUND, ResponseMessage.Message.ACCOUNT_NOT_FOUND),
  fromAccountIdRequired(
      ResponseMessage.Key.FROM_ACCOUNT_ID_MISSING, ResponseMessage.Message.FROM_ACCOUNT_ID_MISSING),
  toAccountIdRequired(
      ResponseMessage.Key.TO_ACCOUNT_ID_MISSING, ResponseMessage.Message.TO_ACCOUNT_ID_MISSING),
  fromAccountIdNotExists(
      ResponseMessage.Key.FROM_ACCOUNT_ID_NOT_EXISTS,
      ResponseMessage.Message.FROM_ACCOUNT_ID_NOT_EXISTS),

  // -------------------------------------------------------------------------
  // Organization Management
  // -------------------------------------------------------------------------
  orgDoesNotExist(ResponseMessage.Key.ORG_NOT_EXIST, ResponseMessage.Message.ORG_NOT_EXIST),
  invalidOrgData(ResponseMessage.Key.INVALID_ORG_DATA, ResponseMessage.Message.INVALID_ORG_DATA),
  organisationIdRequiredError(
      ResponseMessage.Key.ORGANISATION_ID_MISSING, ResponseMessage.Message.ORGANISATION_ID_MISSING),
  orgIdRequired(ResponseMessage.Key.ORG_ID_MISSING, ResponseMessage.Message.ORG_ID_MISSING),
  organisationNameRequired(
      ResponseMessage.Key.ORGANISATION_NAME_MISSING,
      ResponseMessage.Message.ORGANISATION_NAME_MISSING),
  organisationNameError(
      ResponseMessage.Key.NAME_OF_ORGANISATION_ERROR,
      ResponseMessage.Message.NAME_OF_ORGANISATION_ERROR),
  rootOrgIdRequired(
      ResponseMessage.Key.ROOT_ORG_ID_REQUIRED, ResponseMessage.Message.ROOT_ORG_ID_REQUIRED),
  sourceAndExternalIdValidationError(
      ResponseMessage.Key.REQUIRED_DATA_ORG_MISSING,
      ResponseMessage.Message.REQUIRED_DATA_ORG_MISSING),
  invalidRootOrganisationId(
      ResponseMessage.Key.INVALID_ROOT_ORGANIZATION,
      ResponseMessage.Message.INVALID_ROOT_ORGANIZATION),
  invalidParentId(
      ResponseMessage.Key.INVALID_PARENT_ORGANIZATION_ID,
      ResponseMessage.Message.INVALID_PARENT_ORGANIZATION_ID),
  parentCodeAndIdValidationError(
      ResponseMessage.Key.PARENT_CODE_AND_PARENT_ID_MISSING,
      ResponseMessage.Message.PARENT_CODE_AND_PARENT_ID_MISSING),
  invalidOrgId(ResponseMessage.Key.INVALID_ORG_ID, ResponseMessage.Key.INVALID_ORG_ID),
  invalidOrgStatus(ResponseMessage.Key.INVALID_ORG_STATUS, ResponseMessage.Key.INVALID_ORG_STATUS),
  invalidOrgStatusTransition(
      ResponseMessage.Key.INVALID_ORG_STATUS_TRANSITION,
      ResponseMessage.Key.INVALID_ORG_STATUS_TRANSITION),
  orgTypeMandatory(
      ResponseMessage.Key.ORG_TYPE_MANDATORY, ResponseMessage.Message.ORG_TYPE_MANDATORY),
  orgTypeAlreadyExist(
      ResponseMessage.Key.ORG_TYPE_ALREADY_EXIST, ResponseMessage.Message.ORG_TYPE_ALREADY_EXIST),
  orgTypeIdRequired(
      ResponseMessage.Key.ORG_TYPE_ID_REQUIRED_ERROR,
      ResponseMessage.Message.ORG_TYPE_ID_REQUIRED_ERROR),
  invalidOrgTypeId(
      ResponseMessage.Key.INVALID_ORG_TYPE_ID_ERROR,
      ResponseMessage.Message.INVALID_ORG_TYPE_ID_ERROR),
  invalidOrgType(
      ResponseMessage.Key.INVALID_ORG_TYPE_ERROR, ResponseMessage.Message.INVALID_ORG_TYPE_ERROR),
  errorInactiveOrg(
      ResponseMessage.Key.ERROR_INACTIVE_ORG, ResponseMessage.Message.ERROR_INACTIVE_ORG),
  errorNoRootOrgAssociated(
      ResponseMessage.Key.ERROR_NO_ROOT_ORG_ASSOCIATED,
      ResponseMessage.Message.ERROR_NO_ROOT_ORG_ASSOCIATED),
  errorInactiveCustodianOrg(
      ResponseMessage.Key.ERROR_INACTIVE_CUSTODIAN_ORG,
      ResponseMessage.Message.ERROR_INACTIVE_CUSTODIAN_ORG),
  rootOrgAssociationError(
      ResponseMessage.Key.ROOT_ORG_ASSOCIATION_ERROR,
      ResponseMessage.Message.ROOT_ORG_ASSOCIATION_ERROR),
  invalidRootOrgData(
      ResponseMessage.Key.INVALID_ROOT_ORG_DATA, ResponseMessage.Message.INVALID_ROOT_ORG_DATA),
  channelUniquenessInvalid(
      ResponseMessage.Key.CHANNEL_SHOULD_BE_UNIQUE,
      ResponseMessage.Message.CHANNEL_SHOULD_BE_UNIQUE),
  invalidChannel(ResponseMessage.Key.INVALID_CHANNEL, ResponseMessage.Message.INVALID_CHANNEL),
  channelRegFailed(
      ResponseMessage.Key.CHANNEL_REG_FAILED, ResponseMessage.Message.CHANNEL_REG_FAILED),
  slugIsNotUnique(
      ResponseMessage.Key.SLUG_IS_NOT_UNIQUE, ResponseMessage.Message.SLUG_IS_NOT_UNIQUE),
  slugRequired(ResponseMessage.Key.SLUG_REQUIRED, ResponseMessage.Message.SLUG_REQUIRED),
  conflictingOrgLocations(
      ResponseMessage.Key.CONFLICTING_ORG_LOCATIONS,
      ResponseMessage.Message.CONFLICTING_ORG_LOCATIONS),
  invalidLocationId(
      ResponseMessage.Key.INVALID_LOCATION_ID, ResponseMessage.Message.INVALID_LOCATION_ID),
  locationIdRequired(
      ResponseMessage.Key.LOCATION_ID_REQUIRED, ResponseMessage.Message.LOCATION_ID_REQUIRED),
  locationTypeRequired(
      ResponseMessage.Key.LOCATION_TYPE_REQUIRED, ResponseMessage.Message.LOCATION_TYPE_REQUIRED),
  invalidRequestDataForLocation(
      ResponseMessage.Key.INVALID_REQUEST_DATA_FOR_LOCATION,
      ResponseMessage.Message.INVALID_REQUEST_DATA_FOR_LOCATION),
  invalidLocationDeleteRequest(
      ResponseMessage.Key.INVALID_LOCATION_DELETE_REQUEST,
      ResponseMessage.Message.INVALID_LOCATION_DELETE_REQUEST),
  locationTypeConflicts(
      ResponseMessage.Key.LOCATION_TYPE_CONFLICTS, ResponseMessage.Message.LOCATION_TYPE_CONFLICTS),
  parentNotAllowed(
      ResponseMessage.Key.PARENT_NOT_ALLOWED, ResponseMessage.Message.PARENT_NOT_ALLOWED),
  invalidHashTagId(
      ResponseMessage.Key.INVALID_HASHTAG_ID, ResponseMessage.Message.INVALID_HASHTAG_ID),

  // -------------------------------------------------------------------------
  // Course & Batch Management
  // -------------------------------------------------------------------------
  courseIdRequired(
      ResponseMessage.Key.COURSE_ID_MISSING_ERROR, ResponseMessage.Message.COURSE_ID_MISSING_ERROR),
  courseIdRequiredError(
      ResponseMessage.Key.COURSE_ID_MISSING, ResponseMessage.Message.COURSE_ID_MISSING),
  invalidCourseId(ResponseMessage.Key.INVALID_COURSE_ID, ResponseMessage.Message.INVALID_COURSE_ID),
  courseNameRequired(
      ResponseMessage.Key.COURSE_NAME_MISSING, ResponseMessage.Message.COURSE_NAME_MISSING),
  courseDescriptionError(
      ResponseMessage.Key.COURSE_DESCRIPTION_MISSING,
      ResponseMessage.Message.COURSE_DESCRIPTION_MISSING),
  courseVersionRequiredError(
      ResponseMessage.Key.COURSE_VERSION_MISSING, ResponseMessage.Message.COURSE_VERSION_MISSING),
  courseDurationRequiredError(
      ResponseMessage.Key.COURSE_DURATION_MISSING, ResponseMessage.Message.COURSE_DURATION_MISSING),
  courseTocUrlError(
      ResponseMessage.Key.COURSE_TOCURL_MISSING, ResponseMessage.Message.COURSE_TOCURL_MISSING),
  courseCreatedForIsNull(
      ResponseMessage.Key.COURSE_CREATED_FOR_NULL, ResponseMessage.Message.COURSE_CREATED_FOR_NULL),
  courseBatchIdRequired(
      ResponseMessage.Key.COURSE_BATCH_ID_MISSING, ResponseMessage.Message.COURSE_BATCH_ID_MISSING),
  invalidCourseBatchId(
      ResponseMessage.Key.INVALID_COURSE_BATCH_ID, ResponseMessage.Message.INVALID_COURSE_BATCH_ID),
  courseBatchAlreadyCompleted(
      ResponseMessage.Key.COURSE_BATCH_ALREADY_COMPLETED,
      ResponseMessage.Message.COURSE_BATCH_ALREADY_COMPLETED),
  courseBatchEnrollmentDateEnded(
      ResponseMessage.Key.COURSE_BATCH_ENROLLMENT_DATE_ENDED,
      ResponseMessage.Message.COURSE_BATCH_ENROLLMENT_DATE_ENDED),
  courseBatchStartDateRequired(
      ResponseMessage.Key.COURSE_BATCH_START_DATE_REQUIRED,
      ResponseMessage.Message.COURSE_BATCH_START_DATE_REQUIRED),
  courseBatchStartDateError(
      ResponseMessage.Key.COURSE_BATCH_START_DATE_INVALID,
      ResponseMessage.Message.COURSE_BATCH_START_DATE_INVALID),
  courseBatchEndDateError(
      ResponseMessage.Key.COURSE_BATCH_END_DATE_ERROR,
      ResponseMessage.Message.COURSE_BATCH_END_DATE_ERROR),
  BatchCloseError(
      ResponseMessage.Key.COURSE_BATCH_IS_CLOSED_ERROR,
      ResponseMessage.Message.COURSE_BATCH_IS_CLOSED_ERROR),
  courseBatchStartPassedDateError(
      ResponseMessage.Key.COURSE_BATCH_START_PASSED_DATE_INVALID,
      ResponseMessage.Message.COURSE_BATCH_START_PASSED_DATE_INVALID),
  invalidBatchStartDateError(
      ResponseMessage.Key.INVALID_BATCH_START_DATE_ERROR,
      ResponseMessage.Message.INVALID_BATCH_START_DATE_ERROR),
  invalidBatchEndDateError(
      ResponseMessage.Key.INVALID_BATCH_END_DATE_ERROR,
      ResponseMessage.Message.INVALID_BATCH_END_DATE_ERROR),
  multipleCoursesNotAllowedForBatch(
      ResponseMessage.Key.MULTIPLE_COURSES_FOR_BATCH,
      ResponseMessage.Message.MULTIPLE_COURSES_FOR_BATCH),
  invalidCourseCreatorId(
      ResponseMessage.Key.INVALID_COURSE_CREATOR_ID,
      ResponseMessage.Message.INVALID_COURSE_CREATOR_ID),
  enrollmentStartDateRequiredError(
      ResponseMessage.Key.ENROLLMENT_START_DATE_MISSING,
      ResponseMessage.Message.ENROLLMENT_START_DATE_MISSING),
  enrollmentEndDateStartError(
      ResponseMessage.Key.ENROLLMENT_END_DATE_START_ERROR,
      ResponseMessage.Message.ENROLLMENT_END_DATE_START_ERROR),
  enrollmentEndDateEndError(
      ResponseMessage.Key.ENROLLMENT_END_DATE_END_ERROR,
      ResponseMessage.Message.ENROLLMENT_END_DATE_END_ERROR),
  enrollmentEndDateUpdateError(
      ResponseMessage.Key.ENROLLMENT_END_DATE_UPDATE_ERROR,
      ResponseMessage.Message.ENROLLMENT_END_DATE_UPDATE_ERROR),
  enrolmentTypeRequired(
      ResponseMessage.Key.ENROLMENT_TYPE_REQUIRED, ResponseMessage.Message.ENROLMENT_TYPE_REQUIRED),
  enrolmentIncorrectValue(
      ResponseMessage.Key.ENROLMENT_TYPE_VALUE_ERROR,
      ResponseMessage.Message.ENROLMENT_TYPE_VALUE_ERROR),
  enrollmentTypeValidation(
      ResponseMessage.Key.ENROLLMENT_TYPE_VALIDATION,
      ResponseMessage.Message.ENROLLMENT_TYPE_VALIDATION),
  userAlreadyEnrolledCourse(
      ResponseMessage.Key.USER_ALREADY_ENROLLED_COURSE,
      ResponseMessage.Message.USER_ALREADY_ENROLLED_COURSE),
  userNotEnrolledCourse(
      ResponseMessage.Key.USER_NOT_ENROLLED_COURSE,
      ResponseMessage.Message.USER_NOT_ENROLLED_COURSE),
  userAlreadyCompletedCourse(
      ResponseMessage.Key.USER_ALREADY_COMPLETED_COURSE,
      ResponseMessage.Message.USER_ALREADY_COMPLETED_COURSE),
  endDateError(ResponseMessage.Key.END_DATE_ERROR, ResponseMessage.Message.END_DATE_ERROR),
  publishedCourseCanNotBeUpdated(
      ResponseMessage.Key.PUBLISHED_COURSE_CAN_NOT_UPDATED,
      ResponseMessage.Message.PUBLISHED_COURSE_CAN_NOT_UPDATED),
  progressStatusError(
      ResponseMessage.Key.INVALID_PROGRESS_STATUS, ResponseMessage.Message.INVALID_PROGRESS_STATUS),
  missingData(
      ResponseMessage.Key.MISSING_CODE, ResponseMessage.Message.MISSING_MESSAGE),
  contentTypeMismatch(
      ResponseMessage.Key.CONTENT_TYPE_MISMATCH, ResponseMessage.Message.CONTENT_TYPE_MISMATCH),
  mimeTypeMismatch(
      ResponseMessage.Key.MIME_TYPE_MISMATCH, ResponseMessage.Message.MIME_TYPE_MISMATCH),

  // -------------------------------------------------------------------------
  // Content & Assessment
  // -------------------------------------------------------------------------
  contentIdRequired(
      ResponseMessage.Key.CONTENT_ID_MISSING_ERROR,
      ResponseMessage.Message.CONTENT_ID_MISSING_ERROR),
  contentIdRequiredError(
      ResponseMessage.Key.CONTENT_ID_MISSING, ResponseMessage.Message.CONTENT_ID_MISSING),
  contentIdError(ResponseMessage.Key.CONTENT_ID_ERROR, ResponseMessage.Message.CONTENT_ID_ERROR),
  contentVersionRequiredError(
      ResponseMessage.Key.CONTENT_VERSION_MISSING, ResponseMessage.Message.CONTENT_VERSION_MISSING),
  versionRequiredError(
      ResponseMessage.Key.VERSION_MISSING, ResponseMessage.Message.VERSION_MISSING),
  contentStatusRequired(
      ResponseMessage.Key.CONTENT_STATUS_MISSING_ERROR,
      ResponseMessage.Message.CONTENT_STATUS_MISSING_ERROR),
  contentTypeRequiredError(
      ResponseMessage.Key.CONTENT_TYPE_ERROR, ResponseMessage.Message.CONTENT_TYPE_ERROR),
  assessmentItemIdRequired(
      ResponseMessage.Key.ASSESSMENT_ITEM_ID_REQUIRED,
      ResponseMessage.Message.ASSESSMENT_ITEM_ID_REQUIRED),
  assessmentTypeRequired(
      ResponseMessage.Key.ASSESSMENT_TYPE_REQUIRED,
      ResponseMessage.Message.ASSESSMENT_TYPE_REQUIRED),
  assessmentAttemptDateRequired(
      ResponseMessage.Key.ATTEMPTED_DATE_REQUIRED, ResponseMessage.Message.ATTEMPTED_DATE_REQUIRED),
  assessmentAnswersRequired(
      ResponseMessage.Key.ATTEMPTED_ANSWERS_REQUIRED,
      ResponseMessage.Message.ATTEMPTED_ANSWERS_REQUIRED),
  assessmentmaxScoreRequired(
      ResponseMessage.Key.MAX_SCORE_REQUIRED, ResponseMessage.Message.MAX_SCORE_REQUIRED),
  attemptIdRequired(
      ResponseMessage.Key.ATTEMPT_ID_MISSING_ERROR,
      ResponseMessage.Message.ATTEMPT_ID_MISSING_ERROR),

  // -------------------------------------------------------------------------
  // Badge/Certificates (Issuer, Recipient, Assertion)
  // -------------------------------------------------------------------------
  issuerIdRequired(
      ResponseMessage.Key.ISSUER_ID_REQUIRED, ResponseMessage.Message.ISSUER_ID_REQUIRED),
  invalidIssuerId(ResponseMessage.Key.INVALID_ISSUER_ID, ResponseMessage.Message.INVALID_ISSUER_ID),
  recipientIdRequired(
      ResponseMessage.Key.RECIPIENT_ID_REQUIRED, ResponseMessage.Message.RECIPIENT_ID_REQUIRED),
  recipientTypeRequired(
      ResponseMessage.Key.RECIPIENT_TYPE_REQUIRED, ResponseMessage.Message.RECIPIENT_TYPE_REQUIRED),
  invalidRecipientType(
      ResponseMessage.Key.INVALID_RECIPIENT_TYPE, ResponseMessage.Message.INVALID_RECIPIENT_TYPE),
  recipientEmailRequired(
      ResponseMessage.Key.RECIPIENT_EMAIL_REQUIRED,
      ResponseMessage.Message.RECIPIENT_EMAIL_REQUIRED),
  recipientAddressError(
      ResponseMessage.Key.RECIPIENT_ADDRESS_ERROR, ResponseMessage.Message.RECIPIENT_ADDRESS_ERROR),
  receiverIdMandatory(
      ResponseMessage.Key.RECEIVER_ID_ERROR, ResponseMessage.Message.RECEIVER_ID_ERROR),
  invalidReceiverId(
      ResponseMessage.Key.INVALID_RECEIVER_ID, ResponseMessage.Message.INVALID_RECEIVER_ID),
  assertionIdRequired(
      ResponseMessage.Key.ASSERTION_ID_REQUIRED, ResponseMessage.Message.ASSERTION_ID_REQUIRED),
  evidenceRequired(
      ResponseMessage.Key.ASSERTION_EVIDENCE_REQUIRED,
      ResponseMessage.Message.ASSERTION_EVIDENCE_REQUIRED),
  revocationReasonRequired(
      ResponseMessage.Key.REVOCATION_REASON_REQUIRED,
      ResponseMessage.Message.REVOCATION_REASON_REQUIRED),
  endorsedUserIdRequired(
      ResponseMessage.Key.ENDORSED_USER_ID_REQUIRED,
      ResponseMessage.Message.ENDORSED_USER_ID_REQUIRED),
  canNotEndorse(ResponseMessage.Key.CAN_NOT_ENDORSE, ResponseMessage.Message.CAN_NOT_ENDORSE),

  // -------------------------------------------------------------------------
  // Notifications & Email
  // -------------------------------------------------------------------------
  emailSubjectError(
      ResponseMessage.Key.EMAIL_SUBJECT_ERROR, ResponseMessage.Message.EMAIL_SUBJECT_ERROR),
  emailBodyError(ResponseMessage.Key.EMAIL_BODY_ERROR, ResponseMessage.Message.EMAIL_BODY_ERROR),
  emailNotSentRecipientsExceededMaxLimit(
      ResponseMessage.Key.EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT,
      ResponseMessage.Message.EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT),
  emailNotSentRecipientsZero(
      ResponseMessage.Key.NO_EMAIL_RECIPIENTS, ResponseMessage.Message.NO_EMAIL_RECIPIENTS),
  msgIdRequiredError(
      ResponseMessage.Key.MESSAGE_ID_MISSING, ResponseMessage.Message.MESSAGE_ID_MISSING),
  invalidNotificationType(
      ResponseMessage.Key.INVALID_NOTIFICATION_TYPE,
      ResponseMessage.Message.INVALID_NOTIFICATION_TYPE),
  notificationTypeSupport(
      ResponseMessage.Key.INVALID_NOTIFICATION_TYPE_SUPPORT,
      ResponseMessage.Message.INVALID_NOTIFICATION_TYPE_SUPPORT),
  invalidTopic(ResponseMessage.Key.INVALID_TOPIC_NAME, ResponseMessage.Message.INVALID_TOPIC_NAME),
  invalidTopicData(
      ResponseMessage.Key.INVALID_TOPIC_DATA, ResponseMessage.Message.INVALID_TOPIC_DATA),

  // -------------------------------------------------------------------------
  // System, Config & Infrastructure
  // -------------------------------------------------------------------------
  errorInvalidConfigParamValue(
      ResponseMessage.Key.ERROR_INVALID_CONFIG_PARAM_VALUE,
      ResponseMessage.Message.ERROR_INVALID_CONFIG_PARAM_VALUE),
  errorConfigLoadEmptyString(
      ResponseMessage.Key.ERROR_CONFIG_LOAD_EMPTY_STRING,
      ResponseMessage.Message.ERROR_CONFIG_LOAD_EMPTY_STRING),
  errorConfigLoadParseString(
      ResponseMessage.Key.ERROR_CONFIG_LOAD_PARSE_STRING,
      ResponseMessage.Message.ERROR_CONFIG_LOAD_PARSE_STRING),
  errorConfigLoadEmptyConfig(
      ResponseMessage.Key.ERROR_CONFIG_LOAD_EMPTY_CONFIG,
      ResponseMessage.Message.ERROR_CONFIG_LOAD_EMPTY_CONFIG),
  errorConflictingFieldConfiguration(
      ResponseMessage.Key.ERROR_CONFLICTING_FIELD_CONFIGURATION,
      ResponseMessage.Message.ERROR_CONFLICTING_FIELD_CONFIGURATION),
  mandatoryConfigParamMissing(
      ResponseMessage.Key.MANDATORY_CONFIG_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_CONFIG_PARAMETER_MISSING),
  errorLoadConfig(ResponseMessage.Key.ERROR_LOAD_CONFIG, ResponseMessage.Message.ERROR_LOAD_CONFIG),
  errorSystemSettingNotFound(
      ResponseMessage.Key.ERROR_SYSTEM_SETTING_NOT_FOUND,
      ResponseMessage.Message.ERROR_SYSTEM_SETTING_NOT_FOUND),
  errorUpdateSettingNotAllowed(
      ResponseMessage.Key.ERROR_UPDATE_SETTING_NOT_ALLOWED,
      ResponseMessage.Message.ERROR_UPDATE_SETTING_NOT_ALLOWED),
  dbInsertionError(
      ResponseMessage.Key.DB_INSERTION_FAIL, ResponseMessage.Message.DB_INSERTION_FAIL),
  dbUpdateError(ResponseMessage.Key.DB_UPDATE_FAIL, ResponseMessage.Message.DB_UPDATE_FAIL),
  esError(ResponseMessage.Key.ES_ERROR, ResponseMessage.Message.ES_ERROR),
  esUpdateFailed(ResponseMessage.Key.ES_UPDATE_FAILED, ResponseMessage.Message.ES_UPDATE_FAILED),
  unableToConnect(
      ResponseMessage.Key.UNABLE_TO_CONNECT_TO_EKSTEP,
      ResponseMessage.Message.UNABLE_TO_CONNECT_TO_EKSTEP),
  unableToConnectToES(
      ResponseMessage.Key.UNABLE_TO_CONNECT_TO_ES, ResponseMessage.Message.UNABLE_TO_CONNECT_TO_ES),
  unableToCommunicateWithActor(
      ResponseMessage.Key.UNABLE_TO_COMMUNICATE_WITH_ACTOR,
      ResponseMessage.Message.UNABLE_TO_COMMUNICATE_WITH_ACTOR),
  actorConnectionError(
      ResponseMessage.Key.ACTOR_CONNECTION_ERROR, ResponseMessage.Message.ACTOR_CONNECTION_ERROR),
  cassandraConnectionEstablishmentFailed(
      ResponseMessage.Key.CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED,
      ResponseMessage.Message.CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED),
  cloudServiceError(
      ResponseMessage.Key.CLOUD_SERVICE_ERROR, ResponseMessage.Message.CLOUD_SERVICE_ERROR),
  errorUnsupportedCloudStorage(
      ResponseMessage.Key.ERROR_UNSUPPORTED_CLOUD_STORAGE,
      ResponseMessage.Message.ERROR_UNSUPPORTED_CLOUD_STORAGE),
  storageContainerNameMandatory(
      ResponseMessage.Key.STORAGE_CONTAINER_NAME_MANDATORY,
      ResponseMessage.Message.STORAGE_CONTAINER_NAME_MANDATORY),
  errorGenerateDownloadLink(
      ResponseMessage.Key.ERROR_GENERATE_DOWNLOAD_LINK,
      ResponseMessage.Message.ERROR_GENERATE_DOWNLOAD_LINK),
  errorUnavailableDownloadLink(
      ResponseMessage.Key.ERROR_DOWNLOAD_LINK_UNAVAILABLE,
      ResponseMessage.Message.ERROR_DOWNLOAD_LINK_UNAVAILABLE),
  errorSavingStorageDetails(
      ResponseMessage.Key.ERROR_SAVING_STORAGE_DETAILS,
      ResponseMessage.Message.ERROR_SAVING_STORAGE_DETAILS),
  errorUploadQRCodeCSVfailed(
      ResponseMessage.Key.ERROR_UPLOAD_QRCODE_CSV_FAILED,
      ResponseMessage.Message.ERROR_UPLOAD_QRCODE_CSV_FAILED),
  erroCallGrooupAPI(ResponseMessage.Key.ERR_CALLING_GROUP_API, ResponseMessage.Message.ERR_CALLING_GROUP_API),

  // -------------------------------------------------------------------------
  // Files & Uploads
  // -------------------------------------------------------------------------
  csvError(ResponseMessage.Key.INVALID_CSV_FILE, ResponseMessage.Message.INVALID_CSV_FILE),
  errorCsvNoDataRows(
      ResponseMessage.Key.ERROR_CSV_NO_DATA_ROWS, ResponseMessage.Message.ERROR_CSV_NO_DATA_ROWS),
  csvFileEmpty(ResponseMessage.Key.EMPTY_CSV_FILE, ResponseMessage.Message.EMPTY_CSV_FILE),
  emptyHeaderLine(ResponseMessage.Key.EMPTY_HEADER_LINE, ResponseMessage.Message.EMPTY_HEADER_LINE),
  bulkUserUploadError(
      ResponseMessage.Key.BULK_USER_UPLOAD_ERROR, ResponseMessage.Message.BULK_USER_UPLOAD_ERROR),
  dataSizeError(ResponseMessage.Key.DATA_SIZE_EXCEEDED, ResponseMessage.Message.DATA_SIZE_EXCEEDED),
  errorMaxSizeExceeded(
      ResponseMessage.Key.ERROR_MAX_SIZE_EXCEEDED, ResponseMessage.Message.ERROR_MAX_SIZE_EXCEEDED),
  missingFileAttachment(
      ResponseMessage.Key.MISSING_FILE_ATTACHMENT, ResponseMessage.Message.MISSING_FILE_ATTACHMENT),
  invalidFileExtension(
      ResponseMessage.Key.INVALID_FILE_EXTENSION, ResponseMessage.Message.INVALID_FILE_EXTENSION),
  emptyFile(ResponseMessage.Key.EMPTY_FILE, ResponseMessage.Message.EMPTY_FILE),
  fileAttachmentSizeNotConfigured(
      ResponseMessage.Key.FILE_ATTACHMENT_SIZE_NOT_CONFIGURED,
      ResponseMessage.Message.FILE_ATTACHMENT_SIZE_NOT_CONFIGURED),
  errorCreatingFile(
      ResponseMessage.Key.ERROR_CREATING_FILE, ResponseMessage.Message.ERROR_CREATING_FILE),
  errorProcessingFile(
      ResponseMessage.Key.ERROR_PROCESSING_FILE, ResponseMessage.Message.ERROR_PROCESSING_FILE),
  errorProcessingRequest(
      ResponseMessage.Key.ERROR_PROCESSING_REQUEST,
      ResponseMessage.Message.ERROR_PROCESSING_REQUEST),

  // -------------------------------------------------------------------------
  // Miscellaneous
  // -------------------------------------------------------------------------
  pageNameRequired(
      ResponseMessage.Key.PAGE_NAME_REQUIRED, ResponseMessage.Message.PAGE_NAME_REQUIRED),
  pageIdRequired(ResponseMessage.Key.PAGE_ID_REQUIRED, ResponseMessage.Message.PAGE_ID_REQUIRED),
  pageAlreadyExist(
      ResponseMessage.Key.PAGE_ALREADY_EXIST, ResponseMessage.Message.PAGE_ALREADY_EXIST),
  pageDoesNotExist(ResponseMessage.Key.PAGE_NOT_EXIST, ResponseMessage.Message.PAGE_NOT_EXIST),
  invalidPageSource(
      ResponseMessage.Key.INVALID_PAGE_SOURCE, ResponseMessage.Message.INVALID_PAGE_SOURCE),
  sectionNameRequired(
      ResponseMessage.Key.SECTION_NAME_MISSING, ResponseMessage.Message.SECTION_NAME_MISSING),
  sectionDataTypeRequired(
      ResponseMessage.Key.SECTION_DATA_TYPE_MISSING,
      ResponseMessage.Message.SECTION_DATA_TYPE_MISSING),
  sectionIdRequired(
      ResponseMessage.Key.SECTION_ID_REQUIRED, ResponseMessage.Message.SECTION_ID_REQUIRED),
  sectionDoesNotExist(
      ResponseMessage.Key.SECTION_NOT_EXIST, ResponseMessage.Message.SECTION_NOT_EXIST),
  errorInvalidPageSection(
      ResponseMessage.Key.INVALID_PAGE_SECTION, ResponseMessage.Message.INVALID_PAGE_SECTION),
  invalidWebPageData(
      ResponseMessage.Key.INVALID_WEBPAGE_DATA, ResponseMessage.Message.INVALID_WEBPAGE_DATA),
  invalidMediaType(
      ResponseMessage.Key.INVALID_MEDIA_TYPE, ResponseMessage.Message.INVALID_MEDIA_TYPE),
  invalidWebPageUrl(
      ResponseMessage.Key.INVALID_WEBPAGE_URL, ResponseMessage.Message.INVALID_WEBPAGE_URL),
  titleRequired(ResponseMessage.Key.TITLE_REQUIRED, ResponseMessage.Message.TITLE_REQUIRED),
  noteRequired(ResponseMessage.Key.NOTE_REQUIRED, ResponseMessage.Message.NOTE_REQUIRED),
  invalidNoteId(ResponseMessage.Key.NOTE_ID_INVALID, ResponseMessage.Message.NOTE_ID_INVALID),
  invalidTags(ResponseMessage.Key.INVALID_TAGS, ResponseMessage.Message.INVALID_TAGS),
  invalidClientName(
      ResponseMessage.Key.INVALID_CLIENT_NAME, ResponseMessage.Message.INVALID_CLIENT_NAME),
  invalidClientId(ResponseMessage.Key.INVALID_CLIENT_ID, ResponseMessage.Message.INVALID_CLIENT_ID),
  tableOrDocNameError(
      ResponseMessage.Key.TABLE_OR_DOC_NAME_ERROR, ResponseMessage.Message.TABLE_OR_DOC_NAME_ERROR),
  invalidDuplicateValue(
      ResponseMessage.Key.INVALID_DUPLICATE_VALUE, ResponseMessage.Message.INVALID_DUPLICATE_VALUE),
  errorDuplicateEntry(
      ResponseMessage.Key.ERROR_DUPLICATE_ENTRY, ResponseMessage.Message.ERROR_DUPLICATE_ENTRY),
  errorDuplicateEntries(
      ResponseMessage.Key.ERROR_DUPLICATE_ENTRIES, ResponseMessage.Message.ERROR_DUPLICATE_ENTRIES),
  invalidPeriod(ResponseMessage.Key.INVALID_PERIOD, ResponseMessage.Message.INVALID_PERIOD),
  invalidDateRange(
      ResponseMessage.Key.INVALID_DATE_RANGE, ResponseMessage.Message.INVALID_DATE_RANGE),
  cyclicValidationError(
      ResponseMessage.Key.CYCLIC_VALIDATION_FAILURE,
      ResponseMessage.Message.CYCLIC_VALIDATION_FAILURE),
  unupdatableField(
      ResponseMessage.Key.UPDATE_NOT_ALLOWED, ResponseMessage.Message.UPDATE_NOT_ALLOWED),
  statusCanntBeUpdated(
      ResponseMessage.Key.STATUS_CANNOT_BE_UPDATED,
      ResponseMessage.Message.STATUS_CANNOT_BE_UPDATED),
  updateFailed(ResponseMessage.Key.UPDATE_FAILED, ResponseMessage.Message.UPDATE_FAILED),
  InvalidColumnError(
      ResponseMessage.Key.INVALID_COLUMN_NAME, ResponseMessage.Message.INVALID_COLUMN_NAME),
  invalidColumns(ResponseMessage.Key.INVALID_COLUMNS, ResponseMessage.Message.INVALID_COLUMNS),
  requiredHeaderMissing(
      ResponseMessage.Key.REQUIRED_HEADER_MISSING, ResponseMessage.Message.REQUIRED_HEADER_MISSING),
  mandatoryHeadersMissing(
      ResponseMessage.Key.MANDATORY_HEADER_MISSING,
      ResponseMessage.Message.MANDATORY_HEADER_MISSING),
  mandatoryHeaderParamsMissing(
      ResponseMessage.Key.MANDATORY_HEADER_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_HEADER_PARAMETER_MISSING),
  invalidRequestParameter(
      ResponseMessage.Key.INVALID_REQUEST_PARAMETER,
      ResponseMessage.Message.INVALID_REQUEST_PARAMETER),
  dependentParameterMissing(
      ResponseMessage.Key.DEPENDENT_PARAMETER_MISSING,
      ResponseMessage.Message.DEPENDENT_PARAMETER_MISSING),
  dependentParamsMissing(
      ResponseMessage.Key.DEPENDENT_PARAMETER_MISSING,
      ResponseMessage.Message.DEPENDENT_PARAMS_MISSING),
  commonAttributeMismatch(
      ResponseMessage.Key.COMMON_ATTRIBUTE_MISMATCH,
      ResponseMessage.Message.COMMON_ATTRIBUTE_MISMATCH),
  errorAttributeConflict(
      ResponseMessage.Key.ERROR_ATTRIBUTE_CONFLICT,
      ResponseMessage.Message.ERROR_ATTRIBUTE_CONFLICT),
  sourceRequired(ResponseMessage.Key.SOURCE_MISSING, ResponseMessage.Message.SOURCE_MISSING),
  invaidConfiguration(
      ResponseMessage.Key.INVALID_CONFIGURATION, ResponseMessage.Message.INVALID_CONFIGURATION),
  invalidProcessId(
      ResponseMessage.Key.INVALID_PROCESS_ID, ResponseMessage.Message.INVALID_PROCESS_ID),
  invalidTypeValue(ResponseMessage.Key.INVALID_TYPE_VALUE, ResponseMessage.Key.INVALID_TYPE_VALUE),
  errorNoFrameworkFound(
      ResponseMessage.Key.ERROR_NO_FRAMEWORK_FOUND,
      ResponseMessage.Message.ERROR_NO_FRAMEWORK_FOUND),
  eventsRequired(
      ResponseMessage.Key.EVENTS_DATA_MISSING, ResponseMessage.Message.EVENTS_DATA_MISSING),
  groupIdMismatch(ResponseMessage.Key.GROUP_ID_MISSING, ResponseMessage.Message.GROUP_ID_MISSING),
  activityIdMismatch(ResponseMessage.Key.ACTIVITY_ID_MISSING, ResponseMessage.Message.ACTIVITY_ID_MISSING),
  activityTypeMismatch(ResponseMessage.Key.ACTIVITY_TYPE_MISSING, ResponseMessage.Message.ACTIVITY_TYPE_MISSING),
  errorNoDialcodesLinked(
      ResponseMessage.Key.ERROR_NO_DIALCODES_LINKED,
      ResponseMessage.Message.ERROR_NO_DIALCODES_LINKED),
  errorUnsupportedField(
      ResponseMessage.Key.ERROR_UNSUPPORTED_FIELD, ResponseMessage.Message.ERROR_UNSUPPORTED_FIELD),

  // -------------------------------------------------------------------------
  // JSON Transform (Registry)
  // -------------------------------------------------------------------------
  errorJsonTransformInvalidTypeConfig(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_INVALID_TYPE_CONFIG,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_INVALID_TYPE_CONFIG),
  errorJsonTransformInvalidDateFormat(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_INVALID_DATE_FORMAT,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_INVALID_DATE_FORMAT),
  errorJsonTransformInvalidInput(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_INVALID_INPUT,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_INVALID_INPUT),
  errorJsonTransformInvalidEnumInput(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_INVALID_ENUM_INPUT,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_INVALID_ENUM_INPUT),
  errorJsonTransformEnumValuesEmpty(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_ENUM_VALUES_EMPTY,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_ENUM_VALUES_EMPTY),
  errorJsonTransformBasicConfigMissing(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_BASIC_CONFIG_MISSING,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_BASIC_CONFIG_MISSING),
  errorJsonTransformInvalidFilterConfig(
      ResponseMessage.Key.ERROR_JSON_TRANSFORM_INVALID_FILTER_CONFIG,
      ResponseMessage.Message.ERROR_JSON_TRANSFORM_INVALID_FILTER_CONFIG),
  errorRegistryClientCreation(
      ResponseMessage.Key.ERROR_REGISTRY_CLIENT_CREATION,
      ResponseMessage.Message.ERROR_REGISTRY_CLIENT_CREATION),
  errorRegistryAddEntity(
      ResponseMessage.Key.ERROR_REGISTRY_ADD_ENTITY,
      ResponseMessage.Message.ERROR_REGISTRY_ADD_ENTITY),
  errorRegistryReadEntity(
      ResponseMessage.Key.ERROR_REGISTRY_READ_ENTITY,
      ResponseMessage.Message.ERROR_REGISTRY_READ_ENTITY),
  errorRegistryUpdateEntity(
      ResponseMessage.Key.ERROR_REGISTRY_UPDATE_ENTITY,
      ResponseMessage.Message.ERROR_REGISTRY_UPDATE_ENTITY),
  errorRegistryDeleteEntity(
      ResponseMessage.Key.ERROR_REGISTRY_DELETE_ENTITY,
      ResponseMessage.Message.ERROR_REGISTRY_DELETE_ENTITY),
  errorRegistryParseResponse(
      ResponseMessage.Key.ERROR_REGISTRY_PARSE_RESPONSE,
      ResponseMessage.Message.ERROR_REGISTRY_PARSE_RESPONSE),
  errorRegistryEntityTypeBlank(
      ResponseMessage.Key.ERROR_REGISTRY_ENTITY_TYPE_BLANK,
      ResponseMessage.Message.ERROR_REGISTRY_ENTITY_TYPE_BLANK),
  errorRegistryEntityIdBlank(
      ResponseMessage.Key.ERROR_REGISTRY_ENTITY_ID_BLANK,
      ResponseMessage.Message.ERROR_REGISTRY_ENTITY_ID_BLANK),
  errorRegistryAccessTokenBlank(
      ResponseMessage.Key.ERROR_REGISTRY_ACCESS_TOKEN_BLANK,
      ResponseMessage.Message.ERROR_REGISTRY_ACCESS_TOKEN_BLANK),
  invalidDuplicateValueInList(
      ResponseMessage.Key.INVALID_DUPLICATE_VALUE, ResponseMessage.Message.INVALID_DUPLICATE_VALUE),

  // -------------------------------------------------------------------------
  // HTTP Status Codes & System Codes
  // -------------------------------------------------------------------------
  OK(200),
  SUCCESS(200),
  CLIENT_ERROR(400),
  SERVER_ERROR(500),
  ERROR(ResponseMessage.Key.ERR_CALLING_EXHAUST_API, ResponseMessage.Message.ERR_CALLING_EXHAUST_API),
  RESOURCE_NOT_FOUND(404),
  UNAUTHORIZED(401),
  FORBIDDEN(403),
  REDIRECTION_REQUIRED(302),
  TOO_MANY_REQUESTS(429),
  SERVICE_UNAVAILABLE(503),
  PARTIAL_SUCCESS_RESPONSE(206),
  sizeLimitExceed(ResponseMessage.Key.SIZE_LIMIT_EXCEED, ResponseMessage.Message.SIZE_LIMIT_EXCEED),
  invalidConsentStatus(ResponseMessage.Key.INVALID_CONSENT_STATUS, ResponseMessage.Message.INVALID_CONSENT_STATUS),
  invalidCaptcha(ResponseMessage.Key.INVALID_CAPTCHA, ResponseMessage.Message.INVALID_CAPTCHA),
  IM_A_TEAPOT(ResponseMessage.Key.IM_A_TEAPOT, ResponseMessage.Message.IM_A_TEAPOT, 418);

  private int responseCode;
  /** error code contains String value */
  private String errorCode;
  /** errorMessage contains proper error message. */
  private String errorMessage;

  /**
   * Constructor for ResponseCode with errorCode and errorMessage.
   *
   * @param errorCode String - The unique error code identifier.
   * @param errorMessage String - The human-readable error message.
   */
  private ResponseCode(String errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  /**
   * Constructor for ResponseCode with errorCode, errorMessage, and HTTP responseCode.
   *
   * @param errorCode String - The unique error code identifier.
   * @param errorMessage String - The human-readable error message.
   * @param responseCode int - The HTTP status code associated with this error.
   */
  private ResponseCode(String errorCode, String errorMessage, int responseCode) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.responseCode = responseCode;
  }

  /**
   * Constructor for ResponseCode with just HTTP responseCode.
   *
   * @param responseCode int - The HTTP status code.
   */
  ResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  /**
   * Gets the error code identifier.
   *
   * @return String - The error code.
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Sets the error code identifier.
   *
   * @param errorCode String - The error code to set.
   */
  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Gets the human-readable error message.
   *
   * @return String - The error message.
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets the human-readable error message.
   *
   * @param errorMessage String - The error message to set.
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Gets the HTTP response code.
   *
   * @return int - The HTTP status code.
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Gets the HTTP response code.
   *
   * @return int - The HTTP status code.
   */
  public int getCode() {
    return responseCode;
  }

  /**
   * Sets the HTTP response code.
   *
   * @param responseCode int - The HTTP status code to set.
   */
  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  /**
   * Placeholder for retrieving message by error code. Currently returns empty string.
   *
   * @param errorCode int
   * @return String - Empty string.
   */
  public String getMessage(int errorCode) {
    return "";
  }

  /**
   * Retrieves the error message associated with a given error code string.
   *
   * @param code String - The error code to look up.
   * @return String - The corresponding error message, or an empty string if not found.
   */
  public static String getResponseMessage(String code) {
    if (StringUtils.isBlank(code)) {
      return "";
    }
    ResponseCode[] responseCodes = ResponseCode.values();
    for (ResponseCode actionState : responseCodes) {
      if (actionState.getErrorCode().equals(code)) {
        return actionState.getErrorMessage();
      }
    }
    return "";
  }

  /**
   * Maps an integer HTTP status code to a ResponseCode enum.
   * If the code is not found or an error occurs, returns SERVER_ERROR.
   *
   * @param code int - The HTTP status code.
   * @return ResponseCode - The matching ResponseCode enum value.
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
   * Retrieves the ResponseCode enum based on the error code string.
   * Handles special cases like "UNAUTHORIZED" mapping to "unAuthorized" enum.
   *
   * @param errorCode String - The error code string.
   * @return ResponseCode - The matching ResponseCode enum, or null if not found.
   */
  public static ResponseCode getResponse(String errorCode) {
    if (StringUtils.isBlank(errorCode)) {
      return null;
    } else if (JsonKey.UNAUTHORIZED.equals(errorCode)) {
      return ResponseCode.unAuthorized;
    } else {
      ResponseCode[] responseCodes = ResponseCode.values();
      for (ResponseCode response : responseCodes) {
        if (response.getErrorCode().equals(errorCode)) {
          return response;
        }
      }
      return null;
    }
  }

  public static ResponseCode getResponseCodeByCode(int code) {
    return getHeaderResponseCode(code);
  }
}
