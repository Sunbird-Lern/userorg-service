package org.sunbird.error;

public class CsvRowErrorDetails  {

    int rowId;
    private String header;
    private ErrorEnum errorEnum;

    public CsvRowErrorDetails(int rowId, String header, ErrorEnum errorEnum) {
        this.rowId = rowId;
        this.header = header;
        this.errorEnum = errorEnum;
    }

    public CsvRowErrorDetails() {
    }

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public ErrorEnum getErrorEnum() {
        return errorEnum;
    }

    public void setErrorEnum(ErrorEnum errorEnum) {
        this.errorEnum = errorEnum;
    }

    @Override
    public String toString() {
        return "ErrorDetails{" +
                "rowId=" + rowId +
                ", header='" + header + '\'' +
                ", errorEnum=" + errorEnum +
                '}';
    }
}
