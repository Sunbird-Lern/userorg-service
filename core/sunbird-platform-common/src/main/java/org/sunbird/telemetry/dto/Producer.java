package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents the producer of a telemetry event.
 * The Producer is the system/subsystem that generated the event.
 */
@JsonInclude(Include.NON_NULL)
public class Producer {

  private String id;
  private String pid;
  private String ver;

  /**
   * Default constructor.
   */
  public Producer() {}

  /**
   * Parameterized constructor.
   *
   * @param id  The unique identifier of the producer
   * @param ver The version of the producer
   */
  public Producer(String id, String ver) {
    super();
    this.id = id;
    this.ver = ver;
  }

  /**
   * Parameterized constructor.
   *
   * @param id  The unique identifier of the producer
   * @param pid The producer ID (optional/alternative)
   * @param ver The version of the producer
   */
  public Producer(String id, String pid, String ver) {
    this.id = id;
    this.pid = pid;
    this.ver = ver;
  }

  /**
   * Gets the producer ID.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the producer ID.
   *
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the producer PID.
   *
   * @return the pid
   */
  public String getPid() {
    return pid;
  }

  /**
   * Sets the producer PID.
   *
   * @param pid the pid to set
   */
  public void setPid(String pid) {
    this.pid = pid;
  }

  /**
   * Gets the producer version.
   *
   * @return the ver
   */
  public String getVer() {
    return ver;
  }

  /**
   * Sets the producer version.
   *
   * @param ver the ver to set
   */
  public void setVer(String ver) {
    this.ver = ver;
  }
}
