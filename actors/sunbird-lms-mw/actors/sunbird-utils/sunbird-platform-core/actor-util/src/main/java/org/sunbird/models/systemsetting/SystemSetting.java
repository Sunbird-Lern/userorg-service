package org.sunbird.models.systemsetting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SystemSetting implements Serializable {
  private static final long serialVersionUID = 1L;
  private String id;
  private String field;
  private String value;

  public SystemSetting() {}

  public SystemSetting(String id, String field, String value) {
    this.id = id;
    this.field = field;
    this.value = value;
  }

  public String getId() {
    return this.id;
  }

  public String getField() {
    return this.field;
  }

  public String getValue() {
    return this.value;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setField(String field) {
    this.field = field;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
