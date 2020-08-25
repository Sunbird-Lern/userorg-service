package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.mchange.v1.util.ArrayUtils;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.bean.MigrationUser;
import org.sunbird.bean.SelfDeclaredUser;
import org.sunbird.bean.ShadowUserUpload;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.model.BulkMigrationUser;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.telemetry.util.TelemetryUtil;

/** @author anmolgupta */
@ActorConfig(
  tasks = {"userBulkMigration", "userBulkSelfDeclared"},
  asyncTasks = {}
)
public class UserBulkMigrationActor extends BaseBulkUploadActor {

  private SystemSettingClient systemSettingClient = new SystemSettingClientImpl();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  public static final int RETRY_COUNT = 2;
  private CSVReader csvReader;
  private static SystemSetting systemSetting;

  @Override
  public void onReceive(Request request) throws Throwable {
    String env = null;
    String operation = request.getOperation();
    ProjectLogger.log("OnReceive Upload csv processing " + operation, LoggerEnum.INFO.name());
    if (operation.equals("userBulkSelfDeclared")) {
      env = "SelfDeclaredUserUpload";
    } else {
      env = "ShadowUserUpload";
    }
    Util.initializeContext(request, env);
    if (operation.equalsIgnoreCase(BulkUploadActorOperation.USER_BULK_MIGRATION.getValue())) {
      uploadCsv(request);
    } else if (operation.equalsIgnoreCase(
        BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue())) {
      uploadSelfDeclaredCSV(request);
    } else {
      onReceiveUnsupportedOperation("userBulkMigration");
    }
  }

