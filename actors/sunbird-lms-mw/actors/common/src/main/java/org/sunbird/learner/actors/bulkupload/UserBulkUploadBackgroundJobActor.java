package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.actorutil.user.UserClient;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.learner.actors.role.service.RoleService;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.validator.user.UserBulkUploadRequestValidator;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;

@ActorConfig(
  tasks = {},
  asyncTasks = {"userBulkUploadBackground"}
)
public class UserBulkUploadBackgroundJobActor extends BaseBulkUploadBackgroundJobActor {

  private UserClient userClient = new UserClientImpl();
  private OrganisationClient organisationClient = new OrganisationClientImpl();
  private SystemSettingClient systemSettingClient = new SystemSettingClientImpl();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (operation.equalsIgnoreCase("userBulkUploadBackground")) {

      Map outputColumns =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              "userProfileConfig",
              "csv.outputColumns",
              new TypeReference<Map>() {});

      String[] outputColumnsOrder =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              "userProfileConfig",
              "csv.outputColumnsOrder",
              new TypeReference<String[]>() {});

      handleBulkUploadBackground(
          request,
          (baseBulkUpload) -> {
            processBulkUpload(
                (BulkUploadProcess) baseBulkUpload,
                (tasks) -> {
                  processTasks(
                      (List<BulkUploadProcessTask>) tasks, ((BulkUploadProcess) baseBulkUpload));
                  return null;
                },
                outputColumns,
                outputColumnsOrder != null
                    ? outputColumnsOrder
                    : (String[]) request.get(JsonKey.FIELDS));
            return null;
          });
    } else {
      onReceiveUnsupportedOperation("UserBulkUploadBackgroundJobActor");
    }
  }

  private void processTasks(
      List<BulkUploadProcessTask> bulkUploadProcessTasks, BulkUploadProcess bulkUploadProcess) {
    for (BulkUploadProcessTask task : bulkUploadProcessTasks) {
      try {
        if (task.getStatus() != null
            && task.getStatus() != ProjectUtil.BulkProcessStatus.COMPLETED.getValue()) {
          processUser(
              task, bulkUploadProcess.getOrganisationId(), bulkUploadProcess.getUploadedBy());
          task.setLastUpdatedOn(new Timestamp(System.currentTimeMillis()));
          task.setIterationId(task.getIterationId() + 1);
        }
      } catch (Exception ex) {
        ProjectLogger.log("Error in processTasks", ex);
        task.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void processUser(BulkUploadProcessTask task, String organisationId, String uploadedBy) {
    ProjectLogger.log("UserBulkUploadBackgroundJobActor: processUser called", LoggerEnum.INFO);
    String data = task.getData();
    Organisation organisation = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> userMap = mapper.readValue(data, Map.class);
      String[] mandatoryColumnsObject =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              "userProfileConfig",
              "csv.mandatoryColumns",
              new TypeReference<String[]>() {});
      if (mandatoryColumnsObject != null) {
        validateMandatoryFields(userMap, task, mandatoryColumnsObject);
      }
      if (userMap.get(JsonKey.PHONE) != null) {
        userMap.put(JsonKey.PHONE_VERIFIED, true);
      }
      try {
        String roles = (String) userMap.get(JsonKey.ROLES);
        if (roles != null) {
          String[] roleArray = roles.split(",");
          List<String> roleList = new ArrayList<>();
          Arrays.stream(roleArray)
              .forEach(
                  x -> {
                    roleList.add(x.trim());
                  });
          userMap.put(JsonKey.ROLES, roleList);
          RoleService.validateRoles((List<String>) userMap.get(JsonKey.ROLES));
        }
        UserBulkUploadRequestValidator.validateUserBulkUploadRequest(userMap);
      } catch (Exception ex) {
        setTaskStatus(
            task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), userMap, JsonKey.CREATE);
        return;
      }
      String orgId = (String) userMap.get(JsonKey.ORG_ID);
      String orgExternalId = (String) userMap.get(JsonKey.ORG_EXTERNAL_ID);
      HashMap<String, Object> uploaderMap = new HashMap<>();
      uploaderMap.put(JsonKey.ORG_ID, organisationId);
      Organisation uploaderOrg = getOrgDetails(uploaderMap);
      if (StringUtils.isNotBlank(orgId) || StringUtils.isNotBlank(orgExternalId)) {
        organisation = getOrgDetails(userMap);
        if (null == organisation) {
          setTaskStatus(
              task,
              ProjectUtil.BulkProcessStatus.FAILED,
              ResponseCode.invalidOrgId.getErrorMessage(),
              userMap,
              JsonKey.CREATE);
          return;
        } else {
          if (StringUtils.isNotBlank(orgId)
              && StringUtils.isNotBlank(orgExternalId)
              && !(orgId).equalsIgnoreCase(organisation.getId())) {

            String message =
                MessageFormat.format(
                    ResponseCode.errorConflictingValues.getErrorMessage(),
                    JsonKey.ORGANISATION_ID,
                    orgId,
                    JsonKey.ORG_EXTERNAL_ID,
                    orgExternalId);
            setTaskStatus(
                task, ProjectUtil.BulkProcessStatus.FAILED, message, userMap, JsonKey.CREATE);
            return;

          } else {
            if (StringUtils.isNotBlank(orgExternalId)) {
              userMap.put(JsonKey.ORGANISATION_ID, organisation.getId());
            } else {
              userMap.put(JsonKey.ORGANISATION_ID, orgId);
            }
          }
        }
      }
      if (null != organisation
          && (!(organisation.getRootOrgId()).equalsIgnoreCase(organisationId))
          && (!(organisation.getRootOrgId()).equalsIgnoreCase(uploaderOrg.getRootOrgId()))) {
        setTaskStatus(
            task,
            ProjectUtil.BulkProcessStatus.FAILED,
            ResponseCode.errorConflictingRootOrgId.getErrorMessage(),
            userMap,
            JsonKey.CREATE);
        return;
      }

      if (organisation != null
          && !ProjectUtil.OrgStatus.ACTIVE.getValue().equals(organisation.getStatus())) {
        setTaskStatus(
            task,
            ProjectUtil.BulkProcessStatus.FAILED,
            ResponseCode.invalidOrgStatus.getErrorMessage(),
            userMap,
            JsonKey.CREATE);
        return;
      }

      String orgName = "";
      if (null != organisation) {
        orgName = organisation.getOrgName();
      }

      if(StringUtils.isNotEmpty((String) userMap.get(JsonKey.PHONE))) {
        userMap.put(JsonKey.PHONE_VERIFIED, true);
      }
      if(StringUtils.isNotEmpty((String) userMap.get(JsonKey.EMAIL))) {
        userMap.put(JsonKey.EMAIL_VERIFIED, true);
      }
      String userId  = (String) userMap.get(JsonKey.USER_ID);
      if (StringUtils.isEmpty(userId)) {
        userMap.put(JsonKey.CREATED_BY, uploadedBy);
        userMap.put(JsonKey.ROOT_ORG_ID, organisationId);
        callCreateUser(userMap, task, orgName);
      } else {
        userMap.put(JsonKey.UPDATED_BY, uploadedBy);
        callUpdateUser(userMap, task, orgName);
      }
    } catch (Exception e) {
      ProjectLogger.log("Error in process user", data, e);
      task.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  private void callCreateUser(Map<String, Object> user, BulkUploadProcessTask task, String orgName)
      throws JsonProcessingException {
    ProjectLogger.log("UserBulkUploadBackgroundJobActor: callCreateUser called", LoggerEnum.INFO);
    String userId;
    try {
      userId = userClient.createUser(getActorRef(ActorOperations.CREATE_USER.getValue()), user);
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserBulkUploadBackgroundJobActor:callCreateUser: Exception occurred with error message = "
              + ex.getMessage(),
          LoggerEnum.INFO);
      setTaskStatus(
          task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), user, JsonKey.CREATE);
      return;
    }

    if (StringUtils.isEmpty(userId)) {
      ProjectLogger.log(
          "UserBulkUploadBackgroundJobActor:callCreateUser: User ID is null !", LoggerEnum.ERROR);
      setTaskStatus(
          task,
          ProjectUtil.BulkProcessStatus.FAILED,
          ResponseCode.internalError.getErrorMessage(),
              user,
          JsonKey.CREATE);
    } else {
      user.put(JsonKey.ID, userId);
      user.put(JsonKey.ORG_NAME, orgName);
      setSuccessTaskStatus(task, ProjectUtil.BulkProcessStatus.COMPLETED, user, JsonKey.CREATE);
    }
  }

  @SuppressWarnings("unchecked")
  private void callUpdateUser(Map<String, Object> user, BulkUploadProcessTask task, String orgName)
      throws JsonProcessingException {
    ProjectLogger.log("UserBulkUploadBackgroundJobActor: callUpdateUser called", LoggerEnum.INFO);
    try {
      user.put(JsonKey.ORG_NAME, orgName);
      userClient.updateUser(getActorRef(ActorOperations.UPDATE_USER.getValue()), user);
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserBulkUploadBackgroundJobActor:callUpdateUser: Exception occurred with error message = "
              + ex.getMessage(),
          LoggerEnum.INFO);
      user.put(JsonKey.ERROR_MSG, ex.getMessage());
      setTaskStatus(
          task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), user, JsonKey.UPDATE);
    }
    if (task.getStatus() != ProjectUtil.BulkProcessStatus.FAILED.getValue()) {
      ObjectMapper mapper = new ObjectMapper();
      task.setData(mapper.writeValueAsString(user));
      setSuccessTaskStatus(task, ProjectUtil.BulkProcessStatus.COMPLETED, user, JsonKey.UPDATE);
    }
  }

  private Organisation getOrgDetails(Map<String, Object> userMap) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(
          JsonKey.EXTERNAL_ID, ((String) userMap.get(JsonKey.ORG_EXTERNAL_ID)).toLowerCase());
      if (CollectionUtils.isNotEmpty(organisationClient.esSearchOrgByFilter(filters))) {
        return organisationClient.esSearchOrgByFilter(filters).get(0);
      }
      return null;
    } else if (StringUtils.isNotBlank((String) userMap.get(JsonKey.ORG_ID))) {
      return organisationClient.esGetOrgById((String) userMap.get(JsonKey.ORG_ID));
    }
    return null;
  }

  @Override
  public void preProcessResult(Map<String, Object> result) {
    UserUtility.decryptUserData(result);
    Util.addMaskEmailAndPhone(result);
  }
}
