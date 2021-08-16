package org.sunbird.operations;

/**
 * This enum will contains different operation for a learner {addCourse, getCourse, update ,
 * getContent}
 *
 * @author Manzarul
 */
public enum ActorOperations {
  CREATE_USER("createUser"),
  CREATE_SSO_USER("createSSOUser"),
  UPDATE_USER("updateUser"),
  UPDATE_USER_V2("updateUserV2"),
  GET_USER_PROFILE_V3("getUserProfileV3"),
  GET_USER_PROFILE_V4("getUserProfileV4"),
  GET_USER_PROFILE_V5("getUserProfileV5"),
  UPDATE_USER_INFO_ELASTIC("updateUserInfoToElastic"),
  GET_ROLES("getRoles"),
  GET_USER_DETAILS_BY_LOGINID("getUserDetailsByLoginId"),
  GET_USER_BY_KEY("getUserByKey"),
  UPDATE_ORG_INFO_ELASTIC("updateOrgInfoToElastic"),
  INSERT_ORG_INFO_ELASTIC("insertOrgInfoToElastic"),
  BLOCK_USER("blockUser"),
  BULK_UPLOAD("bulkUpload"),
  PROCESS_BULK_UPLOAD("processBulkUpload"),
  ASSIGN_ROLES("assignRoles"),
  ASSIGN_ROLES_V2("assignRolesV2"),
  UNBLOCK_USER("unblockUser"),
  GET_BULK_OP_STATUS("getBulkOpStatus"),
  UPDATE_USER_ORG_ES("updateUserOrgES"),
  UPDATE_USER_ROLES_ES("updateUserRoles"),
  SYNC("sync"),
  BACKGROUND_SYNC("backgroundSync"),
  EMAIL_SERVICE("emailService"),
  FILE_STORAGE_SERVICE("fileStorageService"),
  HEALTH_CHECK("healthCheck"),
  SEND_MAIL("sendMail"),
  ACTOR("actor"),
  CASSANDRA("cassandra"),
  ES("es"),
  EKSTEP("ekstep"),
  CREATE_NOTE("createNote"),
  UPDATE_NOTE("updateNote"),
  SEARCH_NOTE("searchNote"),
  GET_NOTE("getNote"),
  DELETE_NOTE("deleteNote"),
  INSERT_USER_NOTES_ES("insertUserNotesToElastic"),
  UPDATE_USER_NOTES_ES("updateUserNotesToElastic"),
  USER_CURRENT_LOGIN("userCurrentLogin"),
  CREATE_TENANT_PREFERENCE("createTanentPreference"),
  UPDATE_TENANT_PREFERENCE("updateTenantPreference"),
  GET_TENANT_PREFERENCE("getTenantPreference"),
  UPDATE_SYSTEM_SETTINGS("updateSystemSettings"),
  REG_CHANNEL("channelReg"),
  GET_SYSTEM_SETTING("getSystemSetting"),
  GET_ALL_SYSTEM_SETTINGS("getAllSystemSettings"),
  SET_SYSTEM_SETTING("setSystemSetting"),
  USER_TNC_ACCEPT("userTnCAccept"),
  GENERATE_OTP("generateOTP"),
  VERIFY_OTP("verifyOTP"),
  SEND_OTP("sendOTP"),
  GET_USER_TYPES("getUserTypes"),
  CLEAR_CACHE("clearCache"),
  USER_TENANT_MIGRATE("userTenantMigrate"),
  FREEUP_USER_IDENTITY("freeUpUserIdentity"),
  RESET_PASSWORD("resetPassword"),
  MERGE_USER("mergeUser"),
  MERGE_USER_TO_ELASTIC("mergeUserToElastic"),

  MERGE_USER_CERTIFICATE("mergeUserCertificate"),
  MIGRATE_USER("migrateUser"),
  USER_SELF_DECLARED_TENANT_MIGRATE("userSelfDeclaredTenantMigrate"),
  REJECT_MIGRATION("rejectMigration"),
  GET_USER_FEED_BY_ID("getUserFeedById"),
  CREATE_USER_FEED("createUserFeed"),
  DELETE_USER_FEED("deleteUserFeed"),
  UPDATE_USER_FEED("updateUserFeed"),
  CREATE_USER_V3("createUserV3"),
  CREATE_SSU_USER("createSSUUser"),
  CREATE_USER_V4("createUserV4"),
  CREATE_MANAGED_USER("createManagedUser"),
  V2_NOTIFICATION("v2Notification"),
  GET_MANAGED_USERS("getManagedUsers"),
  CHECK_USER_EXISTENCEV2("checkUserExistenceV2"),
  UPDATE_USER_DECLARATIONS("updateUserDeclarations"),
  UPDATE_USER_CONSENT("updateUserConsent"),
  USER_SEARCH("userSearch"),
  USER_SEARCH_V2("userSearchV2"),
  USER_SEARCH_V3("userSearchV3"),
  ORG_SEARCH("orgSearch"),
  ORG_SEARCH_V2("orgSearchV2"),
  USER_LOOKUP("userLookup"),
  GET_USER_CONSENT("getUserConsent");

  private String value;

  /**
   * constructor
   *
   * @param value String
   */
  ActorOperations(String value) {
    this.value = value;
  }

  /**
   * returns the enum value
   *
   * @return String
   */
  public String getValue() {
    return this.value;
  }
}
