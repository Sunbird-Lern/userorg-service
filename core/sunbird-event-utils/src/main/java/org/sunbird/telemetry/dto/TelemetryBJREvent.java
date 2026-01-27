package org.sunbird.telemetry.dto;

import java.util.Map;

/**
 * Represents a Backend Job/Request (BJR) Telemetry Event.
 * This class captures telemetry data for background jobs or requests, often used for map-based telemetry structures.
 */
public class TelemetryBJREvent {

  private String eid;
  private long ets;
  private String mid;
  private Map<String, Object> actor;
  private Map<String, Object> context;
  private Map<String, Object> object;
  private Map<String, Object> edata;

  /**
   * Gets the event ID.
   *
   * @return the eid
   */
  public String getEid() {
    return eid;
  }

  /**
   * Sets the event ID.
   *
   * @param eid the eid to set
   */
  public void setEid(String eid) {
    this.eid = eid;
  }

  /**
   * Gets the event timestamp.
   *
   * @return the ets
   */
  public long getEts() {
    return ets;
  }

  /**
   * Sets the event timestamp.
   *
   * @param ets the ets to set
   */
  public void setEts(long ets) {
    this.ets = ets;
  }

  /**
   * Gets the message ID.
   *
   * @return the mid
   */
  public String getMid() {
    return mid;
  }

  /**
   * Sets the message ID.
   *
   * @param mid the mid to set
   */
  public void setMid(String mid) {
    this.mid = mid;
  }

  /**
   * Gets the actor data map.
   *
   * @return the actor map
   */
  public Map<String, Object> getActor() {
    return actor;
  }

  /**
   * Sets the actor data map.
   *
   * @param actor the actor map to set
   */
  public void setActor(Map<String, Object> actor) {
    this.actor = actor;
  }

  /**
   * Gets the context data map.
   *
   * @return the context map
   */
  public Map<String, Object> getContext() {
    return context;
  }

  /**
   * Sets the context data map.
   *
   * @param context the context map to set
   */
  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  /**
   * Gets the object/target data map.
   *
   * @return the object map
   */
  public Map<String, Object> getObject() {
    return object;
  }

  /**
   * Sets the object/target data map.
   *
   * @param object the object map to set
   */
  public void setObject(Map<String, Object> object) {
    this.object = object;
  }

  /**
   * Gets the event data map.
   *
   * @return the edata map
   */
  public Map<String, Object> getEdata() {
    return edata;
  }

  /**
   * Sets the event data map.
   *
   * @param edata the edata map to set
   */
  public void setEdata(Map<String, Object> edata) {
    this.edata = edata;
  }
}
