package org.sunbird.logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.keys.JsonKey;

/**
 * This class represents the log event structure for entry and exit logs.
 */
public class EntryExitLogEvent {

  private String eid;
  private Map<String, Object> edata = new HashMap<>();

  /**
   * Gets the event ID.
   *
   * @return the event ID
   */
  public String getEid() {
    return eid;
  }

  /**
   * Sets the event ID.
   *
   * @param eid the event ID to set
   */
  public void setEid(String eid) {
    this.eid = eid;
  }

  /**
   * Gets the event data.
   *
   * @return the event data map
   */
  public Map<String, Object> getEdata() {
    return edata;
  }

  /**
   * Sets the event data details.
   *
   * @param type the type of the event
   * @param level the log level
   * @param requestid the request ID
   * @param message the log message
   * @param params the list of parameters associated with the request
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
   * @param params the list of parameters to set in edata
   */
  public void setEdataParams(List<Map<String, Object>> params) {
    this.edata.put(JsonKey.PARAMS, params);
  }

  @Override
  public String toString() {
    return "{" + "eid='" + eid + '\'' + ", edata=" + edata + '}';
  }
}
