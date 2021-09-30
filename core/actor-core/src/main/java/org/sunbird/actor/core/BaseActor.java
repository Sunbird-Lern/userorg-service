package org.sunbird.actor.core;

import akka.actor.UntypedAbstractActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;

public abstract class BaseActor extends UntypedAbstractActor {
  public LoggerUtil logger = new LoggerUtil(this.getClass());

  public abstract void onReceive(Request request) throws Throwable;

  @Override
  public void onReceive(Object message) throws Throwable {
    if (message instanceof Request) {
      Request request = (Request) message;
      String operation = request.getOperation();
      try {
        logger.info(
            request.getRequestContext(),
            "Actor Info: Dispatcher : "
                + context().dispatcher().toString()
                + " , Actor : "
                + context().self().toString()
                + " , called for operation: "
                + operation);
        onReceive(request);
      } catch (Exception e) {
        logger.error(
            request.getRequestContext(), "Error while processing the message : " + operation, e);
        onReceiveException(e);
      }
    }
  }

  protected void onReceiveUnsupportedOperation() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }

  private void onReceiveException(Exception exception) {
    sender().tell(exception, self());
  }
}
