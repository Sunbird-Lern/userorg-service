/*
package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.learner.actors.role.service.RoleService;
import org.sunbird.models.organisation.Organisation;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;

public class ProcessUserTask {

  private void processTasks(
    List<BulkUploadProcessTask> bulkUploadProcessTasks,
    BulkUploadProcess bulkUploadProcess,
    RequestContext context) {
    for (BulkUploadProcessTask task : bulkUploadProcessTasks) {
      try {
        if (task.getStatus() != null
          && task.getStatus() != ProjectUtil.BulkProcessStatus.COMPLETED.getValue()) {
          processUser(
            task,
            bulkUploadProcess.getOrganisationId(),
            bulkUploadProcess.getUploadedBy(),
            context);
          task.setLastUpdatedOn(new Timestamp(System.currentTimeMillis()));
          task.setIterationId(task.getIterationId() + 1);
        }
      } catch (Exception ex) {
        logger.error(context, "Error in processTasks", ex);
        task.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void processUser(
    BulkUploadProcessTask task,
    String organisationId,
    String uploadedBy,
    RequestContext context) {
    logger.info(context, "UserBulkUploadBackgroundJobActor: processUser called");
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
          new TypeReference<String[]>() {},
          context);
      if (mandatoryColumnsObject != null) {
        validateMandatoryFields(userMap, task, mandatoryColumnsObject);
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
          if (roleList.contains(ProjectUtil.UserRole.PUBLIC.getValue())) {
            roleList.remove(ProjectUtil.UserRole.PUBLIC.getValue());
          }
          if (CollectionUtils.isNotEmpty(roleList)) {
            userMap.put(JsonKey.ROLES, roleList);
            RoleService.validateRoles((List<String>) userMap.get(JsonKey.ROLES));
          }
        }
        userRequestValidator.validateUserType(userMap, null, context);
      } catch (Exception ex) {
        logger.error(context, ex.getMessage(), ex);
        setTaskStatus(
          task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), userMap, JsonKey.CREATE);
        return;
      }
      String orgId = (String) userMap.get(JsonKey.ORG_ID);
      String orgExternalId = (String) userMap.get(JsonKey.ORG_EXTERNAL_ID);
      HashMap<String, Object> uploaderMap = new HashMap<>();
      uploaderMap.put(JsonKey.ORG_ID, organisationId);
      Organisation uploaderOrg = getOrgDetails(uploaderMap, context);
      if (StringUtils.isNotBlank(orgId) || StringUtils.isNotBlank(orgExternalId)) {
        organisation = getOrgDetails(userMap, context);
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

      String userId = (String) userMap.get(JsonKey.USER_ID);
      if (StringUtils.isEmpty(userId)) {
        userMap.put(JsonKey.CREATED_BY, uploadedBy);
        userMap.put(JsonKey.ROOT_ORG_ID, organisationId);
        callCreateUser(userMap, task, orgName, context);
      } else {
        userMap.put(JsonKey.UPDATED_BY, uploadedBy);
        callUpdateUser(userMap, task, orgName, context);
        if (userMap.containsKey(JsonKey.ROLES)) {
          callAssignRole(userMap, task, context);
        }
      }
    } catch (Exception e) {
      logger.error(context, "Error in process user" + data, e);
      task.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  private void callCreateUser(
    Map<String, Object> user, BulkUploadProcessTask task, String orgName, RequestContext context)
    throws JsonProcessingException {
    logger.info(context, "UserBulkUploadBackgroundJobActor: callCreateUser called");
    String userId;
    try {
      userId =
        userClient.createUser(getActorRef(ActorOperations.CREATE_USER.getValue()), user, context);
    } catch (Exception ex) {
      logger.error(
        context,
        "UserBulkUploadBackgroundJobActor:callCreateUser: Exception occurred with error message = "
          + ex.getMessage(),
        ex);
      setTaskStatus(
        task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), user, JsonKey.CREATE);
      return;
    }

    if (StringUtils.isEmpty(userId)) {
      logger.info(context, "UserBulkUploadBackgroundJobActor:callCreateUser: User ID is null !");
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
  private void callUpdateUser(
    Map<String, Object> user, BulkUploadProcessTask task, String orgName, RequestContext context)
    throws JsonProcessingException {
    logger.info(context, "UserBulkUploadBackgroundJobActor: callUpdateUser called");
    try {
      user.put(JsonKey.ORG_NAME, orgName);
      userClient.updateUser(getActorRef(ActorOperations.UPDATE_USER.getValue()), user, context);
    } catch (Exception ex) {
      logger.error(
        context,
        "UserBulkUploadBackgroundJobActor:callUpdateUser: Exception occurred with error message = "
          + ex.getMessage(),
        ex);
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

  private void callAssignRole(
    Map<String, Object> user, BulkUploadProcessTask task, RequestContext context)
    throws JsonProcessingException {
    logger.info(context, "UserBulkUploadBackgroundJobActor: callAssignRole called");
    try {
      userClient.updateUser(getActorRef(ActorOperations.ASSIGN_ROLES.getValue()), user, context);
    } catch (Exception ex) {
      logger.error(
        context,
        "UserBulkUploadBackgroundJobActor:callAssignRole: Exception occurred with error message = "
          + ex.getMessage(),
        ex);
      user.put(JsonKey.ERROR_MSG, ex.getMessage());
      setTaskStatus(
        task,
        ProjectUtil.BulkProcessStatus.FAILED,
        ex.getMessage(),
        user,
        ActorOperations.ASSIGN_ROLES.getValue());
    }
    if (task.getStatus() != ProjectUtil.BulkProcessStatus.FAILED.getValue()) {
      ObjectMapper mapper = new ObjectMapper();
      task.setData(mapper.writeValueAsString(user));
      setSuccessTaskStatus(
        task,
        ProjectUtil.BulkProcessStatus.COMPLETED,
        user,
        ActorOperations.ASSIGN_ROLES.getValue());
    }
  }
}
*/
