package org.sunbird.telemetry.dto;

public class Actor {

  private String id;
  private String type;

  public Actor() {}

  public Actor(String id, String type) {
    super();
    this.id = id;
    this.type = type;
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
}
