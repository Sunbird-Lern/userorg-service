package org.sunbird.bean;

import java.util.Arrays;
import java.util.List;
import org.sunbird.validator.user.UserBulkMigrationRequestValidator;

public class ShadowUserUpload {

  private String fileSize;
  private List<String> headers;
  private List<String> mappedHeaders;
  private byte[] fileData;
  private List<String> mandatoryFields;
  private List<String> supportedFields;
  private String processId;
  private List<MigrationUser> values;
  private List<SelfDeclaredUser> user;

  private ShadowUserUpload(ShadowUserUploadBuilder migrationBuilder) {
    this.fileSize = migrationBuilder.fileSize;
    this.headers = migrationBuilder.headers;
    this.fileData = migrationBuilder.fileData;
    this.mandatoryFields = migrationBuilder.mandatoryFields;
    this.supportedFields = migrationBuilder.supportedFields;
    this.processId = migrationBuilder.processId;
    this.values = migrationBuilder.values;
    this.user = migrationBuilder.user;
    this.mappedHeaders = migrationBuilder.mappedHeaders;
  }

  public String getFileSize() {
    return fileSize;
  }

  public List<String> getHeaders() {
    return headers;
  }

  public byte[] getFileData() {
    return fileData;
  }

  public List<String> getMandatoryFields() {
    return mandatoryFields;
  }

  public List<String> getSupportedFields() {
    return supportedFields;
  }

  public List<MigrationUser> getValues() {
    return values;
  }

  public List<SelfDeclaredUser> getUser() {
    return user;
  }

  public List<String> getMappedHeaders() {
    return mappedHeaders;
  }

  @Override
  public String toString() {
    return "ShadowUserUpload{"
        + "fileSize='"
        + fileSize
        + '\''
        + ", headers="
        + headers
        + ", mappedHeaders="
        + mappedHeaders
        + ", fileData="
        + Arrays.toString(fileData)
        + ", mandatoryFields="
        + mandatoryFields
        + ", supportedFields="
        + supportedFields
        + ", processId='"
        + processId
        + '\''
        + ", values="
        + values
        + '}';
  }

  public String getProcessId() {
    return processId;
  }

  public static class ShadowUserUploadBuilder {

    private String fileSize;
    private List<String> headers;
    private byte[] fileData;
    private List<String> mandatoryFields;
    private List<String> supportedFields;
    private String processId;
    private List<MigrationUser> values;
    private List<SelfDeclaredUser> user;
    private List<String> mappedHeaders;

    public ShadowUserUploadBuilder() {}

    public ShadowUserUploadBuilder setFileSize(String fileSize) {
      this.fileSize = fileSize;
      return this;
    }

    public ShadowUserUploadBuilder setHeaders(List<String> headers) {
      this.headers = headers;
      return this;
    }

    public ShadowUserUploadBuilder setFileData(byte[] fileData) {
      this.fileData = fileData;
      return this;
    }

    public ShadowUserUploadBuilder setMandatoryFields(List<String> mandatoryFields) {
      mandatoryFields.replaceAll(String::toLowerCase);
      this.mandatoryFields = mandatoryFields;
      return this;
    }

    public ShadowUserUploadBuilder setSupportedFields(List<String> supportedFields) {
      supportedFields.replaceAll(String::toLowerCase);
      this.supportedFields = supportedFields;
      return this;
    }

    public ShadowUserUploadBuilder setProcessId(String processId) {
      this.processId = processId;
      return this;
    }

    public ShadowUserUploadBuilder setValues(List<MigrationUser> values) {
      this.values = values;
      return this;
    }

    public ShadowUserUploadBuilder setUserValues(List<SelfDeclaredUser> values) {
      this.user = values;
      return this;
    }

    public ShadowUserUploadBuilder setMappedHeaders(List<String> mappedHeaders) {
      this.mappedHeaders = mappedHeaders;
      return this;
    }

    public ShadowUserUpload validate() {
      ShadowUserUpload migration = new ShadowUserUpload(this);
      validate(migration);
      return migration;
    }

    public ShadowUserUpload validateDeclaredUsers() {
      ShadowUserUpload migration = new ShadowUserUpload(this);
      UserBulkMigrationRequestValidator.getInstance(migration).validateDeclaredUsers();
      return migration;
    }

    private void validate(ShadowUserUpload migration) {
      UserBulkMigrationRequestValidator.getInstance(migration).validate();
    }
  }
}
