/** */
package org.sunbird.common.models.util;

import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class ActorOperationTest {

  @Test
  public void testActorOperation() {
    Assert.assertEquals("enrollCourse", ActorOperations.ENROLL_COURSE.getValue());
    Assert.assertEquals("getCourse", ActorOperations.GET_COURSE.getValue());
    Assert.assertEquals("getContent", ActorOperations.GET_CONTENT.getValue());
    Assert.assertEquals("addContent", ActorOperations.ADD_CONTENT.getValue());
    Assert.assertEquals("createCourse", ActorOperations.CREATE_COURSE.getValue());
    Assert.assertEquals("updateCourse", ActorOperations.UPDATE_COURSE.getValue());
    Assert.assertEquals("publishCourse", ActorOperations.PUBLISH_COURSE.getValue());
    Assert.assertEquals("searchCourse", ActorOperations.SEARCH_COURSE.getValue());
    Assert.assertEquals("deleteCourse", ActorOperations.DELETE_COURSE.getValue());
    Assert.assertEquals("sendNotification", ActorOperations.SEND_NOTIFICATION.getValue());
    Assert.assertEquals("syncKeycloak", ActorOperations.SYNC_KEYCLOAK.getValue());
    Assert.assertEquals("updateSystemSettings", ActorOperations.UPDATE_SYSTEM_SETTINGS.getValue());
    Assert.assertEquals("deleteGeoLocation", ActorOperations.DELETE_GEO_LOCATION.getValue());
    Assert.assertEquals("getUserCount", ActorOperations.GET_USER_COUNT.getValue());
    Assert.assertEquals("updateGeoLocation", ActorOperations.UPDATE_GEO_LOCATION.getValue());
    Assert.assertEquals("getGeoLocation", ActorOperations.GET_GEO_LOCATION.getValue());
    Assert.assertEquals("registerClient", ActorOperations.REGISTER_CLIENT.getValue());
    Assert.assertEquals("updateClientKey", ActorOperations.UPDATE_CLIENT_KEY.getValue());
    Assert.assertEquals("getClientKey", ActorOperations.GET_CLIENT_KEY.getValue());
    Assert.assertEquals("createGeoLocation", ActorOperations.CREATE_GEO_LOCATION.getValue());
    Assert.assertEquals(
        "updateTenantPreference", ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("getTenantPreference", ActorOperations.GET_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("addSkill", ActorOperations.ADD_SKILL.getValue());
    Assert.assertEquals("getSkill", ActorOperations.GET_SKILL.getValue());
    Assert.assertEquals("getSkillsList", ActorOperations.GET_SKILLS_LIST.getValue());
    Assert.assertEquals("profileVisibility", ActorOperations.PROFILE_VISIBILITY.getValue());
    Assert.assertEquals(
        "createTanentPreference", ActorOperations.CREATE_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("createUser", ActorOperations.CREATE_USER.getValue());
    Assert.assertEquals("updateUser", ActorOperations.UPDATE_USER.getValue());
    Assert.assertEquals("userAuth", ActorOperations.USER_AUTH.getValue());
    Assert.assertEquals("getUserProfile", ActorOperations.GET_USER_PROFILE.getValue());
    Assert.assertEquals("createOrg", ActorOperations.CREATE_ORG.getValue());
    Assert.assertEquals("updateOrg", ActorOperations.UPDATE_ORG.getValue());
    Assert.assertEquals("updateOrgStatus", ActorOperations.UPDATE_ORG_STATUS.getValue());
    Assert.assertEquals("getOrgDetails", ActorOperations.GET_ORG_DETAILS.getValue());
    Assert.assertEquals("userAuth", ActorOperations.USER_AUTH.getValue());
    Assert.assertEquals("createPage", ActorOperations.CREATE_PAGE.getValue());
    Assert.assertEquals("updatePage", ActorOperations.UPDATE_PAGE.getValue());
    Assert.assertEquals("deletePage", ActorOperations.DELETE_PAGE.getValue());
    Assert.assertEquals("getPageSettings", ActorOperations.GET_PAGE_SETTINGS.getValue());
    Assert.assertEquals("getPageData", ActorOperations.GET_PAGE_DATA.getValue());
    Assert.assertEquals("createSection", ActorOperations.CREATE_SECTION.getValue());
    Assert.assertEquals("updateSection", ActorOperations.UPDATE_SECTION.getValue());
    Assert.assertEquals("getAllSection", ActorOperations.GET_ALL_SECTION.getValue());
    Assert.assertEquals("getSection", ActorOperations.GET_SECTION.getValue());
    Assert.assertEquals("getCourseById", ActorOperations.GET_COURSE_BY_ID.getValue());
    Assert.assertEquals("updateUserCount", ActorOperations.UPDATE_USER_COUNT.getValue());
    Assert.assertEquals(
        "getRecommendedCourses", ActorOperations.GET_RECOMMENDED_COURSES.getValue());
    Assert.assertEquals(
        "updateUserInfoToElastic", ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    Assert.assertEquals("getRoles", ActorOperations.GET_ROLES.getValue());
    Assert.assertEquals("approveOrganisation", ActorOperations.APPROVE_ORGANISATION.getValue());
    Assert.assertEquals(
        "addMemberOrganisation", ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
    Assert.assertEquals(
        "removeMemberOrganisation", ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
    Assert.assertEquals("compositeSearch", ActorOperations.COMPOSITE_SEARCH.getValue());
    Assert.assertEquals(
        "getUserDetailsByLoginId", ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue());
    Assert.assertEquals(
        "updateOrgInfoToElastic", ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue());
    Assert.assertEquals(
        "insertOrgInfoToElastic", ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue());
    Assert.assertEquals("downlaodOrg", ActorOperations.DOWNLOAD_ORGS.getValue());
    Assert.assertEquals("blockUser", ActorOperations.BLOCK_USER.getValue());
    Assert.assertEquals("deleteByIdentifier", ActorOperations.DELETE_BY_IDENTIFIER.getValue());
    Assert.assertEquals("bulkUpload", ActorOperations.BULK_UPLOAD.getValue());
    Assert.assertEquals("processBulkUpload", ActorOperations.PROCESS_BULK_UPLOAD.getValue());
    Assert.assertEquals("assignRoles", ActorOperations.ASSIGN_ROLES.getValue());
    Assert.assertEquals("unblockUser", ActorOperations.UNBLOCK_USER.getValue());
    Assert.assertEquals("createBatch", ActorOperations.CREATE_BATCH.getValue());
    Assert.assertEquals("updateBatch", ActorOperations.UPDATE_BATCH.getValue());
    Assert.assertEquals("removeBatch", ActorOperations.REMOVE_BATCH.getValue());
    Assert.assertEquals("addUserBatch", ActorOperations.ADD_USER_TO_BATCH.getValue());
    Assert.assertEquals("removeUserFromBatch", ActorOperations.REMOVE_USER_FROM_BATCH.getValue());
    Assert.assertEquals("getBatch", ActorOperations.GET_BATCH.getValue());
    Assert.assertEquals("insertCourseBatchToEs", ActorOperations.INSERT_COURSE_BATCH_ES.getValue());
    Assert.assertEquals("updateCourseBatchToEs", ActorOperations.UPDATE_COURSE_BATCH_ES.getValue());
    Assert.assertEquals("getBulkOpStatus", ActorOperations.GET_BULK_OP_STATUS.getValue());
    Assert.assertEquals("orgCreationMetrics", ActorOperations.ORG_CREATION_METRICS.getValue());
    Assert.assertEquals(
        "orgConsumptionMetrics", ActorOperations.ORG_CONSUMPTION_METRICS.getValue());
    Assert.assertEquals(
        "orgCreationMetricsData", ActorOperations.ORG_CREATION_METRICS_DATA.getValue());
    Assert.assertEquals(
        "courseProgressMetrics", ActorOperations.COURSE_PROGRESS_METRICS.getValue());
    Assert.assertEquals(
        "courseConsumptionMetrics", ActorOperations.COURSE_CREATION_METRICS.getValue());
    Assert.assertEquals("userCreationMetrics", ActorOperations.USER_CREATION_METRICS.getValue());
    Assert.assertEquals(
        "userConsumptionMetrics", ActorOperations.USER_CONSUMPTION_METRICS.getValue());
    Assert.assertEquals("getCourseBatchDetail", ActorOperations.GET_COURSE_BATCH_DETAIL.getValue());
    Assert.assertEquals("updateUserOrgES", ActorOperations.UPDATE_USER_ORG_ES.getValue());
    Assert.assertEquals("removeUserOrgES", ActorOperations.REMOVE_USER_ORG_ES.getValue());
    Assert.assertEquals("updateUserRoles", ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    Assert.assertEquals("sync", ActorOperations.SYNC.getValue());
    Assert.assertEquals(
        "insertUserCoursesInfoToElastic",
        ActorOperations.INSERT_USR_COURSES_INFO_ELASTIC.getValue());
    Assert.assertEquals(
        "updateUserCoursesInfoToElastic",
        ActorOperations.UPDATE_USR_COURSES_INFO_ELASTIC.getValue());
    Assert.assertEquals("scheduleBulkUpload", ActorOperations.SCHEDULE_BULK_UPLOAD.getValue());
    Assert.assertEquals(
        "courseProgressMetricsReport", ActorOperations.COURSE_PROGRESS_METRICS_REPORT.getValue());
    Assert.assertEquals(
        "courseConsumptionMetricsReport",
        ActorOperations.COURSE_CREATION_METRICS_REPORT.getValue());
    Assert.assertEquals(
        "orgCreationMetricsReport", ActorOperations.ORG_CREATION_METRICS_REPORT.getValue());
    Assert.assertEquals(
        "orgConsumptionMetricsReport", ActorOperations.ORG_CONSUMPTION_METRICS_REPORT.getValue());
    Assert.assertEquals("fileStorageService", ActorOperations.FILE_STORAGE_SERVICE.getValue());
    Assert.assertEquals("addUserBadgebackground", ActorOperations.ADD_USER_BADGE_BKG.getValue());
    Assert.assertEquals(
        "fileGenerationAndUpload", ActorOperations.FILE_GENERATION_AND_UPLOAD.getValue());
    Assert.assertEquals("healthCheck", ActorOperations.HEALTH_CHECK.getValue());
    Assert.assertEquals("sendMail", ActorOperations.SEND_MAIL.getValue());
    Assert.assertEquals("processData", ActorOperations.PROCESS_DATA.getValue());
    Assert.assertEquals("actor", ActorOperations.ACTOR.getValue());
    Assert.assertEquals("cassandra", ActorOperations.CASSANDRA.getValue());
    Assert.assertEquals("es", ActorOperations.ES.getValue());
    Assert.assertEquals("ekstep", ActorOperations.EKSTEP.getValue());
    Assert.assertEquals("getOrgTypeList", ActorOperations.GET_ORG_TYPE_LIST.getValue());
    Assert.assertEquals("createOrgType", ActorOperations.CREATE_ORG_TYPE.getValue());
    Assert.assertEquals("updateOrgType", ActorOperations.UPDATE_ORG_TYPE.getValue());
    Assert.assertEquals("createNote", ActorOperations.CREATE_NOTE.getValue());
    Assert.assertEquals("updateNote", ActorOperations.UPDATE_NOTE.getValue());
    Assert.assertEquals("searchNote", ActorOperations.SEARCH_NOTE.getValue());
    Assert.assertEquals("getNote", ActorOperations.GET_NOTE.getValue());
    Assert.assertEquals("deleteNote", ActorOperations.DELETE_NOTE.getValue());
    Assert.assertEquals(
        "insertUserNotesToElastic", ActorOperations.INSERT_USER_NOTES_ES.getValue());
    Assert.assertEquals("encryptUserData", ActorOperations.ENCRYPT_USER_DATA.getValue());
    Assert.assertEquals("decryptUserData", ActorOperations.DECRYPT_USER_DATA.getValue());
    Assert.assertEquals(
        "updateUserNotesToElastic", ActorOperations.UPDATE_USER_NOTES_ES.getValue());
    Assert.assertEquals("userCurrentLogin", ActorOperations.USER_CURRENT_LOGIN.getValue());
    Assert.assertEquals("getMediaTypes", ActorOperations.GET_MEDIA_TYPES.getValue());
  }
}
