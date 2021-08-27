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
  private String DLT_TE_ID;
  private List<Sms> sms;

  public ProviderDetails(String sender, String route, String country, int unicode, List<Sms> sms, String DLT_TE_ID) {
    this.sender = sender;
    this.route = route;
    this.country = country;
    this.sms = sms;
    this.unicode = unicode;
    this.DLT_TE_ID = DLT_TE_ID;
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

  /** @return the DLT_TE_ID */
  public String getDLT_TE_ID() {
    return DLT_TE_ID;
  }
}
