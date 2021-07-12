package org.sunbird.exception;

/**
 * This interface will hold all the response key and message
 *
 * @author Manzarul
 */
public interface ResponseMessage {

  interface Message {

    String UNAUTHORIZED_USER = "You are not authorized.";
    String OPERATION_TIMEOUT = "Request processing taking too long time. Please try again later.";
    String INVALID_OPERATION_NAME =
        "Operation name is invalid. Please provide a valid operation name";
    String INVALID_REQUESTED_DATA = "Requested data for this operation is not valid.";
    String API_KEY_MISSING_ERROR = "APi key is mandatory.";
    String INTERNAL_ERROR = "Process failed,please try again later.";
    String SUCCESS_MESSAGE = "Success";
    String EMAIL_FORMAT = "Email is invalid.";
    String FIRST_NAME_MISSING = "First name is mandatory.";
    String REQUIRED_DATA_ORG_MISSING =
        "Organization Id or Provider with External Id values are required for the operation";
    String CHANNEL_SHOULD_BE_UNIQUE =
        "Channel value already used by another organization. Provide different value for channel";
    String ERROR_DUPLICATE_ENTRY = "Value {0} for {1} is already in use.";
    String INVALID_ORG_DATA =
        "Given Organization Data doesn't exist in our records. Please provide a valid one";
    String INVALID_USR_DATA =
        "Given User Data doesn't exist in our records. Please provide a valid one";
    String USR_DATA_VALIDATION_ERROR = "Please provide valid userId or userName and provider";
    String INVALID_ROOT_ORGANIZATION = "Root organization id is invalid";
    String ERROR_INVALID_OTP = "Invalid OTP.";
    String EMAIL_IN_USE = "Email already exists.";
    String USERNAME_EMAIL_IN_USE =
        "Username or Email is already in use. Please try with a different Username or Email.";
    String KEY_CLOAK_DEFAULT_ERROR = "server error at sso.";
    String USERNAME_MISSING = "Username is mandatory.";
    String USERNAME_IN_USE = "Username already exists.";
    String USERID_MISSING = "UserId is mandatory.";
    String AUTH_TOKEN_MISSING = "Auth token is mandatory.";
    String DB_INSERTION_FAIL = "DB insert operation failed.";
    String DB_UPDATE_FAIL = "Db update operation failed.";
    String PHONE_NO_REQUIRED_ERROR = "Phone number is required.";
    String INVALID_DATA = "Incorrect data.";
    String ORG_ID_MISSING = "Organization Id required.";
    String ACTOR_CONNECTION_ERROR = "Service is not able to connect with actor.";
    String USER_ALREADY_EXISTS = "User already exists for given {0}.";
    String INVALID_USER_ID = "User Id does not exists in our records";
    String LOGIN_ID_MISSING = "loginId is required.";
    String USER_NOT_FOUND = "user not found.";
    String ID_REQUIRED_ERROR = "For deleting a record, Id is required.";
    String DATA_TYPE_ERROR = "Data type of {0} should be {1}.";
    String ERROR_ATTRIBUTE_CONFLICT = "Either pass attribute {0} or {1} but not both.";
    String NAME_OF_ORGANISATION_ERROR = "Organization Name is required.";
    String ROLES_MISSING = "user role is required.";
    String EMPTY_ROLES_PROVIDED = "Roles cannot be empty.";
    String CHANNEL_REG_FAILED = "Channel Registration failed.";
    String SLUG_IS_NOT_UNIQUE =
        "Please provide different channel value. This channel value already exist.";
    String EXISTING_ORG_MEMBER = "You already have a membership of this organization.";
    String CONTENT_TYPE_ERROR = "Please add Content-Type header with value application/json";
    String INVALID_PROPERTY_ERROR = "Invalid property {0}.";
    String USER_ACCOUNT_BLOCKED = "User account has been blocked .";
    String DATA_SIZE_EXCEEDED = "Maximum upload data size should be {0}";
    String USER_ALREADY_ACTIVE = "User is already active.";
    String USER_ALREADY_INACTIVE = "User is already inactive.";
    String DATE_FORMAT_ERRROR = "Date format error.";
    String INVALID_CSV_FILE = "Please provide valid csv file.";
    String INVALID_OBJECT_TYPE = "Invalid Object Type.";
    String UNABLE_TO_PARSE_DATA = "Unable to parse the data";
    String EMPTY_CSV_FILE = "CSV file is Empty.";
    String NO_DATA = "You have uploaded an empty file. Fill mandatory details and upload the file.";
    String INVALID_CHANNEL = "Channel value is invalid.";
    String EMAIL_SUBJECT_ERROR = "Email Subject is mandatory.";
    String EMAIL_BODY_ERROR = "Email Body is mandatory.";
    String STORAGE_CONTAINER_NAME_MANDATORY = " Container name can not be null or empty.";
    String INVALID_ROLE = "Invalid role value provided in request.";
    String INVALID_SALT = "Please provide salt value.";
    String TITLE_REQUIRED = "Title is required";
    String NOTE_REQUIRED = "No data to store for notes";
    String CONTENT_ID_ERROR = "Please provide content id or course id";
    String INVALID_TAGS = "Invalid data for tags";
    String NOTE_ID_INVALID = "Invalid note id";
    String USER_DATA_ENCRYPTION_ERROR = "Exception Occurred while encrypting user data.";
    String INVALID_PHONE_NO_FORMAT = "Please provide a valid phone number.";
    String INVALID_ORG_TYPE_ERROR = "INVALID_ORG_TYPE_ERROR";
    // No need to indicate managedby is missing to user.
    String EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING = "Please provide either email or phone.";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED =
        "Please provide only email or phone or managed by";
    String PHONE_ALREADY_IN_USE = "Phone already in use. Please provide different phone number.";
    String UPDATE_FAILED = "Data updation failed due to invalid Request";
    String INVALID_LOCATION_ID = "Please provide valid location id.";
    String INVALID_HASHTAG_ID =
        "Please provide different hashTagId.This HashTagId is associated with some other organization.";
    String INVALID_USR_ORG_DATA =
        "Given User Data doesn't belongs to this organization. Please provide a valid one.";
    String INVALID_NOTIFICATION_TYPE = "Please provide a valid notification type.";
    String INVALID_NOTIFICATION_TYPE_SUPPORT = "Only notification type FCM is supported.";
    String INVALID_PHONE_NUMBER = "Please send Phone and country code seprately.";
    String INVALID_COUNTRY_CODE = "Please provide a valid country code.";
    String ERROR_DUPLICATE_ENTRIES = "System contains duplicate entry for {0}.";
    String LOCATION_ID_REQUIRED = "Please provide Location Id.";
    String RESOURCE_NOT_FOUND = "Requested resource not found";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "Max allowed size is {0}";
    String INACTIVE_USER = "User is Inactive. Please make it active to proceed.";
    String ORG_NOT_EXIST = "Requested organisation does not exist.";
    String ALREADY_EXISTS = "A {0} with {1} already exists. Please retry with a unique value.";
    String INVALID_VALUE = "Invalid {0}: {1}. Valid values are: {2}.";
    String INVALID_PARAMETER = "Please provide valid {0}.";
    String INVALID_LOCATION_DELETE_REQUEST =
        "One or more locations have a parent reference to given location and hence cannot be deleted.";
    String LOCATION_TYPE_CONFLICTS = "Location type conflicts with its parent location type.";
    String MANDATORY_PARAMETER_MISSING = "Mandatory parameter {0} is missing.";
    String ERROR_MANDATORY_PARAMETER_EMPTY = "Mandatory parameter {0} is empty.";
    String ERROR_NO_FRAMEWORK_FOUND = "No framework found.";
    String UPDATE_NOT_ALLOWED = "Update of {0} is not allowed.";
    String INVALID_PARAMETER_VALUE =
        "Invalid value {0} for parameter {1}. Please provide a valid value.";
    String PARENT_NOT_ALLOWED = "For top level location, {0} is not allowed.";
    String MISSING_FILE_ATTACHMENT = "Missing file attachment.";
    String FILE_ATTACHMENT_SIZE_NOT_CONFIGURED = "File attachment max size is not configured.";
    String EMPTY_FILE = "Attached file is empty.";
    String INVALID_COLUMNS = "Invalid column: {0}. Valid columns are: {1}.";
    String CONFLICTING_ORG_LOCATIONS =
        "An organisation cannot be associated to two conflicting locations ({0}, {1}) at {2} level. ";
    String UNABLE_TO_COMMUNICATE_WITH_ACTOR = "Unable to communicate with actor.";
    String EMPTY_HEADER_LINE = "Missing header line in CSV file.";
    String INVALID_REQUEST_PARAMETER = "Invalid parameter {0} in request.";
    String ROOT_ORG_ASSOCIATION_ERROR =
        "No root organisation found which is associated with given {0}.";
    String OR_FORMAT = "{0} or {1}";
    String AND_FORMAT = "{0} and {1}";
    String DEPENDENT_PARAMETER_MISSING = "Missing parameter {0} which is dependent on {1}.";
    String DEPENDENT_PARAMS_MISSING = "Missing parameter value in {0}.";
    String EXTERNALID_NOT_FOUND =
        "External ID (id: {0}, idType: {1}, provider: {2}) not found for given user.";
    String EXTERNAL_ID_FORMAT = "externalId (id: {0}, idType: {1}, provider: {2})";
    String EXTERNALID_ASSIGNED_TO_OTHER_USER =
        "External ID (id: {0}, idType: {1}, provider: {2}) already assigned to another user.";
    String DUPLICATE_EXTERNAL_IDS =
        "Duplicate external IDs for given idType ({0}) and provider ({1}).";
    String INVALID_DUPLICATE_VALUE = "Values for {0} and {1} cannot be same.";
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT =
        "Email notification is not sent as the number of recipients exceeded configured limit ({0}).";
    String NO_EMAIL_RECIPIENTS =
        "Email notification is not sent as the number of recipients is zero.";
    String PARAMETER_MISMATCH = "Mismatch of given parameters: {0}.";
    String FORBIDDEN = "You are forbidden from accessing specified resource.";
    String ERROR_CONFIG_LOAD_EMPTY_STRING =
        "Loading {0} configuration failed as empty string is passed as parameter.";
    String ERROR_CONFIG_LOAD_PARSE_STRING =
        "Loading {0} configuration failed due to parsing error.";
    String MISSING_SELF_DECLARED_MANDATORY_PARAMETERS =
        "Mandatory parameter {0} or {1} is missing.";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "Loading {0} configuration failed.";
    String ERROR_CONFLICTING_FIELD_CONFIGURATION =
        "Field {0} in {1} configuration is conflicting in {2} and {3}.";
    String ERROR_SYSTEM_SETTING_NOT_FOUND = "System Setting not found for id: {0}";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "Not able to associate with root org";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "Unsupported cloud storage type {0}.";
    String ERROR_UNSUPPORTED_FIELD = "Unsupported field {0}.";
    String ERROR_CSV_NO_DATA_ROWS = "No data rows in CSV.";
    String ERROR_INACTIVE_ORG = "Organisation corresponding to given {0} ({1}) is inactive.";
    String ERROR_CONFLICTING_VALUES = "Conflicting values for {0} ({1}) and {2} ({3}).";
    String ERROR_CONFLICTING_ROOT_ORG_ID =
        "Root organisation ID of API user is conflicting with that of specified organisation ID.";
    String ERROR_UPDATE_SETTING_NOT_ALLOWED = "Update of system setting {0} is not allowed.";
    String CSV_ROWS_EXCEEDS = "Number of rows in csv file is more than ";
    String BLANK_CSV_DATA =
        "Did not find any Table of Contents data. Please check and upload again.";
    String ERR_FILE_NOT_FOUND = "File not found. Please select valid file and upload.";
    String ERROR_INVALID_PARAMETER_SIZE =
        "Parameter {0} is of invalid size (expected: {1}, actual: {2}).";
    String ERROR_RATE_LIMIT_EXCEEDED =
        "Your per {0} rate limit has exceeded. You can retry after some time.";
    String INVALID_REQUEST_TIMEOUT = "Invalid request timeout value {0}.";
    String ERROR_USER_UPDATE_PASSWORD = "User is created but password couldn't be updated.";
    String ERROR_USER_MIGRATION_FAILED = "User migration failed.";
    String IDENTIFIER_VALIDATION_FAILED =
        "Valid identifier is not present in List, Valid supported identifiers are ";
    String FROM_ACCOUNT_ID_MISSING = "From Account id is mandatory.";
    String TO_ACCOUNT_ID_MISSING = "To Account id is mandatory.";
    String PARAM_NOT_MATCH = "%s-NOT-MATCH";
    String MANDATORY_HEADER_PARAMETER_MISSING = "Mandatory header parameter {0} is missing.";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "{0} could not be same as {1}";
    String ACCOUNT_NOT_FOUND = "Account not found.";
    String INVALID_EXT_USER_ID = "provided ext user id {0} is incorrect";
    String USER_MIGRATION_FAILED = "user is failed to migrate";
    String INVALID_ELEMENT_IN_LIST =
        "Invalid value supplied for parameter {0}.Supported values are {1}";
    String INVALID_PASSWORD =
        "Password must contain a minimum of 8 characters including numerals, lower and upper case alphabets and special characters";
    String OTP_VERIFICATION_FAILED = "OTP verification failed. Remaining attempt count is {0}.";
    String SERVICE_UNAVAILABLE = "SERVICE UNAVAILABLE";
    String MANAGED_BY_NOT_ALLOWED = "managedBy cannot be updated.";
    String MANAGED_USER_LIMIT_EXCEEDED = "Managed user creation limit exceeded";
    String UNABLE_TO_CONNECT_TO_ADMINUTIL = "Unable to connect to admin util service";
    String DATA_ENCRYPTION_ERROR = "Error in encrypting the data";
    String NO_EMAIL_PHONE_ASSOCIATED = "No {0} associated with the given user.";
    String INVALID_CAPTCHA = "Captcha is invalid";
    String PREFERENCE_ALREADY_EXIST = "preference {0} already exits in the org {1}";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "Declared user error status is not updated";
    String PREFERENCE_NOT_FOUND = "preference {0} not found in the org {1}";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED =
        "Declared user validated status is not updated";
    String USER_CONSENT_NOT_FOUND = "User consent not found.";
    String INVALID_USER_INFO_VALUE = "Null value is not allowed";
    String INVALID_CONSENT_STATUS = "Consent status is invalid";
    String INVALID_LOCATION_TYPE = "Invalid location type {0} is supplied.Supported values are {1}";
    String USER_TYPE_CONFIG_IS_EMPTY = "userType config is empty for the statecode {0}";
    String ROLE_SAVE_ERROR = "Error while saving role";
  }

