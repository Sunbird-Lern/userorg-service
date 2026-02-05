package org.sunbird.response;

/**
 * This interface holds all the response keys and messages, logically grouped by functionality.
 *
 */
public interface ResponseMessage {

  interface Message {

    // -------------------------------------------------------------------------
    // Generic / Common Messages
    // -------------------------------------------------------------------------
    String SUCCESS_MESSAGE = "Success";
    String INTERNAL_ERROR = "Process failed,please try again later.";
    String OPERATION_TIMEOUT = "Request processing taking too long time. Please try again later.";
    String INVALID_OPERATION_NAME =
        "Operation name is invalid. Please provide a valid operation name";
    String INVALID_REQUESTED_DATA = "Requested data for this operation is not valid.";
    String INVALID_DATA = "Incorrect data.";
    String DATA_TYPE_ERROR = "Data type of {0} should be {1}.";
    String ID_REQUIRED_ERROR = "For deleting a record, Id is required.";
    String MANDATORY_PARAMETER_MISSING = "Mandatory parameter {0} is missing.";
    String ERROR_MANDATORY_PARAMETER_EMPTY = "Mandatory parameter {0} is empty.";
    String INVALID_PARAMETER_VALUE =
        "Invalid value {0} for parameter {1}. Please provide a valid value.";
    String INVALID_PARAMETER = "Please provide valid {0}.";
    String INVALID_REQUEST_PARAMETER = "Invalid request parameter {0}.";
    String IDENTIFIER_VALIDATION_FAILED = "Identifier validation failed for {0}.";
    String INVALID_VALUE = "Invalid {0}: {1}. Valid values are: {2}.";
    String ALREADY_EXISTS = "A {0} with {1} already exists. Please retry with a unique value.";
    String RESOURCE_NOT_FOUND = "Requested resource not found";
    String CUSTOM_SERVER_ERROR = "{0}";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "Max allowed size is {0}";
    String UNABLE_TO_PARSE_DATA = "Unable to parse the data";
    String INVALID_JSON = "Unable to process object to JSON/ JSON to Object";
    String NO_DATA = "You have uploaded an empty file. Fill mandatory details and upload the file.";
    String INVALID_DATE_FORMAT =
        "Invalid Date format . Date format should be : yyyy-MM-dd hh:mm:ss:SSSZ";
    String DATE_FORMAT_ERRROR = "Date format error.";
    String INVALID_PROPERTY_ERROR = "Invalid property {0}.";
    String INVALID_OBJECT_TYPE = "Invalid Object Type.";
    String PROCESS_EXE_TIMEOUT = "PROCESS_EXE_TIMEOUT";
    String DATA_ALREADY_EXIST = "data already exist.";
    String SERVICE_UNAVAILABLE = "SERVICE UNAVAILABLE";
    String OR_FORMAT = "{0} or {1}";
    String AND_FORMAT = "{0} and {1}";
    String NOT_SUPPORTED = "Not Supported.";
    String ERROR_UNSUPPORTED_FIELD = "Unsupported field {0}.";
    String INVALID_ELEMENT_IN_LIST =
        "Invalid value supplied for parameter {0}.Supported values are {1}";
    String ERROR_RATE_LIMIT_EXCEEDED =
        "Your per {0} rate limit has exceeded. You can retry after some time.";
    String INVALID_REQUEST_TIMEOUT = "Invalid request timeout value {0}.";

    // -------------------------------------------------------------------------
    // Authentication & Authorization
    // -------------------------------------------------------------------------
    String UNAUTHORIZED_USER = "You are not authorized.";
    String INVALID_USER_CREDENTIALS = "Please check your credentials";
    String API_KEY_MISSING_ERROR = "APi key is mandatory.";
    String API_KEY_INVALID_ERROR = "APi key is invalid.";
    String SESSION_ID_MISSING = "Session id is mandatory.";
    String AUTH_TOKEN_MISSING = "Auth token is mandatory.";
    String INVALID_AUTH_TOKEN = "Auth token is invalid.Please login again.";
    String INVALID_ROLE = "Invalid role value provided in request.";
    String INVALID_SALT = "Please provide salt value.";
    String KEY_CLOAK_DEFAULT_ERROR = "server error at sso.";
    String OTP_VERIFICATION_FAILED = "OTP verification failed. Remaining attempt count is {0}.";
    String ERROR_INVALID_OTP = "Invalid OTP.";
    String FORBIDDEN = "You are forbidden from accessing specified resource.";

    // -------------------------------------------------------------------------
    // User Management
    // -------------------------------------------------------------------------
    String USER_NOT_FOUND = "user not found.";
    String USER_ALREADY_EXISTS = "User already exists for given {0}.";
    String INVALID_USER_ID = "User Id does not exists in our records";
    String USERID_MISSING = "UserId is mandatory.";
    String USERNAME_MISSING = "Username is mandatory.";
    String FIRST_NAME_MISSING = "First name is mandatory.";
    String EMAIL_MISSING = "Email is mandatory.";
    String PHONE_NO_REQUIRED_ERROR = "Phone number is required.";
    String PASSWORD_MISSING = "Password is mandatory.";
    String INVALID_PASSWORD =
        "Password must contain a minimum of 8 characters including numerals, lower and upper case alphabets and special characters";
    String PASSWORD_MIN_LENGHT = "Password should have at least 8 character.";
    String PASSWORD_MAX_LENGHT = "Password should not be more than 12 character.";
    String USERNAME_IN_USE = "Username already exists.";
    String EMAIL_IN_USE = "Email already exists.";
    String PHONE_ALREADY_IN_USE = "Phone already in use. Please provide different phone number.";
    String USER_ACCOUNT_BLOCKED = "User account has been blocked .";
    String USER_ALREADY_ACTIVE = "User is already active.";
    String USER_ALREADY_INACTIVE = "User is already inactive.";
    String USER_REG_UNSUCCESSFUL = "User Registration unsuccessful.";
    String USER_UPDATE_UNSUCCESSFUL = "User update operation is unsuccessful.";
    String USER_PHONE_UPDATE_FAILED = "user phone update is failed.";
    String USER_MIGRATION_FAILED = "user is failed to migrate";
    String USER_DATA_ENCRYPTION_ERROR = "Exception Occurred while encrypting user data.";
    String INVALID_EXT_USER_ID = "provided ext user id {0} is incorrect";
    String EXTERNALID_NOT_FOUND =
        "External ID (id: {0}, idType: {1}, provider: {2}) not found for given user.";
    String EXTERNALID_ASSIGNED_TO_OTHER_USER =
        "External ID (id: {0}, idType: {1}, provider: {2}) already assigned to another user.";
    String DUPLICATE_EXTERNAL_IDS =
        "Duplicate external IDs for given idType ({0}) and provider ({1}).";
    String USERNAME_EMAIL_IN_USE =
        "Username or Email is already in use. Please try with a different Username or Email.";
    String USERNAME_CANNOT_BE_UPDATED = "UserName cann't be updated.";
    String CONFIIRM_PASSWORD_MISSING = "Confirm password is mandatory.";
    String CONFIIRM_PASSWORD_EMPTY = "Confirm password can not be empty.";
    String SAME_PASSWORD_ERROR = "New password can't be same as old password.";
    String EMAIL_VERIFY_ERROR = "Please provide a verified email in order to create user.";
    String PHONE_VERIFY_ERROR =
        "Please provide a verified phone number in order to create/update user.";
    String LOGIN_TYPE_MISSING = "Login type is required.";
    String LOGIN_TYPE_ERROR = "provide login type as null.";
    String LOGIN_ID_MISSING = "loginId is required.";
    String USER_NAME_OR_ID_ERROR = "Please provide either username or userId.";
    String USERNAME_USERID_MISSING = "Please provide either userName or userId.";
    String ROLES_MISSING = "user role is required.";
    String EMPTY_ROLES_PROVIDED = "Roles cannot be empty.";
    String ROLE_MISSING = "Role of the user is required";
    String INVALID_VISIBILITY_REQUEST = "Private and Public fields cannot be same.";
    String ADDRESS_REQUIRED_ERROR = "Please provide address.";
    String EDUCATION_REQUIRED_ERROR = "Please provide education details.";
    String JOBDETAILS_REQUIRED_ERROR = "Please provide job details.";
    String ADDRESS_ERROR = "In {0}, {1} is mandatory.";
    String ADDRESS_TYPE_ERROR = "Please provide correct address Type.";
    String NAME_OF_INSTITUTION_ERROR = "Please provide name of Institution.";
    String EDUCATION_DEGREE_ERROR = "Education degree is required.";
    String JOB_NAME_ERROR = "Job Name is required.";
    String INVALID_USR_DATA =
        "Given User Data doesn't exist in our records. Please provide a valid one";
    String USR_DATA_VALIDATION_ERROR = "Please provide valid userId or userName and provider";
    String INVALID_USR_ORG_DATA =
        "Given User Data doesn't belongs to this organization. Please provide a valid one.";
    String USER_NOT_BELONGS_TO_ANY_ORG = "User does not belongs to any org .";
    String USER_ORG_ASSOCIATION_ERROR = "User is already associated with another organization.";
    String ERROR_USER_HAS_NOT_CREATED_ANY_COURSE =
        "User hasn't created any course, or may not have a creator role";
    String USER_NOT_ASSOCIATED_TO_ROOT_ORG =
        "User (ID = {0}) not associated to course batch creator root org.";
    String INVALID_CREDENTIAL = "Invalid credential.";
    String EMAIL_FORMAT = "Email is invalid.";
    String URL_FORMAT_ERROR = "URL is invalid.";
    String LANGUAGE_MISSING = "Language is mandatory.";
    String TIMESTAMP_REQUIRED = "TimeStamp is required.";
    String INVALID_PHONE_NO_FORMAT = "Please provide a valid phone number.";
    String INVALID_PHONE_NUMBER = "Please send Phone and country code seprately.";
    String INVALID_COUNTRY_CODE = "Please provide a valid country code.";
    String EMAIL_OR_PHONE_MISSING = "Please provide either email or phone.";
    String ACCOUNT_NOT_FOUND = "Account not found.";
    String FROM_ACCOUNT_ID_MISSING = "From Account id is mandatory.";
    String TO_ACCOUNT_ID_MISSING = "To Account id is mandatory.";
    String FROM_ACCOUNT_ID_NOT_EXISTS = "From Account id not exists";

