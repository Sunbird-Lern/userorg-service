package org.sunbird.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolidated RequestContext class for Sunbird services (LMS, UserOrg, Notification).
 *
 * <p>This class serves as a unified context object that combines the fields and behaviors required by
 * different services within the Sunbird platform. It supports:
 *
 * <ul>
 *   <li><b>LMS:</b> Telemetry support with `pdata` (Protocol Data), `channel`, `env` (Environment),
 *       and a nested context map.
 *   <li><b>UserOrg:</b> General request properties such as `appId`, `source`, and `telemetryContext`.
 *   <li><b>Notification:</b> Actor and logging specific fields like `actorId` and `loggerLevel`.
 * </ul>
 *
 * <p>This allows for a standardize way to pass request context information (headers, trace IDs,
 * user info) through the service layers.
 */
public class RequestContext {

  // -------------------------------------------------------------------------
  // Common Fields
  // -------------------------------------------------------------------------

  /** User ID (Actor). */
  private String uid;

  /** Device ID. */
  private String did;

  /** Session ID. */
  private String sid;

  /** Debug mode flag. */
  private String debugEnabled;

  /**
   * Request ID. Mapped to 'requestId' for LMS compatibility using JsonAlias.
   */
  @JsonProperty("reqId")
  @JsonAlias("requestId")
  private String reqId;

  /** Operation name. */
  private String op;

  /**
   * General context map to hold dynamic attributes.
   * In LMS scenarios, this holds the telemetry context.
   */
  private Map<String, Object> contextMap = new HashMap<>();

  // -------------------------------------------------------------------------
  // UserOrg / Notification Specific Fields
  // -------------------------------------------------------------------------

  /** Application ID. */
  private String appId;

  /** Application Version. */
  private String appVer;

  /** Source of the request. */
  private String source;

  /** Specific telemetry context map for UserOrg/Notification services. */
  private Map<String, Object> telemetryContext = new HashMap<>();

  // -------------------------------------------------------------------------
  // LMS Specific Fields
  // -------------------------------------------------------------------------

  /** Channel header value (X-Channel-Id). */
  private String channel;

  /** Environment identifier (e.g., "course", "user"). */
  private String env;

  /**
   * Protocol Data (pdata) map.
   * Contains 'id', 'pid', 'ver' for telemetry.
   */
  private Map<String, Object> pdata = new HashMap<>();

  // -------------------------------------------------------------------------
  // Notification / LMS Common Attributes
  // -------------------------------------------------------------------------

  /** ID of the actor performing the request. */
  private String actorId;

  /** Type of the actor (e.g., "Consumer", "System"). */
  private String actorType;

  /** Logging level for the request context. */
  private String loggerLevel;

  /**
   * Default constructor.
   */
  public RequestContext() {}

  /**
   * Constructor designed for UserOrg and Notification style requests.
   * Initializes general request metadata and populates the context map with these values.
   *
   * @param uid User ID
   * @param did Device ID
   * @param sid Session ID
   * @param appId Application ID
   * @param appVer Application Version
   * @param reqId Request ID
   * @param source Request Source
   * @param debugEnabled Debug flag
   * @param op Operation name
   */
  public RequestContext(
      String uid,
      String did,
      String sid,
      String appId,
      String appVer,
      String reqId,
      String source,
      String debugEnabled,
      String op) {
    this.uid = uid;
    this.did = did;
    this.sid = sid;
    this.appId = appId;
    this.appVer = appVer;
    this.reqId = reqId;
    this.source = source;
    this.debugEnabled = debugEnabled;
    this.op = op;

    // Populate contextMap as done in UserOrg/Notification patterns
    contextMap.put("uid", uid);
    contextMap.put("did", did);
    contextMap.put("sid", sid);
    contextMap.put("appId", appId);
    contextMap.put("appVer", appVer);
    contextMap.put("reqId", reqId);
    contextMap.put("source", source);
    contextMap.put("op", op);
  }

  /**
   * Constructor designed for LMS style requests, focusing on telemetry requirements.
   * Initializes parameters required for constructing the `pdata` and telemetry `contextMap`.
   *
   * @param channel Channel ID
   * @param pdataId Producer Data ID (e.g. producer ID)
   * @param env Environment identifier
   * @param did Device ID
   * @param sid Session ID
   * @param pid Producer ID
   * @param pver Producer Version
   * @param cdata Correlation Data list
   */
  public RequestContext(
      String channel,
      String pdataId,
      String env,
      String did,
      String sid,
      String pid,
      String pver,
      List<Object> cdata) {
    this.did = did;
    this.sid = sid;
    this.channel = channel;
    this.env = env;

    this.pdata.put("id", pdataId);
    this.pdata.put("pid", pid);
    this.pdata.put("ver", pver);

    this.contextMap.put("did", did);
    this.contextMap.put("sid", sid);
    this.contextMap.put("channel", channel);
    this.contextMap.put("env", env);
    this.contextMap.put("pdata", pdata);
    if (cdata != null) {
      this.contextMap.put("cdata", cdata);
    }
  }

  // -------------------------------------------------------------------------
  // Getters and Setters
  // -------------------------------------------------------------------------

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getDid() {
    return did;
  }

  public void setDid(String did) {
    this.did = did;
  }

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAppVer() {
    return appVer;
  }

  public void setAppVer(String appVer) {
    this.appVer = appVer;
  }

  /**
   * Gets the Request ID.
   * @return reqId
   */
  public String getReqId() {
    return reqId;
  }

  /**
   * Sets the Request ID.
   * @param reqId
   */
  public void setReqId(String reqId) {
    this.reqId = reqId;
  }

  /**
   * Alias for getReqId(), primarily for LMS compatibility.
   * @return reqId
   */
  public String getRequestId() {
    return reqId;
  }

  /**
   * Alias for setReqId(), primarily for LMS compatibility.
   * @param requestId
   */
  public void setRequestId(String requestId) {
    this.reqId = requestId;
  }

  public String getDebugEnabled() {
    return debugEnabled;
  }

  public void setDebugEnabled(String debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }

  public Map<String, Object> getPdata() {
    return pdata;
  }

  public void setPdata(Map<String, Object> pdata) {
    this.pdata = pdata;
  }

  public String getActorId() {
    return actorId;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
  }

  public String getActorType() {
    return actorType;
  }

  public void setActorType(String actorType) {
    this.actorType = actorType;
  }

  public String getLoggerLevel() {
    return loggerLevel;
  }

  public void setLoggerLevel(String loggerLevel) {
    this.loggerLevel = loggerLevel;
  }

  public Map<String, Object> getContextMap() {
    return contextMap;
  }

  public void setContextMap(Map<String, Object> contextMap) {
    this.contextMap = contextMap;
  }

  public Map<String, Object> getTelemetryContext() {
    return telemetryContext;
  }

  public void setTelemetryContext(Map<String, Object> telemetryContext) {
    this.telemetryContext = telemetryContext;
  }
}