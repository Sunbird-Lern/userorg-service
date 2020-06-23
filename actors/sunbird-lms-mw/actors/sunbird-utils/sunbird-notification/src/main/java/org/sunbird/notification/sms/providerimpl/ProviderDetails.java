package org.sunbird.notification.sms.providerimpl;

import java.io.Serializable;
import java.util.List;
import org.sunbird.notification.sms.Sms;

/** @author Manzarul */
public class ProviderDetails implements Serializable {
  /** */
  private static final long serialVersionUID = 6602089097922616775L;

  private String sender;
  private String route;
  private String country;
  private int unicode;
  private List<Sms> sms;

  public ProviderDetails(String sender, String route, String country, int unicode, List<Sms> sms) {
    this.sender = sender;
    this.route = route;
    this.country = country;
    this.sms = sms;
    this.unicode = unicode;
  }

  /** @return the serialversionuid */
  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  /** @return the sender */
  public String getSender() {
    return sender;
  }

  /** @return the route */
  public String getRoute() {
    return route;
  }

  /** @return the country */
  public String getCountry() {
    return country;
  }

  /** @return the sms */
  public List<Sms> getSms() {
    return sms;
  }

  /** @return the unicode */
  public int getUnicode() {
    return unicode;
  }
}
