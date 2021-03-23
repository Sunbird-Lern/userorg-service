package org.sunbird.models.organisation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * @desc POJO class for Organisation
 * @author Amit Kumar
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Organisation implements Serializable {

  private static final long serialVersionUID = 3617862727235741692L;
  private String id;
  private String addressId;
  private String channel;
  private String contactDetail;
  private String createdBy;
  private String createdDate;
  private String description;
  private String email;
  private String externalId;
  private String hashTagId;
  private String orgName;
  private Integer organisationType;
  private String provider;
  private String rootOrgId;
  private String slug;
  private Integer status;
  private String updatedBy;
  private String updatedDate;
  private Boolean isSSOEnabled;
  private Boolean isTenant;
  private String orgLocation;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAddressId() {
    return addressId;
  }

  public void setAddressId(String addressId) {
    this.addressId = addressId;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getContactDetail() {
    return contactDetail;
  }

  public void setContactDetail(String contactDetail) {
    this.contactDetail = contactDetail;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public String getHashTagId() {
    return hashTagId;
  }

  public void setHashTagId(String hashTagId) {
    this.hashTagId = hashTagId;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public Integer getOrganisationType() {
    return organisationType;
  }

  public void setOrganisationType(Integer organisationType) {
    this.organisationType = organisationType;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public String getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(String updatedDate) {
    this.updatedDate = updatedDate;
  }

  @JsonProperty(value = "isSSOEnabled")
  public Boolean isSSOEnabled() {
    return isSSOEnabled;
  }

  public void setSSOEnabled(Boolean isSsoEnabled) {
    this.isSSOEnabled = isSsoEnabled;
  }

  @JsonProperty(value = "isTenant")
  public Boolean isTenant() {
    return isTenant;
  }

  public void setTenant(Boolean tenant) {
    isTenant = tenant;
  }

  public String getOrgLocation() {
    return orgLocation;
  }

  public void setOrgLocation(String orgLocation) {
    this.orgLocation = orgLocation;
  }

  public String getRootOrgId() {
    return rootOrgId;
  }

  public void setRootOrgId(String rootOrgId) {
    this.rootOrgId = rootOrgId;
  }
}
