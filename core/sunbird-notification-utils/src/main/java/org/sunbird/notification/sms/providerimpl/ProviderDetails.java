package org.sunbird.notification.sms.providerimpl;

import java.io.Serializable;
import java.util.List;
import org.sunbird.notification.sms.Sms;

/**
 * Data model representing the details required by the SMS provider (Msg91) to send messages.
 * Includes sender information, route, country settings, and the list of SMS objects.
 */
public class ProviderDetails implements Serializable {
  private static final long serialVersionUID = 6602089097922616775L;

  private String sender;
  private String route;
  private String country;
  private int unicode;
  private String DLT_TE_ID;
  private List<Sms> sms;

  /**
   * Initializes a new ProviderDetails instance.
   *
   * @param sender The sender identifier.
   * @param route The SMS route (e.g., transactional, promotional).
   * @param country The target country.
   * @param unicode Whether the message is unicode (0 or 1).
   * @param sms List of {@link Sms} objects to be sent.
   * @param DLT_TE_ID Distributed Ledger Technology (DLT) template ID.
   */
  public ProviderDetails(
      String sender, String route, String country, int unicode, List<Sms> sms, String DLT_TE_ID) {
    this.sender = sender;
    this.route = route;
    this.country = country;
    this.sms = sms;
    this.unicode = unicode;
    this.DLT_TE_ID = DLT_TE_ID;
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
   * Gets the sender identifier.
   *
   * @return the sender.
   */
  public String getSender() {
    return sender;
  }

  /**
   * Gets the SMS route.
   *
   * @return the route.
   */
  public String getRoute() {
    return route;
  }

  /**
   * Gets the country code.
   *
   * @return the country.
   */
  public String getCountry() {
    return country;
  }

  /**
   * Gets the list of SMS objects.
   *
   * @return list of sms.
   */
  public List<Sms> getSms() {
    return sms;
  }

  /**
   * Gets the unicode setting.
   *
   * @return the unicode flag.
   */
  public int getUnicode() {
    return unicode;
  }

  /**
   * Gets the DLT template ID.
   *
   * @return the DLT_TE_ID.
   */
  public String getDLT_TE_ID() {
    return DLT_TE_ID;
  }
}