    // -------------------------------------------------------------------------
    // Organization Management
    // -------------------------------------------------------------------------
    String ORG_NOT_EXIST = "Requested organisation does not exist.";
    String INVALID_ORG_DATA =
        "Given Organization Data doesn't exist in our records. Please provide a valid one";
    String ORGANISATION_ID_MISSING = "Organization id is mandatory.";
    String ORG_ID_MISSING = "Organization Id required.";
    String ORGANISATION_NAME_MISSING = "organization name is mandatory.";
    String NAME_OF_ORGANISATION_ERROR = "Organization Name is required.";
    String ROOT_ORG_ID_REQUIRED = "Please provide root organisation ID.";
    String REQUIRED_DATA_ORG_MISSING =
        "Organization Id or Provider with External Id values are required for the operation";
    String INVALID_ROOT_ORGANIZATION = "Root organization id is invalid";
    String INVALID_PARENT_ORGANIZATION_ID = "Parent organization id is invalid";
    String PARENT_CODE_AND_PARENT_ID_MISSING = "Please provide either parentCode or parentId.";
    String INVALID_ORG_ID = "Please provide valid location id."; // Note: Check context, might be generic org id
    String INVALID_ORG_STATUS = "INVALID_ORG_STATUS";
    String INVALID_ORG_STATUS_TRANSITION = "INVALID_ORG_STATUS_TRANSITION";
    String ORG_TYPE_MANDATORY = "Org Type name is mandatory.";
    String ORG_TYPE_ALREADY_EXIST =
        "Org type with this name already exist.Please provide some other name.";
    String ORG_TYPE_ID_REQUIRED_ERROR = "Org Type Id is required.";
    String INVALID_ORG_TYPE_ID_ERROR = "Please provide valid orgTypeId.";
    String INVALID_ORG_TYPE_ERROR = "Please provide valid orgType.";
    String ERROR_INACTIVE_ORG = "Organisation corresponding to given {0} ({1}) is inactive.";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "Not able to associate with root org";
    String ERROR_INACTIVE_CUSTODIAN_ORG = "Custodian organisation is inactive.";
    String ROOT_ORG_ASSOCIATION_ERROR =
        "No root organisation found which is associated with given {0}.";
    String INVALID_ROOT_ORG_DATA =
        "Root org doesn't exist for this Organization Id and channel {0}";
    String CHANNEL_SHOULD_BE_UNIQUE =
        "Channel value already used by another organization. Provide different value for channel";
    String INVALID_CHANNEL = "Channel value is invalid.";
    String CHANNEL_REG_FAILED = "Channel Registration failed.";
    String SLUG_IS_NOT_UNIQUE =
        "Please provide different channel value. This channel value already exist.";
    String SLUG_REQUIRED = "Slug is required .";
    String CONFLICTING_ORG_LOCATIONS =
        "An organisation cannot be associated to two conflicting locations ({0}, {1}) at {2} level. ";
    String INVALID_LOCATION_ID = "Please provide valid location id.";
    String LOCATION_ID_REQUIRED = "Please provide Location Id.";
    String LOCATION_TYPE_REQUIRED = "Location type required.";
    String INVALID_REQUEST_DATA_FOR_LOCATION = "{0} field required.";
    String INVALID_LOCATION_DELETE_REQUEST =
        "One or more locations have a parent reference to given location and hence cannot be deleted.";
    String LOCATION_TYPE_CONFLICTS = "Location type conflicts with its parent location type.";
    String PARENT_NOT_ALLOWED = "For top level location, {0} is not allowed.";
    String INVALID_HASHTAG_ID =
        "Please provide different hashTagId.This HashTagId is associated with some other organization.";

    // -------------------------------------------------------------------------
    // Course & Batch Management
    // -------------------------------------------------------------------------
    String COURSE_ID_MISSING_ERROR = "Please provide course id.";
    String COURSE_ID_MISSING = "Course id is mandatory.";
    String INVALID_COURSE_ID = "Course doesnot exist. Please provide a valid course identifier";
    String COURSE_NAME_MISSING = "Please provide the course name.";
    String COURSE_DESCRIPTION_MISSING = "Description is mandatory.";
    String COURSE_VERSION_MISSING = "Course version is mandatory.";
    String COURSE_DURATION_MISSING = "Course duration is mandatory.";
    String COURSE_TOCURL_MISSING = "Course tocurl is mandatory.";
    String COURSE_CREATED_FOR_NULL = "Batch does not belong to any organization .";
    String COURSE_BATCH_ID_MISSING = "Course batch Id required";
    String INVALID_COURSE_BATCH_ID = "Invalid course batch id ";
    String COURSE_BATCH_ALREADY_COMPLETED = "Course batch is already completed.";
    String COURSE_BATCH_ENROLLMENT_DATE_ENDED = "Course batch enrollment date has ended.";
    String COURSE_BATCH_START_DATE_REQUIRED = "Batch start date is mandatory.";
    String COURSE_BATCH_START_DATE_INVALID =
        "Batch start date should be either today or future date.";
    String COURSE_BATCH_END_DATE_ERROR = "Batch has been closed.";
    String COURSE_BATCH_IS_CLOSED_ERROR = "Batch has been closed.";
    String COURSE_BATCH_START_PASSED_DATE_INVALID = "This Batch already started.";
    String INVALID_BATCH_START_DATE_ERROR = "Please provide valid Start Date.";
    String INVALID_BATCH_END_DATE_ERROR = "Please provide valid End Date.";
    String MULTIPLE_COURSES_FOR_BATCH = "A batch cannot belong to multiple courses.";
    String INVALID_COURSE_CREATOR_ID = "Course creator id does not exist .";
    String ENROLLMENT_START_DATE_MISSING = "Enrollment start date is mandatory.";
    String ENROLLMENT_END_DATE_START_ERROR =
        "Enrollment End date should be greater than course batch start date.";
    String ENROLLMENT_END_DATE_END_ERROR =
        "Enrollment End date should be lesser than course batch end date.";
    String ENROLLMENT_END_DATE_UPDATE_ERROR =
        "Invalid Enrollment End date. Please provide future date.";
    String ENROLMENT_TYPE_REQUIRED = "Enrolment type is mandatory.";
    String ENROLMENT_TYPE_VALUE_ERROR = "EnrolmentType value must be either open or invite-only.";
    String ENROLLMENT_TYPE_VALIDATION = "Enrollment type should be invite-only.";
    String USER_ALREADY_ENROLLED_COURSE = "User has already Enrolled this course .";
    String USER_NOT_ENROLLED_COURSE = "User is not enrolled to given course batch.";
    String USER_ALREADY_COMPLETED_COURSE = "User already completed given course batch.";
    String END_DATE_ERROR = "End date should be greater than start date.";
    String PUBLISHED_COURSE_CAN_NOT_UPDATED = "Published course can't be updated.";
    String INVALID_PROGRESS_STATUS =
        "Progress status value should be NOT_STARTED(0), STARTED(1), COMPLETED(2).";
    String MISSING_MESSAGE = "Required fields for create course are missing. {0}";
    String CONTENT_TYPE_MISMATCH = "Content Type should be Course.";
    String MIME_TYPE_MISMATCH = "MimeType should be application/vnd.ekstep.content-collection";

