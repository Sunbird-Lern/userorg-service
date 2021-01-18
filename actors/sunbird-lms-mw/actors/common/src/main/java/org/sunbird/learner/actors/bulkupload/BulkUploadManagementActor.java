package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.BulkProcessStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.CloudStorageUtil;
import org.sunbird.common.util.CloudStorageUtil.CloudStorageType;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessTaskDao;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessDaoImpl;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.learner.actors.bulkupload.model.StorageDetails;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import scala.concurrent.Future;

/** This actor will handle bulk upload operation . */
@ActorConfig(
  tasks = {"bulkUpload", "getBulkOpStatus", "getBulkUploadStatusDownloadLink"},
  asyncTasks = {}
)
public class BulkUploadManagementActor extends BaseBulkUploadActor {

  private BulkUploadProcessTaskDao bulkUploadProcessTaskDao = new BulkUploadProcessTaskDaoImpl();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
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
    BulkUploadProcess bulkUploadProcess =
        bulkuploadDao.read(processId, actorMessage.getRequestContext());
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
    ObjectMapper mapper = new ObjectMapper();
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
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
    Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    response =
        cassandraOperation.getRecordById(
            bulkDb.getKeySpace(),
            bulkDb.getTableName(),
            processId,
            fields,
            actorMessage.getRequestContext());
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
                      decryptionService.decryptData(
                          (String) resMap.get(JsonKey.SUCCESS_RESULT),
                          actorMessage.getRequestContext()),
                      Object[].class);
              if (JsonKey.USER.equalsIgnoreCase(objectType)) {
                Arrays.stream(successMap)
                    .forEach(
                        x -> {
                          UserUtility.decryptUserData((Map<String, Object>) x);
                          Util.addMaskEmailAndPhone((Map<String, Object>) x);
                        });
              }
              resMap.put(JsonKey.SUCCESS_RESULT, successMap);
            }
            if (null != resMap.get(JsonKey.FAILURE_RESULT)) {
              failureMap =
                  mapper.readValue(
                      decryptionService.decryptData(
                          (String) resMap.get(JsonKey.FAILURE_RESULT),
                          actorMessage.getRequestContext()),
                      Object[].class);
              if (JsonKey.USER.equalsIgnoreCase(objectType)) {
                Arrays.stream(successMap)
                    .forEach(
                        x -> {
                          UserUtility.decryptUserData((Map<String, Object>) x);
                          Util.addMaskEmailAndPhone((Map<String, Object>) x);
                        });
              }
              resMap.put(JsonKey.FAILURE_RESULT, failureMap);
            }
          } catch (IOException e) {
            logger.error(actorMessage.getRequestContext(), e.getMessage(), e);
          }
        } else {
          Map<String, Object> queryMap = new HashMap<>();
          queryMap.put(JsonKey.PROCESS_ID, processId);
          List<BulkUploadProcessTask> tasks =
              bulkUploadProcessTaskDao.readByPrimaryKeys(
                  queryMap, actorMessage.getRequestContext());

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
      ObjectMapper mapper = new ObjectMapper();
      list.add(mapper.readValue(data, Map.class));
    } catch (IOException ex) {
      logger.error("Error while converting success list to map" + ex.getMessage(), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private void upload(Request actorMessage) throws IOException {
    String processId = ProjectUtil.getUniqueIdFromTimestamp(1);
    Map<String, Object> req = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.DATA);
    req.put(JsonKey.CREATED_BY, req.get(JsonKey.CREATED_BY));
    if (((String) req.get(JsonKey.OBJECT_TYPE)).equals(JsonKey.USER)) {
      processBulkUserUpload(req, processId, actorMessage.getRequestContext());
    } else if (((String) req.get(JsonKey.OBJECT_TYPE)).equals(JsonKey.ORGANISATION)) {
      processBulkOrgUpload(req, processId, actorMessage.getRequestContext());
    }
  }

  private void processBulkOrgUpload(
      Map<String, Object> req, String processId, RequestContext context) throws IOException {
    int orgDataSize = 0;
    logger.info(context, "BulkUploadManagementActor: processBulkOrgUpload called.");
    List<String[]> orgList = null;
    orgList = parseCsvFile((byte[]) req.get(JsonKey.FILE), processId, context);
    if (null != orgList) {
      if (null != PropertiesCache.getInstance().getProperty(JsonKey.BULK_UPLOAD_ORG_DATA_SIZE)) {
        orgDataSize =
            (Integer.parseInt(
                PropertiesCache.getInstance().getProperty(JsonKey.BULK_UPLOAD_ORG_DATA_SIZE)));
        logger.info(context, "bulk upload org data size read from config file " + orgDataSize);
      }
      validateFileSizeAgainstLineNumbers(orgDataSize, orgList.size());
      if (!orgList.isEmpty()) {
        String[] columns = orgList.get(0);
        String[] bulkOrgAllowedFields = DataCacheHandler.bulkOrgAllowedFields;
        validateBulkUploadFields(columns, bulkOrgAllowedFields, false);
      } else {
        throw new ProjectCommonException(
            ResponseCode.dataSizeError.getErrorCode(),
            ProjectUtil.formatMessage(ResponseCode.dataSizeError.getErrorMessage(), orgDataSize),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.dataSizeError.getErrorCode(),
          ProjectUtil.formatMessage(ResponseCode.dataSizeError.getErrorMessage(), orgDataSize),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    // save csv file to db
    uploadCsvToDB(
        orgList,
        processId,
        null,
        JsonKey.ORGANISATION,
        (String) req.get(JsonKey.CREATED_BY),
        null,
        context);
  }

  private void processBulkUserUpload(
      Map<String, Object> req, String processId, RequestContext context) {
    DbInfo orgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    String orgId = "";
    Response response = null;
    if (!StringUtils.isBlank((String) req.get(JsonKey.ORGANISATION_ID))) {
      response =
          cassandraOperation.getRecordById(
              orgDb.getKeySpace(),
              orgDb.getTableName(),
              (String) req.get(JsonKey.ORGANISATION_ID),
              context);
    } else {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.EXTERNAL_ID, ((String) req.get(JsonKey.ORG_EXTERNAL_ID)).toLowerCase());
      filters.put(JsonKey.PROVIDER, ((String) req.get(JsonKey.ORG_PROVIDER)).toLowerCase());
      SearchDTO searchDto = new SearchDTO();
      searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filters);
      Future<Map<String, Object>> resultF =
          esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
      Map<String, Object> result =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
      List<Map<String, Object>> dataMapList =
          (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
      response.getResult().put(JsonKey.RESPONSE, dataMapList);
    }
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

    if (responseList.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidOrgData.getErrorCode(),
          ResponseCode.invalidOrgData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else {
      orgId = (String) responseList.get(0).get(JsonKey.ID);
    }

    String rootOrgId = "";
    Map<String, Object> orgMap = responseList.get(0);
    boolean isRootOrg = false;
    if (null != orgMap.get(JsonKey.IS_ROOT_ORG)) {
      isRootOrg = (boolean) orgMap.get(JsonKey.IS_ROOT_ORG);
    } else {
      isRootOrg = false;
    }
    if (isRootOrg) {
      rootOrgId = orgId;
    } else {
      if (!StringUtils.isBlank((String) orgMap.get(JsonKey.ROOT_ORG_ID))) {
        rootOrgId = (String) orgMap.get(JsonKey.ROOT_ORG_ID);
      } else {
        String msg = "";
        if (StringUtils.isBlank((String) req.get(JsonKey.ORGANISATION_ID))) {
          msg =
              ((String) req.get(JsonKey.ORG_EXTERNAL_ID))
                  + " and "
                  + ((String) req.get(JsonKey.ORG_PROVIDER));
        } else {
          msg = (String) req.get(JsonKey.ORGANISATION_ID);
        }
        throw new ProjectCommonException(
            ResponseCode.rootOrgAssociationError.getErrorCode(),
            ProjectUtil.formatMessage(ResponseCode.rootOrgAssociationError.getErrorMessage(), msg),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    List<String[]> userList = null;
    try {
      userList = parseCsvFile((byte[]) req.get(JsonKey.FILE), processId, context);
    } catch (IOException e) {
      throw new ProjectCommonException(
          ResponseCode.csvError.getErrorCode(),
          ResponseCode.csvError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    int userDataSize = 0;
    if (null != userList) {
      if (StringUtils.isNotBlank(ProjectUtil.getConfigValue(JsonKey.BULK_UPLOAD_USER_DATA_SIZE))) {
        userDataSize =
            (Integer.parseInt(
                ProjectUtil.getConfigValue(JsonKey.BULK_UPLOAD_USER_DATA_SIZE).trim()));

        logger.info(
            context,
            "BulkUploadManagementActor:processBulkUserUpload : bulk upload user data size"
                + userDataSize);
      }
      validateFileSizeAgainstLineNumbers(userDataSize, userList.size());
      String[] bulkUserAllowedFields = DataCacheHandler.bulkUserAllowedFields;
      if (!userList.isEmpty()) {
        String[] columns = userList.get(0);
        validateBulkUploadFields(columns, bulkUserAllowedFields, false);
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
        userList,
        processId,
        orgId,
        JsonKey.USER,
        (String) req.get(JsonKey.CREATED_BY),
        rootOrgId,
        context);
  }

  private void uploadCsvToDB(
      List<String[]> dataList,
      String processId,
      String orgId,
      String objectType,
      String requestedBy,
      String rootOrgId,
      RequestContext context) {
    logger.info(context, "BulkUploadManagementActor: uploadCsvToDB called.");
    List<Map<String, Object>> dataMapList = new ArrayList<>();
    if (dataList.size() > 1) {
      try {
        String[] columnArr = dataList.get(0);
        columnArr = trimColumnAttributes(columnArr);
        Map<String, Object> dataMap = null;
        String channel = null;
        // channel is required only in case of the user type bulk upload.
        if (StringUtils.isNotBlank(objectType) && objectType.equalsIgnoreCase(JsonKey.USER)) {
          channel = Util.getChannel(rootOrgId, context);
        }
        for (int i = 1; i < dataList.size(); i++) {
          dataMap = new HashMap<>();
          String[] valueArr = dataList.get(i);
          for (int j = 0; j < valueArr.length; j++) {
            String value = (valueArr[j].trim().length() == 0 ? null : valueArr[j].trim());
            dataMap.put(columnArr[j], value);
          }
          if (!StringUtils.isBlank(objectType) && objectType.equalsIgnoreCase(JsonKey.USER)) {
            dataMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
            dataMap.put(JsonKey.ORGANISATION_ID, orgId);
            dataMap.put(JsonKey.CHANNEL, channel);
          }
          dataMapList.add(dataMap);
        }
      } catch (Exception e) {
        logger.error(context, e.getMessage(), e);
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
      ObjectMapper mapper = new ObjectMapper();
      map.put(JsonKey.DATA, mapper.writeValueAsString(dataMapList));
    } catch (IOException e) {
      logger.error(
          context,
          "BulkUploadManagementActor:uploadCsvToDB: Exception while converting map to string: "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    map.put(JsonKey.ID, processId);
    map.put(JsonKey.OBJECT_TYPE, objectType);
    map.put(JsonKey.UPLOADED_BY, requestedBy);
    map.put(JsonKey.UPLOADED_DATE, ProjectUtil.getFormattedDate());
    map.put(JsonKey.PROCESS_START_TIME, ProjectUtil.getFormattedDate());
    map.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.NEW.getValue());
    Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    Response res =
        cassandraOperation.insertRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), map, context);
    res.put(JsonKey.PROCESS_ID, processId);
    logger.info(
        context,
        "BulkUploadManagementActor: uploadCsvToDB returned response for processId: " + processId);
    sender().tell(res, self());
    if (((String) res.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      // send processId for data processing to background job
      Request request = new Request();
      request.setRequestContext(context);
      request.put(JsonKey.PROCESS_ID, processId);
      request.setOperation(ActorOperations.PROCESS_BULK_UPLOAD.getValue());
      tellToAnother(request);
    }
    logger.info(
        context,
        "BulkUploadManagementActor: uploadCsvToDB completed processing for processId: "
            + processId);
  }
}
