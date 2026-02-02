package org.sunbird.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;
import org.sunbird.response.ResponseCode;

/**
 * Consolidated Request class for Sunbird services (LMS, UserOrg, Notification).
 *
 * <p>This class standardizes the Request object across services, incorporating:
 * <ul>
 *   <li>The rich feature set of the LMS implementation (utility methods).
 *   <li>The data safety of the Notification implementation (using HashMap instead of WeakHashMap).
 *   <li>Additional fields and constructors for flexibility.
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request implements Serializable {

  private static final long serialVersionUID = -2362783406031347676L;
  private static final Integer MIN_TIMEOUT = 0;
  private static final Integer MAX_TIMEOUT = 30;
  private static final int WAIT_TIME_VALUE = 30;

  protected Map<String, Object> context;
  private RequestContext requestContext;

  private String id;
  private String ver;
  private String ts;
  private RequestParams params;

  // Use HashMap instead of WeakHashMap to prevent premature garbage collection of request data
  private Map<String, Object> request = new HashMap<>();

  private String managerName;
  private String operation;
  private String requestId;
  private int env;

  // Path field from Notification service
  protected String path;

  private Integer timeout; // in seconds

  /** Default constructor initializes context and params. */
  public Request() {
    this.context = new HashMap<>();
    this.params = new RequestParams();
  }

  /**
   * Constructor with RequestContext.
   *
   * @param requestContext The context of the request.
   */
  public Request(RequestContext requestContext) {
    this.context = new HashMap<>();
    this.params = new RequestParams();
    this.requestContext = requestContext;
  }

  /**
   * Copy constructor.
   *
   * <p>Note: This performs a shallow copy of RequestParams.
   *
   * @param request The request object to copy from.
   */
  public Request(Request request) {
    this.params = request.getParams();
    if (this.params == null) {
      this.params = new RequestParams();
    }
    // Ensure msgid is set if available in the source request's params or requestId
    if (StringUtils.isBlank(this.params.getMsgid())
        && StringUtils.isNotBlank(request.getRequestId())) {
      this.params.setMsgid(request.getRequestId());
    }

    this.context = new HashMap<>();
    if (request.getContext() != null) {
      this.context.putAll(request.getContext());
    }

    this.requestContext = request.getRequestContext();
    this.request = new HashMap<>();
    if (request.getRequest() != null) {
      this.request.putAll(request.getRequest());
    }

    this.id = request.getId();
    this.ver = request.getVer();
    this.ts = request.getTs();
    this.managerName = request.getManagerName();
    this.operation = request.getOperation();
    this.requestId = request.getRequestId();
    this.env = request.getEnv();
    this.path = request.getPath();
    this.timeout = request.getTimeout();
  }

  /**
   * Converts configured fields to lowercase in the request map. Configuration key: {@link
   * JsonKey#SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS}
   */
  public void toLower() {
    String configValue = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS);
    if (StringUtils.isNotBlank(configValue)) {
      Arrays.stream(configValue.split(","))
          .forEach(
              field -> {
                Object value = this.getRequest().get(field);
                if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                  this.getRequest().put(field, ((String) value).toLowerCase());
                }
              });
    }
  }

  /**
   * Gets the request ID. Checks params first, then the requestId field.
   *
   * @return The request ID.
   */
  public String getRequestId() {
    // Logic from Notification: check params first
    if (this.params != null && StringUtils.isNotBlank(this.params.getMsgid())) {
      return this.params.getMsgid();
    }
    return requestId;
  }

  /**
   * Sets the request ID.
   *
   * @param requestId The request ID to set.
   */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
    // Sync with params if needed, or leave decoupled as per original implementations
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  public Map<String, Object> getRequest() {
    return request;
  }

  public void setRequest(Map<String, Object> request) {
    this.request = request;
  }

  public Object get(String key) {
    return request.get(key);
  }

  /**
   * Helper method to get a value with a default.
   *
   * @param key The key to look up.
   * @param defaultVal The default value if key is not present.
   * @return The value or the default.
   */
  public Object getOrDefault(String key, Object defaultVal) {
    return request.getOrDefault(key, defaultVal);
  }

  /**
   * Checks if the request map contains the key.
   *
   * @param key The key to check.
   * @return True if key exists.
   */
  public Boolean contains(String key) {
    return request.containsKey(key);
  }

  /**
   * Puts a value into the request map.
   *
   * @param key The key.
   * @param vo The value object.
   */
  public void put(String key, Object vo) {
    request.put(key, vo);
  }

  /**
   * Copies all entries from the given map to the request map.
   *
   * @param map The map to copy from.
   */
  public void copyRequestValueObjects(Map<String, Object> map) {
    if (map != null && !map.isEmpty()) {
      this.request.putAll(map);
    }
  }

  public String getManagerName() {
    return managerName;
  }

  public void setManagerName(String managerName) {
    this.managerName = managerName;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVer() {
    return ver;
  }

  public void setVer(String ver) {
    this.ver = ver;
  }

  public String getTs() {
    return ts;
  }

  public void setTs(String ts) {
    this.ts = ts;
  }

  public RequestParams getParams() {
    return params;
  }

  /**
   * Sets the request parameters. Auto-sets msgid if requestId is present.
   *
   * @param params The request parameters.
   */
  public void setParams(RequestParams params) {
    this.params = params;
    // Auto-set msgid if requestId is present and msgid is not
    if (this.params.getMsgid() == null && requestId != null) {
      this.params.setMsgid(requestId);
    }
  }

  public int getEnv() {
    return env;
  }

  public void setEnv(int env) {
    this.env = env;
  }

  public Integer getTimeout() {
    return timeout == null ? WAIT_TIME_VALUE : timeout;
  }

  /**
   * Sets the timeout value.
   *
   * @param timeout The timeout in seconds.
   * @throws ProjectCommonException If timeout is invalid.
   */
  public void setTimeout(Integer timeout) {
    if (timeout < MIN_TIMEOUT && timeout > MAX_TIMEOUT) {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.invalidRequestTimeout,
          MessageFormat.format(ResponseCode.invalidRequestTimeout.getErrorMessage(), timeout));
    }
    this.timeout = timeout;
  }

  public RequestContext getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(RequestContext requestContext) {
    this.requestContext = requestContext;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    return "Request ["
        + (context != null ? "context=" + context + ", " : "")
        + (request != null ? "request=" + request + ", " : "")
        + (id != null ? "id=" + id + ", " : "")
        + (operation != null ? "operation=" + operation : "")
        + "]";
  }
}