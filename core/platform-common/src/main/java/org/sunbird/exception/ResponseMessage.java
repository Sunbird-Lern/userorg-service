package org.sunbird.exception;

/**
 * This interface will hold all the response key and message
 *
 * @author Manzarul
 */
public interface ResponseMessage {

  interface Message {

    String UNAUTHORIZED_USER = "You are not authorized.";
    // String OPERATION_TIMEOUT = "Request processing taking too long time. Please try again
    // later.";
    String INVALID_OPERATION_NAME = //
        "Operation name is invalid. Please provide a valid operation name";
    String INVALID_REQUESTED_DATA = "Requested data for this operation is not valid.";
    // String API_KEY_MISSING_ERROR = "APi key is mandatory.";
    // String INTERNAL_ERROR = "Process failed,please try again later.";
    String SUCCESS_MESSAGE = "Success";
    String EMAIL_FORMAT = "Email is invalid.";
    // String FIRST_NAME_MISSING = "First name is mandatory.";
    // String CHANNEL_SHOULD_BE_UNIQUE =
    //    "Channel value already used by another organization. Provide different value for channel";
    String ERROR_DUPLICATE_ENTRY = "Value {0} for {1} is already in use.";
    String ERROR_PARAM_EXISTS = "{0} already exists";
    // String INVALID_ORG_DATA =
    //    "Given Organization Data doesn't exist in our records. Please provide a valid one";
    // String INVALID_USR_DATA =
    //    "Given User Data doesn't exist in our records. Please provide a valid one";
    // String INVALID_ROOT_ORGANIZATION = "Root organization id is invalid";
    String ERROR_INVALID_OTP = "Invalid OTP.";
    // String EMAIL_IN_USE = "Email already exists.";
    // String USERNAME_EMAIL_IN_USE =
    //    "Username or Email is already in use. Please try with a different Username or Email.";
    // String KEY_CLOAK_DEFAULT_ERROR = "server error at sso.";
    // String USERNAME_MISSING = "Username is mandatory.";
    // String USERNAME_IN_USE = "Username already exists.";
    // String USERID_MISSING = "UserId is mandatory.";
    // String AUTH_TOKEN_MISSING = "Auth token is mandatory.";
    // String DB_INSERTION_FAIL = "DB insert operation failed.";
    // String DB_UPDATE_FAIL = "Db update operation failed.";
    // String INVALID_DATA = "Incorrect data.";
    // String ACTOR_CONNECTION_ERROR = "Service is not able to connect with actor.";
    // String USER_ALREADY_EXISTS = "User already exists for given {0}.";
    // String INVALID_USER_ID = "User Id does not exists in our records";
    // String LOGIN_ID_MISSING = "loginId is required.";
    // String USER_NOT_FOUND = "user not found.";
    String DATA_TYPE_ERROR = "Data type of {0} should be {1}.";
    String ERROR_ATTRIBUTE_CONFLICT = "Either pass attribute {0} or {1} but not both.";
    // String ROLES_MISSING = "user role is required.";
    // String PROFILE_USER_TYPES_MISSING = "User type is required.";
    // String EMPTY_ROLES_PROVIDED = "Roles cannot be empty.";
    String CHANNEL_REG_FAILED = "Channel Registration failed.";
    // String SLUG_IS_NOT_UNIQUE =
    //    "Please provide different channel value. This channel value already exist.";
    // String CONTENT_TYPE_ERROR = "Please add Content-Type header with value application/json";
    String INVALID_PROPERTY_ERROR = "Invalid property {0}.";
    String USER_ACCOUNT_BLOCKED = "User account has been blocked .";
    String DATA_SIZE_EXCEEDED = "Maximum upload data size should be {0}";
    String USER_ALREADY_ACTIVE = "User is already active.";
    String USER_ALREADY_INACTIVE = "User is already inactive.";
    String DATA_FORMAT_ERROR = "Invalid format for given {0}.";
    String INVALID_CSV_FILE = "Please provide valid csv file.";
    String INVALID_OBJECT_TYPE = "Invalid Object Type.";
    // String UNABLE_TO_PARSE_DATA = "Unable to parse the data";
    String EMPTY_CSV_FILE = "CSV file is Empty.";
    // String INVALID_CHANNEL = "Channel value is invalid.";
    // String EMAIL_SUBJECT_ERROR = "Email Subject is mandatory.";
    // String EMAIL_BODY_ERROR = "Email Body is mandatory.";
    // String STORAGE_CONTAINER_NAME_MANDATORY = " Container name can not be null or empty.";
    // String INVALID_ROLE = "Invalid role value provided in request.";
    // String INVALID_SALT = "Please provide salt value.";
    // String TITLE_REQUIRED = "Title is required";
    // String NOTE_REQUIRED = "No data to store for notes";
    // String CONTENT_ID_ERROR = "Please provide content id or course id";
    // String INVALID_TAGS = "Invalid data for tags";
    // String NOTE_ID_INVALID = "Invalid note id";
    // String USER_DATA_ENCRYPTION_ERROR = "Exception Occurred while encrypting user data.";
    // String INVALID_PHONE_NO_FORMAT = "Please provide a valid phone number.";
    // String INVALID_ORG_TYPE_ERROR = "INVALID_ORG_TYPE_ERROR";
    // No need to indicate managedby is missing to user.
    // String EMAIL_OR_PHONE_OR_MANAGEDBY_MISSING = "Please provide either email or phone.";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED =
        "Please provide only email or phone or managed by";
    // String PHONE_ALREADY_IN_USE = "Phone already in use. Please provide different phone number.";
    // String INVALID_PHONE_NUMBER = "Please send Phone and country code seprately.";
    // String INVALID_COUNTRY_CODE = "Please provide a valid country code.";
    String ERROR_DUPLICATE_ENTRIES = "System contains duplicate entry for {0}.";
    // String LOCATION_ID_REQUIRED = "Please provide Location Id.";
    String RESOURCE_NOT_FOUND = "Requested {0} resource not found";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "Max allowed size is {0}";
    String INACTIVE_USER = "User is Inactive. Please make it active to proceed.";
    // String ORG_NOT_EXIST = "Requested organisation does not exist.";
    // String ALREADY_EXISTS = "A {0} with {1} already exists. Please retry with a unique value.";
    String INVALID_VALUE = "Invalid {0}: {1}. Valid values are: {2}.";
    String INVALID_PARAMETER = "Please provide valid {0}.";
    String INVALID_LOCATION_DELETE_REQUEST =
        "One or more locations have a parent reference to given location and hence cannot be deleted.";
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
    // String UNABLE_TO_COMMUNICATE_WITH_ACTOR = "Unable to communicate with actor.";
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
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT =
        "Email notification is not sent as the number of recipients exceeded configured limit ({0}).";
    String PARAMETER_MISMATCH = "Mismatch of given parameters: {0}.";
    String FORBIDDEN = "You are forbidden from accessing specified resource.";
    String ERROR_CONFIG_LOAD_EMPTY_STRING =
        "Loading {0} configuration failed as empty string is passed as parameter.";
    String ERROR_CONFIG_LOAD_PARSE_STRING =
        "Loading {0} configuration failed due to parsing error.";
    String MISSING_SELF_DECLARED_MANDATORY_PARAMETERS =
        "Mandatory parameter {0} or {1} is missing.";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "Loading {0} configuration failed.";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "Not able to associate with root org";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "Unsupported cloud storage type {0}.";
    String ERROR_UNSUPPORTED_FIELD = "Unsupported field {0}.";
    String ERROR_CSV_NO_DATA_ROWS = "No data rows in CSV.";
    String ERROR_INACTIVE_ORG = "Organisation corresponding to given {0} ({1}) is inactive.";
    String ERROR_CONFLICTING_VALUES = "Conflicting values for {0} ({1}) and {2} ({3}).";
    String ERROR_CONFLICTING_ROOT_ORG_ID =
        "Root organisation ID of API user is conflicting with that of specified organisation ID.";
    String ERROR_INVALID_PARAMETER_SIZE =
        "Parameter {0} is of invalid size (expected: {1}, actual: {2}).";
    String ERROR_RATE_LIMIT_EXCEEDED =
        "Your per {0} rate limit has exceeded. You can retry after some time.";
    String INVALID_REQUEST_TIMEOUT = "Invalid request timeout value {0}.";
    String ERROR_USER_UPDATE_PASSWORD = "User is created but password couldn't be updated.";
    String ERROR_USER_MIGRATION_FAILED = "User migration failed.";
    String IDENTIFIER_VALIDATION_FAILED =
        "Valid identifier is not present in List, Valid supported identifiers are ";
    // String FROM_ACCOUNT_ID_MISSING = "From Account id is mandatory.";
    // String TO_ACCOUNT_ID_MISSING = "To Account id is mandatory.";
    String PARAM_NOT_MATCH = "%s-NOT-MATCH";
    String MANDATORY_HEADER_PARAMETER_MISSING = "Mandatory header parameter {0} is missing.";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "{0} could not be same as {1}";
    String ACCOUNT_NOT_FOUND = "Account not found.";
    // String INVALID_ELEMENT_IN_LIST =
    //   "Invalid value supplied for parameter {0}.Supported values are {1}";
    String INVALID_PASSWORD =
        "Password must contain a minimum of 8 characters including numerals, lower and upper case alphabets and special characters";
    String OTP_VERIFICATION_FAILED = "OTP verification failed. Remaining attempt count is {0}.";
    String SERVICE_UNAVAILABLE = "SERVICE UNAVAILABLE";
    String MANAGED_BY_NOT_ALLOWED = "managedBy cannot be updated.";
    String MANAGED_USER_LIMIT_EXCEEDED = "Managed user creation limit exceeded";
    // String UNABLE_TO_CONNECT_TO_ADMINUTIL = "Unable to connect to admin util service";
    // String DATA_ENCRYPTION_ERROR = "Error in encrypting the data";
    String INVALID_CAPTCHA = "Captcha is invalid";
    String PREFERENCE_ALREADY_EXIST = "preference {0} already exits in the org {1}";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "Declared user error status is not updated";
    String PREFERENCE_NOT_FOUND = "preference {0} not found in the org {1}";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED =
        "Declared user validated status is not updated";
    // String USER_CONSENT_NOT_FOUND = "User consent not found.";
    // String INVALID_USER_INFO_VALUE = "Null value is not allowed";
    String INVALID_CONSENT_STATUS = "Consent status is invalid";
    String USER_TYPE_CONFIG_IS_EMPTY = "userType config is empty for the statecode {0}";
    String SERVER_ERROR = "server error";
  }

