package org.sunbird.models.user;

public enum FeedAction {
  ORG_MIGRATION_ACTION("OrgMigrationAction");

  private String action;

  private FeedAction(String action) {
    this.action = action;
  }

  public String getfeedAction() {
    return action;
  }
}
