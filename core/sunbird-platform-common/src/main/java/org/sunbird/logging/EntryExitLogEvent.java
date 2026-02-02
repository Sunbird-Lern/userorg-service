package org.sunbird.logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.keys.JsonKey;

/**
 * This class represents the log event for entry and exit points in the application.
 * It holds the event ID (eid) and event data (edata).
 */
public class EntryExitLogEvent {
  private String eid;

  private Map<String, Object> edata = new HashMap<>();

  /**
   * Gets the event ID.
   *
   * @return The event ID.
   */
  public String getEid() {
    return eid;
  }

  /**
   * Sets the event ID.
   *
   * @param eid The event ID to set.
   */
  public void setEid(String eid) {
    this.eid = eid;
  }

  /**
   * Gets the event data map.
   *
   * @return A map containing the event data.
   */
  public Map<String, Object> getEdata() {
    return edata;
  }

  /**
   * Sets the event data with specific parameters.
   *
   * @param type      The type of the event.
   * @param level     The log level.
   * @param requestid The request ID associated with the event.
   * @param message   The message to log.
   * @param params    Additional parameters for the event.
   */
  public void setEdata(
      String type,
      String level,
      String requestid,
      String message,
      List<Map<String, Object>> params) {
    this.edata.put(JsonKey.TYPE, type);
    this.edata.put(JsonKey.LEVEL, level);
    this.edata.put(JsonKey.REQUEST_ID, requestid);
    this.edata.put(JsonKey.MESSAGE, message);
    this.edata.put(JsonKey.PARAMS, params);
  }

  /**
   * Sets the parameters in the event data.
   *
   * @param params A list of maps containing the parameters to set.
   */
  public void setEdataParams(List<Map<String, Object>> params) {
    this.edata.put(JsonKey.PARAMS, params);
  }

  /**
   * Returns a string representation of the EntryExitLogEvent object.
   *
   * @return A string representation of the object.
   */
  @Override
  public String toString() {
    return "{" + "eid='" + eid + '\'' + ", edata=" + edata + '}';
  }
}
