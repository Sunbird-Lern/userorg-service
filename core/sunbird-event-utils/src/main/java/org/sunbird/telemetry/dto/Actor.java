package org.sunbird.telemetry.dto;

/**
 * Represents the 'Actor' in a telemetry event.
 * The Actor is the entity (User, System, etc.) that performs the action being logged.
 */
public class Actor {

  private String id;
  private String type;

  /**
   * Default constructor.
   */
  public Actor() {}

  /**
   * Parameterized constructor to initialize the Actor.
   *
   * @param id   The unique identifier of the actor (e.g., User ID).
   * @param type The type of the actor (e.g., 'User', 'System').
   */
  public Actor(String id, String type) {
    super();
    this.id = id;
    this.type = type;
  }

  /**
   * Gets the unique identifier of the actor.
   *
   * @return the id of the actor
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique identifier of the actor.
   *
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the type of the actor.
   *
   * @return the type of the actor
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of the actor.
   *
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }
}
