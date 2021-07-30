package org.sunbird.models.role.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleGroup implements Serializable {
  private static final long serialVersionUID = 1L;
  private String id;
  private String name;
  private List<String> url_Action_Ids;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getUrlActionIds() {
    return url_Action_Ids;
  }

  public void setUrlActionIds(List<String> url_Action_Ids) {
    this.url_Action_Ids = url_Action_Ids;
  }
}
