package org.sunbird.notification.sms;

import java.io.Serializable;
import java.util.List;

/** @author Manzarul */
public class Sms implements Serializable {

  /** */
  private static final long serialVersionUID = -5055157442558614964L;

  private String message;
  private List<String> to;

  public Sms(String message, List<String> to) {
    this.message = message;
    this.to = to;
  }

  /** @return the serialversionuid */
  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  /** @return the message */
  public String getMessage() {
    return message;
  }

  /** @return the to */
  public List<String> getTo() {
    return to;
  }
}
