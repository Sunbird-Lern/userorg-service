package util;

import org.sunbird.actor.BackgroundJobManager;
import org.sunbird.actor.bulkupload.*;
import org.sunbird.actor.feed.UserFeedActor;
import org.sunbird.actor.fileuploadservice.FileUploadServiceActor;
import org.sunbird.actor.health.HealthActor;
import org.sunbird.actor.location.LocationActor;
import org.sunbird.actor.location.LocationBackgroundActor;
import org.sunbird.actor.notes.NotesManagementActor;
import org.sunbird.actor.notification.BackGroundNotificationActor;
import org.sunbird.actor.notification.EmailServiceActor;
import org.sunbird.actor.notification.SendNotificationActor;
import org.sunbird.actor.organisation.OrganisationBackgroundActor;
import org.sunbird.actor.organisation.OrganisationManagementActor;
import org.sunbird.actor.otp.OTPActor;
import org.sunbird.actor.otp.SendOTPActor;
import org.sunbird.actor.role.FetchUserRoleActor;
import org.sunbird.actor.role.UserRoleActor;
import org.sunbird.actor.role.UserRoleBackgroundActor;
import org.sunbird.actor.search.SearchHandlerActor;
import org.sunbird.actor.sync.EsSyncActor;
import org.sunbird.actor.sync.EsSyncBackgroundActor;
import org.sunbird.actor.systemsettings.SystemSettingsActor;
import org.sunbird.actor.tenantpreference.TenantPreferenceManagementActor;
import org.sunbird.actor.user.*;
import org.sunbird.actor.userconsent.UserConsentActor;
import org.sunbird.util.search.SearchTelemetryGenerator;

