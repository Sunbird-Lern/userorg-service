package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Telemetry V3 POJO to generate telemetry event.
 * Represents the structure of a standard Sunbird telemetry event.
 */
@JsonInclude(Include.NON_NULL)
public class Telemetry {

  private String eid;
  private long ets = System.currentTimeMillis();
  private String ver = "3.0";
  private String mid = System.currentTimeMillis() + "." + UUID.randomUUID();
  private Actor actor;
  private Context context;
  private Target object;
  private Map<String, Object> edata;
  private List<String> tags;

  /**
   * Default constructor.
   */
  public Telemetry() {}

  /**
   * Parameterized constructor.
   *
   * @param eid     The event ID (e.g., AUDIT, LOG, SEARCH)
   * @param actor   The actor performing the event
   * @param context The context of the event
   * @param edata   The event data
   * @param object  The target object of the event
   */
  public Telemetry(
      String eid, Actor actor, Context context, Map<String, Object> edata, Target object) {
    super();
    this.eid = eid;
    this.actor = actor;
    this.context = context;
    this.edata = edata;
    this.object = object;
  }

  /**
   * Parameterized constructor (without target object).
   *
   * @param eid     The event ID
   * @param actor   The actor performing the event
   * @param context The context of the event
   * @param edata   The event data
   */
  public Telemetry(String eid, Actor actor, Context context, Map<String, Object> edata) {
    super();
    this.eid = eid;
    this.actor = actor;
    this.context = context;
    this.edata = edata;
  }

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
   * Gets the version.
   *
   * @return the ver
   */
  public String getVer() {
    return ver;
  }

  /**
   * Sets the version.
   *
   * @param ver the ver to set
   */
  public void setVer(String ver) {
    this.ver = ver;
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
   * Gets the actor.
   *
   * @return the actor
   */
  public Actor getActor() {
    return actor;
  }

  /**
   * Sets the actor.
   *
   * @param actor the actor to set
   */
  public void setActor(Actor actor) {
    this.actor = actor;
  }

  /**
   * Gets the context.
   *
   * @return the context
   */
  public Context getContext() {
    return context;
  }

  /**
   * Sets the context.
   *
   * @param context the context to set
   */
  public void setContext(Context context) {
    this.context = context;
  }

  /**
   * Gets the target object.
   *
   * @return the object
   */
  public Target getObject() {
    return object;
  }

  /**
   * Sets the target object.
   *
   * @param object the object to set
   */
  public void setObject(Target object) {
    this.object = object;
  }

  /**
   * Gets the event data.
   *
   * @return the edata
   */
  public Map<String, Object> getEdata() {
    return edata;
  }

  /**
   * Sets the event data.
   *
   * @param edata the edata to set
   */
  public void setEdata(Map<String, Object> edata) {
    this.edata = edata;
  }

  /**
   * Gets the tags.
   *
   * @return the tags
   */
  public List<String> getTags() {
    return tags;
  }

  /**
   * Sets the tags.
   *
   * @param tags the tags to set
   */
  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}
