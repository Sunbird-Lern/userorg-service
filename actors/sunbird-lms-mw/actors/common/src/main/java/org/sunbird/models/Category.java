package org.sunbird.models;


/**
 * @author anmolgupta
 * this class will be used to define the Category for the feeds
 */
public enum  Category {

    USER_EXTERNAL_ID_VALIDATION("USER_EXTERNAL_ID_VALIDATION");
    private String value;
    Category(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