    // -------------------------------------------------------------------------
    // Content & Assessment
    // -------------------------------------------------------------------------
    String CONTENT_ID_MISSING_ERROR = "Please provide content id.";
    String CONTENT_ID_MISSING = "Content id is mandatory.";
    String CONTENT_ID_ERROR = "Please provide content id or course id";
    String CONTENT_VERSION_MISSING = "Content version is mandatory.";
    String VERSION_MISSING = "Version is mandatory.";
    String CONTENT_STATUS_MISSING_ERROR = "content status is required .";
    String CONTENT_TYPE_ERROR = "Please add Content-Type header with value application/json";
    String ASSESSMENT_ITEM_ID_REQUIRED = "Assessment item id is required.";
    String ASSESSMENT_TYPE_REQUIRED = "Assessment type is required.";
    String ATTEMPTED_DATE_REQUIRED = "Attempted data is required.";
    String ATTEMPTED_ANSWERS_REQUIRED = "Attempted answers is required.";
    String MAX_SCORE_REQUIRED = "Max score is required.";
    String ATTEMPT_ID_MISSING_ERROR = "Please provide attempt id.";

    // -------------------------------------------------------------------------
    // Badge/Certificates (Issuer, Recipient, Assertion)
    // -------------------------------------------------------------------------
    String ISSUER_ID_REQUIRED = "Please provide issuer ID.";
    String INVALID_ISSUER_ID = "Invalid issuer ID.";
    String RECIPIENT_ID_REQUIRED = "Please provide a recipient id.";
    String RECIPIENT_TYPE_REQUIRED = "Please provide recipient type.";
    String INVALID_RECIPIENT_TYPE = "Please provide a valid recipient type.";
    String RECIPIENT_EMAIL_REQUIRED = "Please provide recipient email.";
    String RECIPIENT_ADDRESS_ERROR = "Please send recipientEmails or recipientUserIds.";
    String RECEIVER_ID_ERROR = "Receiver id is mandatory.";
    String INVALID_RECEIVER_ID = "Receiver id is invalid.";
    String ASSERTION_ID_REQUIRED = "Please provide assertion ID.";
    String ASSERTION_EVIDENCE_REQUIRED = "Please provide valid assertion url as an evidence.";
    String REVOCATION_REASON_REQUIRED = "Please provide revocation reason.";
    String ENDORSED_USER_ID_REQUIRED = " Endorsed user id required .";
    String CAN_NOT_ENDORSE = "Can not endorse since both belong to different orgs .";

    // -------------------------------------------------------------------------
    // Notifications & Email
    // -------------------------------------------------------------------------
    String EMAIL_SUBJECT_ERROR = "Email Subject is mandatory.";
    String EMAIL_BODY_ERROR = "Email Body is mandatory.";
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT =
        "Email notification is not sent as the number of recipients exceeded configured limit ({0}).";
    String NO_EMAIL_RECIPIENTS =
        "Email notification is not sent as the number of recipients is zero.";
    String MESSAGE_ID_MISSING = "Message id is mandatory.";
    String INVALID_NOTIFICATION_TYPE = "Please provide a valid notification type.";
    String INVALID_NOTIFICATION_TYPE_SUPPORT = "Only notification type FCM is supported.";
    String INVALID_TOPIC_NAME = "Please provide a valid toipc.";
    String INVALID_TOPIC_DATA = "Please provide valid notification data.";

    // -------------------------------------------------------------------------
    // System, Config & Infrastructure
    // -------------------------------------------------------------------------
    String ERROR_INVALID_CONFIG_PARAM_VALUE = "Invalid value {0} for config parameter {1}.";
    String ERROR_CONFIG_LOAD_EMPTY_STRING =
        "Loading {0} configuration failed as empty string is passed as parameter.";
    String ERROR_CONFIG_LOAD_PARSE_STRING =
        "Loading {0} configuration failed due to parsing error.";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "Loading {0} configuration failed.";
    String ERROR_CONFLICTING_FIELD_CONFIGURATION =
        "Field {0} in {1} configuration is conflicting in {2} and {3}.";
    String MANDATORY_CONFIG_PARAMETER_MISSING =
        "Mandatory configuration parameter {0} missing which is required for service startup.";
    String ERROR_LOAD_CONFIG = "Loading failed for configuration file {0}.";
    String ERROR_SYSTEM_SETTING_NOT_FOUND = "System Setting not found for id: {0}";
    String ERROR_UPDATE_SETTING_NOT_ALLOWED = "Update of system setting {0} is not allowed.";
    String DB_INSERTION_FAIL = "DB insert operation failed.";
    String DB_UPDATE_FAIL = "Db update operation failed.";
    String ES_ERROR = "Something went wrong when processing data for search";
    String ES_UPDATE_FAILED = "Data insertion to ES failed.";
    String UNABLE_TO_CONNECT_TO_EKSTEP = "Unable to connect to Ekstep Server";
    String UNABLE_TO_CONNECT_TO_ES = "Unable to connect to Elastic Search";
    String UNABLE_TO_COMMUNICATE_WITH_ACTOR = "Unable to communicate with actor.";
    String ACTOR_CONNECTION_ERROR = "Service is not able to connect with actor.";
    String CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED =
        "Cassandra connection establishment failed in {0} mode.";
    String CLOUD_SERVICE_ERROR = "Cloud storage service error.";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "Unsupported cloud storage type {0}.";
    String STORAGE_CONTAINER_NAME_MANDATORY = " Container name can not be null or empty.";
    String ERROR_GENERATE_DOWNLOAD_LINK = "Error in generating download link.";
    String ERROR_DOWNLOAD_LINK_UNAVAILABLE = "Download link is unavailable.";
    String ERROR_SAVING_STORAGE_DETAILS = "Error saving storage details for download link.";
    String ERROR_UPLOAD_QRCODE_CSV_FAILED = "Uploading the html file to cloud storage has failed.";
    String ERR_CALLING_GROUP_API = "Error while calling group api.";
    String ERR_CALLING_EXHAUST_API = "Error while calling exhaust api";

    // -------------------------------------------------------------------------
    // Files & Uploads
    // -------------------------------------------------------------------------
    String INVALID_CSV_FILE = "Please provide valid csv file.";
    String ERROR_CSV_NO_DATA_ROWS = "No data rows in CSV.";
    String EMPTY_CSV_FILE = "CSV file is Empty.";
    String EMPTY_HEADER_LINE = "Missing header line in CSV file.";
    String BULK_USER_UPLOAD_ERROR =
        "Please provide either organization Id or external Id & provider value.";
    String DATA_SIZE_EXCEEDED = "Maximum upload data size should be {0}";
    String ERROR_MAX_SIZE_EXCEEDED = "Size of {0} exceeds max limit {1}";
    String MISSING_FILE_ATTACHMENT = "Missing file attachment.";
    String EMPTY_FILE = "Attached file is empty.";
    String FILE_ATTACHMENT_SIZE_NOT_CONFIGURED = "File attachment max size is not configured.";
    String ERROR_CREATING_FILE = "Error Reading File";
    String ERROR_PROCESSING_FILE =
        "Something Went Wrong While Reading File. Please Check The File.";
    String ERROR_PROCESSING_REQUEST = "Something went wrong while Processing Request";

