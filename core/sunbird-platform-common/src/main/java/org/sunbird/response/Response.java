package org.sunbird.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.response.ResponseCode;

/**
 * A standardized response class used across all layers of the application.
 * It encapsulates the result of an API request, including status codes,
 * timestamps, versioning, and the actual data payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response implements Serializable, Cloneable {

  private static final long serialVersionUID = -3773253896160786443L;
  
  /** Unique identifier for the response. */
  protected String id;
  
  /** API version. */
  protected String ver;
  
  /** Timestamp of the response generation. */
  protected String ts;
  
  /** Additional response parameters (e.g., status, err, errmsg). */
  protected ResponseParams params;
  
  /** The response code (e.g., OK, CLIENT_ERROR). Defaults to OK. */
  protected ResponseCode responseCode = ResponseCode.OK;
  
  /** The map containing the actual result data. */
  protected Map<String, Object> result = new HashMap<>();

  /**
   * Gets the unique response ID.
   *
   * @return The response ID string.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique response ID.
   *
   * @param id The response ID string.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the API version.
   *
   * @return The API version string.
   */
  public String getVer() {
    return ver;
  }

  /**
   * Sets the API version.
   *
   * @param ver The API version string.
   */
  public void setVer(String ver) {
    this.ver = ver;
  }

  /**
   * Gets the timestamp.
   *
   * @return The timestamp string.
   */
  public String getTs() {
    return ts;
  }

  /**
   * Sets the timestamp.
   *
   * @param ts The timestamp string.
   */
  public void setTs(String ts) {
    this.ts = ts;
  }

  /**
   * Gets the result map containing the data payload.
   *
   * @return A map of result objects.
   */
  public Map<String, Object> getResult() {
    return result;
  }

  /**
   * Retrieves a specific value from the result map.
   *
   * @param key The key to look up.
   * @return The object associated with the key, or null if not found.
   */
  public Object get(String key) {
    return result.get(key);
  }

  /**
   * Adds a key-value pair to the result map.
   *
   * @param key The key for the data.
   * @param vo The value object.
   */
  public void put(String key, Object vo) {
    result.put(key, vo);
  }

  /**
   * Adds all entries from the provided map to the result map.
   *
   * @param map The map of entries to add.
   */
  public void putAll(Map<String, Object> map) {
    result.putAll(map);
  }

  /**
   * Checks if the result map contains a specific key.
   *
   * @param key The key to check.
   * @return True if the key exists, false otherwise.
   */
  public boolean containsKey(String key) {
    return result.containsKey(key);
  }

  /**
   * Gets the response parameters object.
   *
   * @return The ResponseParams object.
   */
  public ResponseParams getParams() {
    return params;
  }

  /**
   * Sets the response parameters object.
   *
   * @param params The ResponseParams object to set.
   */
  public void setParams(ResponseParams params) {
    this.params = params;
  }

  /**
   * Sets the response code.
   *
   * @param code The ResponseCode enum.
   */
  public void setResponseCode(ResponseCode code) {
    this.responseCode = code;
  }

  /**
   * Gets the response code.
   *
   * @return The ResponseCode enum.
   */
  public ResponseCode getResponseCode() {
    return this.responseCode;
  }

  /**
   * Creates a shallow copy of the response object.
   *
   * @param response The response object to clone.
   * @return A cloned Response object, or null if cloning fails.
   */
  public Response clone(Response response) {
    try {
      return (Response) response.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
}
