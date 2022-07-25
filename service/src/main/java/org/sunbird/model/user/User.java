package org.sunbird.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @desc POJO class for User
 * @author Amit Kumar
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class User implements Serializable {

  private static final long serialVersionUID = 7529802960267784945L;

  private String id;
  private String countryCode;
  private String createdBy;
  private String createdDate;
  private String dob;
  private String email;
  private String firstName;
  private Boolean isDeleted;
  private String lastName;
  private String phone;
  private String provider;
  private List<String> roles;
  private String rootOrgId;
  private Integer status;
  private String tcStatus;
  private String tcUpdatedAt;
  private String updatedBy;
  private String updatedDate;
  private String userId;
  private String userName;
  private String externalId;
  private String channel;
  private String loginId;
  private String organisationId;
  private String maskedEmail;
  private String maskedPhone;
  private List<Map<String, String>> externalIds;
  private String userType;
  private String userSubType;
  private Timestamp tncAcceptedOn;
  private String tncAcceptedVersion;
  private Map<String, List<String>> framework;
  private List<String> locationIds;
  private String prevUsedPhone;
  private String prevUsedEmail;
  private int flagsValue;
  private String recoveryEmail;
  private String recoveryPhone;
  private String managedBy;
  private Map<String, String> allTncAccepted;
  private String profileUserType;
  private String profileLocation;
  private String profileUserTypes;
  private String profileDetails;

  public Map<String, String> getAllTncAccepted() {
    return allTncAccepted;
  }

  public void setAllTncAccepted(Map<String, String> allTncAccepted) {
    this.allTncAccepted = allTncAccepted;
  }

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

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @JsonProperty(value = "isDeleted")
  public Boolean getIsDeleted() {
    return isDeleted;
  }

  public void setIsDeleted(Boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
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

  public String getPrevUsedPhone() {
    return prevUsedPhone;
  }

  public void setPrevUsedPhone(String prevUsedPhone) {
    this.prevUsedPhone = prevUsedPhone;
  }

  public String getPrevUsedEmail() {
    return prevUsedEmail;
  }

  public void setPrevUsedEmail(String prevUsedEmail) {
    this.prevUsedEmail = prevUsedEmail;
  }

  public int getFlagsValue() {
    return flagsValue;
  }

  public void setFlagsValue(int flagsValue) {
    this.flagsValue = flagsValue;
  }

  public String getRecoveryEmail() {
    return recoveryEmail;
  }

  public void setRecoveryEmail(String recoveryEmail) {
    this.recoveryEmail = recoveryEmail;
  }

  public String getRecoveryPhone() {
    return recoveryPhone;
  }

  public void setRecoveryPhone(String recoveryPhone) {
    this.recoveryPhone = recoveryPhone;
  }

  public String getManagedBy() {
    return managedBy;
  }

  public void setManagedBy(String managedBy) {
    this.managedBy = managedBy;
  }

  public String getUserSubType() {
    return userSubType;
  }

  public void setUserSubType(String userSubType) {
    this.userSubType = userSubType;
  }

  public String getProfileUserType() {
    return profileUserType;
  }

  public void setProfileUserType(Object profileUserType) {
    if (profileUserType != null && !(profileUserType instanceof String)) {
      ObjectMapper objMap = new ObjectMapper();
      try {
        this.profileUserType = objMap.writeValueAsString(profileUserType);
      } catch (Exception e) {
        this.profileUserType = "";
      }
    } else {
      this.profileUserType = (String) profileUserType;
    }
  }

  public String getProfileUserTypes() {
    return profileUserTypes;
  }

  public void setProfileUserTypes(Object profileUserTypes) {
    if (profileUserTypes != null && !(profileUserTypes instanceof String)) {
      ObjectMapper objMap = new ObjectMapper();
      try {
        this.profileUserTypes = objMap.writeValueAsString(profileUserTypes);
      } catch (Exception e) {
        this.profileUserTypes = "";
      }
    } else {
      this.profileUserTypes = (String) profileUserTypes;
    }
  }

  public String getProfileLocation() {
    return profileLocation;
  }

  public void setProfileLocation(Object profileLocation) {
    if (profileLocation != null && !(profileLocation instanceof String)) {
      ObjectMapper objMap = new ObjectMapper();
      try {
        this.profileLocation = objMap.writeValueAsString(profileLocation);
      } catch (Exception e) {
        this.profileLocation = "";
      }
    } else {
      this.profileLocation = (String) profileLocation;
    }
  }

  public String getProfileDetails() {
    return profileDetails;
  }

  public void setProfileDetails(Object profileDetails) {
    if(profileDetails != null && !(profileDetails instanceof String)) {
      try {
        this.profileDetails = new ObjectMapper().writeValueAsString(profileDetails);
      } catch (Exception e) {
        this.profileDetails = "";
      }
    } else {
      this.profileDetails = (String) profileDetails;
    }
  }
}
