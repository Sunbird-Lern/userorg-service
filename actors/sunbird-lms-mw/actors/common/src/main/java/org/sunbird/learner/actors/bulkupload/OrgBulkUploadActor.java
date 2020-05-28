package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.core.type.TypeReference;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessDao;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessDaoImpl;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ActorConfig(
  tasks = {"orgBulkUpload"},
  asyncTasks = {}
)
public class OrgBulkUploadActor extends BaseBulkUploadActor {

  private SystemSettingClient systemSettingClient = new SystemSettingClientImpl();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);


  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.ORGANISATION);
    String operation = request.getOperation();

    if (operation.equalsIgnoreCase("orgBulkUpload")) {
      upload(request);
    } else {
      onReceiveUnsupportedOperation("OrgBulkUploadActor");
    }
  }

  private void upload(Request request) throws IOException {
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    Object dataObject =
        systemSettingClient.getSystemSettingByFieldAndKey(
            getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
            "orgProfileConfig",
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
      validateFileHeaderFields(req, DataCacheHandler.bulkOrgAllowedFields, false, false);
    }
    BulkUploadProcess bulkUploadProcess =
        handleUpload(JsonKey.ORGANISATION, (String) req.get(JsonKey.CREATED_BY));
    processOrgBulkUpload(
        req, bulkUploadProcess.getId(), bulkUploadProcess, supportedColumnsLowerCaseMap);
  }

  private void processOrgBulkUpload(
      Map<String, Object> req,
      String processId,
      BulkUploadProcess bulkUploadProcess,
      Map<String, Object> supportedColumnsMap)
      throws IOException {
    byte[] fileByteArray = null;
    if (null != req.get(JsonKey.FILE)) {
      fileByteArray = (byte[]) req.get(JsonKey.FILE);
    }
    HashMap<String, Object> additionalInfo = new HashMap<>();
    Map<String, Object> user = getUser((String) req.get(JsonKey.CREATED_BY));
    if (user != null) {
      String rootOrgId = (String) user.get(JsonKey.ROOT_ORG_ID);
      Map<String, Object> org = getOrg(rootOrgId);
      if (org != null) {
        if (org.get(JsonKey.STATUS) == null
            || (int) org.get(JsonKey.STATUS) == ProjectUtil.OrgStatus.ACTIVE.getValue()) {
          additionalInfo.put(JsonKey.CHANNEL, org.get(JsonKey.CHANNEL));
        }
      }
    }
    if (!additionalInfo.containsKey(JsonKey.CHANNEL)) {
      bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
      bulkUploadProcess.setFailureResult(ResponseCode.errorNoRootOrgAssociated.getErrorMessage());
      BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
      bulkUploadDao.update(bulkUploadProcess);
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorNoRootOrgAssociated,
          ResponseCode.errorNoRootOrgAssociated.getErrorMessage());
    }
    Integer recordCount =
        validateAndParseRecords(
            fileByteArray, processId, additionalInfo, supportedColumnsMap, true);
    processBulkUpload(
        recordCount,
        processId,
        bulkUploadProcess,
        BulkUploadActorOperation.ORG_BULK_UPLOAD_BACKGROUND_JOB.getValue(),
        DataCacheHandler.bulkOrgAllowedFields);
  }

  Map<String, Object> getUser(String userId) {
    Future<Map<String, Object>> resultF =
            esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result != null || result.size() > 0) {
      return result;
    }
    return null;
  }

  Map<String, Object> getOrg(String orgId) {
    Future<Map<String, Object>> resultF =
            esService.getDataByIdentifier(ProjectUtil.EsType.organisation.getTypeName(), orgId);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result != null && result.size() > 0) {
      return result;
    }
    return null;
  }
}
