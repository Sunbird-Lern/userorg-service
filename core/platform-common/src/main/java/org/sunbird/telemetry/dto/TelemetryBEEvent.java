package org.sunbird.telemetry.dto;

import java.util.HashMap;
import java.util.Map;

public class TelemetryBEEvent {

  private String eid;
  private long ets;
  private String mid;
  private String ver;
  private String channel;
  private Map<String, String> pdata;
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

  public String getVer() {
    return ver;
  }

  public void setVer(String ver) {
    this.ver = ver;
  }

  public Map<String, String> getPdata() {
    return pdata;
  }

  public void setPdata(Map<String, String> pdata) {
    this.pdata = pdata;
  }

  public Map<String, Object> getEdata() {
    return edata;
  }

  public void setEdata(Map<String, Object> eks) {
    this.edata = new HashMap<>();
    edata.put("eks", eks);
  }

  public void setPdata(String id, String pid, String ver, String uid) {
    this.pdata = new HashMap<>();
    this.pdata.put("id", id);
    this.pdata.put("pid", pid);
    this.pdata.put("ver", ver);
  }

  public void setEdata(
      String cid,
      Object status,
      Object prevState,
      Object size,
      Object pkgVersion,
      Object concepts) {
    this.edata = new HashMap<>();
    Map<String, Object> eks = new HashMap<>();
    eks.put("cid", cid);
    eks.put("state", status);
    eks.put("prevstate", prevState);
    eks.put("size", size);
    eks.put("pkgVersion", pkgVersion);
    eks.put("concepts", concepts);
    edata.put("eks", eks);
  }

  public void setEdata(String query, Object filters, Object sort, String correlationId, int size) {
    this.edata = new HashMap<>();
    Map<String, Object> eks = new HashMap<>();
    eks.put("query", query);
    eks.put("filters", filters);
    eks.put("sort", sort);
    eks.put("correlationid", correlationId);
    eks.put("size", size);
    edata.put("eks", eks);
  }

  public void setEdata(String id, Object state, Object prevState, Object lemma) {
    this.edata = new HashMap<>();
    Map<String, Object> eks = new HashMap<>();
    eks.put("id", id);
    eks.put("state", state);
    eks.put("prevstate", prevState);
    eks.put("lemma", lemma);
    edata.put("eks", eks);
  }

  public String getMid() {
    return mid;
  }

  public void setMid(String mid) {
    this.mid = mid;
  }

  public String getChannel() {
    if (null == channel) {
      channel = "";
    }
    return channel;
  }

  public void setChannel(String channel) {
    String tempChannel = channel;
    if (null == channel) {
      tempChannel = "";
    }
    this.channel = tempChannel;
  }
}
