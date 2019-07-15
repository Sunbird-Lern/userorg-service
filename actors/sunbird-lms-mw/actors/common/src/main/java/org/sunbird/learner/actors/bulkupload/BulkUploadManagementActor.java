package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.BulkProcessStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.CloudStorageUtil;
import org.sunbird.common.util.CloudStorageUtil.CloudStorageType;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessTaskDao;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessDaoImpl;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.learner.actors.bulkupload.model.StorageDetails;
import org.sunbird.learner.util.Util;

/**
 * This actor will handle bulk upload operation .
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {"bulkUpload", "getBulkOpStatus", "getBulkUploadStatusDownloadLink"},
  asyncTasks = {}
)
public class BulkUploadManagementActor extends BaseBulkUploadActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  private int batchDataSize = 0;
  BulkUploadProcessTaskDao bulkUploadProcessTaskDao = new BulkUploadProcessTaskDaoImpl();
  private ObjectMapper mapper = new ObjectMapper();

  private String[] bulkBatchAllowedFields = {JsonKey.BATCH_ID, JsonKey.USER_IDs};

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    // set request id fto thread local...
    ExecutionContext.setRequestId(request.getRequestId());
    if (request.getOperation().equalsIgnoreCase(ActorOperations.BULK_UPLOAD.getValue())) {
      upload(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_BULK_OP_STATUS.getValue())) {
      getUploadStatus(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_BULK_UPLOAD_STATUS_DOWNLOAD_LINK.getValue())) {
      getBulkUploadDownloadStatusLink(request);

    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void getBulkUploadDownloadStatusLink(Request actorMessage) {
    String processId = (String) actorMessage.getRequest().get(JsonKey.PROCESS_ID);
    BulkUploadProcessDaoImpl bulkuploadDao = new BulkUploadProcessDaoImpl();
    BulkUploadProcess bulkUploadProcess = bulkuploadDao.read(processId);
    if (bulkUploadProcess != null) {

      try {
        StorageDetails cloudStorageData = bulkUploadProcess.getDecryptedStorageDetails();
        if (cloudStorageData == null) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.errorUnavailableDownloadLink, null);
        }
        String signedUrl =
            CloudStorageUtil.getSignedUrl(
                CloudStorageType.getByName(cloudStorageData.getStorageType()),
                cloudStorageData.getContainer(),
                cloudStorageData.getFileName());
        Response response = new Response();
        response.setResponseCode(ResponseCode.OK);
        Map<String, Object> resultMap = response.getResult();
        resultMap.put(JsonKey.SIGNED_URL, signedUrl);
        resultMap.put(JsonKey.OBJECT_TYPE, bulkUploadProcess.getObjectType());
        resultMap.put(JsonKey.PROCESS_ID, bulkUploadProcess.getId());
        resultMap.put(JsonKey.STATUS, bulkUploadProcess.getStatus());
        updateResponseStatus(resultMap);
        sender().tell(response, self());
      } catch (IOException e) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorGenerateDownloadLink, null);
      }
    } else {
      ProjectCommonException.throwResourceNotFoundException();
    }
  }

  private void getUploadStatus(Request actorMessage) {
    String processId = (String) actorMessage.getRequest().get(JsonKey.PROCESS_ID);
    DecryptionService decryptionService =
        org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null);
    Response response = null;
    List<String> fields =
        Arrays.asList(
            JsonKey.ID,
            JsonKey.STATUS,
            JsonKey.OBJECT_TYPE,
            JsonKey.SUCCESS_RESULT,
            JsonKey.FAILURE_RESULT);
    response =
        cassandraOperation.getRecordById(
            bulkDb.getKeySpace(), bulkDb.getTableName(), processId, fields);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> resList =
        ((List<Map<String, Object>>) response.get(JsonKey.RESPONSE));
    if (!resList.isEmpty()) {
      Map<String, Object> resMap = resList.get(0);
      String objectType = (String) resMap.get(JsonKey.OBJECT_TYPE);
      if ((int) resMap.get(JsonKey.STATUS) == ProjectUtil.BulkProcessStatus.COMPLETED.getValue()) {
        resMap.put(JsonKey.PROCESS_ID, resMap.get(JsonKey.ID));
        updateResponseStatus(resMap);
        ProjectUtil.removeUnwantedFields(resMap, JsonKey.ID);
        if (!(JsonKey.LOCATION.equalsIgnoreCase(objectType))) {
          Object[] successMap = null;
          Object[] failureMap = null;
          try {
            if (null != resMap.get(JsonKey.SUCCESS_RESULT)) {
              successMap =
                  mapper.readValue(
                      decryptionService.decryptData((String) resMap.get(JsonKey.SUCCESS_RESULT)),
                      Object[].class);
              resMap.put(JsonKey.SUCCESS_RESULT, successMap);
            }
            if (null != resMap.get(JsonKey.FAILURE_RESULT)) {
              failureMap =
                  mapper.readValue(
                      decryptionService.decryptData((String) resMap.get(JsonKey.FAILURE_RESULT)),
                      Object[].class);
              resMap.put(JsonKey.FAILURE_RESULT, failureMap);
            }
          } catch (IOException e) {
            ProjectLogger.log(e.getMessage(), e);
          }
        } else {
          Map<String, Object> queryMap = new HashMap<>();
          queryMap.put(JsonKey.PROCESS_ID, processId);
          List<BulkUploadProcessTask> tasks = bulkUploadProcessTaskDao.readByPrimaryKeys(queryMap);

          List<Map> successList = new ArrayList<>();
          List<Map> failureList = new ArrayList<>();
          tasks
              .stream()
              .forEach(
                  x -> {
                    if (x.getStatus() == BulkProcessStatus.COMPLETED.getValue()) {
                      addTaskDataToList(successList, x.getSuccessResult());
                    } else {
                      addTaskDataToList(failureList, x.getFailureResult());
                    }
                  });
          resMap.put(JsonKey.SUCCESS_RESULT, successList);
          resMap.put(JsonKey.FAILURE_RESULT, failureList);
        }
        sender().tell(response, self());
      } else {
        resMap.put(JsonKey.PROCESS_ID, resMap.get(JsonKey.ID));
        updateResponseStatus(resMap);
        ProjectUtil.removeUnwantedFields(
            resMap,
            JsonKey.ID,
            JsonKey.OBJECT_TYPE,
            JsonKey.SUCCESS_RESULT,
            JsonKey.FAILURE_RESULT);
        sender().tell(response, self());
      }
    } else {
      ProjectCommonException.throwResourceNotFoundException();
    }
  }

  private void updateResponseStatus(Map<String, Object> response) {
    String status = "";
    int progressStatus = (int) response.get(JsonKey.STATUS);
    if (progressStatus == ProjectUtil.BulkProcessStatus.COMPLETED.getValue()) {
      status = BulkUploadJsonKey.COMPLETED;
    } else if (progressStatus == ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue()) {
      status = BulkUploadJsonKey.IN_PROGRESS;
    } else {
      status = BulkUploadJsonKey.NOT_STARTED;
    }
    response.put(JsonKey.STATUS, status);
    response.put(
        JsonKey.MESSAGE,
        MessageFormat.format(BulkUploadJsonKey.OPERATION_STATUS_MSG, status.toLowerCase()));
  }

  private void addTaskDataToList(List<Map> list, String data) {
    try {
      list.add(mapper.readValue(data, Map.class));
    } catch (IOException ex) {
      ProjectLogger.log("Error while converting success list to map" + ex.getMessage(), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private void upload(Request actorMessage) throws IOException {
    String processId = ProjectUtil.getUniqueIdFromTimestamp(1);
    Map<String, Object> req = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.DATA);
    req.put(JsonKey.CREATED_BY, req.get(JsonKey.CREATED_BY));
    if (((String) req.get(JsonKey.OBJECT_TYPE)).equals(JsonKey.BATCH_LEARNER_ENROL)
        || ((String) req.get(JsonKey.OBJECT_TYPE)).equals(JsonKey.BATCH_LEARNER_UNENROL)) {
      processBulkBatchEnrollment(req, processId, (String) req.get(JsonKey.OBJECT_TYPE));
    }
  }

  private void processBulkBatchEnrollment(
      Map<String, Object> req, String processId, String objectType) throws IOException {
    List<String[]> batchList = parseCsvFile((byte[]) req.get(JsonKey.FILE), processId);
    if (null != batchList) {

      if (null != PropertiesCache.getInstance().getProperty(JsonKey.BULK_UPLOAD_BATCH_DATA_SIZE)) {
        batchDataSize =
            (Integer.parseInt(
                PropertiesCache.getInstance().getProperty(JsonKey.BULK_UPLOAD_BATCH_DATA_SIZE)));
        ProjectLogger.log("bulk upload batch data size read from config file " + batchDataSize);
      }
      validateFileSizeAgainstLineNumbers(batchDataSize, batchList.size());
      if (!batchList.isEmpty()) {
        String[] columns = batchList.get(0);
        validateBulkUploadFields(columns, bulkBatchAllowedFields, false);
      } else {
        throw new ProjectCommonException(
            ResponseCode.csvError.getErrorCode(),
            ResponseCode.csvError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.csvError.getErrorCode(),
          ResponseCode.csvError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    // save csv file to db
    uploadCsvToDB(
        batchList, processId, null, objectType, (String) req.get(JsonKey.CREATED_BY), null);
  }

  private void uploadCsvToDB(
      List<String[]> dataList,
      String processId,
      String orgId,
      String objectType,
      String requestedBy,
      String rootOrgId) {
    ProjectLogger.log("BulkUploadManagementActor: uploadCsvToDB called.", LoggerEnum.INFO);
    List<Map<String, Object>> dataMapList = new ArrayList<>();
    if (dataList.size() > 1) {
      try {
        String[] columnArr = dataList.get(0);
        columnArr = trimColumnAttributes(columnArr);
        Map<String, Object> dataMap = null;
        for (int i = 1; i < dataList.size(); i++) {
          dataMap = new HashMap<>();
          String[] valueArr = dataList.get(i);
          for (int j = 0; j < valueArr.length; j++) {
            String value = (valueArr[j].trim().length() == 0 ? null : valueArr[j].trim());
            dataMap.put(columnArr[j], value);
          }
          dataMapList.add(dataMap);
        }
      } catch (Exception e) {
        ProjectLogger.log(e.getMessage(), e);
        throw new ProjectCommonException(
            ResponseCode.csvError.getErrorCode(),
            ResponseCode.csvError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      // tell sender that csv file is empty
      throw new ProjectCommonException(
          ResponseCode.csvError.getErrorCode(),
          ResponseCode.csvError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    // convert userMapList to json string
    Map<String, Object> map = new HashMap<>();

    try {
      map.put(JsonKey.DATA, mapper.writeValueAsString(dataMapList));
    } catch (IOException e) {
      ProjectLogger.log(
          "BulkUploadManagementActor:uploadCsvToDB: Exception while converting map to string: "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    map.put(JsonKey.ID, processId);
    map.put(JsonKey.OBJECT_TYPE, objectType);
    map.put(JsonKey.UPLOADED_BY, requestedBy);
    map.put(JsonKey.UPLOADED_DATE, ProjectUtil.getFormattedDate());
    map.put(JsonKey.PROCESS_START_TIME, ProjectUtil.getFormattedDate());
    map.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.NEW.getValue());
    Response res =
        cassandraOperation.insertRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), map);
    res.put(JsonKey.PROCESS_ID, processId);
    ProjectLogger.log(
        "BulkUploadManagementActor: uploadCsvToDB returned response for processId: " + processId,
        LoggerEnum.INFO);
    sender().tell(res, self());
    if (((String) res.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      // send processId for data processing to background job
      Request request = new Request();
      request.put(JsonKey.PROCESS_ID, processId);
      request.setOperation(ActorOperations.PROCESS_BULK_UPLOAD.getValue());
      tellToAnother(request);
    }
    ProjectLogger.log(
        "BulkUploadManagementActor: uploadCsvToDB completed processing for processId: " + processId,
        LoggerEnum.INFO);
  }
}
