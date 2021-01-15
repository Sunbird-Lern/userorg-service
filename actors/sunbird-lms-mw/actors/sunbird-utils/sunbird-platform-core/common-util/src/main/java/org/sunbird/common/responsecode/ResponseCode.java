package org.sunbird.common.responsecode;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;

/** @author Manzarul */
public enum ResponseCode {
  unAuthorized(ResponseMessage.Key.UNAUTHORIZED_USER, ResponseMessage.Message.UNAUTHORIZED_USER),
  invalidUserCredentials(
      ResponseMessage.Key.INVALID_USER_CREDENTIALS,
      ResponseMessage.Message.INVALID_USER_CREDENTIALS),
  operationTimeout(
      ResponseMessage.Key.OPERATION_TIMEOUT, ResponseMessage.Message.OPERATION_TIMEOUT),
  invalidOperationName(
      ResponseMessage.Key.INVALID_OPERATION_NAME, ResponseMessage.Message.INVALID_OPERATION_NAME),
  invalidRequestData(
      ResponseMessage.Key.INVALID_REQUESTED_DATA, ResponseMessage.Message.INVALID_REQUESTED_DATA),
  invalidCustomerId(
      ResponseMessage.Key.CONSUMER_ID_MISSING_ERROR,
      ResponseMessage.Message.CONSUMER_ID_MISSING_ERROR),
  customerIdRequired(
      ResponseMessage.Key.CONSUMER_ID_INVALID_ERROR,
      ResponseMessage.Message.CONSUMER_ID_INVALID_ERROR),
  deviceIdRequired(
      ResponseMessage.Key.DEVICE_ID_MISSING_ERROR, ResponseMessage.Message.DEVICE_ID_MISSING_ERROR),
  invalidContentId(
      ResponseMessage.Key.CONTENT_ID_INVALID_ERROR,
      ResponseMessage.Message.CONTENT_ID_INVALID_ERROR),
  courseIdRequired(
      ResponseMessage.Key.COURSE_ID_MISSING_ERROR, ResponseMessage.Message.COURSE_ID_MISSING_ERROR),
  contentIdRequired(
      ResponseMessage.Key.CONTENT_ID_MISSING_ERROR,
      ResponseMessage.Message.CONTENT_ID_MISSING_ERROR),
  errorInvalidConfigParamValue(
      ResponseMessage.Key.ERROR_INVALID_CONFIG_PARAM_VALUE,
      ResponseMessage.Message.ERROR_INVALID_CONFIG_PARAM_VALUE),
  errorMaxSizeExceeded(
      ResponseMessage.Key.ERROR_MAX_SIZE_EXCEEDED, ResponseMessage.Message.ERROR_MAX_SIZE_EXCEEDED),
  apiKeyRequired(
      ResponseMessage.Key.API_KEY_MISSING_ERROR, ResponseMessage.Message.API_KEY_MISSING_ERROR),
  invalidApiKey(
      ResponseMessage.Key.API_KEY_INVALID_ERROR, ResponseMessage.Message.API_KEY_INVALID_ERROR),
  internalError(ResponseMessage.Key.INTERNAL_ERROR, ResponseMessage.Message.INTERNAL_ERROR),
  dbInsertionError(
      ResponseMessage.Key.DB_INSERTION_FAIL, ResponseMessage.Message.DB_INSERTION_FAIL),
  dbUpdateError(ResponseMessage.Key.DB_UPDATE_FAIL, ResponseMessage.Message.DB_UPDATE_FAIL),
  courseNameRequired(
      ResponseMessage.Key.COURSE_NAME_MISSING, ResponseMessage.Message.COURSE_NAME_MISSING),
  success(ResponseMessage.Key.SUCCESS_MESSAGE, ResponseMessage.Message.SUCCESS_MESSAGE),
  sessionIdRequiredError(
      ResponseMessage.Key.SESSION_ID_MISSING, ResponseMessage.Message.SESSION_ID_MISSING),
  courseIdRequiredError(
      ResponseMessage.Key.COURSE_ID_MISSING, ResponseMessage.Message.COURSE_ID_MISSING),
  contentIdRequiredError(
      ResponseMessage.Key.CONTENT_ID_MISSING, ResponseMessage.Message.CONTENT_ID_MISSING),
  versionRequiredError(
      ResponseMessage.Key.VERSION_MISSING, ResponseMessage.Message.VERSION_MISSING),
  courseVersionRequiredError(
      ResponseMessage.Key.COURSE_VERSION_MISSING, ResponseMessage.Message.COURSE_VERSION_MISSING),
  contentVersionRequiredError(
      ResponseMessage.Key.CONTENT_VERSION_MISSING, ResponseMessage.Message.CONTENT_VERSION_MISSING),
  courseDescriptionError(
      ResponseMessage.Key.COURSE_DESCRIPTION_MISSING,
      ResponseMessage.Message.COURSE_DESCRIPTION_MISSING),
  courseTocUrlError(
      ResponseMessage.Key.COURSE_TOCURL_MISSING, ResponseMessage.Message.COURSE_TOCURL_MISSING),
  emailRequired(ResponseMessage.Key.EMAIL_MISSING, ResponseMessage.Message.EMAIL_MISSING),
  emailFormatError(ResponseMessage.Key.EMAIL_FORMAT, ResponseMessage.Message.EMAIL_FORMAT),
  urlFormatError(ResponseMessage.Key.URL_FORMAT_ERROR, ResponseMessage.Message.URL_FORMAT_ERROR),
  firstNameRequired(
      ResponseMessage.Key.FIRST_NAME_MISSING, ResponseMessage.Message.FIRST_NAME_MISSING),
  languageRequired(ResponseMessage.Key.LANGUAGE_MISSING, ResponseMessage.Message.LANGUAGE_MISSING),
  passwordRequired(ResponseMessage.Key.PASSWORD_MISSING, ResponseMessage.Message.PASSWORD_MISSING),
  passwordMinLengthError(
      ResponseMessage.Key.PASSWORD_MIN_LENGHT, ResponseMessage.Message.PASSWORD_MIN_LENGHT),
  passwordMaxLengthError(
      ResponseMessage.Key.PASSWORD_MAX_LENGHT, ResponseMessage.Message.PASSWORD_MAX_LENGHT),
  organisationIdRequiredError(
      ResponseMessage.Key.ORGANISATION_ID_MISSING, ResponseMessage.Message.ORGANISATION_ID_MISSING),
  sourceAndExternalIdValidationError(
      ResponseMessage.Key.REQUIRED_DATA_ORG_MISSING,
      ResponseMessage.Message.REQUIRED_DATA_ORG_MISSING),
  organisationNameRequired(
      ResponseMessage.Key.ORGANISATION_NAME_MISSING,
      ResponseMessage.Message.ORGANISATION_NAME_MISSING),
  channelUniquenessInvalid(
      ResponseMessage.Key.CHANNEL_SHOULD_BE_UNIQUE,
      ResponseMessage.Message.CHANNEL_SHOULD_BE_UNIQUE),
  errorDuplicateEntry(
      ResponseMessage.Key.ERROR_DUPLICATE_ENTRY, ResponseMessage.Message.ERROR_DUPLICATE_ENTRY),
  unableToConnect(
      ResponseMessage.Key.UNABLE_TO_CONNECT_TO_EKSTEP,
      ResponseMessage.Message.UNABLE_TO_CONNECT_TO_EKSTEP),
  unableToConnectToES(
      ResponseMessage.Key.UNABLE_TO_CONNECT_TO_ES, ResponseMessage.Message.UNABLE_TO_CONNECT_TO_ES),
  unableToParseData(
      ResponseMessage.Key.UNABLE_TO_PARSE_DATA, ResponseMessage.Message.UNABLE_TO_PARSE_DATA),
  invalidJsonData(ResponseMessage.Key.INVALID_JSON, ResponseMessage.Message.INVALID_JSON),
  invalidOrgData(ResponseMessage.Key.INVALID_ORG_DATA, ResponseMessage.Message.INVALID_ORG_DATA),
  invalidRootOrganisationId(
      ResponseMessage.Key.INVALID_ROOT_ORGANIZATION,
      ResponseMessage.Message.INVALID_ROOT_ORGANIZATION),
  invalidParentId(
      ResponseMessage.Key.INVALID_PARENT_ORGANIZATION_ID,
      ResponseMessage.Message.INVALID_PARENT_ORGANIZATION_ID),
  cyclicValidationError(
      ResponseMessage.Key.CYCLIC_VALIDATION_FAILURE,
      ResponseMessage.Message.CYCLIC_VALIDATION_FAILURE),
  invalidUsrData(ResponseMessage.Key.INVALID_USR_DATA, ResponseMessage.Message.INVALID_USR_DATA),
  usrValidationError(
      ResponseMessage.Key.USR_DATA_VALIDATION_ERROR,
      ResponseMessage.Message.USR_DATA_VALIDATION_ERROR),
  errorInvalidOTP(ResponseMessage.Key.ERROR_INVALID_OTP, ResponseMessage.Message.ERROR_INVALID_OTP),
  enrollmentStartDateRequiredError(
      ResponseMessage.Key.ENROLLMENT_START_DATE_MISSING,
      ResponseMessage.Message.ENROLLMENT_START_DATE_MISSING),
  courseDurationRequiredError(
      ResponseMessage.Key.COURSE_DURATION_MISSING, ResponseMessage.Message.COURSE_DURATION_MISSING),
  loginTypeRequired(
      ResponseMessage.Key.LOGIN_TYPE_MISSING, ResponseMessage.Message.LOGIN_TYPE_MISSING),
  emailAlreadyExistError(ResponseMessage.Key.EMAIL_IN_USE, ResponseMessage.Message.EMAIL_IN_USE),
  invalidCredentials(
      ResponseMessage.Key.INVALID_CREDENTIAL, ResponseMessage.Message.INVALID_CREDENTIAL),
  userNameRequired(ResponseMessage.Key.USERNAME_MISSING, ResponseMessage.Message.USERNAME_MISSING),
  userNameAlreadyExistError(
      ResponseMessage.Key.USERNAME_IN_USE, ResponseMessage.Message.USERNAME_IN_USE),
  userIdRequired(ResponseMessage.Key.USERID_MISSING, ResponseMessage.Message.USERID_MISSING),
  roleRequired(ResponseMessage.Key.ROLE_MISSING, ResponseMessage.Message.ROLE_MISSING),
  msgIdRequiredError(
      ResponseMessage.Key.MESSAGE_ID_MISSING, ResponseMessage.Message.MESSAGE_ID_MISSING),
  userNameCanntBeUpdated(
      ResponseMessage.Key.USERNAME_CANNOT_BE_UPDATED,
      ResponseMessage.Message.USERNAME_CANNOT_BE_UPDATED),
  authTokenRequired(
      ResponseMessage.Key.AUTH_TOKEN_MISSING, ResponseMessage.Message.AUTH_TOKEN_MISSING),
  invalidAuthToken(
      ResponseMessage.Key.INVALID_AUTH_TOKEN, ResponseMessage.Message.INVALID_AUTH_TOKEN),
  timeStampRequired(
      ResponseMessage.Key.TIMESTAMP_REQUIRED, ResponseMessage.Message.TIMESTAMP_REQUIRED),
  publishedCourseCanNotBeUpdated(
      ResponseMessage.Key.PUBLISHED_COURSE_CAN_NOT_UPDATED,
      ResponseMessage.Message.PUBLISHED_COURSE_CAN_NOT_UPDATED),
  sourceRequired(ResponseMessage.Key.SOURCE_MISSING, ResponseMessage.Message.SOURCE_MISSING),
  sectionNameRequired(
      ResponseMessage.Key.SECTION_NAME_MISSING, ResponseMessage.Message.SECTION_NAME_MISSING),
  sectionDataTypeRequired(
      ResponseMessage.Key.SECTION_DATA_TYPE_MISSING,
      ResponseMessage.Message.SECTION_DATA_TYPE_MISSING),
  sectionIdRequired(
      ResponseMessage.Key.SECTION_ID_REQUIRED, ResponseMessage.Message.SECTION_ID_REQUIRED),
  pageNameRequired(
      ResponseMessage.Key.PAGE_NAME_REQUIRED, ResponseMessage.Message.PAGE_NAME_REQUIRED),
  pageIdRequired(ResponseMessage.Key.PAGE_ID_REQUIRED, ResponseMessage.Message.PAGE_ID_REQUIRED),
  invaidConfiguration(
      ResponseMessage.Key.INVALID_CONFIGURATION, ResponseMessage.Message.INVALID_CONFIGURATION),
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
  statusCanntBeUpdated(
      ResponseMessage.Key.STATUS_CANNOT_BE_UPDATED,
      ResponseMessage.Message.STATUS_CANNOT_BE_UPDATED),
  attemptIdRequired(
      ResponseMessage.Key.ATTEMPT_ID_MISSING_ERROR,
      ResponseMessage.Message.ATTEMPT_ID_MISSING_ERROR),
  emailANDUserNameAlreadyExistError(
      ResponseMessage.Key.USERNAME_EMAIL_IN_USE, ResponseMessage.Message.USERNAME_EMAIL_IN_USE),
  keyCloakDefaultError(
      ResponseMessage.Key.KEY_CLOAK_DEFAULT_ERROR, ResponseMessage.Message.KEY_CLOAK_DEFAULT_ERROR),
  userRegUnSuccessfull(
      ResponseMessage.Key.USER_REG_UNSUCCESSFUL, ResponseMessage.Message.USER_REG_UNSUCCESSFUL),
  userUpdationUnSuccessfull(
      ResponseMessage.Key.USER_UPDATE_UNSUCCESSFUL,
      ResponseMessage.Message.USER_UPDATE_UNSUCCESSFUL),
  loginTypeError(ResponseMessage.Key.LOGIN_TYPE_ERROR, ResponseMessage.Message.LOGIN_TYPE_ERROR),
  invalidOrgId(ResponseMessage.Key.INVALID_ORG_ID, ResponseMessage.Key.INVALID_ORG_ID),
  invalidOrgStatus(ResponseMessage.Key.INVALID_ORG_STATUS, ResponseMessage.Key.INVALID_ORG_STATUS),
  invalidOrgStatusTransition(
      ResponseMessage.Key.INVALID_ORG_STATUS_TRANSITION,
      ResponseMessage.Key.INVALID_ORG_STATUS_TRANSITION),
  addressRequired(
      ResponseMessage.Key.ADDRESS_REQUIRED_ERROR, ResponseMessage.Message.ADDRESS_REQUIRED_ERROR),
  educationRequired(
      ResponseMessage.Key.EDUCATION_REQUIRED_ERROR,
      ResponseMessage.Message.EDUCATION_REQUIRED_ERROR),
  phoneNoRequired(
      ResponseMessage.Key.PHONE_NO_REQUIRED_ERROR, ResponseMessage.Message.PHONE_NO_REQUIRED_ERROR),
  jobDetailsRequired(
      ResponseMessage.Key.JOBDETAILS_REQUIRED_ERROR,
      ResponseMessage.Message.JOBDETAILS_REQUIRED_ERROR),
  dataAlreadyExist(
      ResponseMessage.Key.DATA_ALREADY_EXIST, ResponseMessage.Message.DATA_ALREADY_EXIST),
  invalidData(ResponseMessage.Key.INVALID_DATA, ResponseMessage.Message.INVALID_DATA),
  invalidCourseId(ResponseMessage.Key.INVALID_COURSE_ID, ResponseMessage.Message.INVALID_COURSE_ID),
  orgIdRequired(ResponseMessage.Key.ORG_ID_MISSING, ResponseMessage.Message.ORG_ID_MISSING),
  actorConnectionError(
      ResponseMessage.Key.ACTOR_CONNECTION_ERROR, ResponseMessage.Message.ACTOR_CONNECTION_ERROR),
  userAlreadyExists(
      ResponseMessage.Key.USER_ALREADY_EXISTS, ResponseMessage.Message.USER_ALREADY_EXISTS),
  invalidUserId(ResponseMessage.Key.INVALID_USER_ID, ResponseMessage.Message.INVALID_USER_ID),
  loginIdRequired(ResponseMessage.Key.LOGIN_ID_MISSING, ResponseMessage.Message.LOGIN_ID_MISSING),
  contentStatusRequired(
      ResponseMessage.Key.CONTENT_STATUS_MISSING_ERROR,
      ResponseMessage.Message.CONTENT_STATUS_MISSING_ERROR),
  esError(ResponseMessage.Key.ES_ERROR, ResponseMessage.Message.ES_ERROR),
  invalidPeriod(ResponseMessage.Key.INVALID_PERIOD, ResponseMessage.Message.INVALID_PERIOD),
  userNotFound(ResponseMessage.Key.USER_NOT_FOUND, ResponseMessage.Message.USER_NOT_FOUND),
  idRequired(ResponseMessage.Key.ID_REQUIRED_ERROR, ResponseMessage.Message.ID_REQUIRED_ERROR),
  dataTypeError(ResponseMessage.Key.DATA_TYPE_ERROR, ResponseMessage.Message.DATA_TYPE_ERROR),
  errorAttributeConflict(
      ResponseMessage.Key.ERROR_ATTRIBUTE_CONFLICT,
      ResponseMessage.Message.ERROR_ATTRIBUTE_CONFLICT),
  addressError(ResponseMessage.Key.ADDRESS_ERROR, ResponseMessage.Message.ADDRESS_ERROR),
  addressTypeError(
      ResponseMessage.Key.ADDRESS_TYPE_ERROR, ResponseMessage.Message.ADDRESS_TYPE_ERROR),
  educationNameError(
      ResponseMessage.Key.NAME_OF_INSTITUTION_ERROR,
      ResponseMessage.Message.NAME_OF_INSTITUTION_ERROR),
  jobNameError(ResponseMessage.Key.JOB_NAME_ERROR, ResponseMessage.Message.JOB_NAME_ERROR),
  educationDegreeError(
      ResponseMessage.Key.EDUCATION_DEGREE_ERROR, ResponseMessage.Message.EDUCATION_DEGREE_ERROR),
  organisationNameError(
      ResponseMessage.Key.NAME_OF_ORGANISATION_ERROR,
      ResponseMessage.Message.NAME_OF_ORGANISATION_ERROR),
  rolesRequired(ResponseMessage.Key.ROLES_MISSING, ResponseMessage.Message.ROLES_MISSING),
  emptyRolesProvided(
      ResponseMessage.Key.EMPTY_ROLES_PROVIDED, ResponseMessage.Message.EMPTY_ROLES_PROVIDED),
  invalidDateFormat(
      ResponseMessage.Key.INVALID_DATE_FORMAT, ResponseMessage.Message.INVALID_DATE_FORMAT),
  sourceAndExternalIdAlreadyExist(
      ResponseMessage.Key.SRC_EXTERNAL_ID_ALREADY_EXIST,
      ResponseMessage.Message.SRC_EXTERNAL_ID_ALREADY_EXIST),
  userAlreadyEnrolledCourse(
      ResponseMessage.Key.USER_ALREADY_ENROLLED_COURSE,
      ResponseMessage.Message.USER_ALREADY_ENROLLED_COURSE),
  userNotEnrolledCourse(
      ResponseMessage.Key.USER_NOT_ENROLLED_COURSE,
      ResponseMessage.Message.USER_NOT_ENROLLED_COURSE),
  courseBatchAlreadyCompleted(
      ResponseMessage.Key.COURSE_BATCH_ALREADY_COMPLETED,
      ResponseMessage.Message.COURSE_BATCH_ALREADY_COMPLETED),
  courseBatchEnrollmentDateEnded(
      ResponseMessage.Key.COURSE_BATCH_ENROLLMENT_DATE_ENDED,
      ResponseMessage.Message.COURSE_BATCH_ENROLLMENT_DATE_ENDED),
  userAlreadyCompletedCourse(
      ResponseMessage.Key.USER_ALREADY_COMPLETED_COURSE,
      ResponseMessage.Message.USER_ALREADY_COMPLETED_COURSE),
  pageAlreadyExist(
      ResponseMessage.Key.PAGE_ALREADY_EXIST, ResponseMessage.Message.PAGE_ALREADY_EXIST),
  contentTypeRequiredError(
      ResponseMessage.Key.CONTENT_TYPE_ERROR, ResponseMessage.Message.CONTENT_TYPE_ERROR),
  invalidPropertyError(
      ResponseMessage.Key.INVALID_PROPERTY_ERROR, ResponseMessage.Message.INVALID_PROPERTY_ERROR),
  usernameOrUserIdError(
      ResponseMessage.Key.USER_NAME_OR_ID_ERROR, ResponseMessage.Message.USER_NAME_OR_ID_ERROR),
  emailVerifiedError(
      ResponseMessage.Key.EMAIL_VERIFY_ERROR, ResponseMessage.Message.EMAIL_VERIFY_ERROR),
  phoneVerifiedError(
      ResponseMessage.Key.PHONE_VERIFY_ERROR, ResponseMessage.Message.PHONE_VERIFY_ERROR),
  bulkUserUploadError(
      ResponseMessage.Key.BULK_USER_UPLOAD_ERROR, ResponseMessage.Message.BULK_USER_UPLOAD_ERROR),
  dataSizeError(ResponseMessage.Key.DATA_SIZE_EXCEEDED, ResponseMessage.Message.DATA_SIZE_EXCEEDED),
  InvalidColumnError(
      ResponseMessage.Key.INVALID_COLUMN_NAME, ResponseMessage.Message.INVALID_COLUMN_NAME),
  userAccountlocked(
      ResponseMessage.Key.USER_ACCOUNT_BLOCKED, ResponseMessage.Message.USER_ACCOUNT_BLOCKED),
  userAlreadyActive(
      ResponseMessage.Key.USER_ALREADY_ACTIVE, ResponseMessage.Message.USER_ALREADY_ACTIVE),
  userAlreadyInactive(
      ResponseMessage.Key.USER_ALREADY_INACTIVE, ResponseMessage.Message.USER_ALREADY_INACTIVE),
  enrolmentTypeRequired(
      ResponseMessage.Key.ENROLMENT_TYPE_REQUIRED, ResponseMessage.Message.ENROLMENT_TYPE_REQUIRED),
  enrolmentIncorrectValue(
      ResponseMessage.Key.ENROLMENT_TYPE_VALUE_ERROR,
      ResponseMessage.Message.ENROLMENT_TYPE_VALUE_ERROR),
  courseBatchStartDateRequired(
      ResponseMessage.Key.COURSE_BATCH_START_DATE_REQUIRED,
      ResponseMessage.Message.COURSE_BATCH_START_DATE_REQUIRED),
  courseBatchStartDateError(
      ResponseMessage.Key.COURSE_BATCH_START_DATE_INVALID,
      ResponseMessage.Message.COURSE_BATCH_START_DATE_INVALID),
  dateFormatError(
      ResponseMessage.Key.DATE_FORMAT_ERRROR, ResponseMessage.Message.DATE_FORMAT_ERRROR),
  endDateError(ResponseMessage.Key.END_DATE_ERROR, ResponseMessage.Message.END_DATE_ERROR),
  enrollmentEndDateStartError(
      ResponseMessage.Key.ENROLLMENT_END_DATE_START_ERROR,
      ResponseMessage.Message.ENROLLMENT_END_DATE_START_ERROR),
  enrollmentEndDateEndError(
      ResponseMessage.Key.ENROLLMENT_END_DATE_END_ERROR,
      ResponseMessage.Message.ENROLLMENT_END_DATE_END_ERROR),
  enrollmentEndDateUpdateError(
      ResponseMessage.Key.ENROLLMENT_END_DATE_UPDATE_ERROR,
      ResponseMessage.Message.ENROLLMENT_END_DATE_UPDATE_ERROR),
  csvError(ResponseMessage.Key.INVALID_CSV_FILE, ResponseMessage.Message.INVALID_CSV_FILE),
  invalidCourseBatchId(
      ResponseMessage.Key.INVALID_COURSE_BATCH_ID, ResponseMessage.Message.INVALID_COURSE_BATCH_ID),
  courseBatchIdRequired(
      ResponseMessage.Key.COURSE_BATCH_ID_MISSING, ResponseMessage.Message.COURSE_BATCH_ID_MISSING),
  enrollmentTypeValidation(
      ResponseMessage.Key.ENROLLMENT_TYPE_VALIDATION,
      ResponseMessage.Message.ENROLLMENT_TYPE_VALIDATION),
  courseCreatedForIsNull(
      ResponseMessage.Key.COURSE_CREATED_FOR_NULL, ResponseMessage.Message.COURSE_CREATED_FOR_NULL),
  userNotAssociatedToOrg(
      ResponseMessage.Key.USER_NOT_BELONGS_TO_ANY_ORG,
      ResponseMessage.Message.USER_NOT_BELONGS_TO_ANY_ORG),
  invalidObjectType(
      ResponseMessage.Key.INVALID_OBJECT_TYPE, ResponseMessage.Message.INVALID_OBJECT_TYPE),
  progressStatusError(
      ResponseMessage.Key.INVALID_PROGRESS_STATUS, ResponseMessage.Message.INVALID_PROGRESS_STATUS),
  courseBatchStartPassedDateError(
      ResponseMessage.Key.COURSE_BATCH_START_PASSED_DATE_INVALID,
      ResponseMessage.Message.COURSE_BATCH_START_PASSED_DATE_INVALID),
  csvFileEmpty(ResponseMessage.Key.EMPTY_CSV_FILE, ResponseMessage.Message.EMPTY_CSV_FILE),
  invalidRootOrgData(
      ResponseMessage.Key.INVALID_ROOT_ORG_DATA, ResponseMessage.Message.INVALID_ROOT_ORG_DATA),
  noDataForConsumption(ResponseMessage.Key.NO_DATA, ResponseMessage.Message.NO_DATA),
  invalidChannel(ResponseMessage.Key.INVALID_CHANNEL, ResponseMessage.Message.INVALID_CHANNEL),
  invalidProcessId(
      ResponseMessage.Key.INVALID_PROCESS_ID, ResponseMessage.Message.INVALID_PROCESS_ID),
  emailSubjectError(
      ResponseMessage.Key.EMAIL_SUBJECT_ERROR, ResponseMessage.Message.EMAIL_SUBJECT_ERROR),
  emailBodyError(ResponseMessage.Key.EMAIL_BODY_ERROR, ResponseMessage.Message.EMAIL_BODY_ERROR),
  recipientAddressError(
      ResponseMessage.Key.RECIPIENT_ADDRESS_ERROR, ResponseMessage.Message.RECIPIENT_ADDRESS_ERROR),
  storageContainerNameMandatory(
      ResponseMessage.Key.STORAGE_CONTAINER_NAME_MANDATORY,
      ResponseMessage.Message.STORAGE_CONTAINER_NAME_MANDATORY),
  userOrgAssociationError(
      ResponseMessage.Key.USER_ORG_ASSOCIATION_ERROR,
      ResponseMessage.Message.USER_ORG_ASSOCIATION_ERROR),
  cloudServiceError(
      ResponseMessage.Key.CLOUD_SERVICE_ERROR, ResponseMessage.Message.CLOUD_SERVICE_ERROR),
  receiverIdMandatory(
      ResponseMessage.Key.RECEIVER_ID_ERROR, ResponseMessage.Message.RECEIVER_ID_ERROR),
  invalidReceiverId(
      ResponseMessage.Key.INVALID_RECEIVER_ID, ResponseMessage.Message.INVALID_RECEIVER_ID),
  invalidRole(ResponseMessage.Key.INVALID_ROLE, ResponseMessage.Message.INVALID_ROLE),
  saltValue(ResponseMessage.Key.INVALID_SALT, ResponseMessage.Message.INVALID_SALT),
  orgTypeMandatory(
      ResponseMessage.Key.ORG_TYPE_MANDATORY, ResponseMessage.Message.ORG_TYPE_MANDATORY),
  orgTypeAlreadyExist(
      ResponseMessage.Key.ORG_TYPE_ALREADY_EXIST, ResponseMessage.Message.ORG_TYPE_ALREADY_EXIST),
  orgTypeIdRequired(
      ResponseMessage.Key.ORG_TYPE_ID_REQUIRED_ERROR,
      ResponseMessage.Message.ORG_TYPE_ID_REQUIRED_ERROR),
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
  invalidWebPageData(
      ResponseMessage.Key.INVALID_WEBPAGE_DATA, ResponseMessage.Message.INVALID_WEBPAGE_DATA),
  invalidMediaType(
      ResponseMessage.Key.INVALID_MEDIA_TYPE, ResponseMessage.Message.INVALID_MEDIA_TYPE),
  invalidWebPageUrl(
      ResponseMessage.Key.INVALID_WEBPAGE_URL, ResponseMessage.Message.INVALID_WEBPAGE_URL),
  invalidDateRange(
      ResponseMessage.Key.INVALID_DATE_RANGE, ResponseMessage.Message.INVALID_DATE_RANGE),
  invalidBatchEndDateError(
      ResponseMessage.Key.INVALID_BATCH_END_DATE_ERROR,
      ResponseMessage.Message.INVALID_BATCH_END_DATE_ERROR),
  invalidBatchStartDateError(
      ResponseMessage.Key.INVALID_BATCH_START_DATE_ERROR,
      ResponseMessage.Message.INVALID_BATCH_START_DATE_ERROR),
  courseBatchEndDateError(
      ResponseMessage.Key.COURSE_BATCH_END_DATE_ERROR,
      ResponseMessage.Message.COURSE_BATCH_END_DATE_ERROR),
  BatchCloseError(
      ResponseMessage.Key.COURSE_BATCH_IS_CLOSED_ERROR,
      ResponseMessage.Message.COURSE_BATCH_IS_CLOSED_ERROR),
  newPasswordRequired(
      ResponseMessage.Key.CONFIIRM_PASSWORD_MISSING,
      ResponseMessage.Message.CONFIIRM_PASSWORD_MISSING),
  newPasswordEmpty(
      ResponseMessage.Key.CONFIIRM_PASSWORD_EMPTY, ResponseMessage.Message.CONFIIRM_PASSWORD_EMPTY),
  samePasswordError(
      ResponseMessage.Key.SAME_PASSWORD_ERROR, ResponseMessage.Message.SAME_PASSWORD_ERROR),
  endorsedUserIdRequired(
      ResponseMessage.Key.ENDORSED_USER_ID_REQUIRED,
      ResponseMessage.Message.ENDORSED_USER_ID_REQUIRED),
  canNotEndorse(ResponseMessage.Key.CAN_NOT_ENDORSE, ResponseMessage.Message.CAN_NOT_ENDORSE),
  invalidOrgTypeId(
      ResponseMessage.Key.INVALID_ORG_TYPE_ID_ERROR,
      ResponseMessage.Message.INVALID_ORG_TYPE_ID_ERROR),
  invalidOrgType(
      ResponseMessage.Key.INVALID_ORG_TYPE_ERROR, ResponseMessage.Message.INVALID_ORG_TYPE_ERROR),
  tableOrDocNameError(
      ResponseMessage.Key.TABLE_OR_DOC_NAME_ERROR, ResponseMessage.Message.TABLE_OR_DOC_NAME_ERROR),
  emailorPhoneRequired(
      ResponseMessage.Key.EMAIL_OR_PHONE_MISSING, ResponseMessage.Message.EMAIL_OR_PHONE_MISSING),
  emailorPhoneorManagedByRequired(
      ResponseMessage.Key.EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING,
      ResponseMessage.Message.EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING),
  OnlyEmailorPhoneorManagedByRequired(
      ResponseMessage.Key.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED,
      ResponseMessage.Message.ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED),
  PhoneNumberInUse(
      ResponseMessage.Key.PHONE_ALREADY_IN_USE, ResponseMessage.Message.PHONE_ALREADY_IN_USE),
  invalidClientName(
      ResponseMessage.Key.INVALID_CLIENT_NAME, ResponseMessage.Message.INVALID_CLIENT_NAME),
  invalidClientId(ResponseMessage.Key.INVALID_CLIENT_ID, ResponseMessage.Message.INVALID_CLIENT_ID),
  userPhoneUpdateFailed(
      ResponseMessage.Key.USER_PHONE_UPDATE_FAILED,
      ResponseMessage.Message.USER_PHONE_UPDATE_FAILED),
  esUpdateFailed(ResponseMessage.Key.ES_UPDATE_FAILED, ResponseMessage.Message.ES_UPDATE_FAILED),
  updateFailed(ResponseMessage.Key.UPDATE_FAILED, ResponseMessage.Message.UPDATE_FAILED),
  invalidTypeValue(ResponseMessage.Key.INVALID_TYPE_VALUE, ResponseMessage.Key.INVALID_TYPE_VALUE),
  invalidLocationId(
      ResponseMessage.Key.INVALID_LOCATION_ID, ResponseMessage.Message.INVALID_LOCATION_ID),
  invalidLocationType(
      ResponseMessage.Key.INVALID_LOCATION_TYPE, ResponseMessage.Message.INVALID_LOCATION_TYPE),
  invalidHashTagId(
      ResponseMessage.Key.INVALID_HASHTAG_ID, ResponseMessage.Message.INVALID_HASHTAG_ID),
  invalidUsrOrgData(
      ResponseMessage.Key.INVALID_USR_ORG_DATA, ResponseMessage.Message.INVALID_USR_ORG_DATA),
  visibilityInvalid(
      ResponseMessage.Key.INVALID_VISIBILITY_REQUEST,
      ResponseMessage.Message.INVALID_VISIBILITY_REQUEST),
  invalidTopic(ResponseMessage.Key.INVALID_TOPIC_NAME, ResponseMessage.Message.INVALID_TOPIC_NAME),
  invalidTopicData(
      ResponseMessage.Key.INVALID_TOPIC_DATA, ResponseMessage.Message.INVALID_TOPIC_DATA),
  invalidNotificationType(
      ResponseMessage.Key.INVALID_NOTIFICATION_TYPE,
      ResponseMessage.Message.INVALID_NOTIFICATION_TYPE),
  notificationTypeSupport(
      ResponseMessage.Key.INVALID_NOTIFICATION_TYPE_SUPPORT,
      ResponseMessage.Message.INVALID_NOTIFICATION_TYPE_SUPPORT),
  emailInUse(ResponseMessage.Key.EMAIL_IN_USE, ResponseMessage.Message.EMAIL_IN_USE),
  invalidPhoneNumber(
      ResponseMessage.Key.INVALID_PHONE_NUMBER, ResponseMessage.Message.INVALID_PHONE_NUMBER),
  invalidCountryCode(
      ResponseMessage.Key.INVALID_COUNTRY_CODE, ResponseMessage.Message.INVALID_COUNTRY_CODE),
  locationIdRequired(
      ResponseMessage.Key.LOCATION_ID_REQUIRED, ResponseMessage.Message.LOCATION_ID_REQUIRED),
  functionalityMissing(ResponseMessage.Key.NOT_SUPPORTED, ResponseMessage.Message.NOT_SUPPORTED),
  userNameOrUserIdRequired(
      ResponseMessage.Key.USERNAME_USERID_MISSING, ResponseMessage.Message.USERNAME_USERID_MISSING),
  channelRegFailed(
      ResponseMessage.Key.CHANNEL_REG_FAILED, ResponseMessage.Message.CHANNEL_REG_FAILED),
  invalidCourseCreatorId(
      ResponseMessage.Key.INVALID_COURSE_CREATOR_ID,
      ResponseMessage.Message.INVALID_COURSE_CREATOR_ID),
  userNotAssociatedToRootOrg(
      ResponseMessage.Key.USER_NOT_ASSOCIATED_TO_ROOT_ORG,
      ResponseMessage.Message.USER_NOT_ASSOCIATED_TO_ROOT_ORG),
  slugIsNotUnique(
      ResponseMessage.Key.SLUG_IS_NOT_UNIQUE, ResponseMessage.Message.SLUG_IS_NOT_UNIQUE),
  recipientEmailRequired(
      ResponseMessage.Key.RECIPIENT_EMAIL_REQUIRED,
      ResponseMessage.Message.RECIPIENT_EMAIL_REQUIRED),
  evidenceRequired(
      ResponseMessage.Key.ASSERTION_EVIDENCE_REQUIRED,
      ResponseMessage.Message.ASSERTION_EVIDENCE_REQUIRED),
  assertionIdRequired(
      ResponseMessage.Key.ASSERTION_ID_REQUIRED, ResponseMessage.Message.ASSERTION_ID_REQUIRED),
  recipientIdRequired(
      ResponseMessage.Key.RECIPIENT_ID_REQUIRED, ResponseMessage.Message.RECIPIENT_ID_REQUIRED),
  recipientTypeRequired(
      ResponseMessage.Key.RECIPIENT_TYPE_REQUIRED, ResponseMessage.Message.RECIPIENT_TYPE_REQUIRED),
  badgingserverError(
      ResponseMessage.Key.BADGING_SERVER_ERROR, ResponseMessage.Message.BADGING_SERVER_ERROR),
  resourceNotFound(
      ResponseMessage.Key.RESOURCE_NOT_FOUND, ResponseMessage.Message.RESOURCE_NOT_FOUND),
  sizeLimitExceed(
      ResponseMessage.Key.MAX_ALLOWED_SIZE_LIMIT_EXCEED,
      ResponseMessage.Message.MAX_ALLOWED_SIZE_LIMIT_EXCEED),
  slugRequired(ResponseMessage.Key.SLUG_REQUIRED, ResponseMessage.Message.SLUG_REQUIRED),
  invalidIssuerId(ResponseMessage.Key.INVALID_ISSUER_ID, ResponseMessage.Message.INVALID_ISSUER_ID),
  revocationReasonRequired(
      ResponseMessage.Key.REVOCATION_REASON_REQUIRED,
      ResponseMessage.Message.REVOCATION_REASON_REQUIRED),
  invalidRecipientType(
      ResponseMessage.Key.INVALID_RECIPIENT_TYPE, ResponseMessage.Message.INVALID_RECIPIENT_TYPE),
  customClientError(
      ResponseMessage.Key.CUSTOM_CLIENT_ERROR, ResponseMessage.Message.CUSTOM_CLIENT_ERROR),
  customResourceNotFound(
      ResponseMessage.Key.CUSTOM_RESOURCE_NOT_FOUND_ERROR,
      ResponseMessage.Message.CUSTOM_RESOURCE_NOT_FOUND_ERROR),
  customServerError(
      ResponseMessage.Key.CUSTOM_SERVER_ERROR, ResponseMessage.Message.CUSTOM_SERVER_ERROR),
  inactiveUser(ResponseMessage.Key.INACTIVE_USER, ResponseMessage.Message.INACTIVE_USER),
  userInactiveForThisOrg(
      ResponseMessage.Key.USER_INACTIVE_FOR_THIS_ORG,
      ResponseMessage.Message.USER_INACTIVE_FOR_THIS_ORG),
  userUpdateToOrgFailed(
      ResponseMessage.Key.USER_UPDATE_FAILED_FOR_THIS_ORG,
      ResponseMessage.Message.USER_UPDATE_FAILED_FOR_THIS_ORG),
  preferenceKeyMissing(
      ResponseMessage.Key.USER_UPDATE_FAILED_FOR_THIS_ORG,
      ResponseMessage.Message.USER_UPDATE_FAILED_FOR_THIS_ORG),
  pageDoesNotExist(ResponseMessage.Key.PAGE_NOT_EXIST, ResponseMessage.Message.PAGE_NOT_EXIST),
  sectionDoesNotExist(
      ResponseMessage.Key.SECTION_NOT_EXIST, ResponseMessage.Message.SECTION_NOT_EXIST),
  orgDoesNotExist(ResponseMessage.Key.ORG_NOT_EXIST, ResponseMessage.Message.ORG_NOT_EXIST),
  invalidPageSource(
      ResponseMessage.Key.INVALID_PAGE_SOURCE, ResponseMessage.Message.INVALID_PAGE_SOURCE),
  locationTypeRequired(
      ResponseMessage.Key.LOCATION_TYPE_REQUIRED, ResponseMessage.Message.LOCATION_TYPE_REQUIRED),
  invalidRequestDataForLocation(
      ResponseMessage.Key.INVALID_REQUEST_DATA_FOR_LOCATION,
      ResponseMessage.Message.INVALID_REQUEST_DATA_FOR_LOCATION),
  alreadyExists(ResponseMessage.Key.ALREADY_EXISTS, ResponseMessage.Message.ALREADY_EXISTS),
  invalidValue(ResponseMessage.Key.INVALID_VALUE, ResponseMessage.Message.INVALID_VALUE),
  parentCodeAndIdValidationError(
      ResponseMessage.Key.PARENT_CODE_AND_PARENT_ID_MISSING,
      ResponseMessage.Message.PARENT_CODE_AND_PARENT_ID_MISSING),
  invalidParameter(
      ResponseMessage.Key.INVALID_PARAMETER, ResponseMessage.Message.INVALID_PARAMETER),
  invalidLocationDeleteRequest(
      ResponseMessage.Key.INVALID_LOCATION_DELETE_REQUEST,
      ResponseMessage.Message.INVALID_LOCATION_DELETE_REQUEST),
  locationTypeConflicts(
      ResponseMessage.Key.LOCATION_TYPE_CONFLICTS, ResponseMessage.Message.LOCATION_TYPE_CONFLICTS),
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
  mandatoryHeadersMissing(
      ResponseMessage.Key.MANDATORY_HEADER_MISSING,
      ResponseMessage.Message.MANDATORY_HEADER_MISSING),
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
  mandatoryConfigParamMissing(
      ResponseMessage.Key.MANDATORY_CONFIG_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_CONFIG_PARAMETER_MISSING),
  cassandraConnectionEstablishmentFailed(
      ResponseMessage.Key.CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED,
      ResponseMessage.Message.CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED),
  commonAttributeMismatch(
      ResponseMessage.Key.COMMON_ATTRIBUTE_MISMATCH,
      ResponseMessage.Message.COMMON_ATTRIBUTE_MISMATCH),
  multipleCoursesNotAllowedForBatch(
      ResponseMessage.Key.MULTIPLE_COURSES_FOR_BATCH,
      ResponseMessage.Message.MULTIPLE_COURSES_FOR_BATCH),
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
  errorLoadConfig(ResponseMessage.Key.ERROR_LOAD_CONFIG, ResponseMessage.Message.ERROR_LOAD_CONFIG),
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
  duplicateExternalIds(
      ResponseMessage.Key.DUPLICATE_EXTERNAL_IDS, ResponseMessage.Message.DUPLICATE_EXTERNAL_IDS),
  invalidDuplicateValue(
      ResponseMessage.Key.INVALID_DUPLICATE_VALUE, ResponseMessage.Message.INVALID_DUPLICATE_VALUE),
  emailNotSentRecipientsExceededMaxLimit(
      ResponseMessage.Key.EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT,
      ResponseMessage.Message.EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT),
  emailNotSentRecipientsZero(
      ResponseMessage.Key.NO_EMAIL_RECIPIENTS, ResponseMessage.Message.NO_EMAIL_RECIPIENTS),
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
  errorConflictingFieldConfiguration(
      ResponseMessage.Key.ERROR_CONFLICTING_FIELD_CONFIGURATION,
      ResponseMessage.Message.ERROR_CONFLICTING_FIELD_CONFIGURATION),
  errorSystemSettingNotFound(
      ResponseMessage.Key.ERROR_SYSTEM_SETTING_NOT_FOUND,
      ResponseMessage.Message.ERROR_SYSTEM_SETTING_NOT_FOUND),
  errorNoRootOrgAssociated(
      ResponseMessage.Key.ERROR_NO_ROOT_ORG_ASSOCIATED,
      ResponseMessage.Message.ERROR_NO_ROOT_ORG_ASSOCIATED),
  errorInactiveCustodianOrg(
      ResponseMessage.Key.ERROR_INACTIVE_CUSTODIAN_ORG,
      ResponseMessage.Message.ERROR_INACTIVE_CUSTODIAN_ORG),
  errorUnsupportedCloudStorage(
      ResponseMessage.Key.ERROR_UNSUPPORTED_CLOUD_STORAGE,
      ResponseMessage.Message.ERROR_UNSUPPORTED_CLOUD_STORAGE),
  errorUnsupportedField(
      ResponseMessage.Key.ERROR_UNSUPPORTED_FIELD, ResponseMessage.Message.ERROR_UNSUPPORTED_FIELD),
  errorGenerateDownloadLink(
      ResponseMessage.Key.ERROR_GENERATE_DOWNLOAD_LINK,
      ResponseMessage.Message.ERROR_GENERATE_DOWNLOAD_LINK),
  errorUnavailableDownloadLink(
      ResponseMessage.Key.ERROR_DOWNLOAD_LINK_UNAVAILABLE,
      ResponseMessage.Message.ERROR_DOWNLOAD_LINK_UNAVAILABLE),
  errorSavingStorageDetails(
      ResponseMessage.Key.ERROR_SAVING_STORAGE_DETAILS,
      ResponseMessage.Message.ERROR_SAVING_STORAGE_DETAILS),
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
  errorUpdateSettingNotAllowed(
      ResponseMessage.Key.ERROR_UPDATE_SETTING_NOT_ALLOWED,
      ResponseMessage.Message.ERROR_UPDATE_SETTING_NOT_ALLOWED),
  errorCreatingFile(
      ResponseMessage.Key.ERROR_CREATING_FILE, ResponseMessage.Message.ERROR_CREATING_FILE),
  errorProcessingRequest(
      ResponseMessage.Key.ERROR_PROCESSING_REQUEST,
      ResponseMessage.Message.ERROR_PROCESSING_REQUEST),
  errorUnavailableCertificate(
      ResponseMessage.Key.ERROR_UNAVAILABLE_CERTIFICATE,
      ResponseMessage.Message.ERROR_UNAVAILABLE_CERTIFICATE),
  invalidTextbook(ResponseMessage.Key.INVALID_TEXTBOOK, ResponseMessage.Message.INVALID_TEXTBOOK),
  csvRowsExceeds(ResponseMessage.Key.CSV_ROWS_EXCEEDS, ResponseMessage.Message.CSV_ROWS_EXCEEDS),
  invalidTextbookName(
      ResponseMessage.Key.INVALID_TEXTBOOK_NAME, ResponseMessage.Message.INVALID_TEXTBOOK_NAME),
  duplicateRows(ResponseMessage.Key.DUPLICATE_ROWS, ResponseMessage.Message.DUPLICATE_ROWS),
  requiredHeaderMissing(
      ResponseMessage.Key.REQUIRED_HEADER_MISSING, ResponseMessage.Message.REQUIRED_HEADER_MISSING),
  requiredFieldMissing(
      ResponseMessage.Key.REQUIRED_FIELD_MISSING, ResponseMessage.Message.REQUIRED_FIELD_MISSING),
  blankCsvData(ResponseMessage.Key.BLANK_CSV_DATA, ResponseMessage.Message.BLANK_CSV_DATA),
  exceedMaxChildren(
      ResponseMessage.Key.EXCEEDS_MAX_CHILDREN, ResponseMessage.Message.EXCEEDS_MAX_CHILDREN),
  textbookChildrenExist(
      ResponseMessage.Key.TEXTBOOK_CHILDREN_EXISTS,
      ResponseMessage.Message.TEXTBOOK_CHILDREN_EXISTS),
  textbookUpdateFailure(
      ResponseMessage.Key.TEXTBOOK_UPDATE_FAILURE, ResponseMessage.Message.TEXTBOOK_UPDATE_FAILURE),
  noChildrenExists(
      ResponseMessage.Key.TEXTBOOK_CHILDREN_NOT_EXISTS,
      ResponseMessage.Message.TEXTBOOK_CHILDREN_NOT_EXISTS),
  textBookNotFound(
      ResponseMessage.Key.TEXTBOOK_NOT_FOUND, ResponseMessage.Message.TEXTBOOK_NOT_FOUND),
  errorProcessingFile(
      ResponseMessage.Key.ERROR_PROCESSING_FILE, ResponseMessage.Message.ERROR_PROCESSING_FILE),
  fileNotFound(ResponseMessage.Key.ERR_FILE_NOT_FOUND, ResponseMessage.Message.ERR_FILE_NOT_FOUND),
  errorTbUpdate(ResponseMessage.Key.ERROR_TB_UPDATE, ResponseMessage.Message.ERROR_TB_UPDATE),
  errorInvalidParameterSize(
      ResponseMessage.Key.ERROR_INVALID_PARAMETER_SIZE,
      ResponseMessage.Message.ERROR_INVALID_PARAMETER_SIZE),
  errorInvalidPageSection(
      ResponseMessage.Key.INVALID_PAGE_SECTION, ResponseMessage.Message.INVALID_PAGE_SECTION),
  errorRateLimitExceeded(
      ResponseMessage.Key.ERROR_RATE_LIMIT_EXCEEDED,
      ResponseMessage.Message.ERROR_RATE_LIMIT_EXCEEDED),
  errorInvalidDialCode(
      ResponseMessage.Key.ERROR_INVALID_DIAL_CODE, ResponseMessage.Message.ERROR_INVALID_DIAL_CODE),
  errorInvalidTopic(
      ResponseMessage.Key.ERROR_INVALID_TOPIC, ResponseMessage.Message.ERROR_INVALID_TOPIC),
  errorDialCodeDuplicateEntry(
      ResponseMessage.Key.ERROR_DIAL_CODE_DUPLICATE_ENTRY,
      ResponseMessage.Message.ERROR_DIAL_CODE_DUPLICATE_ENTRY),
  errorDialCodeAlreadyAssociated(
      ResponseMessage.Key.ERROR_DIAL_CODE_ALREADY_ASSOCIATED,
      ResponseMessage.Message.ERROR_DIAL_CODE_ALREADY_ASSOCIATED),
  errorDialCodeLinkingFail(
      ResponseMessage.Key.DIAL_CODE_LINKING_FAILED,
      ResponseMessage.Message.DIAL_CODE_LINKING_FAILED),
  errorDialCodeLinkingClientError(
      ResponseMessage.Key.ERROR_TEXTBOOK_UPDATE, ResponseMessage.Message.ERROR_TEXTBOOK_UPDATE),
  errorInvalidLinkedContentId(
      ResponseMessage.Key.ERROR_INVALID_LINKED_CONTENT_ID,
      ResponseMessage.Message.ERROR_INVALID_LINKED_CONTENT_ID),
  errorDuplicateLinkedContentId(
      ResponseMessage.Key.ERROR_DUPLICATE_LINKED_CONTENT,
      ResponseMessage.Message.ERROR_DUPLICATE_LINKED_CONTENT),

