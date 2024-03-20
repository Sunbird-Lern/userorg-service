package org.sunbird.exception;

import org.sunbird.keys.JsonKey;

/**
 * This interface will hold all the response key and message
 *
 * @author Manzarul
 */
public interface ResponseMessage {

  interface Message {

    String UNAUTHORIZED_USER = "You are not authorized.";
    String INVALID_OPERATION_NAME =
        "Operation name is invalid. Please provide a valid operation name";
    String INVALID_REQUESTED_DATA = "Requested data for this operation is not valid.";
    String SUCCESS_MESSAGE = "Success";
    String ERROR_DUPLICATE_ENTRY = "Value {0} for {1} is already in use.";
    String ERROR_PARAM_EXISTS = "{0} already exists";
    String ERROR_INVALID_OTP = "Invalid OTP.";
    String DATA_TYPE_ERROR = "Data type of {0} should be {1}.";
    String ERROR_ATTRIBUTE_CONFLICT = "Either pass attribute {0} or {1} but not both.";
    String CHANNEL_REG_FAILED = "Channel Registration failed.";
    String INVALID_PROPERTY_ERROR = "Invalid property {0}.";
    String USER_ACCOUNT_BLOCKED = "User account has been blocked .";
    String DATA_SIZE_EXCEEDED = "Maximum upload data size should be {0}";
    String USER_STATUS_MSG = "User is already {0}.";
    String DATA_FORMAT_ERROR = "Invalid format for given {0}.";
    String INVALID_CSV_FILE = "Please provide valid csv file.";
    String INVALID_OBJECT_TYPE = "Invalid Object Type.";
    String EMPTY_CSV_FILE = "CSV file is Empty.";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED =
        "Please provide only email or phone or managed by";
    String ERROR_DUPLICATE_ENTRIES = "System contains duplicate entry for {0}.";
    String RESOURCE_NOT_FOUND = "Requested {0} resource not found";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "Max allowed size is {0}";
    String INACTIVE_USER = "User is Inactive. Please make it active to proceed.";
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
    String EMPTY_HEADER_LINE = "Missing header line in CSV file.";
    String INVALID_REQUEST_PARAMETER = "Invalid parameter {0} in request.";
    String ROOT_ORG_ASSOCIATION_ERROR =
        "No root organisation found which is associated with given {0}.";
    String OR_FORMAT = "{0} or {1}";
    String AND_FORMAT = "{0} and {1}";
    String DEPENDENT_PARAMETER_MISSING = "Missing parameter {0} which is dependent on {1}.";
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
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "Loading {0} configuration failed.";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "Not able to associate with root org";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "Unsupported cloud storage type {0}.";
    String ERROR_UNSUPPORTED_FIELD = "Unsupported field {0}.";
    String ERROR_INACTIVE_ORG = "Organisation corresponding to given {0} ({1}) is inactive.";
    String ERROR_CONFLICTING_VALUES = "Conflicting values for {0} ({1}) and {2} ({3}).";
    String ERROR_CONFLICTING_ROOT_ORG_ID =
        "Root organisation channel of uploader user is conflicting with that of specified organisation ID/orgExternalId channel value.";
    String ERROR_INVALID_PARAMETER_SIZE =
        "Parameter {0} is of invalid size (expected: {1}, actual: {2}).";
    String ERROR_RATE_LIMIT_EXCEEDED =
        "Your per {0} rate limit has exceeded. You can retry after some time.";
    String INVALID_REQUEST_TIMEOUT = "Invalid request timeout value {0}.";
    String ERROR_USER_UPDATE_PASSWORD = "User is created but password couldn't be updated.";
    String ERROR_USER_MIGRATION_FAILED = "User migration failed.";
    String IDENTIFIER_VALIDATION_FAILED =
        "Valid identifier is not present in List, Valid supported identifiers are ";
    String PARAM_NOT_MATCH = "%s-NOT-MATCH";
    String MANDATORY_HEADER_PARAMETER_MISSING = "Mandatory header parameter {0} is missing.";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "{0} could not be same as {1}";
    String INVALID_PASSWORD =
        "Password must contain a minimum of 8 characters including numerals, lower and upper case alphabets and special characters";
    String OTP_VERIFICATION_FAILED = "OTP verification failed. Remaining attempt count is {0}.";
    String SERVICE_UNAVAILABLE = "SERVICE UNAVAILABLE";
    String MANAGED_BY_NOT_ALLOWED = "managedBy cannot be updated.";
    String MANAGED_USER_LIMIT_EXCEEDED = "Managed user creation limit exceeded";
    String INVALID_CAPTCHA = "Captcha is invalid";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "Declared user error status is not updated";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED =
        "Declared user validated status is not updated";
    String INVALID_CONSENT_STATUS = "Consent status is invalid";
    String USER_TYPE_CONFIG_IS_EMPTY = "userType config is empty for the statecode {0}";
    String SERVER_ERROR = "server error";
    String EXTENDED_USER_PROFILE_NOT_LOADED =
        "Failed to load extendedProfileSchemaConfig from System_Settings table";
    String ROLE_PROCESSING_INVALID_ORG =
        "Error while processing assign role. Invalid Organisation Id";
    String INVALID_FILE_EXTENSION = "Please provide a valid file. File expected of format: {0}";
    String INVALID_ENCRYPTION_FILE = "Please provide valid public key file.";
    String INVALID_SECURITY_LEVEL =
        "Invalid data security level {0} provided for job {1}. Please provide a valid data security level.";
    String INVALID_SECURITY_LEVEL_LOWER =
        "Invalid data security level {0} provided for job {1}. Cannot be set lower than the default security level: {2}";
    String MISSING_DEFAULT_SECURITY_LEVEL =
        "Default data security policy settings is missing for the job: {0}";
    String INVALID_TENANT_SECURITY_LEVEL_LOWER =
        "Tenant level's security {0} cannot be lower than system level's security {1}. Please provide a valid data security level.";
    String CANNOT_DELETE_USER = "User is restricted from deleting account based on roles!";
    String CANNOT_TRANSFER_OWNERSHIP = "User is restricted from transfering the ownership based on roles!";
  }

