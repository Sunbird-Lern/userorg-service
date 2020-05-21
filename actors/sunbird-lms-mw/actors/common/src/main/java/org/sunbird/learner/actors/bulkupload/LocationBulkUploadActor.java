package org.sunbird.learner.actors.bulkupload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.util.Util;

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
    GeoLocationJsonKey.CODE,
    JsonKey.NAME,
    GeoLocationJsonKey.PARENT_CODE,
    GeoLocationJsonKey.PARENT_ID
  };

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.GEO_LOCATION);
    ExecutionContext.setRequestId(request.getRequestId());
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
        handleUpload(JsonKey.LOCATION, (String) req.get(JsonKey.CREATED_BY));
    String locationType = (String) req.get(GeoLocationJsonKey.LOCATION_TYPE);
    processLocationBulkUpload(req, bulkUploadProcess.getId(), locationType, bulkUploadProcess);
  }

  private void processLocationBulkUpload(
      Map<String, Object> req,
      String processId,
      String locationType,
      BulkUploadProcess bulkUploadProcess)
      throws IOException {
    byte[] fileByteArray = null;
    if (null != req.get(JsonKey.FILE)) {
      fileByteArray = (byte[]) req.get(JsonKey.FILE);
    }
    Map<String, Object> additionalRowFields = new HashMap<>();
    additionalRowFields.put(GeoLocationJsonKey.LOCATION_TYPE, locationType);
    Integer recordCount = validateAndParseRecords(fileByteArray, processId, additionalRowFields);
    processBulkUpload(
        recordCount,
        processId,
        bulkUploadProcess,
        BulkUploadActorOperation.LOCATION_BULK_UPLOAD_BACKGROUND_JOB.getValue(),
        bulkLocationAllowedFields);
  }
}
