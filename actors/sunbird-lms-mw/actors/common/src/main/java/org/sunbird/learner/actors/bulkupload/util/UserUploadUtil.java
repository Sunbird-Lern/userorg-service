package org.sunbird.learner.actors.bulkupload.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.sunbird.bean.SelfDeclaredUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.model.BulkMigrationUser;
import org.sunbird.learner.util.Util;

public class UserUploadUtil {
  public static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  public static Util.DbInfo bulkUploadDbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  public static ObjectMapper mapper = new ObjectMapper();
  public static DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);

  public static Map<String, Object> getFullRecordFromProcessId(String processId) {

    int FIRST_RECORD = 0;
    Response response =
        cassandraOperation.getRecordById(
            bulkUploadDbInfo.getKeySpace(), bulkUploadDbInfo.getTableName(), processId, null);
    List<Map<String, Object>> result = new ArrayList<>();
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((List) response.getResult().get(JsonKey.RESPONSE));
    }
    ProjectLogger.log(
        "UserUploadUtil:getFullRecordFromProcessId:got single row data from bulk_upload_process with processId:"
            + processId,
        LoggerEnum.INFO.name());
    return result.get(FIRST_RECORD);
  }

  public static BulkMigrationUser convertRowToObject(Map<String, Object> row) {
    BulkMigrationUser bulkMigrationUser = null;
    try {
      bulkMigrationUser = mapper.convertValue(row, BulkMigrationUser.class);
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "UserUploadUtil:convertMapToMigrationObject:error occurred while converting map to pojo"
              .concat(e.getMessage() + ""),
          LoggerEnum.ERROR.name());
    }
    return bulkMigrationUser;
  }

  public static List<SelfDeclaredUser> getMigrationUserAsList(BulkMigrationUser bulkMigrationUser) {
    List<SelfDeclaredUser> userList = new ArrayList<>();
    try {
      String decryptedData = decryptionService.decryptData(bulkMigrationUser.getData());
      userList = mapper.readValue(decryptedData, new TypeReference<List<SelfDeclaredUser>>() {});
    } catch (Exception e) {
      e.printStackTrace();
      ProjectLogger.log(
          "UserUploadUtil:getMigrationUserAsList:error occurred while converting map to POJO: " + e,
          LoggerEnum.ERROR.name());
    }
    return userList;
  }

  public static void updateStatusInUserBulkTable(String processId, int statusVal) {
    try {
      ProjectLogger.log(
          "UserUploadUtil:updateStatusInUserBulkTable: got status to change in bulk upload table"
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
          "UserUploadUtil:updateStatusInUserBulkTable: status update failed".concat(e + "")
              + "with processId"
              + processId,
          LoggerEnum.ERROR.name());
    }
  }

  private static void updateBulkUserTable(Map<String, Object> propertiesMap) {
    Response response =
        cassandraOperation.updateRecord(
            bulkUploadDbInfo.getKeySpace(), bulkUploadDbInfo.getTableName(), propertiesMap, null);
    ProjectLogger.log(
        "UserUploadUtil:updateBulkUserTable: status update result".concat(response + ""),
        LoggerEnum.INFO.name());
  }
}
