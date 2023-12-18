package org.sunbird.operations;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * This enum will contain different operation for a userorg {addCourse, getCourse, update ,
 * getContent}
 *
 * @author Manzarul
 */
public enum ActorOperations {
  CREATE_USER("createUser", "USRCRT"),
  CREATE_SSO_USER("createSSOUser", "USRCRT"),

  UPDATE_USER("updateUser", "USRUPD"),
  UPDATE_USER_V2("updateUserV2", "USRUPD"),
  UPDATE_USER_V3("updateUserV3", "USRUPD"),

  GET_USER_PROFILE_V3("getUserProfileV3", "USRRED"),
  GET_USER_PROFILE_V4("getUserProfileV4", "USRRED"),
  GET_USER_PROFILE_V5("getUserProfileV5", "USRRED"),

  UPDATE_USER_INFO_ELASTIC("updateUserInfoToElastic", "UBKGUPD"),

  GET_ROLES("getRoles", "ROLERED"),

  GET_USER_DETAILS_BY_LOGINID("getUserDetailsByLoginId", "USRRED"),
  GET_USER_BY_KEY("getUserByKey", "USRRED"),
  BLOCK_USER("blockUser", "USRBLOK"),
  BULK_UPLOAD("bulkUpload", "BLKUPLD"),
  PROCESS_BULK_UPLOAD("processBulkUpload", "BLKUPLD"),
  ASSIGN_ROLES("assignRoles", "ROLUPD"),
  ASSIGN_ROLES_V2("assignRolesV2", "ROLUPD"),
  UNBLOCK_USER("unblockUser", "USRUNBLOK"),

  UPDATE_USER_ORG_ES("updateUserOrgES", "UOBKGUPD"),
  UPDATE_USER_ROLES_ES("updateUserRoles", "UBKGROLUPD"),
  SYNC("sync", "ESSYNC"),
  BACKGROUND_SYNC("backgroundSync", "BKGESSYNC"),
  EMAIL_SERVICE("emailService", "EMAILNOTI"),
  FILE_STORAGE_SERVICE("fileStorageService", "STRGSER"),
  HEALTH_CHECK("healthCheck", "HLTHCHK"),
  SEND_MAIL("sendMail", "NOTI"),
  ACTOR("actor", "HLTHCHK"),
  CASSANDRA("cassandra", "HLTHCHK"),
  ES("es", "HLTHCHK"),
  EKSTEP("ekstep", "HLTHCHK"),

  CREATE_NOTE("createNote", "NOTECRT"),
  UPDATE_NOTE("updateNote", "NOTEUPD"),
  SEARCH_NOTE("searchNote", "NOTESER"),
  GET_NOTE("getNote", "NOTERED"),
  DELETE_NOTE("deleteNote", "NOTEDEL"),

  INSERT_USER_NOTES_ES("insertUserNotesToElastic", "NBKGCRT"),
  UPDATE_USER_NOTES_ES("updateUserNotesToElastic", "NBKGUPD"),

  CREATE_TENANT_PREFERENCE("createTenantPreference", "TPREFCRT"),
  UPDATE_TENANT_PREFERENCE("updateTenantPreference", "TPREFUPD"),
  GET_TENANT_PREFERENCE("getTenantPreference", "TPREFRED"),
  // REG_CHANNEL("channelReg", "CHNLREG"),

  UPDATE_SYSTEM_SETTINGS("updateSystemSettings", "SYSUPD"),
  GET_SYSTEM_SETTING("getSystemSetting", "SYSRED"),
  GET_ALL_SYSTEM_SETTINGS("getAllSystemSettings", "SYSRED"),
  SET_SYSTEM_SETTING("setSystemSetting", "SYSCRT"),

  USER_TNC_ACCEPT("userTnCAccept", "TNCACCPT"),

  GENERATE_OTP("generateOTP", "OTPCRT"),
  VERIFY_OTP("verifyOTP", "OTPVERFY"),
  SEND_OTP("sendOTP", "OTPNOTI"),

  GET_USER_TYPES("getUserTypes", "UTYPRED"),

  USER_TENANT_MIGRATE("userTenantMigrate", "USRMIG"),
  FREEUP_USER_IDENTITY("freeUpUserIdentity", "IDNTFREE"),
  RESET_PASSWORD("resetPassword", "PASSRST"),
  MERGE_USER("mergeUser", "USRMRG"),
  MERGE_USER_TO_ELASTIC("mergeUserToElastic", "UBKGMRG"),

  MERGE_USER_CERTIFICATE("mergeUserCertificate", "USRCRTMRG"),
  USER_SELF_DECLARED_TENANT_MIGRATE("userSelfDeclaredTenantMigrate", "USDTMIG"),
  // REJECT_MIGRATION("rejectMigration", "UMIGREJ"),

  GET_USER_FEED_BY_ID("getUserFeedById", "FEEDRED"),
  CREATE_USER_FEED("createUserFeed", "FEEDCRT"),
  DELETE_USER_FEED("deleteUserFeed", "FEEDDEL"),
  UPDATE_USER_FEED("updateUserFeed", "FEEDUPD"),

  CREATE_USER_V3("createUserV3", "USRCRT"),
  CREATE_SSU_USER("createSSUUser", "USRCRT"),
  CREATE_USER_V4("createUserV4", "USRCRT"),
  CREATE_MANAGED_USER("createManagedUser", "USRCRT"),

