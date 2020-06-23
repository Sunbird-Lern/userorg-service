/** */
package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class Context {

  private String channel;
  private Producer pdata;
  private String env;
  private String did;
  private List<Map<String, Object>> cdata = new ArrayList<>();
  private Map<String, String> rollup = new HashMap<>();

  public Context() {}

  public Context(String channel, String env, Producer pdata) {
    super();
    this.channel = channel;
    this.env = env;
    this.pdata = pdata;
  }

  public Map<String, String> getRollup() {
    return rollup;
  }

  public void setRollup(Map<String, String> rollup) {
    this.rollup = rollup;
  }

  public List<Map<String, Object>> getCdata() {
    return cdata;
  }

  public void setCdata(List<Map<String, Object>> cdata) {
    this.cdata = cdata;
  }

  /** @return the channel */
  public String getChannel() {
    return channel;
  }

  /** @param channel the channel to set */
  public void setChannel(String channel) {
    this.channel = channel;
  }

  /** @return the pdata */
  public Producer getPdata() {
    return pdata;
  }

  /** @param pdata the pdata to set */
  public void setPdata(Producer pdata) {
    this.pdata = pdata;
  }

  /** @return the env */
  public String getEnv() {
    return env;
  }

  /** @param env the env to set */
  public void setEnv(String env) {
    this.env = env;
  }

  /** @return the did */
  public String getDid() {
    return did;
  }

  /** @param did the did to set */
  public void setDid(String did) {
    this.did = did;
  }
}
