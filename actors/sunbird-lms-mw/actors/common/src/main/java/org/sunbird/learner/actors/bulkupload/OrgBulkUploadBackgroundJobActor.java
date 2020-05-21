package org.sunbird.learner.actors.bulkupload;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.models.organisation.Organisation;

@ActorConfig(
  tasks = {},
  asyncTasks = {"orgBulkUploadBackground"}
)
public class OrgBulkUploadBackgroundJobActor extends BaseBulkUploadBackgroundJobActor {
  private OrganisationClient orgClient = new OrganisationClientImpl();
  private SystemSettingClient systemSettingClient = new SystemSettingClientImpl();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    Util.initializeContext(request, TelemetryEnvKey.ORGANISATION);
    ExecutionContext.setRequestId(request.getRequestId());
    if (operation.equalsIgnoreCase("orgBulkUploadBackground")) {
      Map<String, String> outputColumns =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              "orgProfileConfig",
              "csv.outputColumns",
              new TypeReference<Map<String, String>>() {});

      String[] outputColumnsOrder =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              "orgProfileConfig",
              "csv.outputColumnsOrder",
              new TypeReference<String[]>() {});

      handleBulkUploadBackground(
          request,
          (baseBulkUpload) -> {
            processBulkUpload(
                (BulkUploadProcess) baseBulkUpload,
                (tasks) -> {
                  processTasks((List<BulkUploadProcessTask>) tasks);
                  return null;
                },
                outputColumns,
                outputColumnsOrder != null
                    ? outputColumnsOrder
                    : (String[]) request.get(JsonKey.FIELDS));
            return null;
          });
    } else {
      onReceiveUnsupportedOperation("OrgBulkUploadBackgroundJobActor");
    }
  }

  private void processTasks(List<BulkUploadProcessTask> bulkUploadProcessTasks) {
    Map<String, Location> locationCache = new HashMap<>();
    LocationClient locationClient = new LocationClientImpl();
    ActorRef locationActor = getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue());
    for (BulkUploadProcessTask task : bulkUploadProcessTasks) {
      if (task.getStatus() != null
          && task.getStatus() != ProjectUtil.BulkProcessStatus.COMPLETED.getValue()) {
        processOrg(task, locationClient, locationCache, locationActor);
        task.setLastUpdatedOn(new Timestamp(System.currentTimeMillis()));
        task.setIterationId(task.getIterationId() + 1);
      }
    }
  }

  private void processOrg(
      BulkUploadProcessTask task,
      LocationClient locationClient,
      Map<String, Location> locationCache,
      ActorRef locationActor) {
    ProjectLogger.log("OrgBulkUploadBackgroundJobActor: processOrg called", LoggerEnum.INFO);
    String data = task.getData();
    try {
      Map<String, Object> orgMap = mapper.readValue(data, Map.class);
      Object mandatoryColumnsObject =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              "orgProfileConfig",
              "csv.mandatoryColumns",
              new TypeReference<String[]>() {});
      if (mandatoryColumnsObject != null) {
        validateMandatoryFields(orgMap, task, (String[]) mandatoryColumnsObject);
      }
      int status = getOrgStatus(orgMap);
      if (status == -1) {
        orgMap.put(JsonKey.ERROR_MSG, ResponseCode.invalidOrgStatus.getErrorMessage());
        task.setFailureResult(mapper.writeValueAsString(orgMap));
        task.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
        return;
      }

      List<String> locationCodes = new ArrayList<>();
      if (orgMap.get(JsonKey.LOCATION_CODE) instanceof String) {
        locationCodes.add((String) orgMap.get(JsonKey.LOCATION_CODE));
      } else {
        locationCodes = (List<String>) orgMap.get(JsonKey.LOCATION_CODE);
      }

      Organisation organisation = mapper.convertValue(orgMap, Organisation.class);
      organisation.setStatus(status);
      organisation.setId((String) orgMap.get(JsonKey.ORGANISATION_ID));

      if (StringUtils.isEmpty(organisation.getId())) {
        callCreateOrg(organisation, task, locationCodes);
      } else {
        callUpdateOrg(organisation, task, locationCodes);
      }
      setLocationInformation(task, locationClient, locationCache, locationActor, locationCodes);
    } catch (Exception e) {
      ProjectLogger.log(
          "OrgBulkUploadBackgroundJobActor:callCreateOrg: Exception occurred with error message = "
              + e.getMessage(),
          LoggerEnum.INFO);
    }
  }

  private void setLocationInformation(
      BulkUploadProcessTask task,
      LocationClient locationClient,
      Map<String, Location> locationCache,
      ActorRef locationActor,
      List<String> locationCodes)
      throws IOException, JsonParseException, JsonMappingException {
    if (ProjectUtil.BulkProcessStatus.COMPLETED.getValue() == task.getStatus()) {
      List<String> locationNames = new ArrayList<>();
      for (String locationCode : locationCodes) {
        if (locationCache.containsKey(locationCode)) {
          locationNames.add(locationCache.get(locationCode).getName());
        } else {
          Location location = locationClient.getLocationByCode(locationActor, locationCode);
          locationNames.add(location.getName());
        }
      }
      Map<String, Object> row = mapper.readValue(task.getSuccessResult(), Map.class);
      if (locationNames.size() == 1) {
        row.put(JsonKey.LOCATION_NAME, locationNames.get(0));
        row.put(JsonKey.LOCATION_CODE, locationCodes.get(0));
      } else {
        row.put(JsonKey.LOCATION_NAME, locationNames);
        row.put(JsonKey.LOCATION_CODE, locationCodes);
      }
      task.setSuccessResult(mapper.writeValueAsString(row));
    }
  }

  private int getOrgStatus(Map<String, Object> orgMap) {
    int status = ProjectUtil.OrgStatus.ACTIVE.getValue();
    if (!StringUtils.isEmpty((String) orgMap.get(JsonKey.STATUS))) {
      if (((String) orgMap.get(JsonKey.STATUS)).equalsIgnoreCase(JsonKey.INACTIVE)) {
        status = ProjectUtil.OrgStatus.INACTIVE.getValue();
      } else if (((String) orgMap.get(JsonKey.STATUS)).equalsIgnoreCase(JsonKey.ACTIVE)) {
        status = ProjectUtil.OrgStatus.ACTIVE.getValue();
      } else {
        return -1;
      }
      orgMap.remove(JsonKey.STATUS);
    }
    return status;
  }

  private void callCreateOrg(
      Organisation org, BulkUploadProcessTask task, List<String> locationCodes)
      throws JsonProcessingException {
    Map<String, Object> row = mapper.convertValue(org, Map.class);
    row.put(JsonKey.LOCATION_CODE, locationCodes);
    String orgId;
    try {
      orgId = orgClient.createOrg(getActorRef(ActorOperations.CREATE_ORG.getValue()), row);
    } catch (Exception ex) {
      ProjectLogger.log(
          "OrgBulkUploadBackgroundJobActor:callCreateOrg: Exception occurred with error message = "
              + ex.getMessage(),
          LoggerEnum.INFO);
      setTaskStatus(
          task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), row, JsonKey.CREATE);
      return;
    }

    if (StringUtils.isEmpty(orgId)) {
      ProjectLogger.log(
          "OrgBulkUploadBackgroundJobActor:callCreateOrg: Org ID is null !", LoggerEnum.ERROR);
      setTaskStatus(
          task,
          ProjectUtil.BulkProcessStatus.FAILED,
          ResponseCode.internalError.getErrorMessage(),
          row,
          JsonKey.CREATE);
    } else {
      row.put(JsonKey.ORGANISATION_ID, orgId);
      setSuccessTaskStatus(task, ProjectUtil.BulkProcessStatus.COMPLETED, row, JsonKey.CREATE);
    }
  }

  private void callUpdateOrg(
      Organisation org, BulkUploadProcessTask task, List<String> locationCodes)
      throws JsonProcessingException {
    Map<String, Object> row = mapper.convertValue(org, Map.class);
    row.put(JsonKey.LOCATION_CODE, locationCodes);
    try {
      row.put(JsonKey.ORGANISATION_ID, org.getId());
      orgClient.updateOrg(getActorRef(ActorOperations.UPDATE_ORG.getValue()), row);
    } catch (Exception ex) {
      ProjectLogger.log(
          "OrgBulkUploadBackgroundJobActor:callUpdateOrg: Exception occurred with error message = "
              + ex.getMessage(),
          LoggerEnum.INFO);
      row.put(JsonKey.ERROR_MSG, ex.getMessage());
      setTaskStatus(
          task, ProjectUtil.BulkProcessStatus.FAILED, ex.getMessage(), row, JsonKey.UPDATE);
    }
    if (task.getStatus() != ProjectUtil.BulkProcessStatus.FAILED.getValue()) {
      task.setData(mapper.writeValueAsString(row));
      setSuccessTaskStatus(task, ProjectUtil.BulkProcessStatus.COMPLETED, row, JsonKey.UPDATE);
    }
  }

  @Override
  public void preProcessResult(Map<String, Object> result) {
    // Do nothing
  }
}
