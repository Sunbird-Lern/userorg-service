package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Producer {

  private String id;
  private String pid;
  private String ver;

  public Producer() {}

  public Producer(String id, String ver) {
    super();
    this.id = id;
    this.ver = ver;
  }

  public Producer(String id, String pid, String ver) {
    this.id = id;
    this.pid = pid;
    this.ver = ver;
  }

  /** @return the id */
  public String getId() {
    return id;
  }

  /** @param id the id to set */
  public void setId(String id) {
    this.id = id;
  }

  /** @return the pid */
  public String getPid() {
    return pid;
  }

  /** @param pid the pid to set */
  public void setPid(String pid) {
    this.pid = pid;
  }

  /** @return the ver */
  public String getVer() {
    return ver;
  }

  /** @param ver the ver to set */
  public void setVer(String ver) {
    this.ver = ver;
  }
}
