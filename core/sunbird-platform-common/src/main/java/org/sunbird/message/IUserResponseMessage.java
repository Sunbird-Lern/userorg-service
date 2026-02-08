package org.sunbird.message;

/**
 * Interface defining constants for user-related response messages.
 */
public interface IUserResponseMessage {

  /** Error code for when a user is not found. */
  String USER_NOT_FOUND = "USER_NOT_FOUND";

  /** Error code for invalid requested data. */
  String INVALID_REQUESTED_DATA = "INVALID_REQUESTED_DATA";

  /** Error code for when a template is not found. */
  String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
}
