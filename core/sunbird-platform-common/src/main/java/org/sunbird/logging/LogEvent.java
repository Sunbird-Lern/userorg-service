package org.sunbird.logging;

import java.util.HashMap;
import java.util.Map;
import org.sunbird.keys.JsonKey;

/**
 * LogEvent class to represent the structure of API request, response, and error logs.
 * Used for constructing structured log messages.
 */
public class LogEvent {

  private String eid;
  private long ets;
  private String mid;
  private String ver;
  private Map<String, Object> context;
  private Map<String, Object> edata;

  public String getEid() {
    return eid;
  }

  public void setEid(String eid) {
    this.eid = eid;
  }

  public long getEts() {
    return ets;
  }

  public void setEts(long ets) {
    this.ets = ets;
  }

  public String getMid() {
    return mid;
  }

  public void setMid(String mid) {
    this.mid = mid;
  }

  public String getVer() {
    return ver;
  }

  public void setVer(String ver) {
    this.ver = ver;
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  public Map<String, Object> getEdata() {
    return edata;
  }

  public void setEdata(Map<String, Object> eks) {
    this.edata = new HashMap<String, Object>();
    edata.put(JsonKey.EKS, eks);
  }

  public void setContext(String id, String ver) {
    this.context = new HashMap<String, Object>();
    Map<String, String> pdata = new HashMap<String, String>();
    pdata.put(JsonKey.ID, id);
    pdata.put(JsonKey.VER, ver);
    this.context.put(JsonKey.PDATA, pdata);
  }

  /**
   * Sets the error data for the log event.
   *
   * @param level Log level (e.g., INFO, ERROR).
   * @param className The name of the class where the event occurred.
   * @param method The method name where the event occurred.
   * @param data Additional data related to the event.
   * @param stackTrace Stack trace if an exception occurred.
   * @param exception The exception object.
   */
  public void setEdata(
      String level,
      String className,
      String method,
      Object data,
      Object stackTrace,
      Object exception) {
    this.edata = new HashMap<String, Object>();
    Map<String, Object> eks = new HashMap<String, Object>();
    eks.put(JsonKey.LEVEL, level);
    eks.put(JsonKey.CLASS, className);
    eks.put(JsonKey.METHOD, method);
    eks.put(JsonKey.DATA, data);
    eks.put(JsonKey.STACKTRACE, stackTrace);
    edata.put(JsonKey.EKS, eks);
  }
}
