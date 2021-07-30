package org.sunbird.model.bulkupload;

public enum SelfDeclaredErrorTypeEnum {
  ERROR_DISTRICT("ERROR-DISTRICT"),
  ERROR_PHONE("ERROR-PHONE"),
  ERROR_EMAIL("ERROR-EMAIL"),
  ERROR_SCHOOL_ORG_NAME("ERROR-SCHOOL ORG NAME"),
  ERROR_SCHOOL_ORG_ID("ERROR-SCHOOL ORG ID"),
  ERROR_ID("ERROR-ID"),
  ERROR_NAME("ERROR-NAME"),
  ERROR_STATE("ERROR-STATE");

  public final String errorType;

  SelfDeclaredErrorTypeEnum(String errorType) {
    this.errorType = errorType;
  }

  public String getErrorType() {
    return this.errorType;
  }
}