  interface Key {
    String UNAUTHORIZED_USER = "UNAUTHORIZED_USER";
    String OPERATION_TIMEOUT = "PROCESS_EXE_TIMEOUT";
    String INVALID_OPERATION_NAME = "INVALID_OPERATION_NAME";
    String INVALID_REQUESTED_DATA = "INVALID_REQUESTED_DATA";
    String COURSE_ID_MISSING_ERROR = "COURSE_ID_REQUIRED_ERROR";
    String API_KEY_MISSING_ERROR = "API_KEY_REQUIRED_ERROR";
    String INTERNAL_ERROR = "INTERNAL_ERROR";
    String SUCCESS_MESSAGE = "SUCCESS";
    String EMAIL_FORMAT = "EMAIL_FORMAT_ERROR";
    String FIRST_NAME_MISSING = "FIRST_NAME_REQUIRED_ERROR";
    String REQUIRED_DATA_ORG_MISSING = "REQUIRED_DATA_MISSING";
    String CHANNEL_SHOULD_BE_UNIQUE = "CHANNEL_SHOULD_BE_UNIQUE";
    String ERROR_DUPLICATE_ENTRY = "ERROR_DUPLICATE_ENTRY";
    String INVALID_ORG_DATA = "INVALID_ORGANIZATION_DATA";
    String INVALID_USR_DATA = "INVALID_USER_DATA";
    String USR_DATA_VALIDATION_ERROR = "USER_DATA_VALIDATION_ERROR";
    String INVALID_ROOT_ORGANIZATION = "INVALID ROOT ORGANIZATION";
    String EMAIL_IN_USE = "EMAIL_IN_USE";
    String USERNAME_EMAIL_IN_USE = "USERNAME_EMAIL_IN_USE";
    String KEY_CLOAK_DEFAULT_ERROR = "KEY_CLOAK_DEFAULT_ERROR";
    String USERNAME_MISSING = "USERNAME_MISSING";
    String USERNAME_IN_USE = "USERNAME_IN_USE";
    String USERID_MISSING = "USERID_MISSING";
    String AUTH_TOKEN_MISSING = "X_Authenticated_Userid_MISSING";
    String INVALID_ORG_ID = "INVALID_ORG_ID";
    String INVALID_ORG_STATUS = "INVALID_ORG_STATUS";
    String INVALID_ORG_STATUS_TRANSITION = "INVALID_ORG_STATUS_TRANSITION";
    String DB_INSERTION_FAIL = "DB_INSERTION_FAIL";
    String DB_UPDATE_FAIL = "DB_UPDATE_FAIL";
    String PHONE_NO_REQUIRED_ERROR = "PHONE_NO_REQUIRED_ERROR";
    String INVALID_DATA = "INVALID_DATA";
    String ORG_ID_MISSING = "ORG_ID_MISSING";
    String ACTOR_CONNECTION_ERROR = "ACTOR_CONNECTION_ERROR";
    String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    String INVALID_USER_ID = "INVALID_USER_ID";
    String LOGIN_ID_MISSING = "LOGIN_ID_MISSING";
    String USER_NOT_FOUND = "USER_NOT_FOUND";
    String ID_REQUIRED_ERROR = "ID_REQUIRED_ERROR";
    String DATA_TYPE_ERROR = "DATA_TYPE_ERROR";
    String ERROR_ATTRIBUTE_CONFLICT = "ERROR_ATTRIBUTE_CONFLICT";
    String NAME_OF_ORGANISATION_ERROR = "NAME_OF_ORGANIZATION_ERROR";
    String ROLES_MISSING = "ROLES_REQUIRED_ERROR";
    String EMPTY_ROLES_PROVIDED = "EMPTY_ROLES_PROVIDED";
    String CONTENT_TYPE_ERROR = "CONTENT_TYPE_ERROR";
    String INVALID_PROPERTY_ERROR = "INVALID_PROPERTY_ERROR";
    String USER_ACCOUNT_BLOCKED = "USER_ACCOUNT_BLOCKED";
    String DATA_SIZE_EXCEEDED = "DATA_SIZE_EXCEEDED";
    String USER_ALREADY_ACTIVE = "USER_ALREADY_ACTIVE";
    String USER_ALREADY_INACTIVE = "USER_ALREADY_INACTIVE";
    String DATE_FORMAT_ERRROR = "DATE_FORMAT_ERRROR";
    String INVALID_CSV_FILE = "INVALID_CSV_FILE";
    String INVALID_OBJECT_TYPE = "INVALID_OBJECT_TYPE";
    String UNABLE_TO_PARSE_DATA = "UNABLE_TO_PARSE_DATA";
    String EMPTY_CSV_FILE = "EMPTY_CSV_FILE";
    String NO_DATA = "NO_DATA";
    String INVALID_CHANNEL = "INVALID_CHANNEL";
    String EMAIL_SUBJECT_ERROR = "EMAIL_SUBJECT_ERROR";
    String EMAIL_BODY_ERROR = "EMAIL_BODY_ERROR";
    String STORAGE_CONTAINER_NAME_MANDATORY = "STORAGE_CONTAINER_NAME_MANDATORY";
    String INVALID_ROLE = "INVALID_ROLE";
    String INVALID_SALT = "INVALID_SALT";
    String TITLE_REQUIRED = "TITLE_REQUIRED";
    String NOTE_REQUIRED = "NOTE_REQUIRED";
    String CONTENT_ID_ERROR = "CONTENT_ID_OR_COURSE_ID_REQUIRED";
    String INVALID_TAGS = "INVALID_TAGS";
    String NOTE_ID_INVALID = "NOTE_ID_INVALID";
    String USER_DATA_ENCRYPTION_ERROR = "USER_DATA_ENCRYPTION_ERROR";
    String INVALID_PHONE_NO_FORMAT = "INVALID_PHONE_NO_FORMAT";
    String INVALID_ORG_TYPE_ERROR = "Please provide valid orgType.";
    String EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING = "EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED = "ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED";
    String PHONE_ALREADY_IN_USE = "PHONE_ALREADY_IN_USE";
    String UPDATE_FAILED = "UPDATE_FAILED";
    String INVALID_LOCATION_ID = "INVALID_LOCATION_ID";
    String INVALID_HASHTAG_ID = "INVALID_HASHTAG_ID";
    String INVALID_USR_ORG_DATA = "INVALID_USR_ORG_DATA";
    String INVALID_NOTIFICATION_TYPE = "INVALID_NOTIFICATION_TYPE";
    String INVALID_NOTIFICATION_TYPE_SUPPORT = "INVALID_NOTIFICATION_TYPE_SUPPORT";
    String INVALID_PHONE_NUMBER = "INVALID_PHONE_NUMBER";
    String INVALID_COUNTRY_CODE = "INVALID_COUNTRY_CODE";
    String LOCATION_ID_REQUIRED = "LOCATION_ID_REQUIRED";
    String CHANNEL_REG_FAILED = "CHANNEL_REG_FAILED";
    String SLUG_IS_NOT_UNIQUE = "SLUG_IS_NOT_UNIQUE";
    String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "MAX_ALLOWED_SIZE_LIMIT_EXCEED";
    String INACTIVE_USER = "INACTIVE_USER";
    String ORG_NOT_EXIST = "ORG_NOT_EXIST";
    String ALREADY_EXISTS = "ALREADY_EXISTS";
    String INVALID_VALUE = "INVALID_VALUE";
    String INVALID_PARAMETER = "INVALID_PARAMETER";
    String INVALID_LOCATION_DELETE_REQUEST = "INVALID_LOCATION_DELETE_REQUEST";
    String LOCATION_TYPE_CONFLICTS = "LOCATION_TYPE_CONFLICTS";
    String MANDATORY_PARAMETER_MISSING = "MANDATORY_PARAMETER_MISSING";
    String ERROR_MANDATORY_PARAMETER_EMPTY = "ERROR_MANDATORY_PARAMETER_EMPTY";
    String ERROR_NO_FRAMEWORK_FOUND = "ERROR_NO_FRAMEWORK_FOUND";
    String UPDATE_NOT_ALLOWED = "UPDATE_NOT_ALLOWED";
    String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";
    String PARENT_NOT_ALLOWED = "PARENT_NOT_ALLOWED";
    String MISSING_FILE_ATTACHMENT = "MISSING_FILE_ATTACHMENT";
    String FILE_ATTACHMENT_SIZE_NOT_CONFIGURED = "ATTACHMENT_SIZE_NOT_CONFIGURED";
    String EMPTY_FILE = "EMPTY_FILE";
    String INVALID_COLUMNS = "INVALID_COLUMNS";
    String CONFLICTING_ORG_LOCATIONS = "CONFLICTING_ORG_LOCATIONS";
    String UNABLE_TO_COMMUNICATE_WITH_ACTOR = "UNABLE_TO_COMMUNICATE_WITH_ACTOR";
    String EMPTY_HEADER_LINE = "EMPTY_HEADER_LINE";
    String INVALID_REQUEST_PARAMETER = "INVALID_REQUEST_PARAMETER";
    String ROOT_ORG_ASSOCIATION_ERROR = "ROOT_ORG_ASSOCIATION_ERROR";
    String DEPENDENT_PARAMETER_MISSING = "DEPENDENT_PARAMETER_MISSING";
    String EXTERNALID_NOT_FOUND = "EXTERNALID_NOT_FOUND";
    String EXTERNALID_ASSIGNED_TO_OTHER_USER = "EXTERNALID_ASSIGNED_TO_OTHER_USER";
    String DUPLICATE_EXTERNAL_IDS = "DUPLICATE_EXTERNAL_IDS";
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT = "EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT";
    String NO_EMAIL_RECIPIENTS = "NO_EMAIL_RECIPIENTS";
    String PARAMETER_MISMATCH = "PARAMETER_MISMATCH";
    String FORBIDDEN = "FORBIDDEN";
    String ERROR_CONFIG_LOAD_EMPTY_STRING = "ERROR_CONFIG_LOAD_EMPTY_STRING";
    String ERROR_CONFIG_LOAD_PARSE_STRING = "ERROR_CONFIG_LOAD_PARSE_STRING";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "ERROR_CONFIG_LOAD_EMPTY_CONFIG";
    String ERROR_CONFLICTING_FIELD_CONFIGURATION = "ERROR_CONFLICTING_FIELD_CONFIGURATION";
    String ERROR_SYSTEM_SETTING_NOT_FOUND = "ERROR_SYSTEM_SETTING_NOT_FOUND";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "ERROR_NO_ROOT_ORG_ASSOCIATED";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "ERROR_ UNSUPPORTED_CLOUD_STORAGE";
    String ERROR_UNSUPPORTED_FIELD = "ERROR_UNSUPPORTED_FIELD";
    String ERROR_CSV_NO_DATA_ROWS = "ERROR_CSV_NO_DATA_ROWS";
    String ERROR_INACTIVE_ORG = "ERROR_INACTIVE_ORG";
    String ERROR_DUPLICATE_ENTRIES = "ERROR_DUPLICATE_ENTRIES";
    String ERROR_CONFLICTING_VALUES = "ERROR_CONFLICTING_VALUES";
    String ERROR_CONFLICTING_ROOT_ORG_ID = "ERROR_CONFLICTING_ROOT_ORG_ID";
    String ERROR_UPDATE_SETTING_NOT_ALLOWED = "ERROR_UPDATE_SETTING_NOT_ALLOWED";
    String CSV_ROWS_EXCEEDS = "CSV_ROWS_EXCEEDS";
    String ERROR_INVALID_OTP = "ERROR_INVALID_OTP";
    String BLANK_CSV_DATA = "BLANK_CSV_DATA";
    String ERR_FILE_NOT_FOUND = "ERR_FILE_NOT_FOUND";
    String ERROR_INVALID_PARAMETER_SIZE = "ERROR_INVALID_PARAMETER_SIZE";
    String ERROR_RATE_LIMIT_EXCEEDED = "ERROR_RATE_LIMIT_EXCEEDED";
    String INVALID_REQUEST_TIMEOUT = "INVALID_REQUEST_TIMEOUT";
    String ERROR_USER_MIGRATION_FAILED = "ERROR_USER_MIGRATION_FAILED";
    String VALID_IDENTIFIER_ABSENSE = "IDENTIFIER IN LIST IS NOT SUPPORTED OR INCORRECT";
    String FROM_ACCOUNT_ID_MISSING = "FROM_ACCOUNT_ID_MISSING";
    String TO_ACCOUNT_ID_MISSING = "TO_ACCOUNT_ID_MISSING";
    String PARAM_NOT_MATCH = "%s-NOT-MATCH";
    String MANDATORY_HEADER_PARAMETER_MISSING = "MANDATORY_HEADER_PARAMETER_MISSING";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "RECOVERY_PARAM_MATCH_EXCEPTION";
    String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    String INVALID_EXT_USER_ID = "INVALID_EXT_USER_ID";
    String USER_MIGRATION_FAILED = "USER_MIGRATION_FAILED";
    String INVALID_ELEMENT_IN_LIST = "INVALID_ELEMENT_IN_LIST";
    String INVALID_PASSWORD = "INVALID_PASSWORD";
    String OTP_VERIFICATION_FAILED = "OTP_VERIFICATION_FAILED";
    String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    String MANAGED_BY_NOT_ALLOWED = "MANAGED_BY_NOT_ALLOWED";
    String MANAGED_USER_LIMIT_EXCEEDED = "MANAGED_USER_LIMIT_EXCEEDED";
    String UNABLE_TO_CONNECT_TO_ADMINUTIL = "UNABLE_TO_CONNECT_TO_ADMINUTIL";
    String DATA_ENCRYPTION_ERROR = "DATA_ENCRYPTION_ERROR";
    String NO_EMAIL_PHONE_ASSOCIATED = "NO_EMAIL_PHONE_ASSOCIATED";
    String INVALID_CAPTCHA = "INVALID_CAPTCHA";
    String PREFERENCE_ALREADY_EXIST = "PREFERENCE_ALREADY_EXIST";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED";
    String PREFERENCE_NOT_FOUND = "PREFERENCE_NOT_FOUND";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED =
        "DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED";
    String USER_CONSENT_NOT_FOUND = "USER_CONSENT_NOT_FOUND";
    String INVALID_USER_INFO_VALUE = "INVALID_USER_INFO_VALUE";
    String INVALID_CONSENT_STATUS = "INVALID_CONSENT_STATUS";
    String ROLE_SAVE_ERROR = "ROLE_SAVE_ERROR";
  }
}
