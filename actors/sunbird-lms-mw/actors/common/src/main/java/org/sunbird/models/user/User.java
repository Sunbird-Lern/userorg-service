package org.sunbird.models.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @desc POJO class for User
 * @author Amit Kumar
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class User implements Serializable {

  private static final long serialVersionUID = 7529802960267784945L;

  private String id;
  private String avatar;
  private String countryCode;
  private String createdBy;
  private String createdDate;
  private String dob;
  private String email;
  private Boolean emailVerified;
  private String firstName;
  private String gender;
  private List<String> grade;
  private Boolean isDeleted;
  private List<String> language;
  private String lastLoginTime;
  private String lastName;
  private String location;
  private String phone;
  private Boolean phoneVerified;
  private String profileSummary;
  private Map<String, String> profileVisibility;
  private String provider;
  private List<String> roles;
  private String rootOrgId;
  private Integer status;
  private List<String> subject;
  private String tcStatus;
  private String tcUpdatedAt;
  private String tempPassword;
  private String thumbnail;
  private String updatedBy;
  private String updatedDate;
  private String userId;
  private String userName;
  private List<Map<String, String>> webPages;
  private String externalId;
  private String channel;
  private String loginId;
  private String registryId;
  private String organisationId;
  private String maskedEmail;
  private String maskedPhone;
  private List<Map<String, String>> externalIds;
  private String userType;
  private Timestamp tncAcceptedOn;
  private String tncAcceptedVersion;
  private Map<String, List<String>> framework;
  private List<String> locationIds;

  public List<String> getLocationIds() {
    return locationIds;
  }

  public void setLocationIds(List<String> locationIds) {
    this.locationIds = locationIds;
  }

  public String getUserType() {
    return userType;
  }

  public void setUserType(String userType) {
    this.userType = userType;
  }

  public String getOrganisationId() {
    return organisationId;
  }

  public void setOrganisationId(String organisationId) {
    this.organisationId = organisationId;
  }

  public String getRegistryId() {
    return registryId;
  }

  public void setRegistryId(String registryId) {
    this.registryId = registryId;
  }

  public String getLoginId() {
    return loginId;
  }

  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
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

  public String getDob() {
    return dob;
  }

  public void setDob(String dob) {
    this.dob = dob;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Boolean getEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public List<String> getGrade() {
    return grade;
  }

  public void setGrade(List<String> grade) {
    this.grade = grade;
  }

  @JsonProperty(value = "isDeleted")
  public Boolean getIsDeleted() {
    return isDeleted;
  }

  public void setIsDeleted(Boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public List<String> getLanguage() {
    return language;
  }

  public void setLanguage(List<String> language) {
    this.language = language;
  }

  public String getLastLoginTime() {
    return lastLoginTime;
  }

  public void setLastLoginTime(String lastLoginTime) {
    this.lastLoginTime = lastLoginTime;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getProfileSummary() {
    return profileSummary;
  }

  public void setProfileSummary(String profileSummary) {
    this.profileSummary = profileSummary;
  }

  public Map<String, String> getProfileVisibility() {
    return profileVisibility;
  }

  public void setProfileVisibility(Map<String, String> profileVisibility) {
    this.profileVisibility = profileVisibility;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getRootOrgId() {
    return rootOrgId;
  }

  public void setRootOrgId(String rootOrgId) {
    this.rootOrgId = rootOrgId;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public List<String> getSubject() {
    return subject;
  }

  public void setSubject(List<String> subject) {
    this.subject = subject;
  }

  public String getTcStatus() {
    return tcStatus;
  }

  public void setTcStatus(String tcStatus) {
    this.tcStatus = tcStatus;
  }

  public String getTcUpdatedAt() {
    return tcUpdatedAt;
  }

  public void setTcUpdatedAt(String tcUpdatedAt) {
    this.tcUpdatedAt = tcUpdatedAt;
  }

  public String getTempPassword() {
    return tempPassword;
  }

  public void setTempPassword(String tempPassword) {
    this.tempPassword = tempPassword;
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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public List<Map<String, String>> getWebPages() {
    return webPages;
  }

  public void setWebPages(List<Map<String, String>> webPages) {
    this.webPages = webPages;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public List<Map<String, String>> getExternalIds() {
    return externalIds;
  }

  public void setExternalIds(List<Map<String, String>> externalIds) {
    this.externalIds = externalIds;
  }

  public Map<String, List<String>> getFramework() {
    return framework;
  }

  public void setFramework(Map<String, List<String>> framework) {
    this.framework = framework;
  }

  public Boolean getPhoneVerified() {
    return phoneVerified;
  }

  public void setPhoneVerified(Boolean phoneVerified) {
    this.phoneVerified = phoneVerified;
  }

  public Timestamp getTncAcceptedOn() {
    return tncAcceptedOn;
  }

  public void setTncAcceptedOn(Timestamp tncAcceptedOn) {
    this.tncAcceptedOn = tncAcceptedOn;
  }

  public String getMaskedEmail() {
    return maskedEmail;
  }

  public void setMaskedEmail(String maskedEmail) {
    this.maskedEmail = maskedEmail;
  }

  public String getMaskedPhone() {
    return maskedPhone;
  }

  public void setMaskedPhone(String maskedPhone) {
    this.maskedPhone = maskedPhone;
  }

  public String getTncAcceptedVersion() {

    return tncAcceptedVersion;
  }

  public void setTncAcceptedVersion(String tncAcceptedVersion) {
    this.tncAcceptedVersion = tncAcceptedVersion;
  }
}
