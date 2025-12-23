package org.sunbird.actor.bulkupload;

import org.apache.pekko.actor.ActorRef;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.Util;

public class LocationBulkUploadActor extends BaseBulkUploadActor {
  @Inject
  @Named("location_bulk_upload_background_job_actor")
  private ActorRef locationBulkUploadBackGroundJobActor;

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
        onReceiveUnsupportedOperation();
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
        locationBulkUploadBackGroundJobActor,
        recordCount,
        processId,
        bulkUploadProcess,
        ActorOperations.LOCATION_BULK_UPLOAD_BACKGROUND_JOB.getValue(),
        bulkLocationAllowedFields,
        context);
  }
}
