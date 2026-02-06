package org.sunbird.notification.sms;

import java.io.Serializable;
import java.util.List;

/**
 * Data model for SMS notifications. Holds the message content and the list of recipient phone
 * numbers.
 */
public class Sms implements Serializable {

  private static final long serialVersionUID = -5055157442558614964L;

  private String message;
  private List<String> to;

  /**
   * Initializes a new Sms instance.
   *
   * @param message The text content of the SMS.
   * @param to The list of recipient phone numbers.
   */
  public Sms(String message, List<String> to) {
    this.message = message;
    this.to = to;
  }

  /**
   * Gets the serial version UID.
   *
   * @return the serialversionuid
   */
  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  /**
   * Gets the SMS message content.
   *
   * @return the message text.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the list of recipient phone numbers.
   *
   * @return list of phone numbers.
   */
  public List<String> getTo() {
    return to;
  }
}