    // -------------------------------------------------------------------------
    // Miscellaneous
    // -------------------------------------------------------------------------
    String PAGE_NAME_REQUIRED = "Page name is required.";
    String PAGE_ID_REQUIRED = "Page id is required.";
    String PAGE_ALREADY_EXIST = "page already exist with this Page Name and Org Code.";
    String PAGE_NOT_EXIST = "Requested page does not exist.";
    String INVALID_PAGE_SOURCE = "Invalid page source.";
    String SECTION_NAME_MISSING = "Section name is required.";
    String SECTION_DATA_TYPE_MISSING = "Section data type missing.";
    String SECTION_ID_REQUIRED = "Section id is required.";
    String SECTION_NOT_EXIST = "Requested section does not exist.";
    String INVALID_PAGE_SECTION = "Page section associated with the page is invalid.";
    String INVALID_WEBPAGE_DATA = "Invalid webPage data";
    String INVALID_MEDIA_TYPE = "Invalid media type for webPage";
    String INVALID_WEBPAGE_URL = "Invalid URL for {0}.";
    String TITLE_REQUIRED = "Title is required";
    String NOTE_REQUIRED = "No data to store for notes";
    String NOTE_ID_INVALID = "Invalid note id";
    String INVALID_TAGS = "Invalid data for tags";
    String INVALID_CLIENT_NAME = "Please provide unique valid client name";
    String INVALID_CLIENT_ID = "Please provide valid client id";
    String TABLE_OR_DOC_NAME_ERROR = "Please provide valid table or documentName.";
    String INVALID_DUPLICATE_VALUE = "Values for {0} and {1} cannot be same.";
    String ERROR_DUPLICATE_ENTRY = "Value {0} for {1} is already in use.";
    String ERROR_DUPLICATE_ENTRIES = "System contains duplicate entry for {0}.";
    String INVALID_PERIOD = "Time Period is invalid";
    String INVALID_DATE_RANGE = "Date range should be between 3 Month.";
    String CYCLIC_VALIDATION_FAILURE = "The relation cannot be created as it is cyclic";
    String UPDATE_NOT_ALLOWED = "Update of {0} is not allowed.";
    String STATUS_CANNOT_BE_UPDATED = "status cann't be updated.";
    String UPDATE_FAILED = "Data updation failed due to invalid Request";
    String INVALID_COLUMN_NAME = "Invalid column name.";
    String INVALID_COLUMNS = "Invalid column: {0}. Valid columns are: {1}.";
    String REQUIRED_HEADER_MISSING = "Required set of header missing: ";
    String MANDATORY_HEADER_MISSING = "Mandatory header {0} is missing.";
    String MANDATORY_HEADER_PARAMETER_MISSING = "Mandatory header parameter {0} is missing.";
    String PARAMETER_MISMATCH = "Mismatch of given parameters: {0}.";
    String DEPENDENT_PARAMETER_MISSING = "Missing parameter {0} which is dependent on {1}.";
    String DEPENDENT_PARAMS_MISSING = "Missing parameter value in {0}.";
    String COMMON_ATTRIBUTE_MISMATCH = "{0} mismatch of {1} and {2}";
    String ERROR_ATTRIBUTE_CONFLICT = "Either pass attribute {0} or {1} but not both.";
    String EVENTS_DATA_MISSING = "Events array is mandatory";
    String GROUP_ID_MISSING = "GroupId is mandatory.";
    String ACTIVITY_ID_MISSING = "ActivityId is mandatory.";
    String ACTIVITY_TYPE_MISSING = "ActivityType is mandatory.";
    String ERROR_NO_DIALCODES_LINKED = "No dialcodes are linked to any courses created by user(s)";
    String SOURCE_MISSING = "Source is required.";
    String INVALID_CONFIGURATION = "Invalid configuration data.";
    String INVALID_PROCESS_ID = "Invalid Process Id.";
    String INVALID_TYPE_VALUE = "INVALID_TYPE_VALUE";
    String ERROR_NO_FRAMEWORK_FOUND = "No framework found.";

    // -------------------------------------------------------------------------
    // JSON Transform (Registry)
    // -------------------------------------------------------------------------
    String ERROR_JSON_TRANSFORM_INVALID_TYPE_CONFIG =
        "JSON transformation failed as invalid type configuration found for field {0}.";
    String ERROR_JSON_TRANSFORM_INVALID_DATE_FORMAT =
        "JSON transformation failed as invalid date format configuration found for field {0}.";
    String ERROR_JSON_TRANSFORM_INVALID_INPUT =
        "JSON transformation failed as invalid input provided for field {0}.";
    String ERROR_JSON_TRANSFORM_INVALID_ENUM_INPUT =
        "JSON transformation failed as invalid enum input provided for field {0}.";
    String ERROR_JSON_TRANSFORM_ENUM_VALUES_EMPTY =
        "JSON transformation failed as enum values is empty in configuration for field {0}.";
    String ERROR_JSON_TRANSFORM_BASIC_CONFIG_MISSING =
        "JSON transformation failed as mandatory configuration (toFieldName, fromType or toType) is missing for field {0}.";
    String ERROR_JSON_TRANSFORM_INVALID_FILTER_CONFIG =
        "JSON transformation failed as invalid filter configuration found for field {0}.";
    String ERROR_REGISTRY_CLIENT_CREATION = "Registry client creation failed.";
    String ERROR_REGISTRY_ADD_ENTITY = "Registry add entity API failed.";
    String ERROR_REGISTRY_READ_ENTITY = "Registry read entity API failed.";
    String ERROR_REGISTRY_UPDATE_ENTITY = "Registry update entity API failed.";
    String ERROR_REGISTRY_DELETE_ENTITY = "Registry delete entity API failed.";
    String ERROR_REGISTRY_PARSE_RESPONSE = "Error while parsing response from registry.";
    String ERROR_REGISTRY_ENTITY_TYPE_BLANK = "Request failed as entity type is blank.";
    String ERROR_REGISTRY_ENTITY_ID_BLANK = "Request failed as entity id is not provided.";
    String ERROR_REGISTRY_ACCESS_TOKEN_BLANK =
        "Request failed as user access token is not provided.";
    String INVALID_FILE_EXTENSION = "Please provide a valid file. File expected of format: {0}";
    String DATA_FORMAT_ERROR = "Invalid format for given {0}.";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED = "Please provide only email or phone or managed by";
    String MANAGED_BY_NOT_ALLOWED = "managedBy cannot be updated.";
    String MANAGED_USER_LIMIT_EXCEEDED = "Managed user creation limit exceeded";
    String USER_TYPE_CONFIG_IS_EMPTY = "userType config is empty for the statecode {0}";
    String ERROR_CONFLICTING_VALUES = "Conflicting values for {0} ({1}) and {2} ({3}).";
    String ERROR_CONFLICTING_ROOT_ORG_ID =
        "Root organisation channel of uploader user is conflicting with that of specified organisation ID/orgExternalId channel value.";
    String ERROR_INVALID_PARAMETER_SIZE =
        "Parameter {0} is of invalid size (expected: {1}, actual: {2}).";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "Declared user error status is not updated";
    String INVALID_ENCRYPTION_FILE = "Please provide valid public key file.";
    String ERROR_PARAM_EXISTS = "{0} already exists";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "{0} could not be same as {1}";
    String INVALID_SECURITY_LEVEL =
        "Invalid data security level {0} provided for job {1}. Please provide a valid data security level.";
    String INVALID_SECURITY_LEVEL_LOWER =
        "Invalid data security level {0} provided for job {1}. Cannot be set lower than the default security level: {2}";
    String MISSING_DEFAULT_SECURITY_LEVEL =
        "Default data security policy settings is missing for the job: {0}";
    String INVALID_TENANT_SECURITY_LEVEL_LOWER =
        "Tenant level's security {0} cannot be lower than system level's security {1}. Please provide a valid data security level.";
    String ERROR_USER_MIGRATION_FAILED = "User migration failed.";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED =
        "Declared user validated status is not updated";
    String USER_STATUS_MSG = "User is already {0}.";
    String ERROR_USER_UPDATE_PASSWORD = "User is created but password couldn't be updated.";
    String EXTENDED_USER_PROFILE_NOT_LOADED =
        "Failed to load extendedProfileSchemaConfig from System_Settings table";
    String EXTERNAL_ID_FORMAT = "externalId (id: {0}, idType: {1}, provider: {2})";
    String INACTIVE_USER = "User is Inactive. Please make it active to proceed.";
    String ROLE_PROCESSING_INVALID_ORG = "Error while processing assign role. Invalid Organisation Id";
    String CANNOT_DELETE_USER = "User is restricted from deleting account based on roles!";
    String PARAM_NOT_MATCH = "Parameter mismatch.";
    String CANNOT_TRANSFER_OWNERSHIP = "Ownership cannot be transferred.";
    String SIZE_LIMIT_EXCEED = "Size limit exceeded.";
    String INVALID_CONSENT_STATUS = "Invalid consent status.";
    String INVALID_CAPTCHA = "Invalid captcha.";
    String IM_A_TEAPOT = "I'm a teapot.";
  }

  interface Key {

