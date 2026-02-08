package org.sunbird.message;

/**
 * Interface that holds common response keys and messages used across the application.
 * Extends IUserResponseMessage and IOrgResponseMessage for consolidated access.
 */
public interface IResponseMessage extends IUserResponseMessage, IOrgResponseMessage {

  /** Error code for invalid operation name. */
  String INVALID_OPERATION_NAME = "INVALID_OPERATION_NAME";

  /** Error code for internal server error. */
  String INTERNAL_ERROR = "INTERNAL_ERROR";

  /** Error code for server-side error. */
  String SERVER_ERROR = "SERVER_ERROR";

  /** Template for invalid value error messages. */
  String INVALID_VALUE = "{0} VALUE IS INVALID, {1}";

  /** Template for maximum notification size exceeded error message. */
  String MAX_NOTIFICATION_SIZE = "Max supported id in single playload is {0}";

  /** Error message for service unavailability. */
  String SERVICE_UNAVAILABLE = "SERVICE UNAVAILABLE";

  /** Error code for invalid parameter value. */
  String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";

  /** Error code for data type mismatch errors. */
  String DATA_TYPE_ERROR = "DATA_TYPE_ERROR";

  /**
   * Interface for error code keys.
   */
  interface Key {
    /** Key for mandatory parameter missing error. */
    String MANDATORY_PARAMETER_MISSING = "MANDATORY_PARAMETER_MISSING";

    /** Key for template not found error. */
    String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";

    /** Key for invalid requested data error. */
    String INVALID_REQUESTED_DATA = "INVALID_REQUESTED_DATA";

    /** Key for unauthorized access error. */
    String UNAUTHORIZED = "UNAUTHORIZED";

    /** Key for server error. */
    String SERVER_ERROR = "INTERNAL_ERROR";

    /** Key for invalid parameter value error. */
    String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";
  }

  /**
   * Interface for detailed error messages.
   */
  interface Message {
    /** Template for mandatory parameter missing message. */
    String MANDATORY_PARAMETER_MISSING = "Mandatory parameter {0} is missing";

    /** Template for template not found message. */
    String TEMPLATE_NOT_FOUND = "Template is not pre configured for {0} type";

    /** Message for unauthorized access. */
    String UNAUTHORIZED = "you are an unauthorized user";

    /** Message for invalid request data. */
    String INVALID_REQUESTED_DATA = "Invalid request data is passed";

    /** Message for internal error. */
    String INTERNAL_ERROR = "INTERNAL_ERROR";
  }
}