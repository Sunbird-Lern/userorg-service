package org.sunbird.util.user;

public enum UserActorOperations {
  INSERT_USER_ORG_DETAILS("insertUserOrgDetails"),
  UPDATE_USER_ORG_DETAILS("updateUserOrgDetails"),
  UPSERT_USER_EXTERNAL_IDENTITY_DETAILS("upsertUserExternalIdentityDetails"),
  PROCESS_ONBOARDING_MAIL_AND_SMS("processOnBoardingMailAndSms"),
  PROCESS_PASSWORD_RESET_MAIL_AND_SMS("processPasswordResetMailAndSms"),
  SAVE_USER_ATTRIBUTES("saveUserAttributes"),
  UPSERT_USER_DETAILS_TO_ES("upsertUserDetailsToES"),
  UPSERT_USER_ORG_DETAILS_TO_ES("upsertUserOrgDetailsToES"),
  UPSERT_USER_SELF_DECLARATIONS("upsertUserSelfDeclarations"),
  UPDATE_USER_SELF_DECLARATIONS_ERROR_TYPE("updateUserSelfDeclarationsErrorType");

  private String value;

  UserActorOperations(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