  private void uploadCsv(Request request) throws IOException {
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    systemSetting =
        systemSettingClient.getSystemSettingByField(
            getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()), "shadowdbmandatorycolumn");
    processCsvBytes(req, request);
  }

  private void uploadSelfDeclaredCSV(Request request) throws IOException {
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    processCsvBytes(req, request);
  }

  private void processCsvBytes(Map<String, Object> data, Request request) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> targetObject = null;
    Map<String, Object> values = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    String processId = ProjectUtil.getUniqueIdFromTimestamp(1);
    long validationStartTime = System.currentTimeMillis();
    String userId = getCreatedBy(request);
    Map<String, Object> result = getUserById(userId);
    String channel = getChannel(result);
    String rootOrgId = getRootOrgId(result);
    request.getRequest().put(JsonKey.ROOT_ORG_ID, rootOrgId);
    BulkMigrationUser migrationUser = null;

    if (request
        .getOperation()
        .equals(BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue())) {
      /* PropertiesCache propertiesCache = PropertiesCache.getInstance();
      String mandatoryFields = propertiesCache.getProperty(JsonKey.SELF_DECLARED_MANDATORY_FIELDS);
      String optionalFields = propertiesCache.getProperty(JsonKey.SELF_DECLARED_OPTIONAL_FIELDS);*/
      List<String> mandatoryFields =
          UserUtility.getTenantMandatoryFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS);
      List<String> optionalFields =
          UserUtility.getTenantOptionalFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS);
      Map<String, String> aliasFields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      aliasFields.putAll(UserUtility.getTenantAliasFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS));
      Map fieldsMap = new HashMap();
      fieldsMap.put("mandatoryFields", mandatoryFields);
      fieldsMap.put("optionalFields", optionalFields);
      List<SelfDeclaredUser> selfDeclaredUserList =
          getUsers(processId, (byte[]) data.get(JsonKey.FILE), fieldsMap, aliasFields);

      ProjectLogger.log(
          "UserBulkMigrationActor:processRecord: time taken to validate records of size "
                  .concat(selfDeclaredUserList.size() + "")
              + "is(ms): ".concat((System.currentTimeMillis() - validationStartTime) + ""),
          LoggerEnum.INFO.name());
      migrationUser = prepareSelfDeclaredRecord(request, processId, selfDeclaredUserList);
      ProjectLogger.log(
          "UserBulkMigrationActor:processRecord:processing record for number of users "
              .concat(selfDeclaredUserList.size() + ""),
          LoggerEnum.INFO.name());
    } else {
      values = mapper.readValue(systemSetting.getValue(), Map.class);
      List<MigrationUser> migrationUserList =
          getMigrationUsers(channel, processId, (byte[]) data.get(JsonKey.FILE), values);
      ProjectLogger.log(
          "UserBulkMigrationActor:processRecord: time taken to validate records of size "
                  .concat(migrationUserList.size() + "")
              + "is(ms): ".concat((System.currentTimeMillis() - validationStartTime) + ""),
          LoggerEnum.INFO.name());
      migrationUser = prepareRecord(request, processId, migrationUserList);
      ProjectLogger.log(
          "UserBulkMigrationActor:processRecord:processing record for number of users "
              .concat(migrationUserList.size() + ""),
          LoggerEnum.INFO.name());
    }
    insertRecord(migrationUser);
    if (request
        .getOperation()
        .equals(BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue())) {
      request.setOperation(BulkUploadActorOperation.PROCESS_USER_BULK_SELF_DECLARED.getValue());
      request.getRequest().put(JsonKey.PROCESS_ID, migrationUser.getId());
      tellToAnother(request);
    }
    TelemetryUtil.generateCorrelatedObject(processId, JsonKey.PROCESS_ID, null, correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        migrationUser.getTaskCount() + "", JsonKey.TASK_COUNT, null, correlatedObject);
    targetObject =
        TelemetryUtil.generateTargetObject(
            processId,
            StringUtils.capitalize(JsonKey.MIGRATION_USER_OBJECT),
            "ShadowUserUpload",
            null);
    TelemetryUtil.telemetryProcessingCall(
        mapper.convertValue(migrationUser, Map.class),
        targetObject,
        correlatedObject,
        request.getContext());
  }

  private void insertRecord(BulkMigrationUser bulkMigrationUser) {
    long insertStartTime = System.currentTimeMillis();
    ObjectMapper mapper = new ObjectMapper();
    ProjectLogger.log(
        "UserBulkMigrationActor:insertRecord:record started inserting with "
            .concat(bulkMigrationUser.getId() + ""),
        LoggerEnum.INFO.name());
    Map<String, Object> record = mapper.convertValue(bulkMigrationUser, Map.class);
    long createdOn = System.currentTimeMillis();
    record.put(JsonKey.CREATED_ON, new Timestamp(createdOn));
    record.put(JsonKey.LAST_UPDATED_ON, new Timestamp(createdOn));
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    Response response =
        cassandraOperation.insertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), record);
    response.put(JsonKey.PROCESS_ID, bulkMigrationUser.getId());
    ProjectLogger.log(
        "UserBulkMigrationActor:insertRecord:time taken by cassandra to insert record of size "
                .concat(record.size() + "")
            + "is(ms):".concat((System.currentTimeMillis() - insertStartTime) + ""));
    sender().tell(response, self());
  }

  private BulkMigrationUser prepareRecord(
      Request request, String processID, List<MigrationUser> migrationUserList) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String decryptedData = mapper.writeValueAsString(migrationUserList);
      BulkMigrationUser migrationUser =
          new BulkMigrationUser.BulkMigrationUserBuilder(processID, decryptedData)
              .setObjectType(JsonKey.MIGRATION_USER_OBJECT)
              .setUploadedDate(ProjectUtil.getFormattedDate())
              .setStatus(ProjectUtil.BulkProcessStatus.NEW.getValue())
              .setRetryCount(RETRY_COUNT)
              .setTaskCount(migrationUserList.size())
              .setCreatedBy(getCreatedBy(request))
              .setUploadedBy(getCreatedBy(request))
              .setOrganisationId((String) request.getRequest().get(JsonKey.ROOT_ORG_ID))
              .setTelemetryContext(getContextMap(processID, request))
              .build();
      return migrationUser;
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "UserBulkMigrationActor:prepareRecord:error occurred while getting preparing record with processId"
              .concat(processID + ""),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private BulkMigrationUser prepareSelfDeclaredRecord(
      Request request, String processID, List<SelfDeclaredUser> migrationUserList) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String decryptedData = mapper.writeValueAsString(migrationUserList);
      BulkMigrationUser migrationUser =
          new BulkMigrationUser.BulkMigrationUserBuilder(processID, decryptedData)
              .setObjectType(JsonKey.SELF_DECLARED_USER_OBJECT)
              .setUploadedDate(ProjectUtil.getFormattedDate())
              .setStatus(ProjectUtil.BulkProcessStatus.NEW.getValue())
              .setRetryCount(RETRY_COUNT)
              .setTaskCount(migrationUserList.size())
              .setCreatedBy(getCreatedBy(request))
              .setUploadedBy(getCreatedBy(request))
              .setOrganisationId((String) request.getRequest().get(JsonKey.ROOT_ORG_ID))
              .setTelemetryContext(getContextMap(processID, request))
              .build();
      return migrationUser;
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "UserBulkMigrationActor:prepareRecord:error occurred while getting preparing record with processId"
              .concat(processID + ""),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private Map<String, String> getContextMap(String processId, Request request) {
    Map<String, String> contextMap = (Map) request.getContext();
    ProjectLogger.log(
        "UserBulkMigrationActor:getContextMap:started preparing record for processId:"
            + processId
            + "with request context:"
            + contextMap,
        LoggerEnum.INFO.name());
    contextMap.put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.SYSTEM));
    contextMap.put(JsonKey.ACTOR_ID, ProjectUtil.getUniqueIdFromTimestamp(0));
    Iterables.removeIf(contextMap.values(), value -> StringUtils.isBlank(value));
    return contextMap;
  }

  private String getCreatedBy(Request request) {
    Map<String, String> data = (Map<String, String>) request.getRequest().get(JsonKey.DATA);
    return MapUtils.isNotEmpty(data) ? data.get(JsonKey.CREATED_BY) : null;
  }

  private List<MigrationUser> getMigrationUsers(
      String channel, String processId, byte[] fileData, Map<String, Object> fieldsMap) {
    Map<String, List<String>> columnsMap =
        (Map<String, List<String>>) fieldsMap.get(JsonKey.FILE_TYPE_CSV);
    List<String[]> csvData = readCsv(fileData);
    List<String> csvHeaders = getCsvHeadersAsList(csvData);
    List<String> mandatoryHeaders = columnsMap.get(JsonKey.MANDATORY_FIELDS);
    List<String> supportedHeaders = columnsMap.get(JsonKey.SUPPORTED_COlUMNS);
    mandatoryHeaders.replaceAll(String::toLowerCase);
    supportedHeaders.replaceAll(String::toLowerCase);
    checkCsvHeader(csvHeaders, mandatoryHeaders, supportedHeaders);
    List<String> mappedCsvHeaders = mapCsvColumn(csvHeaders);
    List<MigrationUser> migrationUserList =
        parseCsvRows(channel, getCsvRowsAsList(csvData), mappedCsvHeaders);
    ShadowUserUpload migration =
        new ShadowUserUpload.ShadowUserUploadBuilder()
            .setHeaders(csvHeaders)
            .setMappedHeaders(mappedCsvHeaders)
            .setProcessId(processId)
            .setFileData(fileData)
            .setFileSize(fileData.length + "")
            .setMandatoryFields(columnsMap.get(JsonKey.MANDATORY_FIELDS))
            .setSupportedFields(columnsMap.get(JsonKey.SUPPORTED_COlUMNS))
            .setValues(migrationUserList)
            .validate();
    ProjectLogger.log(
        "UserBulkMigrationActor:validateRequestAndReturnMigrationUsers: the migration object formed "
            .concat(migration.toString()),
        LoggerEnum.INFO.name());
    return migrationUserList;
  }

  private List<SelfDeclaredUser> getUsers(
      String processId,
      byte[] fileData,
      Map<String, List<String>> columnsMap,
      Map<String, String> aliasesFields) {
    List<String[]> csvData = readCsv(fileData);
    List<String> csvHeaders = getCsvHeadersAsList(csvData);
    List<String> mandatoryHeaders = columnsMap.get(JsonKey.MANDATORY_FIELDS);
    List<String> supportedHeaders = columnsMap.get("optionalFields");
    mandatoryHeaders.replaceAll(String::toLowerCase);
    supportedHeaders.replaceAll(String::toLowerCase);
    checkCsvHeader(csvHeaders, mandatoryHeaders, supportedHeaders);
    List<String> mappedCsvHeaders = mapSelfDeclaredCsvColumn(csvHeaders, aliasesFields);

    List<SelfDeclaredUser> selfDeclaredUserList =
        parseSelfDeclaredCsvRows(getCsvRowsAsList(csvData), mappedCsvHeaders);
    ShadowUserUpload migration =
        new ShadowUserUpload.ShadowUserUploadBuilder()
            .setHeaders(csvHeaders)
            .setMappedHeaders(mappedCsvHeaders)
            .setProcessId(processId)
            .setFileData(fileData)
            .setFileSize(fileData.length + "")
            .setMandatoryFields(columnsMap.get(JsonKey.MANDATORY_FIELDS))
            .setSupportedFields(supportedHeaders)
            .setUserValues(selfDeclaredUserList)
            .validateDeclaredUsers();
    ProjectLogger.log(
        "UserBulkMigrationActor:validateRequestAndReturnDeclaredUsers: the migration object formed "
            .concat(migration.toString()),
        LoggerEnum.INFO.name());
    return selfDeclaredUserList;
  }

  private List<String[]> readCsv(byte[] fileData) {
    List<String[]> values = new ArrayList<>();
    try {
      csvReader = getCsvReader(fileData, ',', '"', 0);
      ProjectLogger.log(
          "UserBulkMigrationActor:readCsv:csvReader initialized ".concat(csvReader.toString()),
          LoggerEnum.INFO.name());
      values = csvReader.readAll();
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserBulkMigrationActor:readCsv:error occurred while getting csvReader",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } finally {
      IOUtils.closeQuietly(csvReader);
    }
    return values;
  }

  private List<String> getCsvHeadersAsList(List<String[]> csvData) {
    List<String> headers = new ArrayList<>();
    int CSV_COLUMN_NAMES = 0;
    if (null == csvData || csvData.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.blankCsvData.getErrorCode(),
          ResponseCode.blankCsvData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    headers.addAll(Arrays.asList(csvData.get(CSV_COLUMN_NAMES)));
    headers.replaceAll(String::toLowerCase);
    return headers;
  }

  private List<String[]> getCsvRowsAsList(List<String[]> csvData) {
    return csvData.subList(1, csvData.size());
  }

  private List<String> mapCsvColumn(List<String> csvColumns) {
    List<String> mappedColumns = new ArrayList<>();

    csvColumns.forEach(
        column -> {
          if (column.equalsIgnoreCase(JsonKey.EMAIL)) {
            mappedColumns.add(column);
          }
          if (column.equalsIgnoreCase(JsonKey.PHONE)) {
            mappedColumns.add(column);
          }
          if (column.equalsIgnoreCase(JsonKey.EXTERNAL_USER_ID)) {
            mappedColumns.add(JsonKey.USER_EXTERNAL_ID);
          }
          if (column.equalsIgnoreCase(JsonKey.EXTERNAL_ORG_ID)) {
            mappedColumns.add(JsonKey.ORG_EXTERNAL_ID);
          }
          if (column.equalsIgnoreCase(JsonKey.NAME)) {
            mappedColumns.add(JsonKey.FIRST_NAME);
          }
          if (column.equalsIgnoreCase(JsonKey.INPUT_STATUS)) {
            mappedColumns.add(column);
          }
        });
    return mappedColumns;
  }

  private List<String> mapSelfDeclaredCsvColumn(
      List<String> csvColumns, Map<String, String> aliasFields) {
    List<String> mappedColumns = new ArrayList<>();
    csvColumns.forEach(
        column -> {
          mappedColumns.add(aliasFields.get(column));
        });
    return mappedColumns;
  }

  private List<MigrationUser> parseCsvRows(
      String channel, List<String[]> values, List<String> mappedHeaders) {
    List<MigrationUser> migrationUserList = new ArrayList<>();
    values
        .stream()
        .forEach(
            row -> {
              int index = values.indexOf(row);
              MigrationUser migrationUser = new MigrationUser();
              for (int i = 0; i < row.length; i++) {
                if (row.length > mappedHeaders.size()) {
                  throw new ProjectCommonException(
                      ResponseCode.errorUnsupportedField.getErrorCode(),
                      ResponseCode.errorUnsupportedField.getErrorMessage(),
                      ResponseCode.CLIENT_ERROR.getResponseCode(),
                      "Invalid provided ROW:" + (index + 1));
                }
                String columnName = getColumnNameByIndex(mappedHeaders, i);
                setFieldToMigrationUserObject(migrationUser, columnName, trimValue(row[i]));
              }
              // channel to be added here
              migrationUser.setChannel(channel);
              migrationUserList.add(migrationUser);
            });
    return migrationUserList;
  }

  private List<SelfDeclaredUser> parseSelfDeclaredCsvRows(
      List<String[]> values, List<String> mappedHeaders) {
    List<SelfDeclaredUser> declaredUserList = new ArrayList<>();
    values
        .stream()
        .forEach(
            row -> {
              int index = values.indexOf(row);
              SelfDeclaredUser selfDeclaredUser = new SelfDeclaredUser();
              for (int i = 0; i < row.length; i++) {
                if (row.length > mappedHeaders.size()) {
                  throw new ProjectCommonException(
                      ResponseCode.errorUnsupportedField.getErrorCode(),
                      ResponseCode.errorUnsupportedField.getErrorMessage(),
                      ResponseCode.CLIENT_ERROR.getResponseCode(),
                      "Invalid provided ROW:" + (index + 1));
                }
                String columnName = getColumnNameByIndex(mappedHeaders, i);
                setFieldToDeclaredUserObject(selfDeclaredUser, columnName, trimValue(row[i]));
              }
              declaredUserList.add(selfDeclaredUser);
            });
    return declaredUserList;
  }

  private String trimValue(String value) {
    if (StringUtils.isNotBlank(value)) {
      return value.trim();
    }
    return value;
  }

  private void setFieldToMigrationUserObject(
      MigrationUser migrationUser, String columnAttribute, Object value) {

    if (columnAttribute.equalsIgnoreCase(JsonKey.EMAIL)) {
      String email = (String) value;
      migrationUser.setEmail(email);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.PHONE)) {
      String phone = (String) value;
      migrationUser.setPhone(phone);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.ORG_EXTERNAL_ID)) {
      migrationUser.setOrgExternalId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.USER_EXTERNAL_ID)) {
      migrationUser.setUserExternalId((String) value);
    }

    if (columnAttribute.equalsIgnoreCase(JsonKey.FIRST_NAME)) {
      migrationUser.setName(StringUtils.trim((String) value));
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.INPUT_STATUS)) {
      migrationUser.setInputStatus((String) value);
    }
  }

  private void setFieldToDeclaredUserObject(
      SelfDeclaredUser migrationUser, String columnAttribute, Object value) {

    if (columnAttribute.equalsIgnoreCase(JsonKey.EMAIL)) {
      String email = (String) value;
      migrationUser.setEmail(email);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.PHONE)) {
      String phone = (String) value;
      migrationUser.setPhone(phone);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.USER_EXTERNAL_ID)) {
      migrationUser.setUserExternalId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.INPUT_STATUS)) {
      migrationUser.setInputStatus((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.CHANNEL)) {
      migrationUser.setChannel((String) value);
    }
    if (columnAttribute.equalsIgnoreCase("schoolName")) {
      migrationUser.setSchoolName((String) value);
    }
    if (columnAttribute.equalsIgnoreCase("schoolUdiseId")) {
      migrationUser.setSchoolId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.USER_ID)) {
      migrationUser.setUserId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase("subOrgId")) {
      migrationUser.setSubOrgId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.PERSONA)) {
      migrationUser.setPersona((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.ERROR_TYPE)) {
      migrationUser.setErrorType((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.ORG_ID)) {
      migrationUser.setOrgId((String) value);
    }
  }

  private String getColumnNameByIndex(List<String> mappedHeaders, int index) {
    return mappedHeaders.get(index);
  }

  /**
   * in bulk_upload_process db user will have channel of admin users
   *
   * @param result
   * @return channel
   */
  private String getChannel(Map<String, Object> result) {
    String channel = (String) result.get(JsonKey.CHANNEL);
    ProjectLogger.log(
        "UserBulkMigrationActor:getChannel: the channel of admin user ".concat(channel + ""),
        LoggerEnum.INFO.name());
    return channel;
  }
  /**
   * in bulk_upload_process db organisationId will be of user.
   *
   * @param result
   * @return rootOrgId
   */
  private String getRootOrgId(Map<String, Object> result) {
    String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
    ProjectLogger.log(
        "UserBulkMigrationActor:getRootOrgId:the root org id  of admin user "
            .concat(rootOrgId + ""),
        LoggerEnum.INFO.name());
    return rootOrgId;
  }

  /**
   * this method will fetch user record with userId from cassandra
   *
   * @param userId
   * @return result
   */
  private Map<String, Object> getUserById(String userId) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        cassandraOperation.getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userId);
    if (((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Map<String, Object> result = ((Map) ((List) response.getResult().get(JsonKey.RESPONSE)).get(0));
    return result;
  }

  private void checkCsvHeader(
      List<String> csvHeaders, List<String> mandatoryHeaders, List<String> supportedHeaders) {
    checkMandatoryColumns(csvHeaders, mandatoryHeaders);
    checkSupportedColumns(csvHeaders, supportedHeaders);
  }

  private void checkMandatoryColumns(List<String> csvHeaders, List<String> mandatoryHeaders) {
    ProjectLogger.log(
        "UserBulkMigrationRequestValidator:checkMandatoryColumns:mandatory columns got "
            + mandatoryHeaders,
        LoggerEnum.INFO.name());
    mandatoryHeaders.forEach(
        column -> {
          if (!csvHeaders.contains(column)) {
            ProjectLogger.log(
                "UserBulkMigrationRequestValidator:mandatoryColumns: mandatory column is not present"
                    .concat(column + ""),
                LoggerEnum.ERROR.name());
            throw new ProjectCommonException(
                ResponseCode.mandatoryParamsMissing.getErrorCode(),
                ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                column);
          }
        });
  }

  private void checkSupportedColumns(List<String> csvHeaders, List<String> supportedHeaders) {
    ProjectLogger.log(
        "UserBulkMigrationRequestValidator:checkSupportedColumns:mandatory columns got "
            + supportedHeaders,
        LoggerEnum.INFO.name());
    supportedHeaders.forEach(
        suppColumn -> {
          if (!csvHeaders.contains(suppColumn)) {
            ProjectLogger.log(
                "UserBulkMigrationRequestValidator:supportedColumns: supported column is not present"
                    .concat(suppColumn + ""),
                LoggerEnum.ERROR.name());
            throw new ProjectCommonException(
                ResponseCode.errorUnsupportedField.getErrorCode(),
                ResponseCode.errorUnsupportedField.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                "Invalid provided column:"
                    .concat(suppColumn)
                    .concat(":supported headers are:")
                    .concat(ArrayUtils.stringifyContents(supportedHeaders.toArray())));
          }
        });
  }
}
