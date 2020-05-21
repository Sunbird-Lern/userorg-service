package org.sunbird.user.util;

public enum UserActorOperations {
  INSERT_USER_ADDRESS("insertUserAddress"),
  UPDATE_USER_ADDRESS("updateUserAddress"),
  INSERT_USER_EDUCATION("insertUserEducation"),
  UPDATE_USER_EDUCATION("updateUserEducation"),
  INSERT_USER_JOB_PROFILE("insertUserJobProfile"),
  UPDATE_USER_JOB_PROFILE("updateUserJobProfile"),
  INSERT_USER_ORG_DETAILS("insertUserOrgDetails"),
  UPDATE_USER_ORG_DETAILS("updateUserOrgDetails"),
  UPSERT_USER_EXTERNAL_IDENTITY_DETAILS("upsertUserExternalIdentityDetails"),
  PROCESS_ONBOARDING_MAIL_AND_SMS("processOnBoardingMailAndSms"),
  SAVE_USER_ATTRIBUTES("saveUserAttributes"),
  UPSERT_USER_DETAILS_TO_ES("upsertUserDetailsToES"),
  UPSERT_USER_ADDRESS_TO_ES("upsertUserAddressToES"),
  UPSERT_USER_EDUCATION_TO_ES("upsertUserEducationToES"),
  UPSERT_USER_JOB_PROFILE_TO_ES("upsertUserJobProfileToES"),
  UPSERT_USER_ORG_DETAILS_TO_ES("upsertUserOrgDetailsToES");

  private String value;

  UserActorOperations(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