  interface Key {
    String SUCCESS_MESSAGE = "0001";
    String ERROR_PARAM_EXISTS = "0002";
    String DATA_TYPE_ERROR = "0003";
    String ERROR_DUPLICATE_ENTRY = "0004";
    String ERROR_ATTRIBUTE_CONFLICT = "0005";
    String USER_ACCOUNT_BLOCKED = "0006";
    String DATA_SIZE_EXCEEDED = "0007";
    String USER_STATUS_MSG = "0008";
    String DATA_FORMAT_ERROR = "0009";
    String EMPTY_CSV_FILE = "0010";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED = "0011";
    String CHANNEL_REG_FAILED = "0012";
    String RESOURCE_NOT_FOUND = "0013";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "0014";

    String INVALID_PARAMETER_VALUE = "0017";
    String INVALID_VALUE = "0018";
    String INVALID_PARAMETER = "0019";
    String INVALID_COLUMNS = "0020";
    String INVALID_REQUEST_PARAMETER = "0021";
    String INVALID_REQUEST_TIMEOUT = "0022";
    String VALID_IDENTIFIER_ABSENSE = "0023";
    String INVALID_PASSWORD = "0024";
    String INVALID_CAPTCHA = "0025";
    String INVALID_CONSENT_STATUS = "0026";
    String INVALID_OPERATION_NAME = "0027";
    String INVALID_REQUESTED_DATA = "0028";
    String INVALID_LOCATION_DELETE_REQUEST = "0029";
    String MANDATORY_PARAMETER_MISSING = "0030";
    String ERROR_MANDATORY_PARAMETER_EMPTY = "0031";
    String ERROR_NO_FRAMEWORK_FOUND = "0032";
    String UPDATE_NOT_ALLOWED = "0033";
    String PARENT_NOT_ALLOWED = "0034";
    String MISSING_FILE_ATTACHMENT = "0035";
    String FILE_ATTACHMENT_SIZE_NOT_CONFIGURED = "0036";
    String EMPTY_FILE = "0037";
    String CONFLICTING_ORG_LOCATIONS = "0038";
    String EMPTY_HEADER_LINE = "0039";
    String ROOT_ORG_ASSOCIATION_ERROR = "0040";
    String DEPENDENT_PARAMETER_MISSING = "0041";
    String EXTERNALID_ASSIGNED_TO_OTHER_USER = "0042";
    String DUPLICATE_EXTERNAL_IDS = "0043";
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT = "0044";
    String PARAMETER_MISMATCH = "0045";
    String ERROR_CONFIG_LOAD_EMPTY_STRING = "0046";
    String ERROR_CONFIG_LOAD_PARSE_STRING = "0047";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "0048";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "0049";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "0050";
    String ERROR_UNSUPPORTED_FIELD = "0051";
    String INVALID_PROPERTY_ERROR = "0052";
    String ERROR_INACTIVE_ORG = "0053";
    String ERROR_DUPLICATE_ENTRIES = "0054";
    String ERROR_CONFLICTING_VALUES = "0055";
    String ERROR_CONFLICTING_ROOT_ORG_ID = "0056";
    String ERROR_INVALID_OTP = "0057";
    String ERROR_INVALID_PARAMETER_SIZE = "0058";
    String ERROR_RATE_LIMIT_EXCEEDED = "0059";
    String ERROR_USER_MIGRATION_FAILED = "0060";
    String MANDATORY_HEADER_PARAMETER_MISSING = "0061";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "0062";
    String OTP_VERIFICATION_FAILED = "0063";
    String SERVICE_UNAVAILABLE = "0064";
    String MANAGED_BY_NOT_ALLOWED = "0065";
    String MANAGED_USER_LIMIT_EXCEEDED = "0066";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "0067";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED = "0068";
    String SERVER_ERROR = JsonKey.USER_ORG_SERVICE_PREFIX + "0069";
    String UNAUTHORIZED_USER = JsonKey.USER_ORG_SERVICE_PREFIX + "0070";
    String FORBIDDEN = "0071";
    String INVALID_OBJECT_TYPE = "0072";
    String INACTIVE_USER = "0073";
    String INVALID_CSV_FILE = "0074";
    String EXTENDED_USER_PROFILE_NOT_LOADED = "0075";
    String ROLE_PROCESSING_INVALID_ORG = "0076";
    String INVALID_FILE_EXTENSION = "0077";
    String INVALID_ENCRYPTION_FILE = "0078";
    String INVALID_SECURITY_LEVEL = "0079";
    String INVALID_SECURITY_LEVEL_LOWER = "0080";
    String MISSING_DEFAULT_SECURITY_LEVEL = "0081";
    String INVALID_TENANT_SECURITY_LEVEL_LOWER = "0082";
    String CANNOT_DELETE_USER = "0083";
    String CANNOT_TRANSFER_OWNERSHIP = "0084";
  }
}
