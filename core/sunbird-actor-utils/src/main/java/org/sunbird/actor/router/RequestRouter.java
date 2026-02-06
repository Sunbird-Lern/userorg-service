package org.sunbird.actor.router;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.sunbird.actor.core.BaseRouter;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.ResponseCode;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * RequestRouter is responsible for routing synchronous API requests to the corresponding actors. It
 * uses the {@link Patterns#ask(ActorRef, Object, Timeout)} pattern to communicate with actors and
 * handles timeouts and exceptions, propagating them back to the caller.
 */
public class RequestRouter extends BaseRouter {

  private static String mode;
  private static String name;
  public static final Map<String, ActorRef> routingMap = new HashMap<>();

  /**
   * Default constructor that initializes the router mode.
   */
  public RequestRouter() {
    getMode();
  }

  @Override
  public void preStart() throws Exception {
    super.preStart();
    name = self().path().name();
    initActors(getContext(), RequestRouter.class.getSimpleName());
  }

  @Override
  protected void cacheActor(String key, ActorRef actor) {
    routingMap.put(key, actor);
  }

  @Override
  public void route(Request request) throws Throwable {
    String operation = request.getOperation();
    ActorRef ref = routingMap.get(getKey(self().path().name(), operation));
    if (null != ref) {
      route(ref, request, getContext().dispatcher());
    } else {
      logger.info(request.getRequestContext(), "RequestRouter: Unsupported operation: " + operation);
      onReceiveUnsupportedOperation(operation);
    }
  }

  /**
   * Retrieves the ActorRef for a specific operation.
   *
   * @param operation The operation name.
   * @return The ActorRef handling the operation.
   */
  public static ActorRef getActor(String operation) {
    return routingMap.get(getKey(name, operation));
  }

  @Override
  public String getRouterMode() {
    return getMode();
  }

  /**
   * Retrieves the current router mode from properties.
   *
   * @return The router mode string.
   */
  public static String getMode() {
    if (StringUtils.isBlank(mode)) {
      mode = getPropertyValue(JsonKey.API_ACTOR_PROVIDER);
    }
    return mode;
  }

  /**
   * Routes the request to the corresponding actor and handles the asynchronous response.
   *
   * @param router The target actor reference.
   * @param request The request object.
   * @param ec The execution context for handling the future.
   * @return True if routing was successful.
   */
  private boolean route(ActorRef router, Request request, ExecutionContext ec) {
    long startTime = System.currentTimeMillis();
    logger.info(
        request.getRequestContext(),
        "Actor Service Call start for operation: "
            + request.getOperation()
            + " at time: "
            + startTime);

    Timeout timeout = new Timeout(Duration.create(request.getTimeout(), TimeUnit.SECONDS));
    Future<Object> future = Patterns.ask(router, request, timeout);
    ActorRef parent = sender();

    future.onComplete(
        new OnComplete<Object>() {
          @Override
          public void onComplete(Throwable failure, Object result) {
            if (failure != null) {
              logger.error(
                  request.getRequestContext(),
                  "RequestRouter: Error for operation " + request.getOperation() + ": " + failure.getMessage(),
                  failure);
              if (failure instanceof ProjectCommonException) {
                parent.tell(failure, self());
              } else if (failure instanceof org.apache.pekko.pattern.AskTimeoutException) {
                ProjectCommonException exception =
                    new ProjectCommonException(
                        ResponseCode.operationTimeout.getErrorCode(),
                        ResponseCode.operationTimeout.getErrorMessage(),
                        ResponseCode.SERVER_ERROR.getResponseCode());
                parent.tell(exception, self());
              } else {
                ProjectCommonException exception =
                    new ProjectCommonException(
                        ResponseCode.internalError.getErrorCode(),
                        ResponseCode.internalError.getErrorMessage(),
                        ResponseCode.SERVER_ERROR.getResponseCode());
                parent.tell(exception, self());
              }
            } else {
              parent.tell(result, self());
            }
          }
        },
        ec);
    return true;
  }
}