    // -------------------------------------------------------------------------
    // Generic / Common Keys
    // -------------------------------------------------------------------------
    String SUCCESS_MESSAGE = "SUCCESS";
    String INTERNAL_ERROR = "INTERNAL_ERROR";
    String OPERATION_TIMEOUT = "PROCESS_EXE_TIMEOUT";
    String INVALID_REQUESTED_DATA = "INVALID_REQUESTED_DATA";
    String INVALID_DATA = "INVALID_DATA";
    String DATA_TYPE_ERROR = "DATA_TYPE_ERROR";
    String ID_REQUIRED_ERROR = "ID_REQUIRED_ERROR";
    String MANDATORY_PARAMETER_MISSING = "MANDATORY_PARAMETER_MISSING";
    String ERROR_MANDATORY_PARAMETER_EMPTY = "ERROR_MANDATORY_PARAMETER_EMPTY";
    String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";
    String INVALID_PARAMETER = "INVALID_PARAMETER";
    String INVALID_REQUEST_PARAMETER = "INVALID_REQUEST_PARAMETER";
    String IDENTIFIER_VALIDATION_FAILED = "IDENTIFIER_VALIDATION_FAILED";
    String INVALID_VALUE = "INVALID_VALUE";
    String ALREADY_EXISTS = "ALREADY_EXISTS";
    String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    String MAX_ALLOWED_SIZE_LIMIT_EXCEED = "MAX_ALLOWED_SIZE_LIMIT_EXCEED";
    String UNABLE_TO_PARSE_DATA = "UNABLE_TO_PARSE_DATA";
    String INVALID_JSON = "INVALID_JSON";
    String NO_DATA = "NO_DATA";
    String INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    String DECLARED_USER_VALIDATED_STATUS_IS_NOT_UPDATED = "0068";
    String USER_STATUS_MSG = "0008";
    String EXTENDED_USER_PROFILE_NOT_LOADED = "0075";
    String INACTIVE_USER = "0073";
    String ROLE_PROCESSING_INVALID_ORG = "0076";
    String CANNOT_DELETE_USER = "0083";
    String DATE_FORMAT_ERRROR = "DATE_FORMAT_ERRROR";
    String INVALID_PROPERTY_ERROR = "INVALID_PROPERTY_ERROR";
    String INVALID_OBJECT_TYPE = "INVALID_OBJECT_TYPE";
    String DATA_ALREADY_EXIST = "DATA_ALREADY_EXIST";
    String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    String CUSTOM_SERVER_ERROR = "SERVER_ERROR";
    String NOT_SUPPORTED = "NOT_SUPPORTED";
    String ERROR_UNSUPPORTED_FIELD = "ERROR_UNSUPPORTED_FIELD";
    String INVALID_ELEMENT_IN_LIST = "INVALID_ELEMENT_IN_LIST";
    String ERROR_RATE_LIMIT_EXCEEDED = "ERROR_RATE_LIMIT_EXCEEDED";
    String INVALID_REQUEST_TIMEOUT = "INVALID_REQUEST_TIMEOUT";
    String INVALID_OPERATION_NAME = "INVALID_OPERATION_NAME";

    // -------------------------------------------------------------------------
    // Authentication & Authorization
    // -------------------------------------------------------------------------
    String UNAUTHORIZED_USER = "UNAUTHORIZED_USER";
    String INVALID_USER_CREDENTIALS = "INVALID_USER_CREDENTIALS";
    String API_KEY_MISSING_ERROR = "API_KEY_REQUIRED_ERROR";
    String API_KEY_INVALID_ERROR = "API_KEY_INVALID_ERROR";
    String SESSION_ID_MISSING = "SESSION_ID_REQUIRED_ERROR";
    String AUTH_TOKEN_MISSING = "X_Authenticated_Userid_MISSING";
    String INVALID_AUTH_TOKEN = "INVALID_AUTH_TOKEN";
    String INVALID_ROLE = "INVALID_ROLE";
    String INVALID_SALT = "INVALID_SALT";
    String KEY_CLOAK_DEFAULT_ERROR = "KEY_CLOAK_DEFAULT_ERROR";
    String OTP_VERIFICATION_FAILED = "OTP_VERIFICATION_FAILED";
    String ERROR_INVALID_OTP = "ERROR_INVALID_OTP";
    String FORBIDDEN = "FORBIDDEN";

    // -------------------------------------------------------------------------
    // User Management
    // -------------------------------------------------------------------------
    String USER_NOT_FOUND = "USER_NOT_FOUND";
    String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    String INVALID_USER_ID = "INVALID_USER_ID";
    String USERID_MISSING = "USERID_MISSING";
    String USERNAME_MISSING = "USERNAME_MISSING";
    String FIRST_NAME_MISSING = "FIRST_NAME_REQUIRED_ERROR";
    String EMAIL_MISSING = "EMAIL_ID_REQUIRED_ERROR";
    String PHONE_NO_REQUIRED_ERROR = "PHONE_NO_REQUIRED_ERROR";
    String PASSWORD_MISSING = "PASSWORD_REQUIRED_ERROR";
    String INVALID_PASSWORD = "INVALID_PASSWORD";
    String PASSWORD_MIN_LENGHT = "PASSWORD_MIN_LENGHT_ERROR";
    String PASSWORD_MAX_LENGHT = "PASSWORD_MAX_LENGHT_ERROR";
    String USERNAME_IN_USE = "USERNAME_IN_USE";
    String EMAIL_IN_USE = "EMAIL_IN_USE";
    String PHONE_ALREADY_IN_USE = "PHONE_ALREADY_IN_USE";
    String USER_ACCOUNT_BLOCKED = "USER_ACCOUNT_BLOCKED";
    String USER_ALREADY_ACTIVE = "USER_ALREADY_ACTIVE";
    String USER_ALREADY_INACTIVE = "USER_ALREADY_INACTIVE";
    String USER_REG_UNSUCCESSFUL = "USER_REG_UNSUCCESSFUL";
    String USER_UPDATE_UNSUCCESSFUL = "USER_UPDATE_UNSUCCESSFUL";
    String USER_PHONE_UPDATE_FAILED = "USER_PHONE_UPDATE_FAILED";
    String USER_MIGRATION_FAILED = "USER_MIGRATION_FAILED";
    String USER_DATA_ENCRYPTION_ERROR = "USER_DATA_ENCRYPTION_ERROR";
    String INVALID_EXT_USER_ID = "INVALID_EXT_USER_ID";
    String EXTERNALID_NOT_FOUND = "EXTERNALID_NOT_FOUND";
    String EXTERNALID_ASSIGNED_TO_OTHER_USER = "EXTERNALID_ASSIGNED_TO_OTHER_USER";
    String DUPLICATE_EXTERNAL_IDS = "DUPLICATE_EXTERNAL_IDS";
    String USERNAME_EMAIL_IN_USE = "USERNAME_EMAIL_IN_USE";
    String USERNAME_CANNOT_BE_UPDATED = "USERNAME_CANNOT_BE_UPDATED";
    String CONFIIRM_PASSWORD_MISSING = "CONFIIRM_PASSWORD_MISSING";
    String CONFIIRM_PASSWORD_EMPTY = "CONFIIRM_PASSWORD_EMPTY";
    String SAME_PASSWORD_ERROR = "SAME_PASSWORD_ERROR";
    String EMAIL_VERIFY_ERROR = "EMAIL_VERIFY_ERROR";
    String PHONE_VERIFY_ERROR = "PHONE_VERIFY_ERROR";
    String LOGIN_TYPE_MISSING = "LOGIN_TYPE_MISSING";
    String LOGIN_TYPE_ERROR = "LOGIN_TYPE_ERROR";
    String LOGIN_ID_MISSING = "LOGIN_ID_MISSING";
    String USER_NAME_OR_ID_ERROR = "USER_NAME_OR_ID_ERROR";
    String USERNAME_USERID_MISSING = "USERNAME_USERID_MISSING";
    String ROLES_MISSING = "ROLES_REQUIRED_ERROR";
    String EMPTY_ROLES_PROVIDED = "EMPTY_ROLES_PROVIDED";
    String ROLE_MISSING = "ROLE_MISSING";
    String INVALID_VISIBILITY_REQUEST = "INVALID_VISIBILITY_REQUEST";
    String ADDRESS_REQUIRED_ERROR = "ADDRESS_REQUIRED_ERROR";
    String EDUCATION_REQUIRED_ERROR = "EDUCATION_REQUIRED_ERROR";
    String JOBDETAILS_REQUIRED_ERROR = "JOBDETAILS_REQUIRED_ERROR";
    String ADDRESS_ERROR = "ADDRESS_ERROR";
    String ADDRESS_TYPE_ERROR = "ADDRESS_TYPE_ERROR";
    String NAME_OF_INSTITUTION_ERROR = "NAME_OF_INSTITUTION_ERROR";
    String EDUCATION_DEGREE_ERROR = "EDUCATION_DEGREE_ERROR";
    String JOB_NAME_ERROR = "JOB_NAME_ERROR";
    String INVALID_USR_DATA = "INVALID_USER_DATA";
    String USR_DATA_VALIDATION_ERROR = "USER_DATA_VALIDATION_ERROR";
    String INVALID_USR_ORG_DATA = "INVALID_USR_ORG_DATA";
    String USER_NOT_BELONGS_TO_ANY_ORG = "USER_NOT_BELONGS_TO_ANY_ORG";
    String USER_ORG_ASSOCIATION_ERROR = "USER_ORG_ASSOCIATION_ERROR";
    String ERROR_USER_HAS_NOT_CREATED_ANY_COURSE = "USER_HAS_NOT_CREATED_ANY_COURSE";
    String USER_NOT_ASSOCIATED_TO_ROOT_ORG = "USER_NOT_ASSOCIATED_TO_ROOT_ORG";
    String PARAM_NOT_MATCH = "PARAM_NOT_MATCH";
    String CANNOT_TRANSFER_OWNERSHIP = "CANNOT_TRANSFER_OWNERSHIP";
    String INVALID_CREDENTIAL = "INVALID_CREDENTIAL";
    String EMAIL_FORMAT = "EMAIL_FORMAT_ERROR";
    String URL_FORMAT_ERROR = "URL_FORMAT_ERROR";
    String LANGUAGE_MISSING = "LANGUAGE_REQUIRED_ERROR";
    String TIMESTAMP_REQUIRED = "TIMESTAMP_REQUIRED";
    String INVALID_PHONE_NO_FORMAT = "INVALID_PHONE_NO_FORMAT";
    String INVALID_PHONE_NUMBER = "INVALID_PHONE_NUMBER";
    String INVALID_COUNTRY_CODE = "INVALID_COUNTRY_CODE";
    String EMAIL_OR_PHONE_MISSING = "EMAIL_OR_PHONE_MISSING";
    String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    String FROM_ACCOUNT_ID_MISSING = "FROM_ACCOUNT_ID_MISSING";
    String TO_ACCOUNT_ID_MISSING = "TO_ACCOUNT_ID_MISSING";
    String FROM_ACCOUNT_ID_NOT_EXISTS = "FROM_ACCOUNT_ID_NOT_EXISTS";

