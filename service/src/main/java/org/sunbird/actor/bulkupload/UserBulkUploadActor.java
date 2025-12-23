package org.sunbird.actor.bulkupload;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.systemsettings.SystemSettingsService;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;

public class UserBulkUploadActor extends BaseBulkUploadActor {

  private final SystemSettingsService systemSettingsService = new SystemSettingsService();

  @Inject
  @Named("user_bulk_upload_background_job_actor")
  private ActorRef userBulkUploadBackgroundJobActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase("userBulkUpload")) {
      upload(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  @SuppressWarnings("unchecked")
  private void upload(Request request) throws IOException {
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    Object dataObject =
        systemSettingsService.getSystemSettingByFieldAndKey(
            "userProfileConfig", "csv", new TypeReference<Map>() {}, request.getRequestContext());
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
        handleUpload(
            JsonKey.USER, (String) req.get(JsonKey.CREATED_BY), request.getRequestContext());
    processUserBulkUpload(
        req,
        bulkUploadProcess.getId(),
        bulkUploadProcess,
        supportedColumnsLowerCaseMap,
        request.getRequestContext());
  }

  private void processUserBulkUpload(
      Map<String, Object> req,
      String processId,
      BulkUploadProcess bulkUploadProcess,
      Map<String, Object> supportedColumnsMap,
      RequestContext context)
      throws IOException {
    byte[] fileByteArray = null;
    if (null != req.get(JsonKey.FILE)) {
      fileByteArray = (byte[]) req.get(JsonKey.FILE);
    }
    Integer recordCount =
        validateAndParseRecords(
            fileByteArray, processId, new HashMap(), supportedColumnsMap, true, context);
    processBulkUpload(
        userBulkUploadBackgroundJobActor,
        recordCount,
        processId,
        bulkUploadProcess,
        ActorOperations.USER_BULK_UPLOAD_BACKGROUND_JOB.getValue(),
        DataCacheHandler.bulkUserAllowedFields,
        context);
  }
}
