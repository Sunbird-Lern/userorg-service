package org.sunbird.learner.actors.bulkupload.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;

/** @author arvind. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class BulkUploadProcess implements Serializable {

  private static final long serialVersionUID = 1L;
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);

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

  public String getStorageDetails() {
    return this.storageDetails;
  }

  public void setStorageDetails(String storageDetails) {
    this.storageDetails = storageDetails;
  }

  @JsonIgnore
  public void setEncryptedStorageDetails(StorageDetails cloudStorageData) {
    try {
      setStorageDetails(encryptionService.encryptData(cloudStorageData.toJsonString()));
    } catch (Exception e) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorSavingStorageDetails, null);
    }
  }

  @JsonIgnore
  public StorageDetails getDecryptedStorageDetails()
      throws JsonParseException, JsonMappingException, IOException {
    String rawData = getStorageDetails();
    if (rawData != null) {
      ObjectMapper mapper = new ObjectMapper();
      String decryptedData = decryptionService.decryptData(getStorageDetails());
      return mapper.readValue(decryptedData, StorageDetails.class);
    } else {
      return null;
    }
  }
}
