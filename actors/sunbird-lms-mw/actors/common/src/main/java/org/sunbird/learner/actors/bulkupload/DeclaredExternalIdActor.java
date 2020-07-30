package org.sunbird.learner.actors.bulkupload;

import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BulkUploadActorOperation;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

@ActorConfig(
  tasks = {},
  asyncTasks = {"processExternalId"}
)
public class DeclaredExternalIdActor extends BaseActor {

  private Util.DbInfo usrExtIdDbInfo = Util.dbInfoMap.get(JsonKey.USR_EXT_ID_DB);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase(BulkUploadActorOperation.USER_BULK_MIGRATION.getValue())) {
      processDeclaredExternalId(request);
    } else {
      onReceiveUnsupportedOperation("userBulkMigration");
    }
  }

  private void processDeclaredExternalId(Request request) {
    Map requestMap = request.getRequest();
    String status = (String) requestMap.get(JsonKey.STATUS);
    switch (status) {
      case JsonKey.VALIDATED:
        migrateUser(request);
        break;
      case JsonKey.REJECTED:
        rejectDeclaredDetail(requestMap);
        break;
      case JsonKey.ERROR:
        updateErrorDetail(requestMap);
        break;
      default:
    }
  }

  private void updateErrorDetail(Map requestMap) {
    // cassandraOperation.updateRecord()
  }

  private void rejectDeclaredDetail(Map requestMap) {
    cassandraOperation.deleteRecord(
        usrExtIdDbInfo.getKeySpace(), usrExtIdDbInfo.getTableName(), requestMap);
  }

  private void migrateUser(Request request) {
    Response response = null;
    request.setOperation(BulkUploadActorOperation.USER_BULK_MIGRATION.getValue());
    ProjectLogger.log("DeclaredExternalIdActor:migrateUser ");
    /*try {
      response = (Response)
        interServiceCommunication.getResponse(
          getActorRef(BulkUploadActorOperation.USER_BULK_MIGRATION.getValue()), request);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }*/
    sender().tell(response, self());
  }
}