public enum ACTORS {
  // bulk upload actor
  BULK_UPLOAD_MANAGEMENT_ACTOR(BulkUploadManagementActor.class, "bulk_upload_management_actor"),
  LOCATION_BULK_UPLOAD_ACTOR(LocationBulkUploadActor.class, "location_bulk_upload_actor"),
  LOCATION_BULK_UPLOAD_BACKGROUND_JOB_ACTOR(
      LocationBulkUploadBackGroundJobActor.class, "location_bulk_upload_background_job_actor"),
  ORG_BULK_UPLOAD_ACTOR(OrgBulkUploadActor.class, "org_bulk_upload_actor"),
  ORG_BULK_UPLOAD_BACKGROUND_JOB_ACTOR(
      OrgBulkUploadBackgroundJobActor.class, "org_bulk_upload_background_job_actor"),
  USER_BULK_UPLOAD_ACTOR(UserBulkUploadActor.class, "user_bulk_upload_actor"),
  USER_BULK_UPLOAD_BACKGROUND_JOB_ACTOR(
      UserBulkUploadBackgroundJobActor.class, "user_bulk_upload_background_job_actor"),
  // Feed actor
  USER_FEED_ACTOR(UserFeedActor.class, "user_feed_actor"),
  // File upload Actor
  FILE_UPLOAD_SERVICE_ACTOR(FileUploadServiceActor.class, "file_upload_service_actor"),
  // Health Actor
  HEALTH_ACTOR(HealthActor.class, "health_actor"),
  // Location Actor
  LOCATION_ACTOR(LocationActor.class, "location_actor"),
  LOCATION_BACKGROUND_ACTOR(LocationBackgroundActor.class, "location_background_actor"),
  // Notes Management Actor
  NOTES_MANAGEMENT_ACTOR(NotesManagementActor.class, "notes_management_actor"),
  // Notification Actor
  EMAIL_SERVICE_ACTOR(EmailServiceActor.class, "email_service_actor"),
  SEND_NOTIFICATION_ACTOR(SendNotificationActor.class, "send_notification_actor"),
  BACKGROUND_NOTIFICATION_ACTOR(BackGroundNotificationActor.class, "background_notification_actor"),
  // Org Management Actor
  ORG_MANAGEMENT_ACTOR(OrganisationManagementActor.class, "org_management_actor"),
  ORG_BACKGROUND_ACTOR(OrganisationBackgroundActor.class, "org_background_actor"),
  // OTP Actor
  OTP_ACTOR(OTPActor.class, "otp_actor"),
  SEND_OTP_ACTOR(SendOTPActor.class, "send_otp_actor"),
  // Role Actor
  USER_ROLE_ACTOR(UserRoleActor.class, "user_role_actor"),
  FETCH_USER_ROLE_ACTOR(FetchUserRoleActor.class, "fetch_user_role_actor"),
  USER_ROLE_BACKGROUND_ACTOR(UserRoleBackgroundActor.class, "user_role_background_actor"),
  // Search Handler Actor
  SEARCH_HANDLER_ACTOR(SearchHandlerActor.class, "search_handler_actor"),
  SEARCH_TELEMETRY_ACTOR(SearchTelemetryGenerator.class, "search_telemetry_actor"),
  // ES Sync Actor
  ES_SYNC_ACTOR(EsSyncActor.class, "es_sync_actor"),
  ES_SYNC_BACKGROUND_ACTOR(EsSyncBackgroundActor.class, "es_sync_background_actor"),
  // System Setting Actor
  SYSTEM_SETTINGS_ACTOR(SystemSettingsActor.class, "system_settings_actor"),
  // Tenant preference actor
  TENANT_PREFERENCE_ACTOR(TenantPreferenceManagementActor.class, "tenant_preference_actor"),
  // User Actor
  USER_CONSENT_ACTOR(UserConsentActor.class, "user_consent_actor"),
  CHECK_USER_EXIST_ACTOR(CheckUserExistActor.class, "check_user_exist_actor"),
  IDENTIFIER_FREE_UP_ACTOR(IdentifierFreeUpActor.class, "identifier_free_up_actor"),
  MANAGED_USER_ACTOR(ManagedUserActor.class, "managed_user_actor"),
  RESET_PASSWORD_ACTOR(ResetPasswordActor.class, "reset_password_actor"),
  SSO_USER_CREATE_ACTOR(SSOUserCreateActor.class, "sso_user_create_actor"),
  SSU_USER_CREATE_ACTOR(SSUUserCreateActor.class, "ssu_user_create_actor"),
  TENANT_MIGRATION_ACTOR(TenantMigrationActor.class, "tenant_migration_actor"),
  USER_BACKGROUND_JOB_ACTOR(UserBackgroundJobActor.class, "user_background_job_actor"),
  USER_EXTERNAL_IDENTITY_MANAGEMENT_ACTOR(
      UserExternalIdManagementActor.class, "user_external_identity_management_actor"),
  USER_LOGIN_ACTOR(UserLoginActor.class, "user_login_actor"),
  USER_LOOKUP_ACTOR(UserLookupActor.class, "user_lookup_actor"),
  USER_MERGE_ACTOR(UserMergeActor.class, "user_merge_actor"),
  USER_ON_BOARDING_NOTIFICATION_ACTOR(
      UserOnboardingNotificationActor.class, "user_on_boarding_notification_actor"),
  USER_ORG_MANAGEMENT_ACTOR(UserOrgManagementActor.class, "user_org_management_actor"),
  USER_PROFILE_READ_ACTOR(UserProfileReadActor.class, "user_profile_read_actor"),
  USER_PROFILE_UPDATE_ACTOR(UserProfileUpdateActor.class, "user_profile_update_actor"),
  USER_SELF_DECLARATION_MANAGEMENT_ACTOR(
      UserSelfDeclarationManagementActor.class, "user_self_declaration_management_actor"),
  USER_STATUS_ACTOR(UserStatusActor.class, "user_status_actor"),
  USER_TELEMETRY_ACTOR(UserTelemetryActor.class, "user_telemetry_actor"),
  USER_TNC_ACTOR(UserTnCActor.class, "user_tnc_actor"),
  USER_TYPE_ACTOR(UserTypeActor.class, "user_type_actor"),
  USER_UPDATE_ACTOR(UserUpdateActor.class, "user_update_actor"),
  BACKGROUND_JOB_MANAGER_ACTOR(BackgroundJobManager.class, "background_job_manager_actor"),
  USER_DELETION_BACKGROUND_JOB_ACTOR(
      UserDeletionBackgroundJobActor.class, "user_deletion_background_job_actor"),
  USER_OWNERSHIP_TRANSFER_ACTOR(UserOwnershipTransferActor.class,"user_ownership_transfer_actor");

  ACTORS(Class clazz, String name) {
    actorClass = clazz;
    actorName = name;
  }

  private Class actorClass;
  private String actorName;

  public Class getActorClass() {
    return actorClass;
  }

  public String getActorName() {
    return actorName;
  }
}