  V2_NOTIFICATION("v2Notification", "NOTI"),
  GET_MANAGED_USERS("getManagedUsers", "USRRED"),
  CHECK_USER_EXISTENCE("checkUserExistence", "UEXIST"),
  CHECK_USER_EXISTENCEV2("checkUserExistenceV2", "UEXIST"),
  UPDATE_USER_DECLARATIONS("updateUserDeclarations", "UDECLUPD"),
  UPDATE_USER_CONSENT("updateUserConsent", "UCNSNTUPD"),

  USER_SEARCH("userSearch", "USRSER"),
  USER_SEARCH_V2("userSearchV2", "USRSER"),
  USER_SEARCH_V3("userSearchV3", "USRSER"),
  ORG_SEARCH("orgSearch", "ORGSER"),
  ORG_SEARCH_V2("orgSearchV2", "ORGSER"),
  USER_LOOKUP("userLookup", "USRLKP"),
  GET_USER_CONSENT("getUserConsent", "UCNSTRED"),
  GET_USER_ROLES_BY_ID("getUserRolesById", "UROLERED"),
  // UserActorOperations
  INSERT_USER_ORG_DETAILS("insertUserOrgDetails", "UOBKGCRT"),
  UPDATE_USER_ORG_DETAILS("updateUserOrgDetails", "UOBKGUPD"),

  UPSERT_USER_EXTERNAL_IDENTITY_DETAILS("upsertUserExternalIdentityDetails", "UEXTIDUPSRT"),
  PROCESS_ONBOARDING_MAIL_AND_SMS("processOnBoardingMailAndSms", "WLCMNOTI"),
  PROCESS_PASSWORD_RESET_MAIL_AND_SMS("processPasswordResetMailAndSms", "PASSNOTI"),
  SAVE_USER_ATTRIBUTES("saveUserAttributes", "UATTRUPSRT"),
  UPSERT_USER_DETAILS_TO_ES("upsertUserDetailsToES", "UBKGUPSRT"),
  UPSERT_USER_ORG_DETAILS_TO_ES("upsertUserOrgDetailsToES", "UOBKGUPSRT"),
  UPSERT_USER_SELF_DECLARATIONS("upsertUserSelfDeclarations", "USDUPSRT"),
  UPDATE_USER_SELF_DECLARATIONS_ERROR_TYPE("updateUserSelfDeclarationsErrorType", "USDETUPD"),
  LOCATION_BULK_UPLOAD("locationBulkUpload", "LOCUPLD"),
  LOCATION_BULK_UPLOAD_BACKGROUND_JOB("locationBulkUploadBackground", "LBKGUPLD"),
  GET_BULK_OP_STATUS("getBulkOpStatus", "BLKSTSRED"),
  ORG_BULK_UPLOAD("orgBulkUpload", "ORGUPLD"),
  ORG_BULK_UPLOAD_BACKGROUND_JOB("orgBulkUploadBackground", "OBKGUPLD"),

  USER_BULK_UPLOAD("userBulkUpload", "USRUPLD"),
  USER_BULK_UPLOAD_BACKGROUND_JOB("userBulkUploadBackground", "UBKGUPLD"),

  CREATE_ORG("createOrg", "ORGCRT"),
  UPDATE_ORG("updateOrg", "ORGUPD"),
  UPDATE_ORG_STATUS("updateOrgStatus", "OSTSUPD"),
  GET_ORG_DETAILS("getOrgDetails", "ORGRED"),
  ASSIGN_KEYS("assignKeys", "ASSGNK"),
  UPSERT_ORGANISATION_TO_ES("upsertOrganisationDataToES", "OBKGUPSRT"),
  // Location Actor Operations
  CREATE_LOCATION("createLocation", "LOCCRT"),
  UPDATE_LOCATION("updateLocation", "LOCUPD"),
  SEARCH_LOCATION("searchLocation", "LOCSER"),
  DELETE_LOCATION("deleteLocation", "LOCDEL"),
  GET_RELATED_LOCATION_IDS("getRelatedLocationIds", "LOCRED"),
  READ_LOCATION_TYPE("readLocationType", "LOCTYPRED"),
  UPSERT_LOCATION_TO_ES("upsertLocationDataToES", "LBKGUPSRT"),
  DELETE_LOCATION_FROM_ES("deleteLocationDataFromES", "LBKGDEL"),
  ADD_ENCRYPTION_KEY("addEncryptionKey", "ADENCKEY"),
  USER_CURRENT_LOGIN("userCurrentLogin", "USRLOG"),
  DELETE_USER("deleteUser", "USRDLT"),
  USER_OWNERSHIP_TRANSFER("userOwnershipTransfer","UOWNTRANS");

  private String value;

  private String operationCode;

  public String getOperationCode() {
    return operationCode;
  }

  ActorOperations(String value, String operationCode) {
    this.value = value;
    this.operationCode = operationCode;
  }

  /**
   * returns the enum value
   *
   * @return String
   */
  public String getValue() {
    return this.value;
  }

  private static final Map<String, String> opCodeByActorOption = new HashMap<>();

  static {
    for (ActorOperations actorOperation : values()) {
      opCodeByActorOption.put(actorOperation.getValue(), actorOperation.getOperationCode());
    }
  }

  public static String getOperationCodeByActorOperation(String actorOperation) {
    String opCode = opCodeByActorOption.get(actorOperation);
    if (StringUtils.isNotBlank(opCode)) {
      return opCode;
    } else {
      return "";
    }
  }
}
