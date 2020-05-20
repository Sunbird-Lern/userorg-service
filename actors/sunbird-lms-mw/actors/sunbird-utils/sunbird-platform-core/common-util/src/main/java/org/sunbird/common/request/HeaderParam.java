package org.sunbird.common.request;

/**
 * The keys of the Execution Context Values.
 *
 * @author Manzarul
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
  X_Authenticated_Client_Token("x-authenticated-client-token"),
  X_Authenticated_Client_Id("x-authenticated-client-id"),
  X_APP_ID("x-app-id"),
  CHANNEL_ID("x-channel-id"),
  X_Response_Length("x-response-length");
  /** name of the parameter */
  private String name;

  /**
   * 1-arg constructor
   *
   * @param name String
   */
  private HeaderParam(String name) {
    this.name = name;
  }

  /**
   * this will return parameter default name
   *
   * @return
   */
  public String getParamName() {
    return this.name();
  }

  private HeaderParam() {}

  /**
   * This will provide name of one argument enum
   *
   * @return String
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
