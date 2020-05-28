package org.sunbird.error;

import java.util.Comparator;

public class RowComparator implements Comparator<CsvRowErrorDetails> {
    @Override
    public int compare(CsvRowErrorDetails o1, CsvRowErrorDetails o2) {
        return o1.rowId-o2.rowId;
    }
}
