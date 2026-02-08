package org.sunbird.actor.core;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.util.Timeout;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.response.ResponseCode;
import scala.concurrent.duration.Duration;

/**
 * BaseActor class provides the basic functionality for all actors in the Sunbird platform. It
 * handles the receiving of messages, particularly {@link Request} objects, and provides utility
 * methods for inter-actor communication and error handling.
 */
public abstract class BaseActor extends UntypedAbstractActor {

  /** Default Pekko wait time in seconds. */
  public static final int PEKKO_WAIT_TIME = 30;

  /** Timeout for Pekko operations. */
  protected static Timeout timeout = new Timeout(PEKKO_WAIT_TIME, TimeUnit.SECONDS);

  /** Logger utility instance for this class. */
  public LoggerUtil logger = new LoggerUtil(this.getClass());

  /**
   * Abstract method to be implemented by child classes to handle specific request operations.
   *
   * @param request The request object to process.
   * @throws Throwable If any error occurs during request processing.
   */
  public abstract void onReceive(Request request) throws Throwable;

  /**
   * Main entry point for receiving Pekko messages. It specifically handles {@link Request} objects
   * and routes them to the abstract {@link #onReceive(Request)} method.
   *
   * @param message The received message object.
   * @throws Throwable If any error occurs during message processing.
   */
  @Override
  public void onReceive(Object message) throws Throwable {
    if (message instanceof Request) {
      Request request = (Request) message;
      String operation = request.getOperation();
      logger.debug(
          request.getRequestContext(), "BaseActor: onReceive called for operation: " + operation);
      try {
        onReceive(request);
      } catch (Exception e) {
        logger.error(
            request.getRequestContext(),
            "BaseActor: FAILED onReceive called for operation: " + operation,
            e);
        onReceiveException(request, e);
      }
    } else {
      logger.info( "BaseActor: onReceive called for unsupported message type");
      unSupportedMessage();
    }
  }

  /**
   * Tells a request to another router (BackgroundRequestRouter).
   *
   * @param request The request to be processed in the background.
   */
  public void tellToAnother(Request request) {
    SunbirdMWService.tellToBGRouter(request, self());
  }

  /**
   * Sends an unsupported message exception to the sender.
   *
   * @throws Exception If an error occurs while sending the message.
   */
  public void unSupportedMessage() throws Exception {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }

  /**
   * Handles unsupported operations by logging and sending an exception.
   *
   * @param callerName The name of the caller for logging context.
   * @throws Exception If an error occurs while sending the message.
   */
  public void onReceiveUnsupportedOperation(String callerName) throws Exception {
    logger.info( callerName + ": unsupported operation");
    unSupportedMessage();
  }

  protected void onReceiveUnsupportedOperation() throws Exception {
    onReceiveUnsupportedOperation(this.getClass().getSimpleName());
  }

  /**
   * Handles unsupported messages by logging and sending an exception.
   *
   * @param callerName The name of the caller for logging context.
   */
  public void onReceiveUnsupportedMessage(String callerName) {
    logger.info( callerName + ": unsupported message");
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidOperationName.getErrorCode(),
            ResponseCode.invalidOperationName.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }

  protected void onReceiveUnsupportedMessage() {
    onReceiveUnsupportedMessage(this.getClass().getSimpleName());
  }

  /**
   * Internal exception handler that logs the error and propagates it to the sender.
   *
   * @param request The request context.
   * @param exception The exception to handle.
   * @throws Exception If an error occurs.
   */
  private void onReceiveException(Request request, Exception exception) throws Exception {
    logger.error(
        request.getRequestContext(),
        "Exception in message processing for: "
            + request.getOperation()
            + " :: message: "
            + exception.getMessage(),
        exception);
    sender().tell(exception, self());
  }

  /**
   * Default exception handler for processing errors.
   *
   * @param callerName Identification of the component.
   * @param exception The exception caught.
   * @throws Exception If an error occurs during processing.
   */
  protected void onReceiveException(String callerName, Exception exception) throws Exception {
    logger.error(
        "Exception in message processing for: "
            + callerName
            + " :: message: "
            + exception.getMessage(),
        exception);
    sender().tell(exception, self());
  }

  /**
   * Retrieves an ActorRef for the given operation.
   *
   * @param operation The operation for which to get the actor.
   * @return The ActorRef, or null if not found.
   */
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
      if (select != null) {
        CompletionStage<ActorRef> futureActor =
            select.resolveOneCS(Duration.create(waitTime, "seconds"));
        try {
          actor = futureActor.toCompletableFuture().get();
        } catch (Exception e) {
          logger.error(
              "InterServiceCommunicationImpl : getResponse - unable to get actorref from actorselection "
                  + e.getMessage(),
              e);
        }
      }
      return actor;
    }
  }
}
