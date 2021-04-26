package org.sunbird.user.service;

public class AssociationMechanism {

  public static int SSO = 1;
  public static int SELF_DECLARATION = 2;
  public static int SYSTEM_UPLOAD = 4;
  private String associationName;
  private int associationType;

  public String getAssociationName() {
    return associationName;
  }

  public void setAssociationName(String associationName) {
    this.associationName = associationName;
  }

  public int getAssociationType() {
    return associationType;
  }

  public void setAssociationType(int associationType) {
    this.associationType = associationType;
  }

  public void appendAssociationType(int inAssociationType) {
    this.associationType = this.associationType | inAssociationType;
  }

  public void removeAssociationType(int inAssociationType) {
    this.associationType = this.associationType ^ inAssociationType;
  }

  public boolean isAssociationType(int associationType) {
    return (this.associationType & associationType) == associationType;
  }

  @Override
  public String toString() {
    return " - Association{"
        + "associationName='"
        + associationName
        + '\''
        + ", associationType="
        + associationType
        + '}';
  }
}
