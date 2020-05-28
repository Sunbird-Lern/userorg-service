package org.sunbird.models.certificate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Certificate implements Serializable {

    private String id;
    private String accessCode;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String userId;
    private Map<String,String>store;
    private String otherLink;
    private String oldId;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpTimesatmdAt() {
        return updatedAt;
    }

    public void setUpTimesatmdAt(Timestamp upTimesatmdAt) {
        this.updatedAt = upTimesatmdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("store")
    public Map<String, String> getStoreMap() {
        return store;
    }


    @JsonProperty("store")
    public void setStore(Map<String, String> store) {
        this.store = store;
    }

    public String getOtherLink() {
        return otherLink;
    }

    public void setOtherLink(String otherLink) {
        this.otherLink = otherLink;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "id='" + id + '\'' +
                ", accessCode='" + accessCode + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", userId='" + userId + '\'' +
                ", store=" + store +
                ", otherLink='" + otherLink + '\'' +
                '}';
    }

    public String getOldId() {
        return oldId;
    }

    public void setOldId(String oldId) {
        this.oldId = oldId;
    }
}
