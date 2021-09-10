package org.sunbird.model.bulkupload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.sql.Timestamp;

/** @author arvind. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class BulkUploadProcess implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String data;
  private String failureResult;
  private String objectType;
  private String organisationId;
  private String processEndTime;
  private String processStartTime;
  private Integer retryCount;
  private Integer status;
  private String successResult;
  private String uploadedBy;
  private String uploadedDate;
  private Integer taskCount;
  private String createdBy;
  private Timestamp createdOn;
  private Timestamp lastUpdatedOn;
  private String storageDetails;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getFailureResult() {
    return failureResult;
  }

  public void setFailureResult(String failureResult) {
    this.failureResult = failureResult;
  }

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public String getOrganisationId() {
    return organisationId;
  }

  public void setOrganisationId(String organisationId) {
    this.organisationId = organisationId;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getSuccessResult() {
    return successResult;
  }

  public void setSuccessResult(String successResult) {
    this.successResult = successResult;
  }

  public String getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(String uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public String getUploadedDate() {
    return uploadedDate;
  }

  public void setUploadedDate(String uploadedDate) {
    this.uploadedDate = uploadedDate;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Timestamp getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }

  public Timestamp getLastUpdatedOn() {
    return lastUpdatedOn;
  }

  public void setLastUpdatedOn(Timestamp lastUpdatedOn) {
    this.lastUpdatedOn = lastUpdatedOn;
  }

  public String getProcessEndTime() {
    return processEndTime;
  }

  public void setProcessEndTime(String processEndTime) {
    this.processEndTime = processEndTime;
  }

  public String getProcessStartTime() {
    return processStartTime;
  }

  public void setProcessStartTime(String processStartTime) {
    this.processStartTime = processStartTime;
  }

  public Integer getTaskCount() {
    return taskCount;
  }

  public void setTaskCount(Integer taskCount) {
    this.taskCount = taskCount;
  }
}
