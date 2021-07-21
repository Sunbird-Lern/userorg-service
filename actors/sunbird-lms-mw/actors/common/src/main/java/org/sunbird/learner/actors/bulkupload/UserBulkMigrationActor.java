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
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.bulkupload.model.BulkMigrationUser;
import org.sunbird.learner.util.Util;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.operations.ActorOperations;
import org.sunbird.operations.BulkUploadActorOperation;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;

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
    logger.info(request.getRequestContext(), "OnReceive Upload csv processing " + operation);
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
            getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
            "shadowdbmandatorycolumn",
            request.getRequestContext());
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
    Map<String, Object> result = getUserById(userId, request.getRequestContext());
    String channel = getChannel(result);
    String rootOrgId = getRootOrgId(result);

    request.getRequest().put(JsonKey.ROOT_ORG_ID, rootOrgId);
    BulkMigrationUser migrationUser = null;
    if (request
        .getOperation()
        .equals(BulkUploadActorOperation.USER_BULK_SELF_DECLARED.getValue())) {
      PropertiesCache propertiesCache = PropertiesCache.getInstance();
      String mandatoryFields = propertiesCache.getProperty(JsonKey.SELF_DECLARED_MANDATORY_FIELDS);
      String optionalFields = propertiesCache.getProperty(JsonKey.SELF_DECLARED_OPTIONAL_FIELDS);
      Map fieldsMap = new HashMap();
      fieldsMap.put("mandatoryFields", Arrays.asList(mandatoryFields.split(",")));
      fieldsMap.put("optionalFields", Arrays.asList(optionalFields.split(",")));
      List<SelfDeclaredUser> selfDeclaredUserList =
          getUsers(processId, (byte[]) data.get(JsonKey.FILE), fieldsMap);
      logger.info(
          request.getRequestContext(),
          "UserBulkMigrationActor:processRecord: time taken to validate records of size "
                  .concat(selfDeclaredUserList.size() + "")
              + "is(ms): ".concat((System.currentTimeMillis() - validationStartTime) + ""));
      migrationUser = prepareSelfDeclaredRecord(request, processId, selfDeclaredUserList);
      logger.info(
          request.getRequestContext(),
          "UserBulkMigrationActor:processRecord:processing record for number of users "
              .concat(selfDeclaredUserList.size() + ""));
    } else {
      values = mapper.readValue(systemSetting.getValue(), Map.class);
      List<MigrationUser> migrationUserList =
          getMigrationUsers(channel, processId, (byte[]) data.get(JsonKey.FILE), values);
      logger.info(
          request.getRequestContext(),
          "UserBulkMigrationActor:processRecord: time taken to validate records of size "
                  .concat(migrationUserList.size() + "")
              + "is(ms): ".concat((System.currentTimeMillis() - validationStartTime) + ""));
      migrationUser = prepareRecord(request, processId, migrationUserList);
      logger.info(
          request.getRequestContext(),
          "UserBulkMigrationActor:processRecord:processing record for number of users "
              .concat(migrationUserList.size() + ""));
    }
    insertRecord(migrationUser, request.getRequestContext());
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

  private void insertRecord(BulkMigrationUser bulkMigrationUser, RequestContext context) {
    long insertStartTime = System.currentTimeMillis();
    ObjectMapper mapper = new ObjectMapper();
    logger.info(
        context,
        "UserBulkMigrationActor:insertRecord:record started inserting with "
            .concat(bulkMigrationUser.getId() + ""));
    Map<String, Object> record = mapper.convertValue(bulkMigrationUser, Map.class);
    long createdOn = System.currentTimeMillis();
    record.put(JsonKey.CREATED_ON, new Timestamp(createdOn));
    record.put(JsonKey.LAST_UPDATED_ON, new Timestamp(createdOn));
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    Response response =
        cassandraOperation.insertRecord(
            dbInfo.getKeySpace(), dbInfo.getTableName(), record, context);
    response.put(JsonKey.PROCESS_ID, bulkMigrationUser.getId());
    logger.info(
        context,
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
      logger.error(
          request.getRequestContext(),
          "UserBulkMigrationActor:prepareRecord:error occurred while getting preparing record with processId"
              .concat(processID + ""),
          e);
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
      logger.error(
          "UserBulkMigrationActor:prepareRecord:error occurred while getting preparing record with processId"
              .concat(processID + ""),
          e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private Map<String, String> getContextMap(String processId, Request request) {
    Map<String, String> contextMap = (Map) request.getContext();
    logger.info(
        request.getRequestContext(),
        "UserBulkMigrationActor:getContextMap:started preparing record for processId:"
            + processId
            + "with request context:"
            + contextMap);
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
    logger.info(
        "UserBulkMigrationActor:validateRequestAndReturnMigrationUsers: the migration object formed "
            .concat(migration.toString()));
    return migrationUserList;
  }

  private List<SelfDeclaredUser> getUsers(
      String processId, byte[] fileData, Map<String, List<String>> columnsMap) {
    List<String[]> csvData = readCsv(fileData);
    List<String> csvHeaders = getCsvHeadersAsList(csvData);
    List<String> mandatoryHeaders = columnsMap.get(JsonKey.MANDATORY_FIELDS);
    List<String> supportedHeaders = columnsMap.get("optionalFields");
    mandatoryHeaders.replaceAll(String::toLowerCase);
    supportedHeaders.replaceAll(String::toLowerCase);
    checkCsvHeader(csvHeaders, mandatoryHeaders, supportedHeaders);
    List<String> mappedCsvHeaders = mapSelfDeclaredCsvColumn(csvHeaders);
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
    logger.info(
        "UserBulkMigrationActor:validateRequestAndReturnDeclaredUsers: the migration object formed "
            .concat(migration.toString()));
    return selfDeclaredUserList;
  }

  private List<String[]> readCsv(byte[] fileData) {
    List<String[]> values = new ArrayList<>();
    try {
      csvReader = getCsvReader(fileData, ',', '"', 0);
      logger.info(
          "UserBulkMigrationActor:readCsv:csvReader initialized ".concat(csvReader.toString()));
      values = csvReader.readAll();
    } catch (Exception ex) {
      logger.error("UserBulkMigrationActor:readCsv:error occurred while getting csvReader", ex);
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

  private List<String> mapSelfDeclaredCsvColumn(List<String> csvColumns) {
    List<String> mappedColumns = new ArrayList<>();
    csvColumns.forEach(
        column -> {
          switch (column) {
            case "email id":
              mappedColumns.add(JsonKey.EMAIL);
              break;
            case "phone number":
              mappedColumns.add(JsonKey.PHONE);
              break;
            case "state provided ext. id":
              mappedColumns.add(JsonKey.USER_EXTERNAL_ID);
              break;
            case "status":
              mappedColumns.add(JsonKey.INPUT_STATUS);
              break;
            case "diksha uuid":
              mappedColumns.add(JsonKey.USER_ID);
              break;
            case "channel":
              mappedColumns.add(column);
              break;
            case "school name":
              mappedColumns.add("schoolName");
              break;
            case "school udise id":
              mappedColumns.add("schoolUdiseId");
              break;
            case "persona":
              mappedColumns.add(column);
              break;
            case "error type":
              mappedColumns.add(JsonKey.ERROR_TYPE);
              break;
            case "state":
              mappedColumns.add(JsonKey.STATE);
              break;
            case "district":
              mappedColumns.add("district");
              break;
            case "name":
              mappedColumns.add(JsonKey.NAME);
              break;
            default:
          }
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
    List<String> skipColumns =
        new ArrayList<>(Arrays.asList(JsonKey.NAME, JsonKey.STATE, "district"));
    values
        .stream()
        .forEach(
            row -> {
              int index = values.indexOf(row);
              if (row.length > mappedHeaders.size()) {
                throw new ProjectCommonException(
                    ResponseCode.errorUnsupportedField.getErrorCode(),
                    ResponseCode.errorUnsupportedField.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode(),
                    "Invalid provided ROW:" + (index + 1));
              }
              SelfDeclaredUser selfDeclaredUser = new SelfDeclaredUser();
              for (int i = 0; i < row.length; i++) {
                String columnName = getColumnNameByIndex(mappedHeaders, i);
                if (!skipColumns.contains(columnName)) {
                  setFieldToDeclaredUserObject(selfDeclaredUser, columnName, trimValue(row[i]));
                }
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
    // Assigning sub-org externalid
    if (columnAttribute.equalsIgnoreCase("schoolUdiseId")) {
      migrationUser.setSubOrgExternalId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.USER_ID)) {
      migrationUser.setUserId((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.PERSONA)) {
      migrationUser.setPersona((String) value);
    }
    if (columnAttribute.equalsIgnoreCase(JsonKey.ERROR_TYPE)) {
      migrationUser.setErrorType((String) value);
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
    logger.info(
        "UserBulkMigrationActor:getChannel: the channel of admin user ".concat(channel + ""));
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
    logger.info(
        "UserBulkMigrationActor:getRootOrgId:the root org id  of admin user "
            .concat(rootOrgId + ""));
    return rootOrgId;
  }

  /**
   * this method will fetch user record with userId from cassandra
   *
   * @param userId
   * @return result
   */
  private Map<String, Object> getUserById(String userId, RequestContext context) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        cassandraOperation.getRecordById(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userId, context);
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
    logger.info(
        "UserBulkMigrationRequestValidator:checkMandatoryColumns:mandatory columns got "
            + mandatoryHeaders);
    mandatoryHeaders.forEach(
        column -> {
          if (!csvHeaders.contains(column)) {
            logger.info(
                "UserBulkMigrationRequestValidator:mandatoryColumns: mandatory column is not present"
                    .concat(column + ""));
            throw new ProjectCommonException(
                ResponseCode.mandatoryParamsMissing.getErrorCode(),
                ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                column);
          }
        });
  }

  private void checkSupportedColumns(List<String> csvHeaders, List<String> supportedHeaders) {
    logger.info(
        "UserBulkMigrationRequestValidator:checkSupportedColumns:mandatory columns got "
            + supportedHeaders);
    supportedHeaders.forEach(
        suppColumn -> {
          if (!csvHeaders.contains(suppColumn)) {
            logger.info(
                "UserBulkMigrationRequestValidator:supportedColumns: supported column is not present"
                    .concat(suppColumn + ""));
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
