package org.sunbird.models.role;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Role implements Serializable {
  private static final long serialVersionUID = 1L;
  private String id;
  private String name;
  private List<String> roleGroupId;
  private int status;

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

  public List<String> getRoleGroupId() {
    return roleGroupId;
  }

  public void setRoleGroupId(List<String> roleGroupId) {
    this.roleGroupId = roleGroupId;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }
}
