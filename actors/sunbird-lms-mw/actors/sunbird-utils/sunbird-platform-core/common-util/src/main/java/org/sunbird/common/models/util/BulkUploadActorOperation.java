package org.sunbird.common.models.util;

/** Enum to represent bulk upload operations */
public enum BulkUploadActorOperation {
  LOCATION_BULK_UPLOAD("locationBulkUpload"),
  LOCATION_BULK_UPLOAD_BACKGROUND_JOB("locationBulkUploadBackground"),

  ORG_BULK_UPLOAD("orgBulkUpload"),
  ORG_BULK_UPLOAD_BACKGROUND_JOB("orgBulkUploadBackground"),

  USER_BULK_UPLOAD("userBulkUpload"),
  USER_BULK_UPLOAD_BACKGROUND_JOB("userBulkUploadBackground"),
  USER_BULK_MIGRATION("userBulkMigration");

  private String value;

  BulkUploadActorOperation(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