  interface Key {
    String UNAUTHORIZED_USER = "0073";
  
    String INVALID_OPERATION_NAME = "0027";
    String INVALID_REQUESTED_DATA = "0028";
  
    String SUCCESS_MESSAGE = "0001";
    String EMAIL_FORMAT = "0016";
    
    String ERROR_DUPLICATE_ENTRY = "0004";
    String ERROR_PARAM_EXISTS = "0002";
  
    String DATA_TYPE_ERROR = "0003";
    String ERROR_ATTRIBUTE_CONFLICT = "0005";
  
    String INVALID_PROPERTY_ERROR = "0029";
    String USER_ACCOUNT_BLOCKED = "0006";
    String DATA_SIZE_EXCEEDED = "0007";
    String USER_ALREADY_ACTIVE = "0008";
    String USER_ALREADY_INACTIVE = "0009";
    String DATA_FORMAT_ERROR = "0010";
    String INVALID_CSV_FILE = "0030";
    String INVALID_OBJECT_TYPE = "0031";
  
    String EMPTY_CSV_FILE = "0011";
  
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED = "0012";
  
    String CHANNEL_REG_FAILED = "0013";
  
    String RESOURCE_NOT_FOUND = "0014";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "0015";
    String INACTIVE_USER = "0032";
  
