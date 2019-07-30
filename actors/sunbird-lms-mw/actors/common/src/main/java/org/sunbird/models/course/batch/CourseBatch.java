package org.sunbird.models.course.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ProjectUtil;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseBatch implements Serializable {

  private static final long serialVersionUID = 1L;
  private String batchId;
  private String courseCreator;
  private String courseId;
  private String createdBy;
  private String createdDate;
  private List<String> createdFor;
  private String description;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String endDate;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String enrollmentEndDate;

  private String enrollmentType;
  private String hashTagId;
  private List<String> mentors;
  private String name;
  private String startDate;
  private Integer status;
  private String updatedDate;

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

  public void setContentDetails(Map<String, Object> contentDetails, String createdBy) {
    this.setCreatedBy(createdBy);
    this.setCreatedDate(ProjectUtil.getFormattedDate());
  }

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }
}
