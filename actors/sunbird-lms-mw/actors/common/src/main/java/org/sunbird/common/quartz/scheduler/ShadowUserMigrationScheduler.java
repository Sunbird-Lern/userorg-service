package org.sunbird.common.quartz.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.bean.ClaimStatus;
import org.sunbird.bean.MigrationUser;
import org.sunbird.bean.ShadowUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ShadowUserProcessor;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.model.BulkMigrationUser;
import org.sunbird.learner.util.Util;

public class ShadowUserMigrationScheduler extends BaseJob {

  private Util.DbInfo bulkUploadDbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private HashSet<String> verifiedChannelOrgExternalIdSet = new HashSet<>();
  private ElasticSearchService elasticSearchService = EsClientFactory.getInstance(JsonKey.REST);
  private DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:execute:checking scheduler workflow", LoggerEnum.INFO.name());
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:execute:Running Shadow User Upload Scheduler Job at: "
            + Calendar.getInstance().getTime()
            + " triggered by: "
            + jobExecutionContext.getJobDetail().toString(),
        LoggerEnum.INFO.name());
    Util.initializeContextForSchedulerJob(
        JsonKey.SYSTEM, jobExecutionContext.getFireInstanceId(), JsonKey.SCHEDULER_JOB);
    startMigration();
  }

  public void startMigration() {
    List<String> unprocessedRecordIds = getUnprocessedRecordIds();
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:startMigration:Got Bulk Upload Db migrations started",
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:startMigration:Got Bulk Upload Db unprocessed and failed records size is:"
            + unprocessedRecordIds.size(),
        LoggerEnum.INFO.name());
    processRecords(unprocessedRecordIds);
    ShadowUserProcessor processorObject = new ShadowUserProcessor();
    processorObject.process();
    unprocessedRecordIds.clear();
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:processRecords:Scheduler Job Ended for ShadowUser Migration",
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:execute:Scheduler Job ended for shadow user migration",
        LoggerEnum.INFO.name());
  }

  /**
   * - fetch rows from bulk upload table whose status is less than 2 - update the bulk upload table
   * row status to processing - encrypting email and phone - start processing single row - fetch the
   * single row data - Get List of Migration(csv) user from the row - process each Migration(csv)
   * user - update the bulk upload table row status to completed and add message to success - if
   * fails update the bulk upload row status to failed and add failureResult
   */
  private void processRecords(List<String> unprocessedRecordIds) {
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:processRecords:Scheduler Job Started for ShadowUser Migration",
        LoggerEnum.INFO.name());
    unprocessedRecordIds
        .stream()
        .forEach(
            id -> {
              Map<String, Object> row = getFullRecordFromProcessId(id);
              BulkMigrationUser bulkMigrationUser = convertRowToObject(row);
              row.clear();
              try {
                updateStatusInUserBulkTable(
                    bulkMigrationUser.getId(),
                    ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
                List<MigrationUser> migrationUserList = getMigrationUserAsList(bulkMigrationUser);
                migrationUserList
                    .parallelStream()
                    .forEach(
                        singleMigrationUser -> {
                          encryptEmailAndPhone(singleMigrationUser);
                          processSingleMigUser(
                              bulkMigrationUser.getCreatedBy(),
                              bulkMigrationUser.getId(),
                              singleMigrationUser);
                        });
                updateMessageInBulkUserTable(
                    bulkMigrationUser.getId(), JsonKey.SUCCESS_RESULT, JsonKey.SUCCESS);
              } catch (Exception e) {
                ProjectLogger.log(
                    "ShadowUserMigrationScheduler:processRecords:error occurred ".concat(e + ""),
                    LoggerEnum.ERROR.name());
                updateMessageInBulkUserTable(
                    bulkMigrationUser.getId(), JsonKey.FAILURE_RESULT, e.getMessage());
              }
            });
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:processRecords:started stage3__________________________-",
        LoggerEnum.INFO.name());
  }

  private void processSingleMigUser(
      String createdBy, String processId, MigrationUser singleMigrationUser) {
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:processSingleMigUser:Single migration User Started processing with processId:"
            + processId,
        LoggerEnum.INFO.name());
    Map<String, Object> existingUserDetails =
        getShadowExistingUserDetails(
            singleMigrationUser.getChannel(), singleMigrationUser.getUserExternalId());
    if (MapUtils.isEmpty(existingUserDetails)) {
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:processSingleMigUser:existing user not found with processId:"
              + processId,
          LoggerEnum.INFO.name());
      insertShadowUserToDb(createdBy, processId, singleMigrationUser);
    } else {
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:processSingleMigUser:existing user found with processId:"
              + processId,
          LoggerEnum.INFO.name());
      ShadowUser shadowUser = mapper.convertValue(existingUserDetails, ShadowUser.class);
      updateUser(processId, singleMigrationUser, shadowUser);
    }
  }

  /**
   * this method will read rows from the bulk_upload_process table who has status less than 2
   *
   * @return list
   */
  private List<String> getUnprocessedRecordIds() {
    Response response =
        cassandraOperation.getRecordByObjectType(
            bulkUploadDbInfo.getKeySpace(),
            bulkUploadDbInfo.getTableName(),
            JsonKey.ID,
            JsonKey.STATUS,
            ProjectUtil.BulkProcessStatus.INTERRUPT.getValue(),
            JsonKey.MIGRATION_USER_OBJECT);
    List<Map<String, Object>> result = new ArrayList<>();
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((List) response.getResult().get(JsonKey.RESPONSE));
    }
    List<String> processIds = new ArrayList<>();
    result
        .stream()
        .forEach(
            rowMap -> {
              processIds.add((String) rowMap.get(JsonKey.ID));
            });
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:getUnprocessedRecordIds:got rows from Bulk user table is:"
            .concat(result.size() + ""),
        LoggerEnum.INFO.name());
    return processIds;
  }

  private Map<String, Object> getFullRecordFromProcessId(String processId) {

    int FIRST_RECORD = 0;
    Response response =
        cassandraOperation.getRecordById(
            bulkUploadDbInfo.getKeySpace(), bulkUploadDbInfo.getTableName(), processId);
    List<Map<String, Object>> result = new ArrayList<>();
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((List) response.getResult().get(JsonKey.RESPONSE));
    }
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:getFullRecordFromProcessId:got single row data from bulk_upload_process with processId:"
            + processId,
        LoggerEnum.INFO.name());
    return result.get(FIRST_RECORD);
  }

  private BulkMigrationUser convertRowToObject(Map<String, Object> row) {
    BulkMigrationUser bulkMigrationUser = null;
    try {
      bulkMigrationUser = mapper.convertValue(row, BulkMigrationUser.class);
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:convertMapToMigrationObject:error occurred while converting map to pojo"
              .concat(e.getMessage() + ""),
          LoggerEnum.ERROR.name());
    }
    return bulkMigrationUser;
  }

  private List<MigrationUser> getMigrationUserAsList(BulkMigrationUser bulkMigrationUser) {
    List<MigrationUser> migrationUserList = new ArrayList<>();
    try {
      String decryptedData = decryptionService.decryptData(bulkMigrationUser.getData());
      migrationUserList =
          mapper.readValue(decryptedData, new TypeReference<List<MigrationUser>>() {});
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:getMigrationUserAsList:error occurred while converting map to POJO: "
              + e,
          LoggerEnum.ERROR.name());
    }
    return migrationUserList;
  }

  private Map<String, Object> getShadowExistingUserDetails(String channel, String userExtId) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.CHANNEL, channel);
    propertiesMap.put("userExtId", userExtId);
    Map<String, Object> result = new HashMap<>();
    Response response =
        cassandraOperation.getRecordsByProperties(
            JsonKey.SUNBIRD, JsonKey.SHADOW_USER, propertiesMap);
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((Map) ((List) response.getResult().get(JsonKey.RESPONSE)).get(0));
    }
    return result;
  }

  /**
   * this method will prepare the shawdow user object and insert the record into shawdow_user table
   *
   * @param createdBy
   * @param processId
   * @param migrationUser
   */
  private void insertShadowUserToDb(
      String createdBy, String processId, MigrationUser migrationUser) {
    Map<String, Object> dbMap = new WeakHashMap<>();
    dbMap.put(JsonKey.USER_EXT_ID, migrationUser.getUserExternalId());
    dbMap.put(JsonKey.ORG_EXT_ID, migrationUser.getOrgExternalId());
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:insertShadowUser: email got  "
            + migrationUser.getEmail()
            + " "
            + migrationUser.getPhone(),
        LoggerEnum.INFO.name());
    dbMap.put(JsonKey.EMAIL, migrationUser.getEmail());
    dbMap.put(JsonKey.PHONE, migrationUser.getPhone());
    dbMap.put(JsonKey.ADDED_BY, createdBy);
    dbMap.put(JsonKey.CHANNEL, migrationUser.getChannel());
    dbMap.put(JsonKey.NAME, migrationUser.getName());
    dbMap.put(JsonKey.PROCESS_ID, processId);
    dbMap.put(JsonKey.ATTEMPTED_COUNT, 0);
    dbMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.UNCLAIMED.getValue());
    dbMap.put(JsonKey.USER_STATUS, getInputStatus(migrationUser.getInputStatus()));
    dbMap.put(JsonKey.CREATED_ON, new Timestamp(System.currentTimeMillis()));
    if (!isOrgExternalIdValid(migrationUser)) {
      dbMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.ORGEXTERNALIDMISMATCH.getValue());
    }
    Response response =
        cassandraOperation.insertRecord(JsonKey.SUNBIRD, JsonKey.SHADOW_USER, dbMap);
    dbMap.clear();
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:insertShadowUser: record status in cassandra "
            .concat(response + ""),
        LoggerEnum.INFO.name());
  }

  private int getInputStatus(String inputStatus) {
    if (inputStatus.equalsIgnoreCase(JsonKey.ACTIVE)) {
      return ProjectUtil.Status.ACTIVE.getValue();
    }
    return ProjectUtil.Status.INACTIVE.getValue();
  }

  private void updateUser(String processId, MigrationUser migrationUser, ShadowUser shadowUser) {
    updateUserInShadowDb(processId, migrationUser, shadowUser);
  }

  /**
   * @param processId
   * @param migrationUser
   * @param existingShadowUser
   */
  private void updateUserInShadowDb(
      String processId, MigrationUser migrationUser, ShadowUser existingShadowUser) {
    int currentClaimStatus = existingShadowUser.getClaimStatus();
    int newClaimStatus = currentClaimStatus;
    boolean isClaimed = (currentClaimStatus == ClaimStatus.CLAIMED.getValue());
    boolean isNotSame = !isSame(existingShadowUser, migrationUser);

    if (isNotSame && !isOrgExternalIdValid(migrationUser)) {
      newClaimStatus = ClaimStatus.ORGEXTERNALIDMISMATCH.getValue();
    } else if (!isClaimed) {
      // Allow all non-claimed users another chance
      newClaimStatus = ClaimStatus.UNCLAIMED.getValue();
    }

    if (isNotSame || (currentClaimStatus != newClaimStatus)) {
      Map<String, Object> propertiesMap = populateUserMap(processId, migrationUser);

      propertiesMap.put(JsonKey.CLAIM_STATUS, newClaimStatus);
      propertiesMap.put(
          JsonKey.ATTEMPTED_COUNT,
          0); // we wanted to reset the attempted count for the failed records

      Map<String, Object> compositeKeysMap = new HashMap<>();
      compositeKeysMap.put(JsonKey.CHANNEL, migrationUser.getChannel());
      compositeKeysMap.put(JsonKey.USER_EXT_ID, migrationUser.getUserExternalId());

      cassandraOperation.updateRecord(
          JsonKey.SUNBIRD, JsonKey.SHADOW_USER, propertiesMap, compositeKeysMap);

      if (isClaimed) {
        ProjectLogger.log(
            "ShadowUserMigrationScheduler:updateUserInShadowDb: isClaimed ",
            LoggerEnum.INFO.name());
        ShadowUser newShadowUser = getUpdatedShadowUser(compositeKeysMap);
        new ShadowUserProcessor().processClaimedUser(newShadowUser);
      }
    }

    ProjectLogger.log(
        "ShadowUserMigrationScheduler:updateUserInShadowDb channel:"
            + migrationUser.getChannel()
            + " userExternalId: "
            + migrationUser.getUserExternalId()
            + " isNotSame: "
            + isNotSame
            + " newClaimStatus:"
            + newClaimStatus,
        LoggerEnum.INFO.name());
  }

  private Map<String, Object> populateUserMap(String processId, MigrationUser migrationUser) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.EMAIL, migrationUser.getEmail());
    propertiesMap.put(JsonKey.PHONE, migrationUser.getPhone());
    propertiesMap.put(JsonKey.PROCESS_ID, processId);
    propertiesMap.put(JsonKey.NAME, migrationUser.getName());
    propertiesMap.put(JsonKey.ORG_EXT_ID, migrationUser.getOrgExternalId());
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    propertiesMap.put(JsonKey.USER_STATUS, getInputStatus(migrationUser.getInputStatus()));
    return propertiesMap;
  }

  /**
   * this method will return the updated shadow user object when new orgExtId , name is been passed
   *
   * @param compositeKeysMap
   * @return shawdow user
   */
  private ShadowUser getUpdatedShadowUser(Map<String, Object> compositeKeysMap) {
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            JsonKey.SUNBIRD, JsonKey.SHADOW_USER, compositeKeysMap);
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:getUpdatedShadowUser: record status in cassandra for getting the updated shawdow user object "
            .concat(response.getResult() + ""),
        LoggerEnum.INFO.name());
    Map<String, Object> resultmap =
        ((List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE)).get(0);
    ShadowUser shadowUser = mapper.convertValue(resultmap, ShadowUser.class);
    return shadowUser;
  }

  private void updateStatusInUserBulkTable(String processId, int statusVal) {
    try {
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:updateStatusInUserBulkTable: got status to change in bulk upload table"
                  .concat(statusVal + "")
              + "with processId"
              + processId,
          LoggerEnum.INFO.name());
      Map<String, Object> propertiesMap = new WeakHashMap<>();
      propertiesMap.put(JsonKey.ID, processId);
      propertiesMap.put(JsonKey.STATUS, statusVal);
      updateBulkUserTable(propertiesMap);
    } catch (Exception e) {
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:updateStatusInUserBulkTable: status update failed"
                  .concat(e + "")
              + "with processId"
              + processId,
          LoggerEnum.ERROR.name());
    }
  }

  private void updateBulkUserTable(Map<String, Object> propertiesMap) {
    Response response =
        cassandraOperation.updateRecord(
            bulkUploadDbInfo.getKeySpace(), bulkUploadDbInfo.getTableName(), propertiesMap);
    ProjectLogger.log(
        "ShadowUserMigrationScheduler:updateBulkUserTable: status update result"
            .concat(response + ""),
        LoggerEnum.INFO.name());
  }

  private void updateMessageInBulkUserTable(String processId, String key, String value) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.ID, processId);
    propertiesMap.put(key, value);
    if (StringUtils.equalsIgnoreCase(value, JsonKey.SUCCESS)) {
      propertiesMap.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
      propertiesMap.put(JsonKey.FAILURE_RESULT, "");
    } else {
      propertiesMap.put(JsonKey.SUCCESS_RESULT, "");
      propertiesMap.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.FAILED.getValue());
    }
    updateBulkUserTable(propertiesMap);
    propertiesMap.clear();
  }

  /**
   * this method will take descision wheather to update the record or not.
   *
   * @param shadowUser
   * @param migrationUser
   * @return boolean
   */
  private boolean isSame(ShadowUser shadowUser, MigrationUser migrationUser) {

    if (StringUtils.isBlank(migrationUser.getOrgExternalId())) {
      return false;
    }
    if (!shadowUser.getName().equalsIgnoreCase(migrationUser.getName())) {
      return false;
    }
    if (!StringUtils.equalsIgnoreCase(shadowUser.getEmail(), migrationUser.getEmail())) {
      return false;
    }
    if (!StringUtils.equalsIgnoreCase(shadowUser.getOrgExtId(), migrationUser.getOrgExternalId())) {
      return false;
    }
    if (!StringUtils.equalsIgnoreCase(shadowUser.getPhone(), migrationUser.getPhone())) {
      return false;
    }
    if ((getInputStatus(migrationUser.getInputStatus()) != shadowUser.getUserStatus())) {
      return false;
    }

    if (!migrationUser.getName().equalsIgnoreCase(shadowUser.getName())) {
      return false;
    }
    return true;
  }

  /**
   * this method will check weather the provided orgExternalId is correct or not
   *
   * @param migrationUser
   * @return true if correct else false
   */
  private boolean isOrgExternalIdValid(MigrationUser migrationUser) {
    if (StringUtils.isBlank(migrationUser.getOrgExternalId())) {
      return true;
    } else if (verifiedChannelOrgExternalIdSet.contains(
        migrationUser.getChannel() + ":" + migrationUser.getOrgExternalId())) {
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:isOrgExternalIdValid: found orgexternalid in cache:"
              + migrationUser.getOrgExternalId(),
          LoggerEnum.INFO.name());
      return true;
    }
    Map<String, Object> request = new WeakHashMap<>();
    Map<String, Object> filters = new WeakHashMap<>();
    filters.put(JsonKey.EXTERNAL_ID, migrationUser.getOrgExternalId());
    filters.put(JsonKey.CHANNEL, migrationUser.getChannel());
    request.put(JsonKey.FILTERS, filters);
    SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(request);
    searchDTO.setFields(new ArrayList<>(Arrays.asList(JsonKey.ID)));
    Map<String, Object> response =
        (Map<String, Object>)
            ElasticSearchHelper.getResponseFromFuture(
                elasticSearchService.search(
                    searchDTO, ProjectUtil.EsType.organisation.getTypeName()));
    if (CollectionUtils.isNotEmpty((List<Map<String, Object>>) response.get(JsonKey.CONTENT))) {
      verifiedChannelOrgExternalIdSet.add(
          migrationUser.getChannel() + ":" + migrationUser.getOrgExternalId());
      response.clear();
      return true;
    }
    response.clear();
    return false;
  }

  private String encryptValue(String key) {
    try {
      return encryptionService.encryptData(key);
    } catch (Exception e) {
      ProjectLogger.log(
          "ShadowUserMigrationScheduler:getEncryptedValue: error occurred in encrypting value "
              + key,
          LoggerEnum.ERROR.name());
      return key;
    }
  }

  private void encryptEmailAndPhone(MigrationUser migrationUser) {

    if (StringUtils.isNotBlank(migrationUser.getEmail())) {
      migrationUser.setEmail(encryptValue(migrationUser.getEmail().toLowerCase()));
    }
    if (StringUtils.isNotBlank(migrationUser.getPhone())) {
      migrationUser.setPhone(encryptValue(migrationUser.getPhone()));
    }
  }
}