  errorTeacherCannotBelongToCustodianOrg(
      ResponseMessage.Key.TEACHER_CANNOT_BELONG_TO_CUSTODIAN_ORG,
      ResponseMessage.Message.TEACHER_CANNOT_BELONG_TO_CUSTODIAN_ORG),
  errorDduplicateDialCodeEntry(
      ResponseMessage.Key.ERROR_DUPLICATE_QR_CODE_ENTRY,
      ResponseMessage.Message.ERROR_DUPLICATE_QR_CODE_ENTRY),
  errorInvalidTextbookUnitId(
      ResponseMessage.Key.ERROR_INVALID_TEXTBOOK_UNIT_ID,
      ResponseMessage.Message.ERROR_INVALID_TEXTBOOK_UNIT_ID),
  invalidRequestTimeout(
      ResponseMessage.Key.INVALID_REQUEST_TIMEOUT, ResponseMessage.Message.INVALID_REQUEST_TIMEOUT),
  errorBGMSMismatch(
      ResponseMessage.Key.ERROR_BGMS_MISMATCH, ResponseMessage.Message.ERROR_BGMS_MISMATCH),
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
  fromAccountIdNotExists(
      ResponseMessage.Key.FROM_ACCOUNT_ID_NOT_EXISTS,
      ResponseMessage.Message.FROM_ACCOUNT_ID_NOT_EXISTS),
  mandatoryHeaderParamsMissing(
      ResponseMessage.Key.MANDATORY_HEADER_PARAMETER_MISSING,
      ResponseMessage.Message.MANDATORY_HEADER_PARAMETER_MISSING),
  recoveryParamsMatchException(
      ResponseMessage.Key.RECOVERY_PARAM_MATCH_EXCEPTION,
      ResponseMessage.Message.RECOVERY_PARAM_MATCH_EXCEPTION),
  PARAM_NOT_MATCH(ResponseMessage.Key.PARAM_NOT_MATCH, ResponseMessage.Message.PARAM_NOT_MATCH),
  emptyContentsForUpdateBatchStatus(
      ResponseMessage.Key.EMPTY_CONTENTS_FOR_UPDATE_BATCH_STATUS,
      ResponseMessage.Message.EMPTY_CONTENTS_FOR_UPDATE_BATCH_STATUS),
  errorUserHasNotCreatedAnyCourse(
      ResponseMessage.Key.ERROR_USER_HAS_NOT_CREATED_ANY_COURSE,
      ResponseMessage.Message.ERROR_USER_HAS_NOT_CREATED_ANY_COURSE),
  errorUploadQRCodeCSVfailed(
      ResponseMessage.Key.ERROR_UPLOAD_QRCODE_CSV_FAILED,
      ResponseMessage.Message.ERROR_UPLOAD_QRCODE_CSV_FAILED),
  errorNoDialcodesLinked(
      ResponseMessage.Key.ERROR_NO_DIALCODES_LINKED,
      ResponseMessage.Message.ERROR_NO_DIALCODES_LINKED),
  eventsRequired(
      ResponseMessage.Key.EVENTS_DATA_MISSING, ResponseMessage.Message.EVENTS_DATA_MISSING),
  accountNotFound(ResponseMessage.Key.ACCOUNT_NOT_FOUND, ResponseMessage.Message.ACCOUNT_NOT_FOUND),
  userMigrationFiled(
      ResponseMessage.Key.USER_MIGRATION_FAILED, ResponseMessage.Message.USER_MIGRATION_FAILED),
  invalidUserExternalId(
      ResponseMessage.Key.INVALID_EXT_USER_ID, ResponseMessage.Message.INVALID_EXT_USER_ID),
  invalidElementInList(
      ResponseMessage.Key.INVALID_ELEMENT_IN_LIST, ResponseMessage.Message.INVALID_ELEMENT_IN_LIST),
  passwordValidation(
      ResponseMessage.Key.INVALID_PASSWORD, ResponseMessage.Message.INVALID_PASSWORD),
  otpVerificationFailed(
      ResponseMessage.Key.OTP_VERIFICATION_FAILED, ResponseMessage.Message.OTP_VERIFICATION_FAILED),
  serviceUnAvailable(
      ResponseMessage.Key.SERVICE_UNAVAILABLE, ResponseMessage.Message.SERVICE_UNAVAILABLE),
  missingData(ResponseMessage.Key.MISSING_CODE, ResponseMessage.Message.MISSING_MESSAGE),
  managedByNotAllowed(
      ResponseMessage.Key.MANAGED_BY_NOT_ALLOWED, ResponseMessage.Message.MANAGED_BY_NOT_ALLOWED),
  managedByEmailPhoneUpdateError(
      ResponseMessage.Key.MANAGED_BY_EMAIL_PHONE_UPDATE_ERROR,
      ResponseMessage.Message.MANAGED_BY_EMAIL_PHONE_UPDATE_ERROR),
  managedUserLimitExceeded(
      ResponseMessage.Key.MANAGED_USER_LIMIT_EXCEEDED,
      ResponseMessage.Message.MANAGED_USER_LIMIT_EXCEEDED),
  unableToConnectToAdminUtil(
      ResponseMessage.Key.UNABLE_TO_CONNECT_TO_ADMINUTIL,
      ResponseMessage.Message.UNABLE_TO_CONNECT_TO_ADMINUTIL),
  dataEncryptionError(
      ResponseMessage.Key.DATA_ENCRYPTION_ERROR, ResponseMessage.Message.DATA_ENCRYPTION_ERROR),
  notificationNotSent(
      ResponseMessage.Key.NO_EMAIL_PHONE_ASSOCIATED,
      ResponseMessage.Message.NO_EMAIL_PHONE_ASSOCIATED),
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
