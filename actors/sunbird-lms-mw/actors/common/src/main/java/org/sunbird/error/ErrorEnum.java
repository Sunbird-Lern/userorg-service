package org.sunbird.error;

public enum  ErrorEnum {
    invalid("invalid"),
    duplicate("duplicate"),
    missing("missing");
    private String value;
     ErrorEnum(String value) {
    this.value=value;
    }

    public String getValue() {
        return value;
    }
}
