package org.sunbird.telemetry.dto;

import java.util.Map;

public class TelemetryBJREvent {

  private String eid;
  private long ets;
  private String mid;
  private Map<String, Object> actor;
  private Map<String, Object> context;
  private Map<String, Object> object;
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

  public Map<String, Object> getActor() {
    return actor;
  }

  public void setActor(Map<String, Object> actor) {
    this.actor = actor;
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  public Map<String, Object> getObject() {
    return object;
  }

  public void setObject(Map<String, Object> object) {
    this.object = object;
  }

  public Map<String, Object> getEdata() {
    return edata;
  }

  public void setEdata(Map<String, Object> edata) {
    this.edata = edata;
  }
}
