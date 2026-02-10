package org.sunbird.operations.lms;

/**
 * Enum representing various bulk upload operations within the LMS.
 * Includes operations for locations, organizations, and users.
 */
public enum BulkUploadActorOperation {
  LOCATION_BULK_UPLOAD("locationBulkUpload"),
  LOCATION_BULK_UPLOAD_BACKGROUND_JOB("locationBulkUploadBackground"),

  ORG_BULK_UPLOAD("orgBulkUpload"),
  ORG_BULK_UPLOAD_BACKGROUND_JOB("orgBulkUploadBackground"),

  USER_BULK_UPLOAD("userBulkUpload"),
  USER_BULK_UPLOAD_BACKGROUND_JOB("userBulkUploadBackground"),
  USER_BULK_MIGRATION("userBulkMigration");

  private final String value;

  /**
   * Constructor for BulkUploadActorOperation.
   *
   * @param value The string value associated with the operation.
   */
  BulkUploadActorOperation(String value) {
    this.value = value;
  }

  /**
   * Retrieves the string value of the operation.
   *
   * @return The operation value string.
   */
  public String getValue() {
    return this.value;
  }
}
