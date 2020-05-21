package org.sunbird.learner.actors.bulkupload.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StorageDetails {

  private String storageType;
  private String container;
  private String fileName;

  public StorageDetails(String storageType, String container, String fileName) {
    super();
    this.storageType = storageType;
    this.container = container;
    this.fileName = fileName;
  }

  public StorageDetails() {}

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(String storageType) {
    this.storageType = storageType;
  }

  public String getContainer() {
    return container;
  }

  public void setContainer(String container) {
    this.container = container;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String toJsonString() throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(this);
  }
}
