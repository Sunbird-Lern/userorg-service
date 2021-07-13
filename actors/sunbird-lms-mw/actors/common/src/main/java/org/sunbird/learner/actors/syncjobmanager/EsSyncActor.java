package org.sunbird.learner.actors.syncjobmanager;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/** Sync data between Cassandra and Elastic Search. */
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
    Map<String, Object> dataMap = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    String operationType = (String) dataMap.get(JsonKey.OPERATION_TYPE);

    Request backgroundSyncRequest = new Request();
    backgroundSyncRequest.setRequestContext(request.getRequestContext());
    backgroundSyncRequest.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    backgroundSyncRequest.getRequest().put(JsonKey.DATA, dataMap);

    try {
      if (StringUtils.isBlank(operationType)) {
        Response response = new Response();
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        sender().tell(response, self());

        tellToAnother(backgroundSyncRequest);
      } else {
        Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
        ActorRef actorRef =
            BackgroundRequestRouter.getActor(ActorOperations.BACKGROUND_SYNC.getValue());
        Future<Object> future = Patterns.ask(actorRef, backgroundSyncRequest, t);
        Response response = (Response) Await.result(future, t.duration());
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        sender().tell(response, self());
      }
    } catch (Exception e) {
      logger.error(
          request.getRequestContext(),
          "EsSyncActor:triggerBackgroundSync: Exception occurred with error message = "
              + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }
}
