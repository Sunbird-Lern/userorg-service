package org.sunbird.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the context of a telemetry event.
 * Contains information about the environment, actor, channel, and other contextual data.
 */
@JsonInclude(Include.NON_NULL)
public class Context {

  private String channel;
  private Producer pdata;
  private String env;
  private String did;
  private List<Map<String, Object>> cdata = new ArrayList<>();
  private Map<String, String> rollup = new HashMap<>();

  /**
   * Default constructor.
   */
  public Context() {}

  /**
   * Parameterized constructor to initialize the Context.
   *
   * @param channel The channel ID
   * @param env     The environment (e.g., 'dev', 'prod')
   * @param pdata   The producer data
   */
  public Context(String channel, String env, Producer pdata) {
    super();
    this.channel = channel;
    this.env = env;
    this.pdata = pdata;
  }

  /**
   * Gets the rollup data.
   *
   * @return a map containing rollup data
   */
  public Map<String, String> getRollup() {
    return rollup;
  }

  /**
   * Sets the rollup data.
   *
   * @param rollup a map containing rollup data to set
   */
  public void setRollup(Map<String, String> rollup) {
    this.rollup = rollup;
  }

  /**
   * Gets the correlation data (cdata).
   *
   * @return a list of maps containing correlation data
   */
  public List<Map<String, Object>> getCdata() {
    return cdata;
  }

  /**
   * Sets the correlation data (cdata).
   *
   * @param cdata a list of maps containing correlation data to set
   */
  public void setCdata(List<Map<String, Object>> cdata) {
    this.cdata = cdata;
  }

  /**
   * Gets the channel ID.
   *
   * @return the channel
   */
  public String getChannel() {
    return channel;
  }

  /**
   * Sets the channel ID.
   *
   * @param channel the channel to set
   */
  public void setChannel(String channel) {
    this.channel = channel;
  }

  /**
   * Gets the producer data.
   *
   * @return the pdata
   */
  public Producer getPdata() {
    return pdata;
  }

  /**
   * Sets the producer data.
   *
   * @param pdata the pdata to set
   */
  public void setPdata(Producer pdata) {
    this.pdata = pdata;
  }

  /**
   * Gets the environment.
   *
   * @return the env
   */
  public String getEnv() {
    return env;
  }

  /**
   * Sets the environment.
   *
   * @param env the env to set
   */
  public void setEnv(String env) {
    this.env = env;
  }

  /**
   * Gets the device ID.
   *
   * @return the did
   */
  public String getDid() {
    return did;
  }

  /**
   * Sets the device ID.
   *
   * @param did the did to set
   */
  public void setDid(String did) {
    this.did = did;
  }
}
