package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class Target {

  private String id;
  private String type;
  private String ver;
  private Map<String, String> rollup;

  public Target() {}

  public Target(String id, String type) {
    super();
    this.id = id;
    this.type = type;
  }

  public Map<String, String> getRollup() {
    return rollup;
  }

  public void setRollup(Map<String, String> rollup) {
    this.rollup = rollup;
  }

  /** @return the id */
  public String getId() {
    return id;
  }

  /** @param id the id to set */
  public void setId(String id) {
    this.id = id;
  }

  /** @return the type */
  public String getType() {
    return type;
  }

  /** @param type the type to set */
  public void setType(String type) {
    this.type = type;
  }

  /** @return the ver */
  public String getVer() {
    return ver;
  }

  /** @param ver the ver to set */
  public void setVer(String ver) {
    this.ver = ver;
  }
}
