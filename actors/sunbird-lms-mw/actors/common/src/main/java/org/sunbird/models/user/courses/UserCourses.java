package org.sunbird.models.user.courses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCourses implements Serializable {

  private static final long serialVersionUID = 1L;
  private String id;
  private boolean active;
  private String addedBy;
  private String batchId;
  private String contentId;
  private String courseId;
  private String courseLogoUrl;
  private String courseName;
  private String timestamp;
  private String delta;
  private String description;
  private String enrolledDate;
  private String grade;
  private String lastReadContentId;
  private int lastReadContentStatus;
  private int leafNodesCount;
  private int progress;
  private int status;
  private String tocUrl;
  private String userId;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getAddedBy() {
    return addedBy;
  }

  public void setAddedBy(String addedBy) {
    this.addedBy = addedBy;
  }

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public String getCourseId() {
    return courseId;
  }

  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  public String getCourseLogoUrl() {
    return courseLogoUrl;
  }

  public void setCourseLogoUrl(String courseLogoUrl) {
    this.courseLogoUrl = courseLogoUrl;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getDelta() {
    return delta;
  }

  public void setDelta(String delta) {
    this.delta = delta;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEnrolledDate() {
    return enrolledDate;
  }

  public void setEnrolledDate(String enrolledDate) {
    this.enrolledDate = enrolledDate;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public String getLastReadContentId() {
    return lastReadContentId;
  }

  public void setLastReadContentId(String lastReadContentId) {
    this.lastReadContentId = lastReadContentId;
  }

  public int getLastReadContentStatus() {
    return lastReadContentStatus;
  }

  public void setLastReadContentStatus(int lastReadContentStatus) {
    this.lastReadContentStatus = lastReadContentStatus;
  }

  public int getLeafNodesCount() {
    return leafNodesCount;
  }

  public void setLeafNodesCount(int leafNodesCount) {
    this.leafNodesCount = leafNodesCount;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getTocUrl() {
    return tocUrl;
  }

  public void setTocUrl(String tocUrl) {
    this.tocUrl = tocUrl;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
