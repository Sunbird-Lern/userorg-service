package org.sunbird.learner.actors.bulkupload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.operations.BulkUploadActorOperation;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.util.Util;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.telemetry.dto.TelemetryEnvKey;

/**
 * Class to provide the location bulk upload functionality.
 *
 * @author arvind.
 */
@ActorConfig(
  tasks = {"locationBulkUpload"},
  asyncTasks = {}
)
public class LocationBulkUploadActor extends BaseBulkUploadActor {

  String[] bulkLocationAllowedFields = {
    JsonKey.CODE, JsonKey.NAME, JsonKey.PARENT_CODE, JsonKey.PARENT_ID
  };

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.GEO_LOCATION);
    String operation = request.getOperation();

    switch (operation) {
      case "locationBulkUpload":
        upload(request);
        break;
      default:
        onReceiveUnsupportedOperation("LocationBulkUploadActor");
    }
  }

  private void upload(Request request) throws IOException {
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    validateFileHeaderFields(req, bulkLocationAllowedFields, true);
    BulkUploadProcess bulkUploadProcess =
        handleUpload(
            JsonKey.LOCATION, (String) req.get(JsonKey.CREATED_BY), request.getRequestContext());
    String locationType = (String) req.get(JsonKey.LOCATION_TYPE);
    processLocationBulkUpload(
        req,
        bulkUploadProcess.getId(),
        locationType,
        bulkUploadProcess,
        request.getRequestContext());
  }

  private void processLocationBulkUpload(
      Map<String, Object> req,
      String processId,
      String locationType,
      BulkUploadProcess bulkUploadProcess,
      RequestContext context)
      throws IOException {
    byte[] fileByteArray = null;
    if (null != req.get(JsonKey.FILE)) {
      fileByteArray = (byte[]) req.get(JsonKey.FILE);
    }
    Map<String, Object> additionalRowFields = new HashMap<>();
    additionalRowFields.put(JsonKey.LOCATION_TYPE, locationType);
    Integer recordCount =
        validateAndParseRecords(fileByteArray, processId, additionalRowFields, context);
    processBulkUpload(
        recordCount,
        processId,
        bulkUploadProcess,
        BulkUploadActorOperation.LOCATION_BULK_UPLOAD_BACKGROUND_JOB.getValue(),
        bulkLocationAllowedFields,
        context);
  }
}
