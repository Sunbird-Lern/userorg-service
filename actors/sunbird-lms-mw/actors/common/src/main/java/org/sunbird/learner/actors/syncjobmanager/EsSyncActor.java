package org.sunbird.learner.actors.syncjobmanager;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;

/**
 * Sync data between Cassandra and Elastic Search.
 */
@ActorConfig(
  tasks = {"sync"},
  asyncTasks = {}
)
public class EsSyncActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();

    if (operation.equalsIgnoreCase(ActorOperations.SYNC.getValue())) {
        triggerBackgroundSync(request);
    } else {
        onReceiveUnsupportedOperation("EsSyncActor");
    }
  }
  
  private void triggerBackgroundSync(Request request) {
      Response response = new Response();
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());

      Request backgroundSyncRequest = new Request();
      backgroundSyncRequest.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
      backgroundSyncRequest.getRequest().put(JsonKey.DATA, request.getRequest().get(JsonKey.DATA));
    
      try {
        tellToAnother(backgroundSyncRequest);
      } catch (Exception e) {
        ProjectLogger.log("EsSyncActor:triggerBackgroundSync: Exception occurred with error message = " + e.getMessage(), e);
      }    
  }  
  
}
