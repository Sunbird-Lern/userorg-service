package org.sunbird.actor.core;

import org.apache.pekko.actor.UntypedAbstractActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;

public abstract class BaseActor extends UntypedAbstractActor {
  public final LoggerUtil logger = new LoggerUtil(this.getClass());

  public abstract void onReceive(Request request) throws Throwable;

  @Override
  public void onReceive(Object message) throws Throwable {
    if (message instanceof Request) {
      Request request = (Request) message;
      String operation = request.getOperation();
      try {
        onReceive(request);
      } catch (Exception e) {
        logger.error(
            request.getRequestContext(),
            "Error while processing the message for operation: " + operation,
            e);
        if (e instanceof ProjectCommonException) {
          ProjectCommonException exception =
              new ProjectCommonException(
                  (ProjectCommonException) e,
                  ActorOperations.getOperationCodeByActorOperation(request.getOperation()));
          sender().tell(exception, self());
        }
        sender().tell(e, self());
      }
    }
  }

  protected void onReceiveUnsupportedOperation() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidOperationName,
            ResponseCode.invalidOperationName.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }
}