    String INVALID_VALUE = "0018";
    String INVALID_PARAMETER = "0019";
    String INVALID_LOCATION_DELETE_REQUEST = "0029";
    String MANDATORY_PARAMETER_MISSING = "0030";
    String ERROR_MANDATORY_PARAMETER_EMPTY = "0031";
    String ERROR_NO_FRAMEWORK_FOUND = "0032";
    String UPDATE_NOT_ALLOWED = "0033";
    String INVALID_PARAMETER_VALUE = "0017";
    String PARENT_NOT_ALLOWED = "0034";
    String MISSING_FILE_ATTACHMENT = "0035";
    String FILE_ATTACHMENT_SIZE_NOT_CONFIGURED = "0036";
    String EMPTY_FILE = "0037";
    String INVALID_COLUMNS = "0020";
    String CONFLICTING_ORG_LOCATIONS = "0038";
  
    String EMPTY_HEADER_LINE = "0039";
    String INVALID_REQUEST_PARAMETER = "0021";
    String ROOT_ORG_ASSOCIATION_ERROR = "0040";
    String DEPENDENT_PARAMETER_MISSING = "0041";
    String EXTERNALID_NOT_FOUND = "0042";
    String EXTERNALID_ASSIGNED_TO_OTHER_USER = "0043";
    String DUPLICATE_EXTERNAL_IDS = "0044";
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT = "0045";
    String PARAMETER_MISMATCH = "0046";
    String FORBIDDEN = "0074";
    String ERROR_CONFIG_LOAD_EMPTY_STRING = "0047";
    String ERROR_CONFIG_LOAD_PARSE_STRING = "0048";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "0049";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "0050";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "ERROR_ 0051";
    String ERROR_UNSUPPORTED_FIELD = "0052";
    String ERROR_CSV_NO_DATA_ROWS = "0053";
    String ERROR_INACTIVE_ORG = "0054";
    String ERROR_DUPLICATE_ENTRIES = "0055";
    String ERROR_CONFLICTING_VALUES = "0056";
    String ERROR_CONFLICTING_ROOT_ORG_ID = "0057";
    String ERROR_INVALID_OTP = "0058";
    String ERROR_INVALID_PARAMETER_SIZE = "0059";
    String ERROR_RATE_LIMIT_EXCEEDED = "0060";
    String INVALID_REQUEST_TIMEOUT = "0022";
    String ERROR_USER_MIGRATION_FAILED = "0061";
    String VALID_IDENTIFIER_ABSENSE = "0023";
  
    String MANDATORY_HEADER_PARAMETER_MISSING = "0062";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "0063";
    String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
  
    String INVALID_PASSWORD = "0024";
    String OTP_VERIFICATION_FAILED = "0064";
    String SERVICE_UNAVAILABLE = "0065";
    String MANAGED_BY_NOT_ALLOWED = "0066";
    String MANAGED_USER_LIMIT_EXCEEDED = "0067";
  
    String INVALID_CAPTCHA = "0025";
    String PREFERENCE_ALREADY_EXIST = "0068";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "0069";
    String PREFERENCE_NOT_FOUND = "0070";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED =
      "0071";
  
    String INVALID_CONSENT_STATUS = "0026";
    String SERVER_ERROR = "0072";
  }
}
