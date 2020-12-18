package org.sunbird.common.models.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryExitLogEvent {
  private String eid;
  private Map<String, Object> edata;

  public String getEid() {
    return eid;
  }

  public void setEid(String eid) {
    this.eid = eid;
  }

  public void setEdata(
      String type,
      String level,
      String requestid,
      String message,
      List<Map<String, Object>> params) {
    this.edata = new HashMap<>();
    Map<String, Object> eks = new HashMap<>();
    eks.put(JsonKey.TYPE, type);
    eks.put(JsonKey.LEVEL, level);
    eks.put(JsonKey.REQUEST_ID, requestid);
    eks.put(JsonKey.MESSAGE, message);
    eks.put(JsonKey.PARAMS, params);
    edata.putAll(eks);
  }

  @Override
  public String toString() {
    return "{" + "eid='" + eid + '\'' + ", edata=" + edata + '}';
  }
}
