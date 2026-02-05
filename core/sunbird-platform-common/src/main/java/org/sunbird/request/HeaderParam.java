package org.sunbird.request;

/**
 * Enum representing the keys for Execution Context Values and HTTP Headers.
 * Used to maintain consistency across services for request/response headers.
 */
public enum HeaderParam {
  REQUEST_ID,
  REQUEST_PATH,
  REQUEST_ST_ED_PATH,
  CURRENT_INVOCATION_PATH,
  USER_DATA,
  USER_LOCALE,
  SYSTEM_LOCALE,
  USER_ID,
  PROXY_USER_ID,
  USER_NAME,
  PROXY_USER_NAME,
  SCOPE_ID,
  X_Consumer_ID("x-consumer-id"),
  X_Session_ID("x-session-id"),
  X_Device_ID("x-device-id"),
  X_Authenticated_Userid("x-authenticated-userid"),
  ts("ts"),
  Content_Type("content-type"),
  X_Authenticated_User_Token("x-authenticated-user-token"),
  X_Authenticated_For("x-authenticated-for"),
  X_Authenticated_Client_Token("x-authenticated-client-token"),
  X_Authenticated_Client_Id("x-authenticated-client-id"),
  X_APP_ID("x-app-id"),
  CHANNEL_ID("x-channel-id"),
  X_Trace_ID("x-trace-id"),
  X_REQUEST_ID("x-request-id"),
  X_TRACE_ENABLED("x-trace-enabled"),
  X_APP_VERSION("x-app-ver"),
  X_APP_VERSION_PORTAL("x-app-version"),
  X_SOURCE("x-source"),
  X_Response_Length("x-response-length");

  /** Name of the parameter/header. */
  private String name;

  /**
   * Constructor with name.
   *
   * @param name The string representation of the header/parameter.
   */
  private HeaderParam(String name) {
    this.name = name;
  }

  /**
   * Default constructor.
   */
  private HeaderParam() {}

  /**
   * Returns the parameter name. If a specific name provided in constructor, returns that.
   * Otherwise, returns the enum name.
   *
   * @return The parameter name.
   */
  public String getParamName() {
    return this.name();
  }

  /**
   * Returns the specific name associated with the enum constant, if any.
   *
   * @return The name value.
   */
  public String getName() {
    return name;
  }
}
