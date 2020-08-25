package org.sunbird.learner.actors.bulkupload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.bean.SelfDeclaredUser;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.model.BulkMigrationUser;
import org.sunbird.learner.actors.bulkupload.util.UserUploadUtil;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.UserDeclareEntity;

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
    Map<String, Object> row = UserUploadUtil.getFullRecordFromProcessId(processId);
    BulkMigrationUser bulkMigrationUser = UserUploadUtil.convertRowToObject(row);
    List<SelfDeclaredUser> userList = UserUploadUtil.getMigrationUserAsList(bulkMigrationUser);
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
        bulkMigrationUser.getId(), ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    ProjectLogger.log(
        "DeclaredExternalIdActor:processSelfDeclaredExternalId: processing the DeclaredUser of processId: "
            + bulkMigrationUser.getId()
            + "is completed",
        LoggerEnum.INFO.name());
    sender().tell(response, self());
  }

  private void updateErrorDetail(Request request, SelfDeclaredUser declaredUser) {
    try {
      request.setOperation("updateUserSelfDeclarationsErrorType");
      Map<String, Object> requestMap = new HashMap();
      UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
      userDeclareEntity.setOrgId(declaredUser.getOrgId());
      userDeclareEntity.setPersona(declaredUser.getPersona());
      userDeclareEntity.setUserId(declaredUser.getUserId());
      userDeclareEntity.setErrorType(declaredUser.getErrorType());
      userDeclareEntity.setStatus(declaredUser.getInputStatus());
      requestMap.put(JsonKey.DECLARATIONS, userDeclareEntity);
      request.setRequest(requestMap);
      tellToAnother(request);
    } catch (Exception e) {
      ProjectLogger.log(
          "DeclaredExternalIdActor:updateErrorDetail:Exception in processing the DeclaredUser: "
              + e.getCause(),
          declaredUser.getUserId(),
          LoggerEnum.ERROR.name());
    }
  }

  private void rejectDeclaredDetail(Request request, SelfDeclaredUser declaredUser) {
    try {
      request.setOperation("upsertUserSelfDeclarations");
      Map<String, Object> requestMap = new HashMap();
      UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
      userDeclareEntity.setOrgId(declaredUser.getOrgId());
      userDeclareEntity.setPersona(declaredUser.getPersona());
      userDeclareEntity.setUserId(declaredUser.getUserId());
      userDeclareEntity.setOperation(JsonKey.REMOVE);
      List userDeclareEntityLst = new ArrayList<UserDeclareEntity>();
      userDeclareEntityLst.add(userDeclareEntity);
      requestMap.put(JsonKey.DECLARATIONS, userDeclareEntityLst);
      request.setRequest(requestMap);
      tellToAnother(request);
    } catch (Exception e) {
      ProjectLogger.log(
          "DeclaredExternalIdActor:rejectDeclaredDetail:Exception in processing the DeclaredUser: "
              + e.getCause(),
          declaredUser.getUserId(),
          LoggerEnum.ERROR.name());
    }
  }

  private void migrateDeclaredUser(Request request, SelfDeclaredUser declaredUser) {
    request.setOperation(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE.getValue());
    ProjectLogger.log("DeclaredExternalIdActor:migrateDeclaredUser ");
    try {
      if (StringUtils.isNotEmpty(declaredUser.getSubOrgId())) {
        declaredUser.setOrgExternalId(getOrgDetails(declaredUser.getSubOrgId()).getExternalId());
      }
      Map<String, Object> requestMap = new HashMap();
      Map<String, String> externalIdMap = new HashMap();
      List<Map<String, String>> externalIdLst = new ArrayList();
      requestMap.put(JsonKey.USER_ID, declaredUser.getUserId());
      requestMap.put(JsonKey.CHANNEL, declaredUser.getChannel());
      requestMap.put(JsonKey.ORG_EXTERNAL_ID, declaredUser.getSubOrgId());
      externalIdMap.put(JsonKey.ID, declaredUser.getUserExternalId());
      externalIdMap.put(JsonKey.ID_TYPE, declaredUser.getChannel());
      externalIdMap.put(JsonKey.PROVIDER, declaredUser.getChannel());
      externalIdLst.add(externalIdMap);
      requestMap.put(JsonKey.EXTERNAL_IDS, externalIdLst);
      request.setRequest(requestMap);
      tellToAnother(request);
    } catch (Exception e) {
      ProjectLogger.log(
          "DeclaredExternalIdActor:migrateDeclaredUser:Exception in processing the DeclaredUser: "
              + e.getCause(),
          declaredUser.getUserId(),
          LoggerEnum.ERROR.name());
    }
  }

  private Organisation getOrgDetails(String subOrgId) {
    OrganisationClient organisationClient = new OrganisationClientImpl();
    return organisationClient.esGetOrgById(subOrgId);
  }
}
