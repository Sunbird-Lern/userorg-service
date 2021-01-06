/** */
package org.sunbird.common.models.util;

import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class ActorOperationTest {

  @Test
  public void testActorOperation() {
    Assert.assertEquals("sendNotification", ActorOperations.SEND_NOTIFICATION.getValue());
    Assert.assertEquals("syncKeycloak", ActorOperations.SYNC_KEYCLOAK.getValue());
    Assert.assertEquals("updateSystemSettings", ActorOperations.UPDATE_SYSTEM_SETTINGS.getValue());
    Assert.assertEquals("deleteGeoLocation", ActorOperations.DELETE_GEO_LOCATION.getValue());
    Assert.assertEquals("getUserCount", ActorOperations.GET_USER_COUNT.getValue());
    Assert.assertEquals("updateGeoLocation", ActorOperations.UPDATE_GEO_LOCATION.getValue());
    Assert.assertEquals("getGeoLocation", ActorOperations.GET_GEO_LOCATION.getValue());
    Assert.assertEquals("createGeoLocation", ActorOperations.CREATE_GEO_LOCATION.getValue());
    Assert.assertEquals(
        "updateTenantPreference", ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("getTenantPreference", ActorOperations.GET_TENANT_PREFERENCE.getValue());
    Assert.assertEquals(
        "createTanentPreference", ActorOperations.CREATE_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("createUser", ActorOperations.CREATE_USER.getValue());
    Assert.assertEquals("updateUser", ActorOperations.UPDATE_USER.getValue());
    Assert.assertEquals("userAuth", ActorOperations.USER_AUTH.getValue());
    Assert.assertEquals("createOrg", ActorOperations.CREATE_ORG.getValue());
    Assert.assertEquals("updateOrg", ActorOperations.UPDATE_ORG.getValue());
    Assert.assertEquals("updateOrgStatus", ActorOperations.UPDATE_ORG_STATUS.getValue());
    Assert.assertEquals("getOrgDetails", ActorOperations.GET_ORG_DETAILS.getValue());
    Assert.assertEquals("userAuth", ActorOperations.USER_AUTH.getValue());
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
    Assert.assertEquals("getBulkOpStatus", ActorOperations.GET_BULK_OP_STATUS.getValue());
    Assert.assertEquals("updateUserOrgES", ActorOperations.UPDATE_USER_ORG_ES.getValue());
    Assert.assertEquals("removeUserOrgES", ActorOperations.REMOVE_USER_ORG_ES.getValue());
    Assert.assertEquals("updateUserRoles", ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    Assert.assertEquals("sync", ActorOperations.SYNC.getValue());
    Assert.assertEquals("scheduleBulkUpload", ActorOperations.SCHEDULE_BULK_UPLOAD.getValue());
    Assert.assertEquals("fileStorageService", ActorOperations.FILE_STORAGE_SERVICE.getValue());
    Assert.assertEquals(
        "fileGenerationAndUpload", ActorOperations.FILE_GENERATION_AND_UPLOAD.getValue());
    Assert.assertEquals("healthCheck", ActorOperations.HEALTH_CHECK.getValue());
    Assert.assertEquals("sendMail", ActorOperations.SEND_MAIL.getValue());
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
    Assert.assertEquals(
        "updateUserNotesToElastic", ActorOperations.UPDATE_USER_NOTES_ES.getValue());
    Assert.assertEquals("userCurrentLogin", ActorOperations.USER_CURRENT_LOGIN.getValue());
  }
}
