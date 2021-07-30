package org.sunbird.models.user;

public enum FeedStatus {
  READ("read"),
  UNREAD("unread");

  private String status;

  private FeedStatus(String status) {
    this.status = status;
  }

  public String getfeedStatus() {
    return status;
  }
}
