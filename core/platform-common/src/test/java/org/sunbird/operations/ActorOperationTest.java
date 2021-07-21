/** */
package org.sunbird.operations;

import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class ActorOperationTest {

  @Test
  public void testActorOperation() {
    Assert.assertEquals("updateSystemSettings", ActorOperations.UPDATE_SYSTEM_SETTINGS.getValue());
    Assert.assertEquals(
        "updateTenantPreference", ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("getTenantPreference", ActorOperations.GET_TENANT_PREFERENCE.getValue());
    Assert.assertEquals(
        "createTanentPreference", ActorOperations.CREATE_TENANT_PREFERENCE.getValue());
    Assert.assertEquals("createUser", ActorOperations.CREATE_USER.getValue());
    Assert.assertEquals("updateUser", ActorOperations.UPDATE_USER.getValue());
    Assert.assertEquals("createOrg", ActorOperations.CREATE_ORG.getValue());
    Assert.assertEquals("updateOrg", ActorOperations.UPDATE_ORG.getValue());
    Assert.assertEquals("updateOrgStatus", ActorOperations.UPDATE_ORG_STATUS.getValue());
    Assert.assertEquals("getOrgDetails", ActorOperations.GET_ORG_DETAILS.getValue());
    Assert.assertEquals(
        "updateUserInfoToElastic", ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    Assert.assertEquals("getRoles", ActorOperations.GET_ROLES.getValue());
    Assert.assertEquals(
        "getUserDetailsByLoginId", ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue());
    Assert.assertEquals(
        "updateOrgInfoToElastic", ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue());
    Assert.assertEquals(
        "insertOrgInfoToElastic", ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue());
    Assert.assertEquals("blockUser", ActorOperations.BLOCK_USER.getValue());
    Assert.assertEquals("bulkUpload", ActorOperations.BULK_UPLOAD.getValue());
    Assert.assertEquals("processBulkUpload", ActorOperations.PROCESS_BULK_UPLOAD.getValue());
    Assert.assertEquals("assignRoles", ActorOperations.ASSIGN_ROLES.getValue());
    Assert.assertEquals("unblockUser", ActorOperations.UNBLOCK_USER.getValue());
    Assert.assertEquals("getBulkOpStatus", ActorOperations.GET_BULK_OP_STATUS.getValue());
    Assert.assertEquals("updateUserOrgES", ActorOperations.UPDATE_USER_ORG_ES.getValue());
    Assert.assertEquals("updateUserRoles", ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    Assert.assertEquals("sync", ActorOperations.SYNC.getValue());
    Assert.assertEquals("scheduleBulkUpload", ActorOperations.SCHEDULE_BULK_UPLOAD.getValue());
    Assert.assertEquals("fileStorageService", ActorOperations.FILE_STORAGE_SERVICE.getValue());
    Assert.assertEquals("healthCheck", ActorOperations.HEALTH_CHECK.getValue());
    Assert.assertEquals("sendMail", ActorOperations.SEND_MAIL.getValue());
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
    Assert.assertEquals("userSearch", ActorOperations.USER_SEARCH.getValue());
    Assert.assertEquals("orgSearch", ActorOperations.ORG_SEARCH.getValue());
  }
}
