package org.sunbird.actor.bulkupload;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.user.validator.UserRequestValidator;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.model.bulkupload.BulkUploadProcessTask;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.role.RoleService;
import org.sunbird.service.systemsettings.SystemSettingsService;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.UserUtility;
import org.sunbird.util.Util;

public class UserBulkUploadBackgroundJobActor extends BaseBulkUploadBackgroundJobActor {

  private final OrgService orgService = OrgServiceImpl.getInstance();
  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private final SystemSettingsService systemSettingsService = new SystemSettingsService();

  @Inject
  @Named("user_role_actor")
  private ActorRef userRoleActor;

  @Inject
  @Named("sso_user_create_actor")
  private ActorRef ssoUserCreateActor;

  @Inject
  @Named("user_update_actor")
  private ActorRef userUpdateActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (operation.equalsIgnoreCase("userBulkUploadBackground")) {
      handleBulkUploadBackground(
          request,
          (baseBulkUpload) -> {
            processBulkUpload(
                (BulkUploadProcess) baseBulkUpload,
                (tasks) -> {
                  processTasks(
                      (List<BulkUploadProcessTask>) tasks,
                      ((BulkUploadProcess) baseBulkUpload),
                      request.getRequestContext());
                  return null;
                },
                request.getRequestContext());
            return null;
          });
    } else {
      onReceiveUnsupportedOperation();
    }
  }

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
          systemSettingsService.getSystemSettingByFieldAndKey(
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
          } else {
            userMap.remove(JsonKey.ROLES);
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
        // will upload user to same channel as uploader means org admin channel
        userMap.put(JsonKey.CHANNEL, uploaderOrg.getChannel());
        organisation = getOrgDetails(userMap, context);
        if (null == organisation) {
          setTaskStatus(
              task,
              ProjectUtil.BulkProcessStatus.FAILED,
              MessageFormat.format(
                  ResponseCode.invalidParameter.getErrorMessage(),
                  JsonKey.ORGANISATION + JsonKey.ID),
              userMap,
              JsonKey.CREATE);
          return;
        } else {
          if (StringUtils.isNotBlank(orgId) // orgId is csv orgId
              && StringUtils.isNotBlank(orgExternalId)
              && !(orgId)
                  .equalsIgnoreCase(
                      organisation
                          .getId())) { // organisation is the org response from csv orgExternalId
            // and uploader channel search response

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
      } else {
        // userMap.put(JsonKey.CHANNEL, uploaderOrg.getChannel());
        userMap.put(JsonKey.ORGANISATION_ID, uploaderOrg.getId());
      }
      if (null != organisation // (uploaded user orgId or orgExternalId org details)
          && (!(organisation.getChannel()).equalsIgnoreCase(uploaderOrg.getChannel()))) {
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
            MessageFormat.format(
                ResponseCode.invalidParameter.getErrorMessage(),
                JsonKey.ORGANISATION + JsonKey.STATUS),
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
        Map<String, Object> newUserReqMap = SerializationUtils.clone(new HashMap<>(userMap));
        newUserReqMap.put(JsonKey.ORG_NAME, orgName);
        newUserReqMap.remove(JsonKey.CHANNEL);
        callUpdateUser(
            userUpdateActor,
            ActorOperations.UPDATE_USER.getValue(),
            JsonKey.UPDATE,
            newUserReqMap,
            task,
            context);
        if (userMap.containsKey(JsonKey.ROLES)) {
          callUpdateUser(
              userRoleActor,
              ActorOperations.ASSIGN_ROLES.getValue(),
              ActorOperations.ASSIGN_ROLES.getValue(),
              userMap,
              task,
              context);
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
          upsertUser(ssoUserCreateActor, user, ActorOperations.CREATE_USER.getValue(), context);
    } catch (Exception ex) {
      logger.error(
          context,
          "UserBulkUploadBackgroundJobActor:callCreateUser: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
      user.put(JsonKey.ERROR_MSG, ex.getMessage());
      setTaskStatus(
          task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), user, JsonKey.CREATE);
      return;
    }

    if (StringUtils.isEmpty(userId)) {
      logger.info(context, "UserBulkUploadBackgroundJobActor:callCreateUser: User ID is null !");
      setTaskStatus(
          task,
          ProjectUtil.BulkProcessStatus.FAILED,
          ResponseCode.serverError.getErrorMessage(),
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
      ActorRef actorRef,
      String operation,
      String taskAction,
      Map<String, Object> user,
      BulkUploadProcessTask task,
      RequestContext context)
      throws JsonProcessingException {
    logger.info(context, "UserBulkUploadBackgroundJobActor: " + operation + " called");
    try {
      upsertUser(actorRef, user, operation, context);
    } catch (Exception ex) {
      logger.error(
          context,
          "UserBulkUploadBackgroundJobActor:"
              + operation
              + ": Exception occurred with error message = "
              + ex.getMessage(),
          ex);
      user.put(JsonKey.ERROR_MSG, ex.getMessage());
      setTaskStatus(task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), user, taskAction);
    }
    if (task.getStatus() != ProjectUtil.BulkProcessStatus.FAILED.getValue()) {
      ObjectMapper mapper = new ObjectMapper();
      task.setData(mapper.writeValueAsString(user));
      setSuccessTaskStatus(task, ProjectUtil.BulkProcessStatus.COMPLETED, user, taskAction);
    }
  }

  private Organisation getOrgDetails(Map<String, Object> userMap, RequestContext context) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(
          JsonKey.EXTERNAL_ID, ((String) userMap.get(JsonKey.ORG_EXTERNAL_ID)).toLowerCase());
      filters.put(JsonKey.CHANNEL, ((String) userMap.get(JsonKey.CHANNEL)).toLowerCase());
      List<Organisation> orgList = orgService.organisationObjSearch(filters, context);
      if (CollectionUtils.isNotEmpty(orgList)) {
        return orgList.get(0);
      }
      return null;
    } else if (StringUtils.isNotBlank((String) userMap.get(JsonKey.ORG_ID))) {
      return orgService.getOrgObjById((String) userMap.get(JsonKey.ORG_ID), context);
    }
    return null;
  }

  @Override
  public void preProcessResult(Map<String, Object> result) {
    UserUtility.decryptUserData(result);
    Util.addMaskEmailAndPhone(result);
  }

  private String upsertUser(
      ActorRef actorRef, Map<String, Object> userMap, String operation, RequestContext context) {
    String userId = null;

    Request request = new Request();
    request.setRequestContext(context);
    request.setRequest(userMap);
    request.setOperation(operation);
    request.getContext().put(JsonKey.CALLER_ID, JsonKey.BULK_USER_UPLOAD);
    request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_2);
    request.getContext().put(JsonKey.ROOT_ORG_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    userMap.remove(JsonKey.ROOT_ORG_ID);
    Object obj = actorCall(actorRef, request, context);

    if (obj instanceof Response) {
      Response response = (Response) obj;
      if (response.get(JsonKey.USER_ID) != null) {
        userId = (String) response.get(JsonKey.USER_ID);
      }
    }
    return userId;
  }
}
