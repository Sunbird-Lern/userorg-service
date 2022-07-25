package org.sunbird.model.organisation;

import java.util.List;

public class OrganisationType {
    private String name;
    private String description;
    private String displayName;
    private List<String> flagNameList;
    private Integer value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getFlagNameList() {
        return flagNameList;
    }

    public void setFlagNameList(List<String> flagNameList) {
        this.flagNameList = flagNameList;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}