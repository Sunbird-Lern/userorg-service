package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.core.type.TypeReference;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ActorConfig(
  tasks = {"userBulkUpload"},
  asyncTasks = {}
)
public class UserBulkUploadActor extends BaseBulkUploadActor {

  private SystemSettingClient systemSettingClient = new SystemSettingClientImpl();


  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase("userBulkUpload")) {
      upload(request);
    } else {
      onReceiveUnsupportedOperation("UserBulkUploadActor");
    }
  }

  @SuppressWarnings("unchecked")
  private void upload(Request request) throws IOException {
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    Object dataObject =
        systemSettingClient.getSystemSettingByFieldAndKey(
            getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
            "userProfileConfig",
            "csv",
            new TypeReference<Map>() {});
    Map<String, Object> supportedColumnsMap = null;
    Map<String, Object> supportedColumnsLowerCaseMap = null;
    if (dataObject != null) {
      supportedColumnsMap =
          ((Map<String, Object>) ((Map<String, Object>) dataObject).get("supportedColumns"));
      List<String> supportedColumnsList = new ArrayList<>();
      supportedColumnsLowerCaseMap =
          supportedColumnsMap
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      entry -> (entry.getKey()).toLowerCase(), entry -> entry.getValue()));
      
      Map<String, Object> internalNamesLowerCaseMap = new HashMap<>();
      supportedColumnsMap.forEach(
          (String k, Object v) -> {
            internalNamesLowerCaseMap.put(v.toString().toLowerCase(), v.toString());
          });
      supportedColumnsLowerCaseMap.putAll(internalNamesLowerCaseMap);
      supportedColumnsLowerCaseMap.forEach(
          (key, value) -> {
            supportedColumnsList.add(key);
            supportedColumnsList.add((String) value);
          });
      List<String> mandatoryColumns =
          (List<String>) (((Map<String, Object>) dataObject).get("mandatoryColumns"));
      validateFileHeaderFields(
          req,
          supportedColumnsList.toArray(new String[supportedColumnsList.size()]),
          false,
          true,
          mandatoryColumns,
          supportedColumnsLowerCaseMap);

    } else {
      validateFileHeaderFields(req, DataCacheHandler.bulkUserAllowedFields, false);
    }
    BulkUploadProcess bulkUploadProcess =
        handleUpload(JsonKey.USER, (String) req.get(JsonKey.CREATED_BY));
    processUserBulkUpload(
        req, bulkUploadProcess.getId(), bulkUploadProcess, supportedColumnsLowerCaseMap);
  }

  private void processUserBulkUpload(
      Map<String, Object> req,
      String processId,
      BulkUploadProcess bulkUploadProcess,
      Map<String, Object> supportedColumnsMap)
      throws IOException {
    byte[] fileByteArray = null;
    if (null != req.get(JsonKey.FILE)) {
      fileByteArray = (byte[]) req.get(JsonKey.FILE);
    }
    Integer recordCount =
        validateAndParseRecords(fileByteArray, processId, new HashMap(), supportedColumnsMap, true);
    processBulkUpload(
        recordCount,
        processId,
        bulkUploadProcess,
        BulkUploadActorOperation.USER_BULK_UPLOAD_BACKGROUND_JOB.getValue(),
        DataCacheHandler.bulkUserAllowedFields);
  }
}
