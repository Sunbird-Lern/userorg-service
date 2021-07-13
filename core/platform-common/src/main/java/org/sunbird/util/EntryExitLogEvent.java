package org.sunbird.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryExitLogEvent {
  private String eid;

  private Map<String, Object> edata = new HashMap<>();

  public String getEid() {
    return eid;
  }

  public void setEid(String eid) {
    this.eid = eid;
  }

  public Map<String, Object> getEdata() {
    return edata;
  }

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

  public void setEdataParams(List<Map<String, Object>> params) {
    this.edata.put(JsonKey.PARAMS, params);
  }

  @Override
  public String toString() {
    return "{" + "eid='" + eid + '\'' + ", edata=" + edata + '}';
  }
}
