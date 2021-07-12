package org.sunbird.actor.core;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedAbstractActor;
import akka.util.Timeout;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import scala.concurrent.duration.Duration;

public abstract class BaseActor extends UntypedAbstractActor {
  public LoggerUtil logger = new LoggerUtil(this.getClass());

  public abstract void onReceive(Request request) throws Throwable;

  public static final int AKKA_WAIT_TIME = 30;
  protected static Timeout timeout = new Timeout(AKKA_WAIT_TIME, TimeUnit.SECONDS);

  @Override
  public void onReceive(Object message) throws Throwable {
    if (message instanceof Request) {
      Request request = (Request) message;
      String operation = request.getOperation();
      logger.debug(request.getRequestContext(), "onReceive called for operation: " + operation);
      try {
        onReceive(request);
      } catch (Exception e) {
        logger.error(
            request.getRequestContext(), "Error while processing the message : " + operation, e);
        onReceiveException(operation, e);
      }
    }
  }

  public void tellToAnother(Request request) {
    SunbirdMWService.tellToBGRouter(request, self());
  }

  public void unSupportedMessage() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }

  public void onReceiveUnsupportedOperation(String callerName) {
    unSupportedMessage();
  }

  public void onReceiveUnsupportedMessage(String callerName) {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidOperationName.getErrorCode(),
            ResponseCode.invalidOperationName.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }

  protected void onReceiveException(String callerName, Exception exception) {
    sender().tell(exception, self());
  }

  protected ActorRef getActorRef(String operation) {
    int waitTime = 10;
    ActorSelection select = null;
    ActorRef actor = RequestRouter.getActor(operation);
    if (null != actor) {
      return actor;
    } else {
      select =
          (BaseMWService.getRemoteRouter(RequestRouter.class.getSimpleName()) == null
              ? (BaseMWService.getRemoteRouter(BackgroundRequestRouter.class.getSimpleName()))
              : BaseMWService.getRemoteRouter(RequestRouter.class.getSimpleName()));
      CompletionStage<ActorRef> futureActor =
          select.resolveOneCS(Duration.create(waitTime, "seconds"));
      try {
        actor = futureActor.toCompletableFuture().get();
      } catch (Exception e) {

        logger.error("unable to get actorRef from actorSelection " + e.getMessage(), e);
      }
      return actor;
    }
  }
}
