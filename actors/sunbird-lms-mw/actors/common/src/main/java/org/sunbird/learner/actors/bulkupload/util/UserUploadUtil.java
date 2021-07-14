package org.sunbird.learner.actors.bulkupload.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.sunbird.bean.SelfDeclaredUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.bulkupload.model.BulkMigrationUser;
import org.sunbird.learner.util.Util;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public class UserUploadUtil {
  private static LoggerUtil logger = new LoggerUtil(UserUploadUtil.class);

  public static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  public static Util.DbInfo bulkUploadDbInfo = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  public static ObjectMapper mapper = new ObjectMapper();
  public static DecryptionService decryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);

  public static Map<String, Object> getFullRecordFromProcessId(
      String processId, RequestContext context) {

    int FIRST_RECORD = 0;
    Response response =
        cassandraOperation.getRecordById(
            bulkUploadDbInfo.getKeySpace(), bulkUploadDbInfo.getTableName(), processId, context);
    List<Map<String, Object>> result = new ArrayList<>();
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      result = ((List) response.getResult().get(JsonKey.RESPONSE));
    }
    logger.info(
        context,
        "UserUploadUtil:getFullRecordFromProcessId:got single row data from bulk_upload_process with processId:"
            + processId);
    return result.get(FIRST_RECORD);
  }

  public static BulkMigrationUser convertRowToObject(
      Map<String, Object> row, RequestContext context) {
    BulkMigrationUser bulkMigrationUser = null;
    try {
      bulkMigrationUser = mapper.convertValue(row, BulkMigrationUser.class);
    } catch (Exception e) {
      logger.error(
          context,
          "UserUploadUtil:convertMapToMigrationObject:error occurred while converting map to pojo"
              .concat(e.getMessage() + ""),
          e);
    }
    return bulkMigrationUser;
  }

  public static List<SelfDeclaredUser> getMigrationUserAsList(
      BulkMigrationUser bulkMigrationUser, RequestContext context) {
    List<SelfDeclaredUser> userList = new ArrayList<>();
    try {
      String decryptedData = decryptionService.decryptData(bulkMigrationUser.getData(), context);
      userList = mapper.readValue(decryptedData, new TypeReference<List<SelfDeclaredUser>>() {});
    } catch (Exception e) {
      logger.error(
          context,
          "UserUploadUtil:getMigrationUserAsList:error occurred while converting map to POJO: " + e,
          e);
    }
    return userList;
  }

  public static void updateStatusInUserBulkTable(
      String processId, int statusVal, RequestContext context) {
    try {
      logger.info(
          context,
          "UserUploadUtil:updateStatusInUserBulkTable: got status to change in bulk upload table"
                  .concat(statusVal + "")
              + "with processId"
              + processId);
      Map<String, Object> propertiesMap = new WeakHashMap<>();
      propertiesMap.put(JsonKey.ID, processId);
      propertiesMap.put(JsonKey.STATUS, statusVal);
      updateBulkUserTable(propertiesMap, context);
    } catch (Exception e) {
      logger.error(
          context,
          "UserUploadUtil:updateStatusInUserBulkTable: status update failed".concat(e + "")
              + "with processId"
              + processId,
          e);
    }
  }

  private static void updateBulkUserTable(
      Map<String, Object> propertiesMap, RequestContext context) {
    Response response =
        cassandraOperation.updateRecord(
            bulkUploadDbInfo.getKeySpace(),
            bulkUploadDbInfo.getTableName(),
            propertiesMap,
            context);
    logger.info(
        context, "UserUploadUtil:updateBulkUserTable: status update result".concat(response + ""));
  }
}
