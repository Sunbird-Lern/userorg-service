package org.sunbird.utils;

import java.util.List;
import java.util.Map;

/**
 * Represents a notification object containing metadata for sending messages.
 */
public class Notification {

  private List<String> ids;
  private int priority;
  private String type;
  private Map<String, Object> action;

  /**
   * Gets the list of recipient identifiers.
   *
   * @return List of user IDs or device IDs.
   */
  public List<String> getIds() {
    return ids;
  }

  /**
   * Sets the list of recipient identifiers.
   *
   * @param ids List of recipient IDs.
   */
  public void setIds(List<String> ids) {
    this.ids = ids;
  }

  /**
   * Gets the priority of the notification.
   *
   * @return Numeric priority value.
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Sets the priority of the notification.
   *
   * @param priority Numeric priority value.
   */
  public void setPriority(int priority) {
    this.priority = priority;
  }

  /**
   * Gets the type of the notification (e.g., member-update, group-delete).
   *
   * @return Notification type string.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of the notification.
   *
   * @param type Notification type identifier.
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the action metadata associated with the notification.
   *
   * @return Map containing action details.
   */
  public Map<String, Object> getAction() {
    return action;
  }

  /**
   * Sets the action metadata associated with the notification.
   *
   * @param action Map containing action details.
   */
  public void setAction(Map<String, Object> action) {
    this.action = action;
  }
}