    // -------------------------------------------------------------------------
    // Organization Management
    // -------------------------------------------------------------------------
    String ORG_NOT_EXIST = "ORG_NOT_EXIST";
    String INVALID_ORG_DATA = "INVALID_ORGANIZATION_DATA";
    String ORGANISATION_ID_MISSING = "ORGANIZATION_ID_MISSING";
    String ORG_ID_MISSING = "ORG_ID_MISSING";
    String ORGANISATION_NAME_MISSING = "ORGANIZATION_NAME_MISSING";
    String NAME_OF_ORGANISATION_ERROR = "NAME_OF_ORGANIZATION_ERROR";
    String ROOT_ORG_ID_REQUIRED = "BADGE_ROOT_ORG_ID_REQUIRED";
    String REQUIRED_DATA_ORG_MISSING = "REQUIRED_DATA_MISSING";
    String INVALID_ROOT_ORGANIZATION = "INVALID ROOT ORGANIZATION";
    String INVALID_PARENT_ORGANIZATION_ID = "INVALID_PARENT_ORGANIZATION_ID";
    String PARENT_CODE_AND_PARENT_ID_MISSING = "PARENT_CODE_AND_PARENT_ID_MISSING";
    String INVALID_ORG_ID = "INVALID_ORG_ID";
    String INVALID_ORG_STATUS = "INVALID_ORG_STATUS";
    String INVALID_ORG_STATUS_TRANSITION = "INVALID_ORG_STATUS_TRANSITION";
    String ORG_TYPE_MANDATORY = "ORG_TYPE_MANDATORY";
    String ORG_TYPE_ALREADY_EXIST = "ORG_TYPE_ALREADY_EXIST";
    String ORG_TYPE_ID_REQUIRED_ERROR = "ORG_TYPE_ID_REQUIRED_ERROR";
    String INVALID_ORG_TYPE_ID_ERROR = "INVALID_ORG_TYPE_ID_ERROR";
    String INVALID_ORG_TYPE_ERROR = "INVALID_ORG_TYPE_ERROR";
    String ERROR_INACTIVE_ORG = "ERROR_INACTIVE_ORG";
    String ERROR_NO_ROOT_ORG_ASSOCIATED = "ERROR_NO_ROOT_ORG_ASSOCIATED";
    String ERROR_INACTIVE_CUSTODIAN_ORG = "ERROR_INACTIVE_CUSTODIAN_ORG";
    String ROOT_ORG_ASSOCIATION_ERROR = "ROOT_ORG_ASSOCIATION_ERROR";
    String INVALID_ROOT_ORG_DATA = "INVALID_ROOT_ORG_DATA";
    String CHANNEL_SHOULD_BE_UNIQUE = "CHANNEL_SHOULD_BE_UNIQUE";
    String INVALID_CHANNEL = "INVALID_CHANNEL";
    String CHANNEL_REG_FAILED = "CHANNEL_REG_FAILED";
    String SLUG_IS_NOT_UNIQUE = "SLUG_IS_NOT_UNIQUE";
    String SLUG_REQUIRED = "SLUG_REQUIRED";
    String CONFLICTING_ORG_LOCATIONS = "CONFLICTING_ORG_LOCATIONS";
    String INVALID_LOCATION_ID = "INVALID_LOCATION_ID";
    String LOCATION_ID_REQUIRED = "LOCATION_ID_REQUIRED";
    String LOCATION_TYPE_REQUIRED = "LOCATION_TYPE_REQUIRED";
    String INVALID_REQUEST_DATA_FOR_LOCATION = "INVALID_REQUEST_DATA_CREATE_LOCATION";
    String INVALID_LOCATION_DELETE_REQUEST = "INVALID_LOCATION_DELETE_REQUEST";
    String LOCATION_TYPE_CONFLICTS = "LOCATION_TYPE_CONFLICTS";
    String PARENT_NOT_ALLOWED = "PARENT_NOT_ALLOWED";
    String INVALID_HASHTAG_ID = "INVALID_HASHTAG_ID";

    // -------------------------------------------------------------------------
    // Course & Batch Management
    // -------------------------------------------------------------------------
    String COURSE_ID_MISSING_ERROR = "COURSE_ID_REQUIRED_ERROR";
    String COURSE_ID_MISSING = "COURSE_ID_REQUIRED_ERROR";
    String INVALID_COURSE_ID = "INVALID_COURSE_ID";
    String COURSE_NAME_MISSING = "COURSE_NAME_REQUIRED_ERROR";
    String COURSE_DESCRIPTION_MISSING = "COURSE_DESCRIPTION_REQUIRED_ERROR";
    String COURSE_VERSION_MISSING = "COURSE_VERSION_REQUIRED_ERROR";
    String COURSE_DURATION_MISSING = "COURSE_DURATION_MISSING";
    String COURSE_TOCURL_MISSING = "COURSE_TOCURL_REQUIRED_ERROR";
    String COURSE_CREATED_FOR_NULL = "COURSE_CREATED_FOR_NULL";
    String COURSE_BATCH_ID_MISSING = "COURSE_BATCH_ID_MISSING";
    String INVALID_COURSE_BATCH_ID = "INVALID_COURSE_BATCH_ID";
    String COURSE_BATCH_ALREADY_COMPLETED = "COURSE_BATCH_ALREADY_COMPLETED";
    String COURSE_BATCH_ENROLLMENT_DATE_ENDED = "COURSE_BATCH_ENROLLMENT_DATE_ENDED";
    String COURSE_BATCH_START_DATE_REQUIRED = "COURSE_BATCH_START_DATE_REQUIRED";
    String COURSE_BATCH_START_DATE_INVALID = "COURSE_BATCH_START_DATE_INVALID";
    String COURSE_BATCH_END_DATE_ERROR = "COURSE_BATCH_END_DATE_ERROR";
    String COURSE_BATCH_IS_CLOSED_ERROR = "COURSE_BATCH_IS_CLOSED_ERROR";
    String COURSE_BATCH_START_PASSED_DATE_INVALID = "COURSE_BATCH_START_PASSED_DATE_INVALID";
    String INVALID_BATCH_START_DATE_ERROR = "INVALID_BATCH_START_DATE_ERROR";
    String INVALID_BATCH_END_DATE_ERROR = "INVALID_BATCH_END_DATE_ERROR";
    String MULTIPLE_COURSES_FOR_BATCH = "MULTIPLE_COURSES_FOR_BATCH";
    String INVALID_COURSE_CREATOR_ID = "INVALID_COURSE_CREATOR_ID";
    String ENROLLMENT_START_DATE_MISSING = "ENROLLMENT_START_DATE_MISSING";
    String ENROLLMENT_END_DATE_START_ERROR = "ENROLLMENT_END_DATE_START_ERROR";
    String ENROLLMENT_END_DATE_END_ERROR = "ENROLLMENT_END_DATE_END_ERROR";
    String ENROLLMENT_END_DATE_UPDATE_ERROR = "ENROLLMENT_END_DATE_UPDATE_ERROR";
    String ENROLMENT_TYPE_REQUIRED = "ENROLMENT_TYPE_REQUIRED";
    String ENROLMENT_TYPE_VALUE_ERROR = "ENROLMENT_TYPE_VALUE_ERROR";
    String ENROLLMENT_TYPE_VALIDATION = "ENROLLMENT_TYPE_VALIDATION";
    String USER_ALREADY_ENROLLED_COURSE = "USER_ALREADY_ENROLLED_COURSE";
    String USER_NOT_ENROLLED_COURSE = "USER_NOT_ENROLLED_COURSE";
    String USER_ALREADY_COMPLETED_COURSE = "USER_ALREADY_COMPLETED_COURSE";
    String END_DATE_ERROR = "END_DATE_ERROR";
    String PUBLISHED_COURSE_CAN_NOT_UPDATED = "PUBLISHED_COURSE_CAN_NOT_UPDATED";
    String INVALID_PROGRESS_STATUS = "INVALID_PROGRESS_STATUS";
    String MISSING_CODE = "ERR_COURSE_CREATE_FIELDS_MISSING";
    String CONTENT_TYPE_MISMATCH = "CONTENT_TYPE_MISMATCH";
    String MIME_TYPE_MISMATCH = "MIME_TYPE_MISMATCH";

