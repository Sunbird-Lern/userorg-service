package org.sunbird.models.course.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseBatch implements Serializable {

  private static final long serialVersionUID = 1L;
  private String id;
  private String countDecrementDate;
  private Boolean countDecrementStatus;
  private String countIncrementDate;
  private Boolean countIncrementStatus;
  private Map<String, String> courseAdditionalInfo;
  private String courseCreator;
  private String courseId;
  private String createdBy;
  private String createdDate;
  private List<String> createdFor;
  private String description;
  private String endDate;
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String enrollmentEndDate;
  private String enrollmentType;
  private String hashTagId;
  private List<String> mentors;
  private String name;
  private List<String> participant;
  private String startDate;
  private Integer status;
  private String updatedDate;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCountDecrementDate() {
    return countDecrementDate;
  }

  public void setCountDecrementDate(String countDecrementDate) {
    this.countDecrementDate = countDecrementDate;
  }

  public boolean isCountDecrementStatus() {
    return countDecrementStatus;
  }

  public void setCountDecrementStatus(boolean countDecrementStatus) {
    this.countDecrementStatus = countDecrementStatus;
  }

  public String getCountIncrementDate() {
    return countIncrementDate;
  }

  public void setCountIncrementDate(String countIncrementDate) {
    this.countIncrementDate = countIncrementDate;
  }

  public boolean isCountIncrementStatus() {
    return countIncrementStatus;
  }

  public void setCountIncrementStatus(boolean countIncrementStatus) {
    this.countIncrementStatus = countIncrementStatus;
  }

  public Map<String, String> getCourseAdditionalInfo() {
    return courseAdditionalInfo;
  }

  public void setCourseAdditionalInfo(Map<String, String> courseAdditionalInfo) {
    this.courseAdditionalInfo = courseAdditionalInfo;
  }

  public String getCourseCreator() {
    return courseCreator;
  }

  public void setCourseCreator(String courseCreator) {
    this.courseCreator = courseCreator;
  }

  public String getCourseId() {
    return courseId;
  }

  public void setCourseId(String courseId) {
    this.courseId = courseId;
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

  public List<String> getCreatedFor() {
    return createdFor;
  }

  public void setCreatedFor(List<String> createdFor) {
    this.createdFor = createdFor;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public String getEnrollmentType() {
    return enrollmentType;
  }

  public void setEnrollmentType(String enrollmentType) {
    this.enrollmentType = enrollmentType;
  }

  public String getEnrollmentEndDate() {
    return enrollmentEndDate;
  }

  public void setEnrollmentEndDate(String enrollmentEndDate) {
    this.enrollmentEndDate = enrollmentEndDate;
  }

  public String getHashTagId() {
    return hashTagId;
  }

  public void setHashTagId(String hashTagId) {
    this.hashTagId = hashTagId;
  }

  public List<String> getMentors() {
    return mentors;
  }

  public void setMentors(List<String> mentors) {
    this.mentors = mentors;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getParticipant() {
    return participant;
  }

  public void setParticipant(List<String> participant) {
    this.participant = participant;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(String updatedDate) {
    this.updatedDate = updatedDate;
  }

  public void initCount() {
    this.setCountDecrementStatus(false);
    this.setCountIncrementStatus(false);
  }

  public void setContentDetails(Map<String, Object> contentDetails, String createdBy) {
    this.setCourseCreator((String) contentDetails.get(JsonKey.CREATED_BY));
    this.setCourseAdditionalInfo(getAdditionalCourseInfo(contentDetails));
    this.setCreatedBy(createdBy);
    this.setCreatedDate(ProjectUtil.getFormattedDate());
  }

  private Map<String, String> getAdditionalCourseInfo(Map<String, Object> contentDetails) {

    Map<String, String> courseMap = new HashMap<>();
    courseMap.put(JsonKey.COURSE_LOGO_URL, getContentAttribute(contentDetails, JsonKey.APP_ICON));
    courseMap.put(JsonKey.COURSE_NAME, getContentAttribute(contentDetails, JsonKey.NAME));
    courseMap.put(JsonKey.DESCRIPTION, getContentAttribute(contentDetails, JsonKey.DESCRIPTION));
    courseMap.put(JsonKey.TOC_URL, getContentAttribute(contentDetails, "toc_url"));
    if (contentDetails.get(JsonKey.LEAF_NODE_COUNT) != null) {
      courseMap.put(
          JsonKey.LEAF_NODE_COUNT, (contentDetails.get(JsonKey.LEAF_NODE_COUNT)).toString());
    }
    courseMap.put(JsonKey.STATUS, (String) contentDetails.getOrDefault(JsonKey.STATUS, ""));
    return courseMap;
  }

  private String getContentAttribute(Map<String, Object> contentDetails, String key) {
    return (String) contentDetails.getOrDefault(key, "");
  }
}
