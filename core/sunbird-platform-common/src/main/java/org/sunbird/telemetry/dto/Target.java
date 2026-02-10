package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

/**
 * Represents the target object of a telemetry event.
 * The Target identifies the object being acted upon (e.g., Content, Item, User).
 */
@JsonInclude(Include.NON_NULL)
public class Target {

  private String id;
  private String type;
  private String ver;
  private Map<String, String> rollup;

  /**
   * Default constructor.
   */
  public Target() {}

  /**
   * Parameterized constructor to initialize the Target.
   *
   * @param id   The unique identifier of the target object
   * @param type The type of the target object
   */
  public Target(String id, String type) {
    super();
    this.id = id;
    this.type = type;
  }

  /**
   * Gets the rollup data.
   *
   * @return a map containing rollup data
   */
  public Map<String, String> getRollup() {
    return rollup;
  }

  /**
   * Sets the rollup data.
   *
   * @param rollup a map containing rollup data to set
   */
  public void setRollup(Map<String, String> rollup) {
    this.rollup = rollup;
  }

  /**
   * Gets the target ID.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the target ID.
   *
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the target type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the target type.
   *
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the target version.
   *
   * @return the ver
   */
  public String getVer() {
    return ver;
  }

  /**
   * Sets the target version.
   *
   * @param ver the ver to set
   */
  public void setVer(String ver) {
    this.ver = ver;
  }
}