    // -------------------------------------------------------------------------
    // Content & Assessment
    // -------------------------------------------------------------------------
    String CONTENT_ID_MISSING_ERROR = "CONTENT_ID_REQUIRED_ERROR";
    String CONTENT_ID_MISSING = "CONTENT_ID_REQUIRED_ERROR";
    String CONTENT_ID_ERROR = "CONTENT_ID_OR_COURSE_ID_REQUIRED";
    String CONTENT_VERSION_MISSING = "CONTENT_VERSION_REQUIRED_ERROR";
    String VERSION_MISSING = "VERSION_REQUIRED_ERROR";
    String CONTENT_STATUS_MISSING_ERROR = "CONTENT_STATUS_MISSING_ERROR";
    String CONTENT_TYPE_ERROR = "CONTENT_TYPE_ERROR";
    String ASSESSMENT_ITEM_ID_REQUIRED = "ASSESSMENT_ITEM_ID_REQUIRED";
    String ASSESSMENT_TYPE_REQUIRED = "ASSESSMENT_TYPE_REQUIRED";
    String ATTEMPTED_DATE_REQUIRED = "ATTEMPTED_DATE_REQUIRED";
    String ATTEMPTED_ANSWERS_REQUIRED = "ATTEMPTED_ANSWERS_REQUIRED";
    String MAX_SCORE_REQUIRED = "MAX_SCORE_REQUIRED";
    String ATTEMPT_ID_MISSING_ERROR = "ATTEMPT_ID_REQUIRED_ERROR";

    // -------------------------------------------------------------------------
    // Badge/Certificates (Issuer, Recipient, Assertion)
    // -------------------------------------------------------------------------
    String ISSUER_ID_REQUIRED = "ISSUER_ID_REQUIRED";
    String INVALID_ISSUER_ID = "INVALID_ISSUER_ID";
    String RECIPIENT_ID_REQUIRED = "RECIPIENT_ID_REQUIRED";
    String RECIPIENT_TYPE_REQUIRED = "RECIPIENT_TYPE_REQUIRED";
    String INVALID_RECIPIENT_TYPE = "INVALID_RECIPIENT_TYPE";
    String RECIPIENT_EMAIL_REQUIRED = "RECIPIENT_EMAIL_REQUIRED";
    String RECIPIENT_ADDRESS_ERROR = "RECIPIENT_ADDRESS_ERROR";
    String RECEIVER_ID_ERROR = "RECEIVER_ID_ERROR";
    String INVALID_RECEIVER_ID = "INVALID_RECEIVER_ID";
    String ASSERTION_ID_REQUIRED = "ASSERTION_ID_REQUIRED";
    String ASSERTION_EVIDENCE_REQUIRED = "ASSERTION_EVIDENCE_REQUIRED";
    String REVOCATION_REASON_REQUIRED = "REVOCATION_REASON_REQUIRED";
    String ENDORSED_USER_ID_REQUIRED = "ENDORSED_USER_ID_REQUIRED";
    String CAN_NOT_ENDORSE = "CAN_NOT_ENDORSE";

    // -------------------------------------------------------------------------
    // Notifications & Email
    // -------------------------------------------------------------------------
    String EMAIL_SUBJECT_ERROR = "EMAIL_SUBJECT_ERROR";
    String EMAIL_BODY_ERROR = "EMAIL_BODY_ERROR";
    String EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT = "EMAIL_RECIPIENTS_EXCEEDS_MAX_LIMIT";
    String NO_EMAIL_RECIPIENTS = "NO_EMAIL_RECIPIENTS";
    String MESSAGE_ID_MISSING = "MESSAGE_ID_MISSING";
    String INVALID_NOTIFICATION_TYPE = "INVALID_NOTIFICATION_TYPE";
    String INVALID_NOTIFICATION_TYPE_SUPPORT = "INVALID_NOTIFICATION_TYPE_SUPPORT";
    String INVALID_TOPIC_NAME = "INVALID_TOPIC_NAME";
    String INVALID_TOPIC_DATA = "INVALID_TOPIC_DATA";

    // -------------------------------------------------------------------------
    // System, Config & Infrastructure
    // -------------------------------------------------------------------------
    String ERROR_INVALID_CONFIG_PARAM_VALUE = "ERROR_INVALID_CONFIG_PARAM_VALUE";
    String ERROR_CONFIG_LOAD_EMPTY_STRING = "ERROR_CONFIG_LOAD_EMPTY_STRING";
    String ERROR_CONFIG_LOAD_PARSE_STRING = "ERROR_CONFIG_LOAD_PARSE_STRING";
    String ERROR_CONFIG_LOAD_EMPTY_CONFIG = "ERROR_CONFIG_LOAD_EMPTY_CONFIG";
    String ERROR_CONFLICTING_FIELD_CONFIGURATION = "ERROR_CONFLICTING_FIELD_CONFIGURATION";
    String MANDATORY_CONFIG_PARAMETER_MISSING = "MANDATORY_CONFIG_PARAMETER_MISSING";
    String ERROR_LOAD_CONFIG = "ERROR_LOAD_CONFIG";
    String ERROR_SYSTEM_SETTING_NOT_FOUND = "ERROR_SYSTEM_SETTING_NOT_FOUND";
    String ERROR_UPDATE_SETTING_NOT_ALLOWED = "ERROR_UPDATE_SETTING_NOT_ALLOWED";
    String DB_INSERTION_FAIL = "DB_INSERTION_FAIL";
    String DB_UPDATE_FAIL = "DB_UPDATE_FAIL";
    String ES_ERROR = "ELASTICSEARCH_ERROR";
    String ES_UPDATE_FAILED = "ES_UPDATE_FAILED";
    String UNABLE_TO_CONNECT_TO_EKSTEP = "UNABLE_TO_CONNECT_TO_EKSTEP";
    String UNABLE_TO_CONNECT_TO_ES = "UNABLE_TO_CONNECT_TO_ES";
    String UNABLE_TO_COMMUNICATE_WITH_ACTOR = "UNABLE_TO_COMMUNICATE_WITH_ACTOR";
    String ACTOR_CONNECTION_ERROR = "ACTOR_CONNECTION_ERROR";
    String CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED = "CASSANDRA_CONNECTION_ESTABLISHMENT_FAILED";
    String CLOUD_SERVICE_ERROR = "CLOUD_SERVICE_ERROR";
    String ERROR_UNSUPPORTED_CLOUD_STORAGE = "ERROR_ UNSUPPORTED_CLOUD_STORAGE";
    String STORAGE_CONTAINER_NAME_MANDATORY = "STORAGE_CONTAINER_NAME_MANDATORY";
    String ERROR_GENERATE_DOWNLOAD_LINK = "ERROR_GENERATING_DOWNLOAD_LINK";
    String ERROR_DOWNLOAD_LINK_UNAVAILABLE = "ERROR_DOWNLOAD_LINK_UNAVAILABLE";
    String ERROR_SAVING_STORAGE_DETAILS = "ERROR_SAVING_STORAGE_DETAILS";
    String ERROR_UPLOAD_QRCODE_CSV_FAILED = "ERROR_UPLOAD_QRCODE_CSV_FAILED";
    String ERR_CALLING_GROUP_API = "ERR_CALLING_GROUOP_API";
    String ERR_CALLING_EXHAUST_API = "ERR_CALLING_EXHAUST_API";

    // -------------------------------------------------------------------------
    // Files & Uploads
    // -------------------------------------------------------------------------
    String INVALID_CSV_FILE = "INVALID_CSV_FILE";
    String ERROR_CSV_NO_DATA_ROWS = "ERROR_CSV_NO_DATA_ROWS";
    String EMPTY_CSV_FILE = "EMPTY_CSV_FILE";
    String EMPTY_HEADER_LINE = "EMPTY_HEADER_LINE";
    String BULK_USER_UPLOAD_ERROR = "BULK_USER_UPLOAD_ERROR";
    String DATA_SIZE_EXCEEDED = "DATA_SIZE_EXCEEDED";
    String ERROR_MAX_SIZE_EXCEEDED = "ERROR_MAX_SIZE_EXCEEDED";
    String MISSING_FILE_ATTACHMENT = "MISSING_FILE_ATTACHMENT";
    String EMPTY_FILE = "EMPTY_FILE";
    String FILE_ATTACHMENT_SIZE_NOT_CONFIGURED = "ATTACHMENT_SIZE_NOT_CONFIGURED";
    String ERROR_CREATING_FILE = "ERROR_CREATING_FILE";
    String ERROR_PROCESSING_FILE = "ERROR_PROCESSING_FILE";
    String ERROR_PROCESSING_REQUEST = "ERROR_PROCESSING_REQUEST";

