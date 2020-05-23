package org.sunbird.models.organisation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

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
  private String approvedBy;
  private String approvedDate;
  private String channel;
  private String communityId;
  private String contactDetail;
  private String createdBy;
  private String createdDate;
  private Timestamp dateTime;
  private String description;
  private String email;
  private String externalId;
  private String hashTagId;
  private String homeUrl;
  private String imgUrl;
  private Boolean isApproved;
  private Boolean isDefault;
  private Boolean isRootOrg;
  private String locationId;
  private Integer noOfMembers;
  private String orgCode;
  private String orgName;
  private String orgType;
  private String orgTypeId;
  private String parentOrgId;
  private String preferredLanguage;
  private String provider;
  private String rootOrgId;
  private String slug;
  private Integer status;
  private String theme;
  private String thumbnail;
  private String updatedBy;
  private String updatedDate;
  private List<String> locationIds;

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

  public String getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(String approvedBy) {
    this.approvedBy = approvedBy;
  }

  public String getApprovedDate() {
    return approvedDate;
  }

  public void setApprovedDate(String approvedDate) {
    this.approvedDate = approvedDate;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getCommunityId() {
    return communityId;
  }

  public void setCommunityId(String communityId) {
    this.communityId = communityId;
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

  public Timestamp getDateTime() {
    return dateTime;
  }

  public void setDateTime(Timestamp dateTime) {
    this.dateTime = dateTime;
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

  public String getHomeUrl() {
    return homeUrl;
  }

  public void setHomeUrl(String homeUrl) {
    this.homeUrl = homeUrl;
  }

  public String getImgUrl() {
    return imgUrl;
  }

  public void setImgUrl(String imgUrl) {
    this.imgUrl = imgUrl;
  }

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public Integer getNoOfMembers() {
    return noOfMembers;
  }

  public void setNoOfMembers(Integer noOfMembers) {
    this.noOfMembers = noOfMembers;
  }

  public String getOrgCode() {
    return orgCode;
  }

  public void setOrgCode(String orgCode) {
    this.orgCode = orgCode;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getOrgType() {
    return orgType;
  }

  public void setOrgType(String orgType) {
    this.orgType = orgType;
  }

  public String getOrgTypeId() {
    return orgTypeId;
  }

  public void setOrgTypeId(String orgTypeId) {
    this.orgTypeId = orgTypeId;
  }

  public String getParentOrgId() {
    return parentOrgId;
  }

  public void setParentOrgId(String parentOrgId) {
    this.parentOrgId = parentOrgId;
  }

  public String getPreferredLanguage() {
    return preferredLanguage;
  }

  public void setPreferredLanguage(String preferredLanguage) {
    this.preferredLanguage = preferredLanguage;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getRootOrgId() {
    return rootOrgId;
  }

  public void setRootOrgId(String rootOrgId) {
    this.rootOrgId = rootOrgId;
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

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
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

  public List<String> getLocationIds() {
    return locationIds;
  }

  public void setLocationIds(List<String> locationIds) {
    this.locationIds = locationIds;
  }

  @JsonProperty(value = "isApproved")
  public Boolean isApproved() {
    return isApproved;
  }

  public void setApproved(Boolean isApproved) {
    this.isApproved = isApproved;
  }

  @JsonProperty(value = "isDefault")
  public Boolean isDefault() {
    return isDefault;
  }

  public void setDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  @JsonProperty(value = "isRootOrg")
  public Boolean isRootOrg() {
    return isRootOrg;
  }

  public void setRootOrg(Boolean isRootOrg) {
    this.isRootOrg = isRootOrg;
  }
}
