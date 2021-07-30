package org.sunbird.model.formutil;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class FormUtilRequest implements Serializable {

  private static final long serialVersionUID = 351766241059464964L;

  @JsonProperty("type")
  private String type;

  @JsonProperty("subType")
  private String subType;

  @JsonProperty("action")
  private String action;

  @JsonProperty("component")
  private String component;

  /** No args constructor for use in serialization */
  public FormUtilRequest() {}

  public String getType() {
    return type;
  }

  public String getSubType() {
    return subType;
  }

  public String getAction() {
    return action;
  }

  public String getComponent() {
    return component;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setSubType(String subType) {
    this.subType = subType;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public void setComponent(String component) {
    this.component = component;
  }
}