    // -------------------------------------------------------------------------
    // Miscellaneous
    // -------------------------------------------------------------------------
    String PAGE_NAME_REQUIRED = "PAGE_NAME_REQUIRED";
    String PAGE_ID_REQUIRED = "PAGE_ID_REQUIRED";
    String PAGE_ALREADY_EXIST = "PAGE_ALREADY_EXIST";
    String PAGE_NOT_EXIST = "PAGE_NOT_EXIST";
    String INVALID_PAGE_SOURCE = "INVALID_PAGE_SOURCE";
    String SECTION_NAME_MISSING = "SECTION_NAME_MISSING";
    String SECTION_DATA_TYPE_MISSING = "SECTION_DATA_TYPE_MISSING";
    String SECTION_ID_REQUIRED = "SECTION_ID_REQUIRED";
    String SECTION_NOT_EXIST = "SECTION_NOT_EXIST";
    String INVALID_PAGE_SECTION = "INVALID_PAGE_SECTION";
    String INVALID_WEBPAGE_DATA = "INVALID_WEBPAGE_DATA";
    String INVALID_MEDIA_TYPE = "INVALID_MEDIA_TYPE";
    String INVALID_WEBPAGE_URL = "INVALID_WEBPAGE_URL";
    String TITLE_REQUIRED = "TITLE_REQUIRED";
    String NOTE_REQUIRED = "NOTE_REQUIRED";
    String NOTE_ID_INVALID = "NOTE_ID_INVALID";
    String INVALID_TAGS = "INVALID_TAGS";
    String INVALID_CLIENT_NAME = "INVALID_CLIENT_NAME";
    String INVALID_CLIENT_ID = "INVALID_CLIENT_ID";
    String TABLE_OR_DOC_NAME_ERROR = "TABLE_OR_DOC_NAME_ERROR";
    String INVALID_DUPLICATE_VALUE = "INVALID_DUPLICATE_VALUE";
    String ERROR_DUPLICATE_ENTRY = "ERROR_DUPLICATE_ENTRY";
    String ERROR_DUPLICATE_ENTRIES = "ERROR_DUPLICATE_ENTRIES";
    String INVALID_PERIOD = "INVALID_PERIOD";
    String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    String CYCLIC_VALIDATION_FAILURE = "CYCLIC_VALIDATION_FAILURE";
    String UPDATE_NOT_ALLOWED = "UPDATE_NOT_ALLOWED";
    String STATUS_CANNOT_BE_UPDATED = "STATUS_CANNOT_BE_UPDATED";
    String UPDATE_FAILED = "UPDATE_FAILED";
    String INVALID_COLUMN_NAME = "INVALID_COLUMN_NAME";
    String INVALID_COLUMNS = "INVALID_COLUMNS";
    String REQUIRED_HEADER_MISSING = "REQUIRED_HEADER_MISSING";
    String MANDATORY_HEADER_MISSING = "MANDATORY_HEADER_MISSING";
    String MANDATORY_HEADER_PARAMETER_MISSING = "MANDATORY_HEADER_PARAMETER_MISSING";
    String PARAMETER_MISMATCH = "PARAMETER_MISMATCH";
    String DEPENDENT_PARAMETER_MISSING = "DEPENDENT_PARAMETER_MISSING";
    String DEPENDENT_PARAMS_MISSING = "DEPENDENT_PARAMETER_MISSING";
    String COMMON_ATTRIBUTE_MISMATCH = "COMMON_ATTRIBUTE_MISMATCH";
    String ERROR_ATTRIBUTE_CONFLICT = "ERROR_ATTRIBUTE_CONFLICT";
    String EVENTS_DATA_MISSING = "EVENTS_DATA_MISSING";
    String GROUP_ID_MISSING = "GROUP_ID_MISSING";
    String ACTIVITY_ID_MISSING = "ACTIVITY_ID_MISSING";
    String ACTIVITY_TYPE_MISSING = "ACTIVITY_TYPE_MISSING";
    String DATA_FORMAT_ERROR = "0009";
    String ONLY_EMAIL_OR_PHONE_OR_MANAGEDBY_REQUIRED = "0011";
    String MANAGED_BY_NOT_ALLOWED = "0065";
    String MANAGED_USER_LIMIT_EXCEEDED = "0066";
    String ERROR_CONFLICTING_VALUES = "0055";
    String ERROR_CONFLICTING_ROOT_ORG_ID = "0056";
    String ERROR_INVALID_PARAMETER_SIZE = "0058";
    String DECLARED_USER_ERROR_STATUS_IS_NOT_UPDATED = "0067";
    String INVALID_ENCRYPTION_FILE = "0078";
    String ERROR_PARAM_EXISTS = "0002";
    String RECOVERY_PARAM_MATCH_EXCEPTION = "0062";
    String INVALID_SECURITY_LEVEL = "0079";
    String INVALID_SECURITY_LEVEL_LOWER = "0080";
    String MISSING_DEFAULT_SECURITY_LEVEL = "0081";
    String INVALID_TENANT_SECURITY_LEVEL_LOWER = "0082";
    String ERROR_USER_MIGRATION_FAILED = "0060";
    String ERROR_NO_DIALCODES_LINKED = "ERROR_NO_DIALCODES_LINKED";
    String SOURCE_MISSING = "SOURCE_MISSING";
    String INVALID_CONFIGURATION = "INVALID_CONFIGURATION";
    String INVALID_PROCESS_ID = "INVALID_PROCESS_ID";
    String INVALID_TYPE_VALUE = "INVALID_TYPE_VALUE";
    String ERROR_NO_FRAMEWORK_FOUND = "ERROR_NO_FRAMEWORK_FOUND";
    String VALID_IDENTIFIER_ABSENSE = "IDENTIFIER IN LIST IS NOT SUPPORTED OR INCORRECT";

    // -------------------------------------------------------------------------
    // JSON Transform (Registry)
    // -------------------------------------------------------------------------
    String ERROR_JSON_TRANSFORM_INVALID_TYPE_CONFIG = "ERROR_JSON_TRANSFORM_INVALID_TYPE_CONFIG";
    String ERROR_JSON_TRANSFORM_INVALID_DATE_FORMAT = "ERROR_JSON_TRANSFORM_INVALID_DATE_FORMAT";
    String ERROR_JSON_TRANSFORM_INVALID_INPUT = "ERROR_JSON_TRANSFORM_INVALID_INPUT";
    String ERROR_JSON_TRANSFORM_INVALID_ENUM_INPUT = "ERROR_JSON_TRANSFORM_INVALID_ENUM_INPUT";
    String ERROR_JSON_TRANSFORM_ENUM_VALUES_EMPTY = "ERROR_JSON_TRANSFORM_ENUM_VALUES_EMPTY";
    String ERROR_JSON_TRANSFORM_BASIC_CONFIG_MISSING = "ERROR_JSON_TRANSFORM_BASIC_CONFIG_MISSING";
    String ERROR_JSON_TRANSFORM_INVALID_FILTER_CONFIG =
        "ERROR_JSON_TRANSFORM_INVALID_FILTER_CONFIG";
    String ERROR_REGISTRY_CLIENT_CREATION = "ERROR_REGISTRY_CLIENT_CREATION";
    String ERROR_REGISTRY_ADD_ENTITY = "ERROR_REGISTRY_ADD_ENTITY";
    String ERROR_REGISTRY_READ_ENTITY = "ERROR_REGISTRY_READ_ENTITY";
    String ERROR_REGISTRY_UPDATE_ENTITY = "ERROR_REGISTRY_UPDATE_ENTITY";
    String ERROR_REGISTRY_DELETE_ENTITY = "ERROR_REGISTRY_DELETE_ENTITY";
    String ERROR_REGISTRY_PARSE_RESPONSE = "ERROR_REGISTRY_PARSE_RESPONSE";
    String ERROR_REGISTRY_ENTITY_TYPE_BLANK = "ERROR_REGISTRY_ENTITY_TYPE_BLANK";
    String ERROR_REGISTRY_ENTITY_ID_BLANK = "ERROR_REGISTRY_ENTITY_ID_BLANK";
    String ERROR_REGISTRY_ACCESS_TOKEN_BLANK = "ERROR_REGISTRY_ACCESS_TOKEN_BLANK";
    String INVALID_FILE_EXTENSION = "INVALID_FILE_EXTENSION";
    String SIZE_LIMIT_EXCEED = "SIZE_LIMIT_EXCEED";
    String INVALID_CONSENT_STATUS = "INVALID_CONSENT_STATUS";
    String INVALID_CAPTCHA = "INVALID_CAPTCHA";
    String IM_A_TEAPOT = "IM_A_TEAPOT";
  }

}
