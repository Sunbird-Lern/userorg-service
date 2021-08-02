package org.sunbird.actor.bulkupload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.client.org.OrganisationClient;
import org.sunbird.client.org.impl.OrganisationClientImpl;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.SelfDeclaredUser;
import org.sunbird.model.bulkupload.BulkMigrationUser;
import org.sunbird.model.bulkupload.SelfDeclaredErrorTypeEnum;
import org.sunbird.model.bulkupload.SelfDeclaredStatusEnum;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.operations.BulkUploadActorOperation;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.bulkupload.UserUploadUtil;

@ActorConfig(
  tasks = {},
  asyncTasks = {"processUserBulkSelfDeclared"}
)
public class DeclaredExternalIdActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase(
        BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED.getValue())) {
      processSelfDeclaredExternalId(request);
    } else {
      onReceiveUnsupportedOperation("userBulkMigration");
    }
  }

  private void processSelfDeclaredExternalId(Request request) {
    Response response = new Response();
    response.setResponseCode(ResponseCode.OK);
    Map requestMap = request.getRequest();
    String processId = (String) requestMap.get(JsonKey.PROCESS_ID);
    String rootOrgId = (String) requestMap.get(JsonKey.ROOT_ORG_ID);
    Map<String, Object> row =
        UserUploadUtil.getFullRecordFromProcessId(processId, request.getRequestContext());
    BulkMigrationUser bulkMigrationUser =
        UserUploadUtil.convertRowToObject(row, request.getRequestContext());
    List<SelfDeclaredUser> userList =
        UserUploadUtil.getMigrationUserAsList(bulkMigrationUser, request.getRequestContext());
    userList
        .parallelStream()
        .forEach(
            migrateUser -> {
              // add entry in usr_external_id
              // modify status to validated to user_declarations
              // call to migrate api
              migrateUser.setOrgId(rootOrgId);
              if (migrateUser.getPersona().equals(JsonKey.TEACHER_PERSONA)) {
                switch (migrateUser.getInputStatus()) {
                  case JsonKey.VALIDATED:
                    migrateDeclaredUser(request, migrateUser);
                    break;
                  case JsonKey.REJECTED:
                    rejectDeclaredDetail(request, migrateUser);
                    break;
                  case JsonKey.SELF_DECLARED_ERROR:
                    updateErrorDetail(request, migrateUser);
                    break;
                  default:
                }
              }
            });
    UserUploadUtil.updateStatusInUserBulkTable(
        bulkMigrationUser.getId(),
        ProjectUtil.BulkProcessStatus.COMPLETED.getValue(),
        request.getRequestContext());
    logger.info(
        request.getRequestContext(),
        "DeclaredExternalIdActor:processSelfDeclaredExternalId: processing the DeclaredUser of processId: "
            + bulkMigrationUser.getId()
            + "is completed");
    sender().tell(response, self());
  }

  private void updateErrorDetail(Request request, SelfDeclaredUser declaredUser) {

    Request req = new Request();
    try {
      req.setRequestContext(request.getRequestContext());
      req.setOperation("updateUserSelfDeclarationsErrorType");
      Map<String, Object> requestMap = new HashMap();
      UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
      userDeclareEntity.setOrgId(declaredUser.getOrgId());
      userDeclareEntity.setPersona(declaredUser.getPersona());
      userDeclareEntity.setUserId(declaredUser.getUserId());
      userDeclareEntity.setErrorType(declaredUser.getErrorType());
      userDeclareEntity.setStatus(declaredUser.getInputStatus());
      requestMap.put(JsonKey.DECLARATIONS, userDeclareEntity);
      req.setRequest(requestMap);
      tellToAnother(req);
    } catch (Exception e) {
      logger.error(
          req.getRequestContext(),
          "DeclaredExternalIdActor:updateErrorDetail:Exception in processing the DeclaredUser: "
              + e.getCause()
              + declaredUser.getUserId(),
          e);
    }
  }

  private void rejectDeclaredDetail(Request request, SelfDeclaredUser declaredUser) {
    Request req = new Request();
    try {
      req.setRequestContext(request.getRequestContext());
      req.setOperation("upsertUserSelfDeclarations");
      Map<String, Object> requestMap = new HashMap();
      UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
      userDeclareEntity.setOrgId(declaredUser.getOrgId());
      userDeclareEntity.setPersona(declaredUser.getPersona());
      userDeclareEntity.setUserId(declaredUser.getUserId());
      userDeclareEntity.setOperation(JsonKey.REMOVE);
      List userDeclareEntityLst = new ArrayList<UserDeclareEntity>();
      userDeclareEntityLst.add(userDeclareEntity);
      requestMap.put(JsonKey.DECLARATIONS, userDeclareEntityLst);
      req.setRequest(requestMap);
      tellToAnother(req);
    } catch (Exception e) {
      logger.error(
          req.getRequestContext(),
          "DeclaredExternalIdActor:rejectDeclaredDetail:Exception in processing the DeclaredUser: "
              + e.getCause()
              + declaredUser.getUserId(),
          e);
    }
  }

  private void migrateDeclaredUser(Request request, SelfDeclaredUser declaredUser) {
    Request req = new Request();
    try {
      req.setRequestContext(request.getRequestContext());
      req.setOperation(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE.getValue());
      if (StringUtils.isNotEmpty(declaredUser.getSubOrgExternalId())) {
        Organisation org =
            getOrgDetails(
                declaredUser.getSubOrgExternalId(),
                declaredUser.getChannel(),
                req.getRequestContext());
        if (org == null || (org != null && !org.getRootOrgId().equals(declaredUser.getOrgId()))) {
          declaredUser.setErrorType(
              SelfDeclaredErrorTypeEnum.ERROR_STATE.getErrorType().replace("_", "-"));
          declaredUser.setInputStatus(SelfDeclaredStatusEnum.ERROR.name());
          updateErrorDetail(req, declaredUser);
          return;
        }
      }
      Map<String, Object> requestMap = new HashMap();
      Map<String, String> externalIdMap = new HashMap();
      List<Map<String, String>> externalIdLst = new ArrayList();
      requestMap.put(JsonKey.USER_ID, declaredUser.getUserId());
      requestMap.put(JsonKey.CHANNEL, declaredUser.getChannel());
      requestMap.put(JsonKey.ORG_EXTERNAL_ID, declaredUser.getSubOrgExternalId());
      externalIdMap.put(JsonKey.ID, declaredUser.getUserExternalId());
      externalIdMap.put(JsonKey.ID_TYPE, declaredUser.getChannel());
      externalIdMap.put(JsonKey.PROVIDER, declaredUser.getChannel());
      externalIdLst.add(externalIdMap);
      requestMap.put(JsonKey.EXTERNAL_IDS, externalIdLst);
      req.setRequest(requestMap);
      tellToAnother(req);
    } catch (Exception e) {
      logger.error(
          req.getRequestContext(),
          "DeclaredExternalIdActor:migrateDeclaredUser:Exception in processing the DeclaredUser: "
              + e.getCause()
              + declaredUser.getUserId(),
          e);
    }
  }

  private Organisation getOrgDetails(
      String orgExternalId, String provider, RequestContext context) {
    OrganisationClient organisationClient = OrganisationClientImpl.getInstance();
    return organisationClient.esGetOrgByExternalId(orgExternalId, provider, context);
  }
}
