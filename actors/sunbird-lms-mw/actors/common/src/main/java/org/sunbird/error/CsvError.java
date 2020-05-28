package org.sunbird.error;

import java.util.ArrayList;
import java.util.List;

public class CsvError {

    private List<CsvRowErrorDetails> errorsList=new ArrayList<>();

    public CsvError() {
    }

    public List<CsvRowErrorDetails> getErrorsList() {
        return errorsList;
    }

    public void setErrorsList(List<CsvRowErrorDetails> errorsList) {
        this.errorsList = errorsList;
    }

    public void setError(CsvRowErrorDetails errorDetails){
        errorsList.add(errorDetails);
    }
}
