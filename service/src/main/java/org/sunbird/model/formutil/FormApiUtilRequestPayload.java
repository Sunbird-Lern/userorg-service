package org.sunbird.model.formutil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.sunbird.model.adminutil.Params;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormApiUtilRequestPayload implements Serializable {
  private static final long serialVersionUID = -2362783406031347676L;

  @JsonProperty private String id;

  @JsonProperty private String ver;

  @JsonProperty private long ts;

  @JsonProperty private Params params;

  @JsonProperty private FormUtilRequest request;

  public FormApiUtilRequestPayload() {}
  /**
   * @param request
   * @param ver
   * @param id
   * @param params
   * @param ts
   */
  public FormApiUtilRequestPayload(
      String id, String ver, long ts, Params params, FormUtilRequest request) {
    super();
    this.id = id;
    this.ver = ver;
    this.ts = ts;
    this.params = params;
    this.request = request;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("ver")
  public String getVer() {
    return ver;
  }

  @JsonProperty("ver")
  public void setVer(String ver) {
    this.ver = ver;
  }

  @JsonProperty("ts")
  public long getTs() {
    return ts;
  }

  @JsonProperty("ts")
  public void setTs(long ts) {
    this.ts = ts;
  }

  @JsonProperty("params")
  public Params getParams() {
    return params;
  }

  @JsonProperty("params")
  public void setParams(Params params) {
    this.params = params;
  }

  @JsonProperty("request")
  public FormUtilRequest getRequest() {
    return request;
  }

  @JsonProperty("request")
  public void setRequest(FormUtilRequest request) {
    this.request = request;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", id)
        .append("ver", ver)
        .append("ts", ts)
        .append("params", params)
        .append("request", request)
        .toString();
  }
}
