package org.sunbird.model.adminutil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminUtilRequest implements Serializable
{

    @JsonProperty("data")
    private List<AdminUtilRequestData> data = null;
    private final static long serialVersionUID = 8702012703305240394L;

    /**
     * No args constructor for use in serialization
     *
     */
    public AdminUtilRequest() {
    }

    /**
     *
     * @param data
     */
    public AdminUtilRequest(List<AdminUtilRequestData> data) {
        super();
        this.data = data;
    }

    @JsonProperty("data")
    public List<AdminUtilRequestData> getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(List<AdminUtilRequestData> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("data", data).toString();
    }

}
