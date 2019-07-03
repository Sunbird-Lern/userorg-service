package org.sunbird.learner.actors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;

/** @author Amit Kumar */
@ActorConfig(
  tasks = {},
  asyncTasks = {"scheduleBulkUpload"}
)
public class SchedularActor extends BaseActor {

  @Override
  public void onReceive(Request actorMessage) throws Throwable {
    if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.SCHEDULE_BULK_UPLOAD.getValue())) {
      schedule(actorMessage);
    } else {
      ProjectLogger.log("UNSUPPORTED OPERATION");
    }
  }

  @SuppressWarnings("unchecked")
  private void schedule(Request request) {
    List<Map<String, Object>> result = (List<Map<String, Object>>) request.get(JsonKey.DATA);
    Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    for (Map<String, Object> map : result) {
      int retryCount = 0;
      if (null != map.get(JsonKey.RETRY_COUNT)) {
        retryCount = (int) map.get(JsonKey.RETRY_COUNT);
      }
      if (retryCount > 2) {
        String data = (String) map.get(JsonKey.DATA);
        try {
          Map<String, Object> bulkMap = new HashMap<>();
          bulkMap.put(JsonKey.DATA, UserUtility.encryptData(data));
          bulkMap.put(JsonKey.PROCESS_ID, map.get(JsonKey.ID));
          bulkMap.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.FAILED.getValue());
          cassandraOperation.updateRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), bulkMap);
        } catch (Exception e) {
          ProjectLogger.log(
              "Exception occurred while encrypting data while running scheduler for bulk upload process : ",
              e);
        }
      } else {
        Map<String, Object> bulkMap = new HashMap<>();
        bulkMap.put(JsonKey.RETRY_COUNT, retryCount + 1);
        bulkMap.put(JsonKey.ID, map.get(JsonKey.ID));
        bulkMap.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
        cassandraOperation.updateRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), bulkMap);
        Request req = new Request();
        req.put(JsonKey.PROCESS_ID, map.get(JsonKey.ID));
        ProjectLogger.log(
            "SchedularActor: scheduleBulkUpload called with processId "
                + map.get(JsonKey.ID)
                + " and type "
                + map.get(JsonKey.OBJECT_TYPE),
            LoggerEnum.INFO);
        if (JsonKey.LOCATION.equalsIgnoreCase((String) map.get(JsonKey.OBJECT_TYPE))) {
          req.setOperation(BulkUploadActorOperation.LOCATION_BULK_UPLOAD_BACKGROUND_JOB.getValue());
        } else if (JsonKey.ORGANISATION.equalsIgnoreCase((String) map.get(JsonKey.OBJECT_TYPE))) {
          req.setOperation(BulkUploadActorOperation.ORG_BULK_UPLOAD_BACKGROUND_JOB.getValue());
        } else if (JsonKey.USER.equals((String) map.get(JsonKey.OBJECT_TYPE))) {
          req.setOperation(BulkUploadActorOperation.USER_BULK_UPLOAD_BACKGROUND_JOB.getValue());
        } else {
          req.setOperation(ActorOperations.PROCESS_BULK_UPLOAD.getValue());
        }
        tellToAnother(req);
      }
    }
  }
}
