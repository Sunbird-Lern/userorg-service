package org.sunbird.models.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

/** @author anmolgupta Pojo class for user_feed table. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Feed implements Serializable {
  private String id;
  private String userId;
  private String category;
  private int priority;
  private String createdBy;
  private String status;
  private Map<String, Object> data;
  private String updatedBy;
  private Timestamp expireOn;
  private Timestamp updatedOn;
  private Timestamp createdOn;

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getCategory() {
    return category;
  }

  public int getPriority() {
    return priority;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getStatus() {
    return status;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public Timestamp getExpireOn() {
    return expireOn;
  }

  public Timestamp getUpdatedOn() {
    return updatedOn;
  }

  public Timestamp getCreatedOn() {
    return createdOn;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public void setExpireOn(Timestamp expireOn) {
    this.expireOn = expireOn;
  }

  public void setUpdatedOn(Timestamp updatedOn) {
    this.updatedOn = updatedOn;
  }

  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }
}
