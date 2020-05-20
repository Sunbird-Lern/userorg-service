package org.sunbird.models.user.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Skill {
  private String skillName;

  private Timestamp createdOn;

  private List<HashMap<String, String>> endorsersList = null;
  private String createdBy;
  private Integer endorsementCount;
  private String id;
  private String skillNameToLowercase;
  private String userId;
  private String lastUpdatedBy;

  private Timestamp lastUpdatedOn;

  public String getSkillName() {
    return skillName;
  }

  public void setSkillName(String skillName) {
    this.skillName = skillName;
  }

  public List<HashMap<String, String>> getEndorsersList() {
    return endorsersList;
  }

  public void setEndorsersList(List<HashMap<String, String>> endorsersList) {
    this.endorsersList = endorsersList;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSkillNameToLowercase() {
    return skillNameToLowercase;
  }

  public void setSkillNameToLowercase(String skillNameToLowercase) {
    this.skillNameToLowercase = skillNameToLowercase;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Integer getEndorsementCount() {
    return endorsementCount;
  }

  public void setEndorsementCount(Integer endorsementCount) {
    this.endorsementCount = endorsementCount;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public Timestamp getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }

  public Timestamp getLastUpdatedOn() {
    return lastUpdatedOn;
  }

  public void setLastUpdatedOn(Timestamp lastUpdatedOn) {
    this.lastUpdatedOn = lastUpdatedOn;
  }
}
