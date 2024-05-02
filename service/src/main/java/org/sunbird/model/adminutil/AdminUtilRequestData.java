package org.sunbird.model.adminutil;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class AdminUtilRequestData implements Serializable
{

    @JsonProperty("parentId")
    private String parentId;
    @JsonProperty("sub")
    private String sub;
    private final static long serialVersionUID = 351766241059464964L;

    /**
     * No args constructor for use in serialization
     *
     */
    public AdminUtilRequestData() {
    }

    /**
     *
     * @param sub
     * @param parentId
     */
    public AdminUtilRequestData(String parentId, String sub) {
        super();
        this.parentId = parentId;
        this.sub = sub;
    }

    @JsonProperty("parentId")
    public String getParentId() {
        return parentId;
    }

    @JsonProperty("parentId")
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @JsonProperty("sub")
    public String getSub() {
        return sub;
    }

    @JsonProperty("sub")
    public void setSub(String sub) {
        this.sub = sub;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("parentId", parentId).append("sub", sub).toString();
    }
}
